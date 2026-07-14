package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.DamageConsumptionCapability;
import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.Random;

/**
 * クライアント側パケットハンドラ。
 */
public class ClientPacketHandler {

    /** 脱出中かどうか（サーバーから同期）。クライアント側の移動入力抑制に使用。 */
    public static volatile boolean clientEscaping = false;

    public static void handleSyncCapabilities(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                               boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean systemActive,
                                               float legDamageAccum, boolean leaking) {
        clientEscaping = isEscaping;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
                cap.syncFromServer(headHealth, bodyHealth, maxHeadHealth, maxBodyHealth, isActive);
            });

            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                cap.setHasItem(systemActive);
            });

            player.getCapability(DamageConsumptionCapability.CAPABILITY).ifPresent(cap -> {
                cap.setLegDamageAccum(legDamageAccum);
                cap.setClientLeaking(leaking);
            });
        }
    }

    /**
     * 他プレイヤーのガスパーティクルを表示する。
     * 自分自身のガスは表示しない（視界の邪魔になるため）。
     */
    public static void handleSpawnGasParticles(int entityId, int gasType, int bonusLevel, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 自分自身のパーティクルは表示しない（視界の邪魔になるため）
        if (mc.player.getId() == entityId) return;

        Level level = mc.level;
        Entity entity = level.getEntity(entityId);
        if (entity == null) return;

        spawnGasParticles(level, x, y, z, gasType, bonusLevel);
    }

    private static void spawnGasParticles(Level level, double x, double y, double z, int type, int bonusLevel) {
        Random random = new Random();

        // 足デバフ（加算）状態のエフェクト。周囲から負傷が分かるようにする。
        // 仕様: 加算1=普段の黒ふわ＋薄いグレーのちらちら / 加算2=さらに紫のちらちら
        int effectCount = ModConfig.LEG_EFFECT_COUNT.get();
        if (bonusLevel >= 1) {
            // 白（薄いグレー）のふわ
            DustParticleOptions grey = new DustParticleOptions(new Vector3f(0.82f, 0.83f, 0.86f), 1.0f);
            for (int i = 0; i < effectCount; i++) {
                double ox = (random.nextDouble() - 0.5) * 0.7;
                double oy = random.nextDouble() * 1.6 + 0.2;
                double oz = (random.nextDouble() - 0.5) * 0.7;
                level.addParticle(grey, x + ox, y + oy, z + oz, 0, 0.03, 0);
            }
        }
        if (bonusLevel >= 2) {
            // さらに紫のふわ
            DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.62f, 0.24f, 0.85f), 1.1f);
            for (int i = 0; i < effectCount; i++) {
                double ox = (random.nextDouble() - 0.5) * 0.7;
                double oy = random.nextDouble() * 1.6 + 0.2;
                double oz = (random.nextDouble() - 0.5) * 0.7;
                level.addParticle(purple, x + ox, y + oy, z + oz, 0, 0.03, 0);
            }
        }

        if (type == 1) {
            // 重ガス - 大ダメージ即時消費時の噴射エフェクト
            for (int i = 0; i < 5; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.6;
                double offsetY = random.nextDouble() * 1.5 + 0.3;
                double offsetZ = (random.nextDouble() - 0.5) * 0.6;
                double speedX = (random.nextDouble() - 0.5) * 0.15;
                double speedY = random.nextDouble() * 0.05 + 0.02;
                double speedZ = (random.nextDouble() - 0.5) * 0.15;
                level.addParticle(ParticleTypes.LARGE_SMOKE, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
            // イカ墨パーティクルで重量感を追加
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.4;
                double offsetY = random.nextDouble() * 1.2 + 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.4;
                double speedX = (random.nextDouble() - 0.5) * 0.2;
                double speedY = -0.02;
                double speedZ = (random.nextDouble() - 0.5) * 0.2;
                level.addParticle(ParticleTypes.SQUID_INK, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
        } else if (type == 2) {
            // 軽ガス - 持続・少量消費時のふわふわエフェクト
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = random.nextDouble() * 1.5 + 0.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;
                double speedX = (random.nextDouble() - 0.5) * 0.03;
                double speedY = random.nextDouble() * 0.05 + 0.01;
                double speedZ = (random.nextDouble() - 0.5) * 0.03;
                level.addParticle(ParticleTypes.SMOKE, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
        }
    }

    public static void handleSpawnParticles(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null) {
            Random random = new Random();

            // 爆発パーティクル
            for (int i = 0; i < 50; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 2;
                double offsetY = (random.nextDouble() - 0.5) * 2;
                double offsetZ = (random.nextDouble() - 0.5) * 2;
                level.addParticle(ParticleTypes.EXPLOSION, x + offsetX, y + offsetY + 1, z + offsetZ, 0, 0, 0);
            }

            // 白色発光パーティクル
            for (int i = 0; i < 100; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 3;
                double offsetY = random.nextDouble() * 2;
                double offsetZ = (random.nextDouble() - 0.5) * 3;
                double speedX = (random.nextDouble() - 0.5) * 0.5;
                double speedY = random.nextDouble() * 0.5;
                double speedZ = (random.nextDouble() - 0.5) * 0.5;
                level.addParticle(ParticleTypes.END_ROD, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }

            // ワールドの建築限界高度までの白色ビーム
            int maxBuildHeight = level.getMaxBuildHeight();
            double maxHeight = maxBuildHeight - y;
            for (double height = 0; height < maxHeight; height += 0.5) {
                level.addParticle(ParticleTypes.END_ROD, x, y + height, z, 0, 0.1, 0);
            }

            // フラッシュエフェクト
            for (int i = 0; i < 30; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 1;
                double offsetZ = (random.nextDouble() - 0.5) * 1;
                level.addParticle(ParticleTypes.FLASH, x + offsetX, y + 1, z + offsetZ, 0, 0, 0);
            }
        }
    }
}
