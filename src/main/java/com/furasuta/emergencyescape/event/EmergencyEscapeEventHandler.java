package com.furasuta.emergencyescape.event;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.DamageConsumptionCapability;
import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.init.ModItems;
import com.furasuta.emergencyescape.init.ModSounds;
import com.furasuta.emergencyescape.network.NetworkHandler;
import com.furasuta.emergencyescape.network.SyncCapabilitiesPacket;
import com.furasuta.emergencyescape.network.SpawnParticlesPacket;
import com.furasuta.emergencyescape.util.BodyPartHitbox;
import com.furasuta.emergencyescape.util.HitPositionTracker;
import com.furasuta.emergencyescape.util.PlayerModelPartCache;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID)
public class EmergencyEscapeEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyEscapeEventHandler.class);

    // Helper method to check debug mode from config
    private static boolean isDebugMode() {
        return ModConfig.DEBUG_MODE.get();
    }

    // Helper method to play zero sound when health/level reaches 0
    private static void playZeroSound(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            LOGGER.info("[EmergencyEscape] Playing ZERO sound at {}, {}, {}", player.getX(), player.getY(), player.getZ());
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ZERO.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;

        boolean hasEscapeItem = hasEmergencyEscapeItem(player);

        // Update capability states based on item presence
        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            cap.setHasItem(hasEscapeItem);
        });

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (hasEscapeItem && !cap.isActive()) {
                cap.setActive(true);
            } else if (!hasEscapeItem && cap.isActive()) {
                cap.setActive(false);
            }
        });

        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            if (hasEscapeItem && !cap.isActive()) {
                cap.setActive(true);
            } else if (!hasEscapeItem && cap.isActive()) {
                cap.setActive(false);
            }

            if (cap.isActive()) {
                cap.tick(player);

                // Check if level has been consumed to zero -> trigger emergency escape
                // Only check if not already escaping
                boolean isEscaping = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                        .map(EmergencyEscapeCapability::isEscaping).orElse(false);
                if (!isEscaping && cap.getActiveTimers().size() > 0) {
                    int currentLevel = DamageConsumptionCapability.getPlayerLevel(player);
                    if (currentLevel <= 0) {
                        if (isDebugMode()) {
                            LOGGER.info("[EmergencyEscape] Triggering escape due to level consumption reaching zero");
                            if (player instanceof ServerPlayer sp) {
                                sp.sendSystemMessage(Component.literal("§c[Debug] Level consumed to 0 -> Emergency Escape triggered!"));
                            }
                        }
                        playZeroSound(player);
                        triggerEmergencyEscape(player);
                    }
                }
            }
        });

        // Handle hunger (no hunger when item is present)
        if (hasEscapeItem) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);
        }

        // Handle emergency escape state
        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEscaping()) {
                // Lock player position
                player.setPos(cap.getEscapeX(), cap.getEscapeY(), cap.getEscapeZ());
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;

                // Add slow falling effect
                if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
                }

                // Log escape countdown every second
                if (cap.getEscapeTicksRemaining() % 20 == 0) {
                    LOGGER.info("[EmergencyEscape] Escape countdown: {}s remaining",
                        cap.getEscapeTicksRemaining() / 20);
                }

                cap.tick();

                if (cap.shouldDie()) {
                    LOGGER.info("[EmergencyEscape] Escape timer reached 0, killing player: {}",
                        player.getName().getString());

                    // Spawn death effects
                    if (player instanceof ServerPlayer serverPlayer) {
                        spawnDeathEffects(serverPlayer, cap.getEscapeX(), cap.getEscapeY(), cap.getEscapeZ());
                    }

                    // IMPORTANT: Kill the player BEFORE stopping escape
                    // This ensures isEscaping() is still true when onLivingDamage fires,
                    // so the damage won't be canceled and won't trigger another escape
                    player.hurt(player.damageSources().generic(), Float.MAX_VALUE);
                    cap.stopEscape();
                }
            }
        });

        // Sync capabilities to client
        if (player instanceof ServerPlayer serverPlayer && player.tickCount % 5 == 0) {
            syncCapabilities(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!hasEmergencyEscapeItem(player)) return;

        // Skip if already in escape state (prevent re-triggering and cancel additional damage)
        boolean isAlreadyEscaping = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                .map(EmergencyEscapeCapability::isEscaping).orElse(false);
        if (isAlreadyEscaping) {
            // Check if this is the final death damage (escape timer has run out)
            boolean shouldDie = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                    .map(EmergencyEscapeCapability::shouldDie).orElse(false);
            if (shouldDie) {
                // Allow the death damage through - don't cancel, don't process further
                LOGGER.info("[EmergencyEscape] Allowing final death damage through (shouldDie=true)");
                return;
            }
            // Cancel all other damage while in escape state - player is still in countdown
            event.setCanceled(true);
            return;
        }

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        // Get current level before damage
        int levelBeforeDamage = DamageConsumptionCapability.getPlayerLevel(player);

        // Determine which body part was hit
        BodyPart hitPart = determineHitBodyPart(player, source);

        // Apply body part damage and check for death trigger
        // Store health values before damage for logging
        final float[] healthBefore = new float[2]; // [0]=head, [1]=body
        final float[] healthAfter = new float[2];
        final int[] maxHealth = new int[2];
        final boolean[] shouldTriggerEscape = new boolean[1]; // [0] = should trigger escape due to body part health

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

            // Store health before damage
            healthBefore[0] = cap.getHeadHealth();
            healthBefore[1] = cap.getBodyHealth();
            maxHealth[0] = cap.getMaxHeadHealth();
            maxHealth[1] = cap.getMaxBodyHealth();

            switch (hitPart) {
                case HEAD:
                    cap.damageHead(damage);
                    break;
                case BODY:
                    cap.damageBody(damage);
                    break;
                case LEGS:
                    // Legs take no body part damage, but vanilla damage still applies
                    break;
            }

            // Store health after damage
            healthAfter[0] = cap.getHeadHealth();
            healthAfter[1] = cap.getBodyHealth();

            // Check if head or body health reached 0 -> trigger emergency escape
            if (cap.getHeadHealth() <= 0 || cap.getBodyHealth() <= 0) {
                shouldTriggerEscape[0] = true;
            }
        });

        // Apply level consumption based on damage size
        boolean isLargeDamage = damage >= ModConfig.LARGE_DAMAGE_THRESHOLD.get();

        // Log damage info
        String damageSourceType = source.type().msgId();
        String attackerInfo = source.getEntity() != null ? source.getEntity().getType().toShortString() : "none";
        LOGGER.info("[EmergencyEscape] === DAMAGE EVENT ===");
        LOGGER.info("[EmergencyEscape] Player: {} | HP: {}/{} | Level: {}",
            player.getName().getString(), player.getHealth(), player.getMaxHealth(), levelBeforeDamage);
        LOGGER.info("[EmergencyEscape] Damage: {} ({}) | Source: {} | Attacker: {}",
            damage, isLargeDamage ? "LARGE" : "small", damageSourceType, attackerInfo);
        LOGGER.info("[EmergencyEscape] Hit Part: {} | Pose Synced: {}",
            hitPart.name(), BodyPartHitbox.hasSyncedPoseData(player));
        LOGGER.info("[EmergencyEscape] 頭体力: {}/{} ({}→{}) | 胴体力: {}/{} ({}→{})",
            healthAfter[0], maxHealth[0], healthBefore[0], healthAfter[0],
            healthAfter[1], maxHealth[1], healthBefore[1], healthAfter[1]);
        applyDamageConsumption(player, isLargeDamage);

        // Check if head or body health reached 0 -> trigger emergency escape
        if (shouldTriggerEscape[0]) {
            String reason = healthAfter[0] <= 0 ? "頭体力が0" : "胴体力が0";
            LOGGER.info("[EmergencyEscape] Triggering escape due to body part health: {}", reason);
            if (isDebugMode() && player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c[Debug] " + reason + " -> Emergency Escape triggered!"));
            }
            // Play zero sound for head/body health reaching 0
            playZeroSound(player);
            // Cancel the damage - emergency escape will handle death
            event.setCanceled(true);
            triggerEmergencyEscape(player);
            return;
        }

        // Check if level has hit zero -> trigger emergency escape
        int currentLevel = DamageConsumptionCapability.getPlayerLevel(player);
        if (currentLevel <= 0) {
            if (isDebugMode()) {
                LOGGER.info("[EmergencyEscape] Triggering escape due to zero level");
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§c[Debug] Level = 0 -> Emergency Escape triggered!"));
                }
            }
            playZeroSound(player);
            triggerEmergencyEscape(player);
            return;
        }

        // Check if this damage would kill the player (vanilla HP reaching 0)
        // Note: LivingDamageEvent fires BEFORE damage is applied, so we need to calculate
        float currentHealth = player.getHealth();
        float healthAfterDamage = currentHealth - damage;

        if (healthAfterDamage <= 0) {
            if (isDebugMode()) {
                LOGGER.info("[EmergencyEscape] Triggering escape due to lethal damage. Current HP: {}, Damage: {}", currentHealth, damage);
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal(
                        String.format("§c[Debug] Lethal damage (HP: %.1f - %.1f = %.1f) -> Emergency Escape triggered!",
                            currentHealth, damage, healthAfterDamage)));
                }
            }
            // Play zero sound for HP reaching 0
            playZeroSound(player);
            // Cancel the damage - emergency escape will handle death
            event.setCanceled(true);
            triggerEmergencyEscape(player);
        }

        // Do NOT cancel vanilla damage otherwise - let default HP system work normally
        // The body part system is an ADDITIONAL display system, not a replacement
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Clear damage consumption timers on death
        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            cap.clearAllTimers();
        });

        // Reset body part health
        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            cap.reset();
        });

        // Clear escape state
        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            cap.stopEscape();
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Reset capabilities on respawn
        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            cap.reset();
            cap.setActive(hasEmergencyEscapeItem(player));
        });

        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            cap.clearAllTimers();
            cap.setActive(hasEmergencyEscapeItem(player));
        });

        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            cap.stopEscape();
        });

        // Set player level to configured respawn level
        int respawnLevel = ModConfig.RESPAWN_LEVEL.get();
        // Reset experience first, then set to desired level
        player.giveExperienceLevels(-player.experienceLevel);
        player.giveExperienceLevels(respawnLevel);
        player.experienceProgress = 0;
        LOGGER.info("[EmergencyEscape] Player {} respawned with level {}", player.getName().getString(), respawnLevel);

        if (player instanceof ServerPlayer serverPlayer) {
            syncCapabilities(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Copy capability data on death/respawn
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();

            original.reviveCaps();

            original.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(oldCap -> {
                newPlayer.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(newCap -> {
                    newCap.reset();
                });
            });

            original.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(oldCap -> {
                newPlayer.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(newCap -> {
                    newCap.clearAllTimers();
                });
            });

            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clear cached model part data when player logs out
        Player player = event.getEntity();
        PlayerModelPartCache.removePlayer(player.getUUID());
        HitPositionTracker.clearHitInfo(player.getUUID());
    }

    public static void triggerEmergencyEscape(Player player) {
        LOGGER.info("[EmergencyEscape] triggerEmergencyEscape called for player: {}", player.getName().getString());

        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEscaping()) {
                LOGGER.info("[EmergencyEscape] Already escaping, ignoring trigger");
                return;
            }

            int deathDelayTicks = ModConfig.ESCAPE_DEATH_DELAY.get() * 20;
            cap.startEscape(player, deathDelayTicks);

            LOGGER.info("[EmergencyEscape] Escape started! Duration: {}s ({} ticks)",
                ModConfig.ESCAPE_DEATH_DELAY.get(), deathDelayTicks);
        });
    }

    private static void applyDamageConsumption(Player player, boolean isLargeDamage) {
        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

            if (isLargeDamage) {
                // Large damage instant consumption
                if (ModConfig.LARGE_DAMAGE_INSTANT_ENABLED.get()) {
                    int durationTicks = ModConfig.LARGE_DAMAGE_INSTANT_DURATION.get() * 20;
                    int intervalTicks = (int) (ModConfig.LARGE_DAMAGE_INSTANT_INTERVAL.get() * 20);
                    int amount = ModConfig.LARGE_DAMAGE_INSTANT_AMOUNT.get();
                    cap.addConsumption(true, true, durationTicks, intervalTicks, amount);
                }

                // Large damage sustained consumption (starts after instant)
                if (ModConfig.LARGE_DAMAGE_SUSTAINED_ENABLED.get()) {
                    int instantDuration = ModConfig.LARGE_DAMAGE_INSTANT_DURATION.get() * 20;
                    int sustainedDuration = ModConfig.LARGE_DAMAGE_SUSTAINED_DURATION.get() * 20;
                    int intervalTicks = ModConfig.LARGE_DAMAGE_SUSTAINED_INTERVAL.get() * 20;
                    int amount = ModConfig.LARGE_DAMAGE_SUSTAINED_AMOUNT.get();
                    // Add with delay by including instant duration
                    cap.addConsumption(true, false, instantDuration + sustainedDuration, intervalTicks, amount);
                }
            } else {
                // Small damage instant consumption
                if (ModConfig.SMALL_DAMAGE_INSTANT_ENABLED.get()) {
                    int durationTicks = ModConfig.SMALL_DAMAGE_INSTANT_DURATION.get() * 20;
                    int intervalTicks = (int) (ModConfig.SMALL_DAMAGE_INSTANT_INTERVAL.get() * 20);
                    int amount = ModConfig.SMALL_DAMAGE_INSTANT_AMOUNT.get();
                    cap.addConsumption(false, true, durationTicks, intervalTicks, amount);
                }

                // Small damage sustained consumption
                if (ModConfig.SMALL_DAMAGE_SUSTAINED_ENABLED.get()) {
                    int instantDuration = ModConfig.SMALL_DAMAGE_INSTANT_DURATION.get() * 20;
                    int sustainedDuration = ModConfig.SMALL_DAMAGE_SUSTAINED_DURATION.get() * 20;
                    int intervalTicks = ModConfig.SMALL_DAMAGE_SUSTAINED_INTERVAL.get() * 20;
                    int amount = ModConfig.SMALL_DAMAGE_SUSTAINED_AMOUNT.get();
                    cap.addConsumption(false, false, instantDuration + sustainedDuration, intervalTicks, amount);
                }
            }
        });
    }

    private static BodyPart determineHitBodyPart(Player player, DamageSource source) {
        double playerY = player.getY();
        double playerHeight = player.getBbHeight();

        String hitSource = "unknown";
        BodyPart result = BodyPart.BODY; // Default

        // First, try to get accurate body part from Mixin tracker (uses raycast & hitbox detection)
        HitPositionTracker.HitInfo hitInfo = HitPositionTracker.getLastHitInfo(player.getUUID());
        if (hitInfo != null && hitInfo.bodyPart != null) {
            hitSource = hitInfo.source;

            // Convert from BodyPartHitbox.BodyPart to our simplified BodyPart enum
            // Use simplifyBodyPart to convert LEFT_ARM/RIGHT_ARM to BODY
            BodyPartHitbox.BodyPart simplifiedPart = BodyPartHitbox.simplifyBodyPart(hitInfo.bodyPart);
            switch (simplifiedPart) {
                case HEAD:
                    result = BodyPart.HEAD;
                    break;
                case BODY:
                    result = BodyPart.BODY;
                    break;
                case LEGS:
                    result = BodyPart.LEGS;
                    break;
                default:
                    result = BodyPart.BODY;
                    break;
            }
        }
        // Fallback: use simpler detection methods
        else {
            // Based on corrected HumanoidModel proportions:
            // Head: 75% - 100% of player height (model Y -8 to 0)
            // Body: 37.5% - 75% of player height (model Y 0 to 12)
            // Legs: 0% - 37.5% of player height (model Y 12 to 24)
            double headThreshold = playerY + playerHeight * 0.75;
            double bodyThreshold = playerY + playerHeight * 0.375;

            double hitY = playerY + playerHeight * 0.5; // Default to center

            // For projectiles - use projectile's current position
            if (source.getDirectEntity() != null && source.getDirectEntity() != source.getEntity()) {
                hitY = source.getDirectEntity().getY();
                hitSource = "projectile_fallback (" + source.getDirectEntity().getType().toShortString() + ")";

                if (hitY >= headThreshold) {
                    result = BodyPart.HEAD;
                } else if (hitY >= bodyThreshold) {
                    result = BodyPart.BODY;
                } else {
                    result = BodyPart.LEGS;
                }
            }
            // For melee attacks - use attacker's eye height
            else if (source.getEntity() != null) {
                net.minecraft.world.entity.Entity attacker = source.getEntity();
                hitY = attacker.getEyeY();
                hitSource = "melee_fallback (" + attacker.getType().toShortString() + ")";

                // Clamp to player's bounds
                hitY = Math.max(playerY, Math.min(hitY, playerY + playerHeight));

                if (hitY >= headThreshold) {
                    result = BodyPart.HEAD;
                } else if (hitY >= bodyThreshold) {
                    result = BodyPart.BODY;
                } else {
                    result = BodyPart.LEGS;
                }
            } else {
                // Default to body for environmental damage
                hitSource = "environmental (" + source.type().msgId() + ")";
                result = BodyPart.BODY;
            }
        }

        // Debug output
        if (isDebugMode()) {
            String debugMsg = String.format("[HitDetect] Source: %s | Result: %s", hitSource, result.name());
            LOGGER.info(debugMsg);

            // Also send to player chat for easy debugging in-game
            if (player instanceof ServerPlayer serverPlayer) {
                // Color code based on body part
                String colorCode = switch (result) {
                    case HEAD -> "§c"; // Red for head
                    case BODY -> "§e"; // Yellow for body
                    case LEGS -> "§a"; // Green for legs
                };
                serverPlayer.sendSystemMessage(Component.literal(
                    String.format("§7[Debug] §f%s → %s%s", hitSource, colorCode, result.name())
                ));
            }
        }

        return result;
    }

    private static void spawnDeathEffects(ServerPlayer player, double x, double y, double z) {
        ServerLevel level = player.serverLevel();

        // Play explosion sound
        LOGGER.info("[EmergencyEscape] Playing death EXPLOSION sound at {}, {}, {}", x, y, z);
        level.playSound(null, x, y, z, ModSounds.EXPLOSION.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        // Send packet to spawn particles on all nearby clients
        SpawnParticlesPacket packet = new SpawnParticlesPacket(x, y, z);
        NetworkHandler.CHANNEL.send(packet, PacketDistributor.NEAR.with(
                new PacketDistributor.TargetPoint(x, y, z, 64, level.dimension())));
    }

    public static boolean hasEmergencyEscapeItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.EMERGENCY_ESCAPE_ITEM.get()) {
                return true;
            }
        }
        return false;
    }

    private static void syncCapabilities(ServerPlayer player) {
        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(bodyPartCap -> {
            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(escapeCap -> {
                SyncCapabilitiesPacket packet = new SyncCapabilitiesPacket(
                        bodyPartCap.getHeadHealth(),
                        bodyPartCap.getBodyHealth(),
                        bodyPartCap.getMaxHeadHealth(),
                        bodyPartCap.getMaxBodyHealth(),
                        bodyPartCap.isActive(),
                        escapeCap.isEscaping(),
                        escapeCap.getEscapeTicksRemaining(),
                        escapeCap.hasItem()
                );
                NetworkHandler.CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
            });
        });
    }

    public enum BodyPart {
        HEAD, BODY, LEGS
    }
}
