package com.furasuta.emergencyescape.util;

import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last hit position and body part for entities.
 * Used to get accurate damage location from Mixin.
 */
public class HitPositionTracker {

    public static class HitInfo {
        public final Vec3 hitPosition;
        public final Vec3 attackOrigin;
        public final Vec3 attackDirection;
        public final BodyPartHitbox.BodyPart bodyPart;
        public final String source;

        public HitInfo(Vec3 hitPosition, Vec3 attackOrigin, Vec3 attackDirection,
                       BodyPartHitbox.BodyPart bodyPart, String source) {
            this.hitPosition = hitPosition;
            this.attackOrigin = attackOrigin;
            this.attackDirection = attackDirection;
            this.bodyPart = bodyPart;
            this.source = source;
        }
    }

    // Map of entity UUID to last hit info
    private static final Map<UUID, HitInfo> lastHitInfo = new ConcurrentHashMap<>();

    // Map of entity UUID to last damage timestamp (to expire old data)
    private static final Map<UUID, Long> hitTimestamps = new ConcurrentHashMap<>();

    // Hit data expires after 100ms
    private static final long EXPIRY_TIME_MS = 100;

    public static void setLastHitInfo(UUID entityId, HitInfo info) {
        lastHitInfo.put(entityId, info);
        hitTimestamps.put(entityId, System.currentTimeMillis());
    }

    public static HitInfo getLastHitInfo(UUID entityId) {
        Long timestamp = hitTimestamps.get(entityId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < EXPIRY_TIME_MS) {
            return lastHitInfo.get(entityId);
        }
        // Clear expired data
        lastHitInfo.remove(entityId);
        hitTimestamps.remove(entityId);
        return null;
    }

    public static void clearHitInfo(UUID entityId) {
        lastHitInfo.remove(entityId);
        hitTimestamps.remove(entityId);
    }

    // Legacy methods for compatibility
    public static void setLastHitPosition(UUID entityId, Vec3 position) {
        setLastHitInfo(entityId, new HitInfo(position, null, null, null, "legacy"));
    }

    public static Vec3 getLastHitPosition(UUID entityId) {
        HitInfo info = getLastHitInfo(entityId);
        return info != null ? info.hitPosition : null;
    }
}
