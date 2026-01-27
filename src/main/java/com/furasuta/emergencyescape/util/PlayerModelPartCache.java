package com.furasuta.emergencyescape.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side cache for player model part positions and rotations.
 * This data is synced from clients to allow accurate pose-aware hit detection.
 */
public class PlayerModelPartCache {

    /**
     * Stores model part data for each player.
     * Key: Player UUID
     * Value: PartData array containing position and rotation for each body part
     */
    private static final Map<UUID, CachedPlayerParts> playerPartsCache = new ConcurrentHashMap<>();

    /**
     * Time in milliseconds after which cached data is considered stale.
     * If data is older than this, we fall back to default pose calculations.
     */
    private static final long CACHE_EXPIRY_MS = 500; // 500ms = 10 ticks

    /**
     * Part indices for the data array.
     */
    public static final int PART_HEAD = 0;
    public static final int PART_BODY = 1;
    public static final int PART_LEFT_ARM = 2;
    public static final int PART_RIGHT_ARM = 3;
    public static final int PART_LEFT_LEG = 4;
    public static final int PART_RIGHT_LEG = 5;

    /**
     * Update the cached part data for a player.
     * Called when receiving a SyncModelPartPacket from the client.
     *
     * @param playerUUID The player's UUID
     * @param partData Array of 36 floats (6 parts × 6 values each)
     */
    public static void updatePlayerParts(UUID playerUUID, float[] partData) {
        CachedPlayerParts cached = new CachedPlayerParts(partData, System.currentTimeMillis());
        playerPartsCache.put(playerUUID, cached);
    }

    /**
     * Get the cached part data for a player.
     *
     * @param playerUUID The player's UUID
     * @return The cached data, or null if not available or expired
     */
    public static CachedPlayerParts getPlayerParts(UUID playerUUID) {
        CachedPlayerParts cached = playerPartsCache.get(playerUUID);
        if (cached == null) {
            return null;
        }

        // Check if data is still fresh
        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS) {
            return null; // Data is stale
        }

        return cached;
    }

    /**
     * Check if we have valid (non-stale) data for a player.
     */
    public static boolean hasValidData(UUID playerUUID) {
        return getPlayerParts(playerUUID) != null;
    }

    /**
     * Remove cached data for a player (e.g., when they disconnect).
     */
    public static void removePlayer(UUID playerUUID) {
        playerPartsCache.remove(playerUUID);
    }

    /**
     * Clear all cached data.
     */
    public static void clearAll() {
        playerPartsCache.clear();
    }

    /**
     * Holds cached part data for a single player.
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
                    data[offset],      // x
                    data[offset + 1],  // y
                    data[offset + 2],  // z
                    data[offset + 3],  // xRot
                    data[offset + 4],  // yRot
                    data[offset + 5]   // zRot
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
     * Represents the transform (position offset and rotation) of a model part.
     * Rotations are in radians, matching ModelPart's format.
     */
    public static class PartTransform {
        public final float x, y, z;      // Position offset in model pixels
        public final float xRot, yRot, zRot;  // Rotation in radians

        public PartTransform(float x, float y, float z, float xRot, float yRot, float zRot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
        }

        /**
         * Check if this part has significant rotation from default pose.
         * Used to determine if we need to apply rotation transforms for hitbox.
         */
        public boolean hasSignificantRotation() {
            float threshold = 0.1f; // ~5.7 degrees
            return Math.abs(xRot) > threshold || Math.abs(yRot) > threshold || Math.abs(zRot) > threshold;
        }

        /**
         * Apply this rotation to a point around a pivot point.
         * Uses standard Euler rotation order: X, then Y, then Z.
         *
         * @param point The point to rotate (in world coordinates)
         * @param pivot The pivot point (in world coordinates)
         * @return The rotated point
         */
        public Vec3 rotatePoint(Vec3 point, Vec3 pivot) {
            // Translate to pivot origin
            double px = point.x - pivot.x;
            double py = point.y - pivot.y;
            double pz = point.z - pivot.z;

            // Apply X rotation (pitch)
            double cosX = Math.cos(xRot);
            double sinX = Math.sin(xRot);
            double y1 = py * cosX - pz * sinX;
            double z1 = py * sinX + pz * cosX;
            py = y1;
            pz = z1;

            // Apply Y rotation (yaw)
            double cosY = Math.cos(yRot);
            double sinY = Math.sin(yRot);
            double x1 = px * cosY + pz * sinY;
            double z2 = -px * sinY + pz * cosY;
            px = x1;
            pz = z2;

            // Apply Z rotation (roll)
            double cosZ = Math.cos(zRot);
            double sinZ = Math.sin(zRot);
            double x2 = px * cosZ - py * sinZ;
            double y2 = px * sinZ + py * cosZ;
            px = x2;
            py = y2;

            // Translate back
            return new Vec3(px + pivot.x, py + pivot.y, pz + pivot.z);
        }

        /**
         * Get the position offset as a Vec3.
         * Converts from model pixels to blocks (÷16).
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
