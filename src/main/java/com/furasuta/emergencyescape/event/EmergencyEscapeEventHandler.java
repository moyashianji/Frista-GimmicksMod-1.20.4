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
import com.furasuta.emergencyescape.network.SpawnGasParticlesPacket;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID)
public class EmergencyEscapeEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyEscapeEventHandler.class);

    // プレイヤーごとのアラート再生済みフラグ: [0]=頭, [1]=胴
    private static final Map<UUID, boolean[]> alertTracker = new HashMap<>();

    private static boolean isDebugMode() {
        return ModConfig.DEBUG_MODE.get();
    }

    private static void playZeroSound(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            LOGGER.info("[EmergencyEscape] ZERO効果音再生: {}, {}, {}", player.getX(), player.getY(), player.getZ());
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ZERO.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static void checkAndPlayAlertSounds(Player player, float headHealthPercent, float bodyHealthPercent) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        UUID playerId = player.getUUID();
        boolean[] alerts = alertTracker.computeIfAbsent(playerId, k -> new boolean[]{false, false});
        float alertVolume = ModConfig.ALERT_VOLUME.get().floatValue();

        // 頭: 50%未満
        if (headHealthPercent < 50 && headHealthPercent > 0 && !alerts[0]) {
            alerts[0] = true;
            LOGGER.info("[EmergencyEscape] ALERT1再生 (頭<50%) プレイヤー: {}", player.getName().getString());
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ALERT1.get(), net.minecraft.sounds.SoundSource.PLAYERS, alertVolume, 1.0f);
        }

        // 胴: 30%未満
        if (bodyHealthPercent < 30 && bodyHealthPercent > 0 && !alerts[1]) {
            alerts[1] = true;
            LOGGER.info("[EmergencyEscape] ALERT2再生 (胴<30%) プレイヤー: {}", player.getName().getString());
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ALERT2.get(), net.minecraft.sounds.SoundSource.PLAYERS, alertVolume, 1.0f);
        }
    }

    // ガスパーティクル種別判定: 0=なし, 1=重(大ダメージ即時), 2=軽(その他)
    private static int getGasParticleType(Player player) {
        final int[] result = {0};
        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive() || cap.getActiveTimers().isEmpty()) return;

            for (DamageConsumptionCapability.ConsumptionTimer timer : cap.getActiveTimers()) {
                if (timer.isLargeDamage() && timer.isInstant()) {
                    result[0] = 1;
                    return;
                } else if (result[0] == 0) {
                    result[0] = 2;
                }
            }
        });
        return result[0];
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;

        // コマンドで制御されるアクティブ状態を取得
        boolean isActive = player.getCapability(BodyPartHealthCapability.CAPABILITY)
                .map(BodyPartHealthCapability::isActive).orElse(false);

        // DamageConsumptionのアクティブ状態をBodyPartHealthに連動
        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            if (isActive && !cap.isActive()) {
                cap.setActive(true);
            } else if (!isActive && cap.isActive()) {
                cap.setActive(false);
            }

            if (cap.isActive()) {
                cap.tick(player);

                // レベル消費で0到達 -> 緊急脱出発動
                boolean isEscaping = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                        .map(EmergencyEscapeCapability::isEscaping).orElse(false);
                if (!isEscaping && cap.getActiveTimers().size() > 0) {
                    int currentLevel = DamageConsumptionCapability.getPlayerLevel(player);
                    if (currentLevel <= 0) {
                        if (isDebugMode()) {
                            LOGGER.info("[EmergencyEscape] レベル消費0到達により脱出発動");
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

        // システム有効中は空腹無効
        if (isActive) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);
        }

        // 脱出状態の処理
        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEscaping()) {
                // 位置凍結（視点は自由に動かせる）
                player.setPos(cap.getEscapeX(), cap.getEscapeY(), cap.getEscapeZ());
                player.setDeltaMovement(0, 0, 0);
                player.setOnGround(true);
                player.fallDistance = 0;

                // 移動抑制エフェクト
                if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        || player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() < 200) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false, false));
                }

                if (cap.getEscapeTicksRemaining() % 20 == 0) {
                    LOGGER.info("[EmergencyEscape] 脱出カウントダウン: 残り{}秒",
                        cap.getEscapeTicksRemaining() / 20);
                }

                cap.tick();

                if (cap.shouldDie()) {
                    LOGGER.info("[EmergencyEscape] 脱出タイマー終了、プレイヤー死亡: {}",
                        player.getName().getString());

                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                    if (player instanceof ServerPlayer serverPlayer) {
                        spawnDeathEffects(serverPlayer, cap.getEscapeX(), cap.getEscapeY(), cap.getEscapeZ());
                    }

                    // stopEscapeの前にkillすることでisEscaping()がtrueのままダメージが通る
                    player.hurt(player.damageSources().generic(), Float.MAX_VALUE);
                    cap.stopEscape();
                }
            }
        });

        // ガスパーティクルを他プレイヤーにブロードキャスト（自分には送らない）
        if (player instanceof ServerPlayer sp && player.tickCount % 5 == 0) {
            syncCapabilities(sp);

            int gasType = getGasParticleType(player);
            if (gasType > 0) {
                SpawnGasParticlesPacket gasPacket = new SpawnGasParticlesPacket(
                    player.getId(), gasType, player.getX(), player.getY(), player.getZ());
                NetworkHandler.CHANNEL.send(gasPacket, PacketDistributor.TRACKING_ENTITY.with(player));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!isSystemActive(player)) return;

        // 脱出中のダメージ処理
        boolean isAlreadyEscaping = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                .map(EmergencyEscapeCapability::isEscaping).orElse(false);
        if (isAlreadyEscaping) {
            // 最終死亡ダメージのみ通す
            boolean shouldDie = player.getCapability(EmergencyEscapeCapability.CAPABILITY)
                    .map(EmergencyEscapeCapability::shouldDie).orElse(false);
            if (shouldDie) {
                LOGGER.info("[EmergencyEscape] 最終死亡ダメージを許可 (shouldDie=true)");
                return;
            }
            event.setCanceled(true);
            return;
        }

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        int levelBeforeDamage = DamageConsumptionCapability.getPlayerLevel(player);

        BodyPart hitPart = determineHitBodyPart(player, source);

        final float[] healthBefore = new float[2];
        final float[] healthAfter = new float[2];
        final int[] maxHealth = new int[2];
        final boolean[] shouldTriggerEscape = new boolean[1];

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

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
                    break;
            }

            healthAfter[0] = cap.getHeadHealth();
            healthAfter[1] = cap.getBodyHealth();

            checkAndPlayAlertSounds(player, cap.getHeadHealthPercent(), cap.getBodyHealthPercent());

            if (cap.getHeadHealth() <= 0 || cap.getBodyHealth() <= 0) {
                shouldTriggerEscape[0] = true;
            }
        });

        boolean isLargeDamage = damage >= ModConfig.LARGE_DAMAGE_THRESHOLD.get();

        String damageSourceType = source.type().msgId();
        String attackerInfo = source.getEntity() != null ? source.getEntity().getType().toShortString() : "none";
        LOGGER.info("[EmergencyEscape] === ダメージイベント ===");
        LOGGER.info("[EmergencyEscape] プレイヤー: {} | HP: {}/{} | レベル: {}",
            player.getName().getString(), player.getHealth(), player.getMaxHealth(), levelBeforeDamage);
        LOGGER.info("[EmergencyEscape] ダメージ: {} ({}) | ソース: {} | 攻撃者: {}",
            damage, isLargeDamage ? "大" : "小", damageSourceType, attackerInfo);
        LOGGER.info("[EmergencyEscape] 命中部位: {} | ポーズ同期済: {}",
            hitPart.name(), BodyPartHitbox.hasSyncedPoseData(player));
        LOGGER.info("[EmergencyEscape] 頭体力: {}/{} ({}→{}) | 胴体力: {}/{} ({}→{})",
            healthAfter[0], maxHealth[0], healthBefore[0], healthAfter[0],
            healthAfter[1], maxHealth[1], healthBefore[1], healthAfter[1]);
        applyDamageConsumption(player, isLargeDamage);

        // 部位体力0で脱出発動
        if (shouldTriggerEscape[0]) {
            String reason = healthAfter[0] <= 0 ? "頭体力が0" : "胴体力が0";
            LOGGER.info("[EmergencyEscape] 部位体力による脱出発動: {}", reason);
            if (isDebugMode() && player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c[Debug] " + reason + " -> Emergency Escape triggered!"));
            }
            playZeroSound(player);
            event.setCanceled(true);
            triggerEmergencyEscape(player);
            return;
        }

        // レベル0で脱出発動
        int currentLevel = DamageConsumptionCapability.getPlayerLevel(player);
        if (currentLevel <= 0) {
            if (isDebugMode()) {
                LOGGER.info("[EmergencyEscape] レベル0により脱出発動");
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§c[Debug] Level = 0 -> Emergency Escape triggered!"));
                }
            }
            playZeroSound(player);
            event.setCanceled(true);
            triggerEmergencyEscape(player);
            return;
        }

        // バニラHPダメージを無効化（部位体力システムで代替）
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            cap.clearAllTimers();
        });

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            cap.setActive(false);
            cap.reset();
        });

        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            cap.stopEscape();
        });
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        alertTracker.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // 緊急脱出後はシステム無効化
        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            cap.setActive(false);
            cap.reset();
        });

        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            cap.clearAllTimers();
            cap.setActive(false);
        });

        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            cap.stopEscape();
        });

        alertTracker.remove(player.getUUID());

        int respawnLevel = ModConfig.RESPAWN_LEVEL.get();
        player.giveExperienceLevels(-player.experienceLevel);
        player.giveExperienceLevels(respawnLevel);
        player.experienceProgress = 0;
        LOGGER.info("[EmergencyEscape] プレイヤー{}がレベル{}でリスポーン", player.getName().getString(), respawnLevel);

        if (player instanceof ServerPlayer serverPlayer) {
            syncCapabilities(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();

            original.reviveCaps();

            original.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(oldCap -> {
                newPlayer.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(newCap -> {
                    newCap.setActive(false);
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
        Player player = event.getEntity();
        PlayerModelPartCache.removePlayer(player.getUUID());
        HitPositionTracker.clearHitInfo(player.getUUID());
        alertTracker.remove(player.getUUID());
    }

    public static void triggerEmergencyEscape(Player player) {
        LOGGER.info("[EmergencyEscape] 緊急脱出開始 プレイヤー: {}", player.getName().getString());

        player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
            if (cap.isEscaping()) {
                LOGGER.info("[EmergencyEscape] 既に脱出中のため無視");
                return;
            }

            int deathDelayTicks = ModConfig.ESCAPE_DEATH_DELAY.get() * 20;
            cap.startEscape(player, deathDelayTicks);

            LOGGER.info("[EmergencyEscape] 脱出開始 持続時間: {}秒 ({}tick)",
                ModConfig.ESCAPE_DEATH_DELAY.get(), deathDelayTicks);
        });
    }

    private static void applyDamageConsumption(Player player, boolean isLargeDamage) {
        player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

            if (isLargeDamage) {
                // 大ダメージ即時消費
                if (ModConfig.LARGE_DAMAGE_INSTANT_ENABLED.get()) {
                    int durationTicks = ModConfig.LARGE_DAMAGE_INSTANT_DURATION.get() * 20;
                    int intervalTicks = (int) (ModConfig.LARGE_DAMAGE_INSTANT_INTERVAL.get() * 20);
                    int amount = ModConfig.LARGE_DAMAGE_INSTANT_AMOUNT.get();
                    cap.addConsumption(true, true, durationTicks, intervalTicks, amount);
                }

                // 大ダメージ持続消費（即時消費の後に開始）
                if (ModConfig.LARGE_DAMAGE_SUSTAINED_ENABLED.get()) {
                    int instantDuration = ModConfig.LARGE_DAMAGE_INSTANT_DURATION.get() * 20;
                    int sustainedDuration = ModConfig.LARGE_DAMAGE_SUSTAINED_DURATION.get() * 20;
                    int intervalTicks = ModConfig.LARGE_DAMAGE_SUSTAINED_INTERVAL.get() * 20;
                    int amount = ModConfig.LARGE_DAMAGE_SUSTAINED_AMOUNT.get();
                    cap.addConsumption(true, false, instantDuration + sustainedDuration, intervalTicks, amount);
                }
            } else {
                // 小ダメージ即時消費
                if (ModConfig.SMALL_DAMAGE_INSTANT_ENABLED.get()) {
                    int durationTicks = ModConfig.SMALL_DAMAGE_INSTANT_DURATION.get() * 20;
                    int intervalTicks = (int) (ModConfig.SMALL_DAMAGE_INSTANT_INTERVAL.get() * 20);
                    int amount = ModConfig.SMALL_DAMAGE_INSTANT_AMOUNT.get();
                    cap.addConsumption(false, true, durationTicks, intervalTicks, amount);
                }

                // 小ダメージ持続消費
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

        double headThresholdPercent = ModConfig.HEAD_THRESHOLD_PERCENT.get() / 100.0;
        double bodyThresholdPercent = ModConfig.BODY_THRESHOLD_PERCENT.get() / 100.0;

        String hitSource = "unknown";
        BodyPart result = BodyPart.BODY;

        // Mixinトラッカーから正確な部位を取得
        HitPositionTracker.HitInfo hitInfo = HitPositionTracker.getLastHitInfo(player.getUUID());
        if (hitInfo != null && hitInfo.bodyPart != null) {
            hitSource = hitInfo.source;

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
        // フォールバック: 設定閾値による簡易判定
        else {
            double headThreshold = playerY + playerHeight * headThresholdPercent;
            double bodyThreshold = playerY + playerHeight * bodyThresholdPercent;

            double hitY = playerY + playerHeight * 0.5;

            // 飛び道具
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
            // 近接攻撃
            else if (source.getEntity() != null) {
                net.minecraft.world.entity.Entity attacker = source.getEntity();
                hitY = attacker.getEyeY();
                hitSource = "melee_fallback (" + attacker.getType().toShortString() + ")";

                hitY = Math.max(playerY, Math.min(hitY, playerY + playerHeight));

                if (hitY >= headThreshold) {
                    result = BodyPart.HEAD;
                } else if (hitY >= bodyThreshold) {
                    result = BodyPart.BODY;
                } else {
                    result = BodyPart.LEGS;
                }
            } else {
                // 環境ダメージはデフォルトで胴
                hitSource = "environmental (" + source.type().msgId() + ")";
                result = BodyPart.BODY;
            }
        }

        if (isDebugMode()) {
            String debugMsg = String.format("[HitDetect] ソース: %s | 結果: %s", hitSource, result.name());
            LOGGER.info(debugMsg);

            if (player instanceof ServerPlayer serverPlayer) {
                String colorCode = switch (result) {
                    case HEAD -> "§c";
                    case BODY -> "§e";
                    case LEGS -> "§a";
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

        int explosionRange = ModConfig.EXPLOSION_SOUND_RANGE.get();
        float explosionVolume = explosionRange / 16.0f;
        LOGGER.info("[EmergencyEscape] 死亡爆発音再生 座標: {}, {}, {} (範囲={}ブロック, 音量={})",
            x, y, z, explosionRange, explosionVolume);
        level.playSound(null, x, y, z, ModSounds.EXPLOSION.get(), net.minecraft.sounds.SoundSource.PLAYERS, explosionVolume, 1.0f);

        SpawnParticlesPacket packet = new SpawnParticlesPacket(x, y, z);
        NetworkHandler.CHANNEL.send(packet, PacketDistributor.NEAR.with(
                new PacketDistributor.TargetPoint(x, y, z, 64, level.dimension())));
    }

    /**
     * コマンドで有効化された特殊体力システムがアクティブかどうか。
     * クライアント・サーバー両側で使用可能。
     */
    public static boolean isSystemActive(Player player) {
        return player.getCapability(BodyPartHealthCapability.CAPABILITY)
                .map(BodyPartHealthCapability::isActive).orElse(false);
    }

    @Deprecated
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
                        bodyPartCap.isActive()
                );
                NetworkHandler.CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
            });
        });
    }

    public enum BodyPart {
        HEAD, BODY, LEGS
    }
}
