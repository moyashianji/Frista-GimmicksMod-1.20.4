package com.furasuta.emergencyescape.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーモデルパーツの位置・回転をサーバー側にキャッシュする。
 * クライアントから同期されたデータを使い、ポーズを考慮した被弾判定を行う。
 */
public class PlayerModelPartCache {

    private static final Map<UUID, CachedPlayerParts> playerPartsCache = new ConcurrentHashMap<>();

    /** キャッシュの有効期限 (500ms = 10tick) */
    private static final long CACHE_EXPIRY_MS = 500;

    /** パーツインデックス */
    public static final int PART_HEAD = 0;
    public static final int PART_BODY = 1;
    public static final int PART_LEFT_ARM = 2;
    public static final int PART_RIGHT_ARM = 3;
    public static final int PART_LEFT_LEG = 4;
    public static final int PART_RIGHT_LEG = 5;

    /**
     * プレイヤーのパーツデータを更新する。
     * @param playerUUID プレイヤーのUUID
     * @param partData 36個のfloat配列 (6パーツ x 6値)
     */
    public static void updatePlayerParts(UUID playerUUID, float[] partData) {
        CachedPlayerParts cached = new CachedPlayerParts(partData, System.currentTimeMillis());
        playerPartsCache.put(playerUUID, cached);
    }

    /**
     * プレイヤーのキャッシュ済みパーツデータを取得する。期限切れの場合はnullを返す。
     */
    public static CachedPlayerParts getPlayerParts(UUID playerUUID) {
        CachedPlayerParts cached = playerPartsCache.get(playerUUID);
        if (cached == null) {
            return null;
        }

        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS) {
            return null;
        }

        return cached;
    }

    /** 有効なデータが存在するかを返す。 */
    public static boolean hasValidData(UUID playerUUID) {
        return getPlayerParts(playerUUID) != null;
    }

    /** プレイヤーのキャッシュを削除する（切断時など）。 */
    public static void removePlayer(UUID playerUUID) {
        playerPartsCache.remove(playerUUID);
    }

    /** 全キャッシュをクリアする。 */
    public static void clearAll() {
        playerPartsCache.clear();
    }

    /**
     * プレイヤー1人分のキャッシュ済みパーツデータ。
     */
    public static class CachedPlayerParts {
        private final PartTransform[] parts;
        private final long timestamp;

        public CachedPlayerParts(float[] data, long timestamp) {
            this.timestamp = timestamp;
            this.parts = new PartTransform[6];

            for (int i = 0; i < 6; i++) {
                int offset = i * 6;
                parts[i] = new PartTransform(
                    data[offset],
                    data[offset + 1],
                    data[offset + 2],
                    data[offset + 3],
                    data[offset + 4],
                    data[offset + 5]
                );
            }
        }

        public PartTransform getHead() { return parts[PART_HEAD]; }
        public PartTransform getBody() { return parts[PART_BODY]; }
        public PartTransform getLeftArm() { return parts[PART_LEFT_ARM]; }
        public PartTransform getRightArm() { return parts[PART_RIGHT_ARM]; }
        public PartTransform getLeftLeg() { return parts[PART_LEFT_LEG]; }
        public PartTransform getRightLeg() { return parts[PART_RIGHT_LEG]; }

        public PartTransform getPart(int index) {
            if (index < 0 || index >= parts.length) {
                return null;
            }
            return parts[index];
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * モデルパーツの位置オフセットと回転を保持する。回転はラジアン単位。
     */
    public static class PartTransform {
        public final float x, y, z;
        public final float xRot, yRot, zRot;

        public PartTransform(float x, float y, float z, float xRot, float yRot, float zRot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
        }

        /**
         * デフォルトポーズから有意な回転があるかを返す。
         */
        public boolean hasSignificantRotation() {
            float threshold = 0.1f; // 約5.7度
            return Math.abs(xRot) > threshold || Math.abs(yRot) > threshold || Math.abs(zRot) > threshold;
        }

        /**
         * ピボット点を中心にオイラー回転(X→Y→Z)を適用する。
         */
        public Vec3 rotatePoint(Vec3 point, Vec3 pivot) {
            double px = point.x - pivot.x;
            double py = point.y - pivot.y;
            double pz = point.z - pivot.z;

            // X回転 (ピッチ)
            double cosX = Math.cos(xRot);
            double sinX = Math.sin(xRot);
            double y1 = py * cosX - pz * sinX;
            double z1 = py * sinX + pz * cosX;
            py = y1;
            pz = z1;

            // Y回転 (ヨー)
            double cosY = Math.cos(yRot);
            double sinY = Math.sin(yRot);
            double x1 = px * cosY + pz * sinY;
            double z2 = -px * sinY + pz * cosY;
            px = x1;
            pz = z2;

            // Z回転 (ロール)
            double cosZ = Math.cos(zRot);
            double sinZ = Math.sin(zRot);
            double x2 = px * cosZ - py * sinZ;
            double y2 = px * sinZ + py * cosZ;
            px = x2;
            py = y2;

            return new Vec3(px + pivot.x, py + pivot.y, pz + pivot.z);
        }

        /**
         * 位置オフセットをブロック単位のVec3として返す（モデルピクセル / 16）。
         */
        public Vec3 getPositionOffset() {
            return new Vec3(x / 16.0, y / 16.0, z / 16.0);
        }

        @Override
        public String toString() {
            return String.format("PartTransform[pos=(%.2f,%.2f,%.2f), rot=(%.2f,%.2f,%.2f)]",
                x, y, z, Math.toDegrees(xRot), Math.toDegrees(yRot), Math.toDegrees(zRot));
        }
    }
}
