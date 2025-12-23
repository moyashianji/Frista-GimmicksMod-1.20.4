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
import com.furasuta.emergencyescape.util.HitPositionTracker;
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

    // Set to true to enable debug messages in chat and console
    private static final boolean DEBUG_HIT_DETECTION = true;

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

                cap.tick();

                if (cap.shouldDie()) {
                    // Spawn death effects
                    if (player instanceof ServerPlayer serverPlayer) {
                        spawnDeathEffects(serverPlayer, cap.getEscapeX(), cap.getEscapeY(), cap.getEscapeZ());
                    }

                    cap.stopEscape();
                    player.hurt(player.damageSources().generic(), Float.MAX_VALUE);
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

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        // Determine which body part was hit
        BodyPart hitPart = determineHitBodyPart(player, source);

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

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

            // Check if should trigger emergency escape
            boolean shouldEscape = false;
            if (cap.getHeadHealth() <= 0 || cap.getBodyHealth() <= 0) {
                shouldEscape = true;
            }

            // Apply damage consumption
            boolean isLargeDamage = damage >= ModConfig.LARGE_DAMAGE_THRESHOLD.get();
            applyDamageConsumption(player, isLargeDamage);

            // Check experience
            int totalExp = DamageConsumptionCapability.getPlayerTotalExperience(player);
            if (totalExp <= 0) {
                shouldEscape = true;
            }

            if (shouldEscape) {
                triggerEmergencyEscape(player);
            }
        });

        // Do NOT cancel vanilla damage - let default HP system work normally
        // The body part system is an ADDITIONAL system, not a replacement
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

    public static void triggerEmergencyEscape(Player player) {
        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEscaping()) return;

            int deathDelayTicks = ModConfig.ESCAPE_DEATH_DELAY.get() * 20;
            cap.startEscape(player, deathDelayTicks);

            // Play activation sound
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.ZERO.get(), player.getSoundSource(), 1.0f, 1.0f);
            }
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

            // Convert from BodyPartHitbox.BodyPart to our BodyPart enum
            switch (hitInfo.bodyPart) {
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
            // Head: top 28% of player (0.72 - 1.0, matching BodyPartHitbox)
            double headThreshold = playerY + playerHeight * 0.72;
            // Body: middle 41% of player (0.31 - 0.72)
            double bodyThreshold = playerY + playerHeight * 0.31;

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
        if (DEBUG_HIT_DETECTION) {
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
        level.playSound(null, x, y, z, ModSounds.EXPLOSION.get(), player.getSoundSource(), 1.0f, 1.0f);

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
