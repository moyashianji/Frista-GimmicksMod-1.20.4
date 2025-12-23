package com.furasuta.emergencyescape.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Defines hitboxes for different body parts of a player.
 * Based on player model dimensions.
 *
 * Player model reference (standing):
 * - Total height: 1.8 blocks
 * - Head: 8 pixels = 0.5 blocks (top 0.5 blocks, Y: 1.3 - 1.8)
 * - Body: 12 pixels = 0.75 blocks (Y: 0.55 - 1.3)
 * - Legs: 12 pixels = 0.75 blocks (bottom, Y: 0 - 0.75, but we use 0 - 0.55 for overlap)
 *
 * When sneaking:
 * - Total height: 1.5 blocks
 * - Proportions scaled accordingly
 */
public class BodyPartHitbox {

    public enum BodyPart {
        HEAD,
        BODY,
        LEGS,
        NONE
    }

    // Standing player proportions (relative to total height 1.8)
    private static final double HEAD_TOP = 1.0;      // 100% of height
    private static final double HEAD_BOTTOM = 0.72;  // ~72% (1.3/1.8)
    private static final double BODY_BOTTOM = 0.31;  // ~31% (0.55/1.8)
    // LEGS: 0 - 0.31

    // Head width is narrower than body
    private static final double HEAD_WIDTH = 0.5;    // 8 pixels = 0.5 blocks
    private static final double BODY_WIDTH = 0.6;    // Player width

    /**
     * Get the AABB for a specific body part of a player.
     */
    public static AABB getBodyPartAABB(Player player, BodyPart part) {
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double height = player.getBbHeight();
        double width = player.getBbWidth();

        switch (part) {
            case HEAD:
                double headBottom = playerY + height * HEAD_BOTTOM;
                double headTop = playerY + height * HEAD_TOP;
                double headHalfWidth = HEAD_WIDTH / 2;
                return new AABB(
                    playerX - headHalfWidth, headBottom, playerZ - headHalfWidth,
                    playerX + headHalfWidth, headTop, playerZ + headHalfWidth
                );

            case BODY:
                double bodyBottom = playerY + height * BODY_BOTTOM;
                double bodyTop = playerY + height * HEAD_BOTTOM;
                double bodyHalfWidth = BODY_WIDTH / 2;
                return new AABB(
                    playerX - bodyHalfWidth, bodyBottom, playerZ - bodyHalfWidth,
                    playerX + bodyHalfWidth, bodyTop, playerZ + bodyHalfWidth
                );

            case LEGS:
                double legsBottom = playerY;
                double legsTop = playerY + height * BODY_BOTTOM;
                double legsHalfWidth = width / 2;
                return new AABB(
                    playerX - legsHalfWidth, legsBottom, playerZ - legsHalfWidth,
                    playerX + legsHalfWidth, legsTop, playerZ + legsHalfWidth
                );

            default:
                return player.getBoundingBox();
        }
    }

    /**
     * Determine which body part was hit by a ray from attacker to player.
     *
     * @param player The player being hit
     * @param attackOrigin The origin point of the attack (attacker's eye position)
     * @param attackDirection The direction of the attack (normalized look vector)
     * @return The body part that was hit, or NONE if no hit
     */
    public static BodyPart getHitBodyPart(Player player, Vec3 attackOrigin, Vec3 attackDirection) {
        // Check each body part from most specific (head) to least specific
        AABB headBox = getBodyPartAABB(player, BodyPart.HEAD);
        AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY);
        AABB legsBox = getBodyPartAABB(player, BodyPart.LEGS);

        // Perform ray-box intersection tests
        Optional<Vec3> headHit = headBox.clip(attackOrigin, attackOrigin.add(attackDirection.scale(10)));
        Optional<Vec3> bodyHit = bodyBox.clip(attackOrigin, attackOrigin.add(attackDirection.scale(10)));
        Optional<Vec3> legsHit = legsBox.clip(attackOrigin, attackOrigin.add(attackDirection.scale(10)));

        // Find the closest hit
        double headDist = headHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double bodyDist = bodyHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double legsDist = legsHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);

        // Return the closest hit body part
        if (headDist <= bodyDist && headDist <= legsDist && headHit.isPresent()) {
            return BodyPart.HEAD;
        } else if (bodyDist <= legsDist && bodyHit.isPresent()) {
            return BodyPart.BODY;
        } else if (legsHit.isPresent()) {
            return BodyPart.LEGS;
        }

        return BodyPart.NONE;
    }

    /**
     * Determine which body part was hit by a point (e.g., projectile impact point).
     *
     * @param player The player being hit
     * @param hitPoint The point where the hit occurred
     * @return The body part at that point, or BODY as default
     */
    public static BodyPart getBodyPartAtPoint(Player player, Vec3 hitPoint) {
        AABB headBox = getBodyPartAABB(player, BodyPart.HEAD);
        AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY);
        AABB legsBox = getBodyPartAABB(player, BodyPart.LEGS);

        // Check which hitbox contains the point
        // Use a small expansion to account for edge cases
        if (headBox.inflate(0.1).contains(hitPoint)) {
            return BodyPart.HEAD;
        } else if (bodyBox.inflate(0.1).contains(hitPoint)) {
            return BodyPart.BODY;
        } else if (legsBox.inflate(0.1).contains(hitPoint)) {
            return BodyPart.LEGS;
        }

        // Fallback: use Y position relative to player
        double relativeY = (hitPoint.y - player.getY()) / player.getBbHeight();
        if (relativeY >= HEAD_BOTTOM) {
            return BodyPart.HEAD;
        } else if (relativeY >= BODY_BOTTOM) {
            return BodyPart.BODY;
        } else {
            return BodyPart.LEGS;
        }
    }
}
