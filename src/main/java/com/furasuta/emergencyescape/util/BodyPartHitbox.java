package com.furasuta.emergencyescape.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Defines hitboxes for different body parts of a player.
 * Based on exact values from Minecraft's HumanoidModel.createMesh().
 *
 * ====== HumanoidModel Cube Definitions (from Minecraft source) ======
 *
 * Model coordinate system: 16 pixels = 1 block
 * Model origin: at the neck/body junction (Y=0 in model space)
 * Player feet are at model Y=24 (24/16 = 1.5 blocks below origin)
 *
 * HEAD:
 *   addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
 *   PartPose.offset(0.0F, 0.0F, 0.0F)
 *   → Model Y: -8 to 0 (8 pixels tall)
 *   → Size: 8x8x8 pixels = 0.5x0.5x0.5 blocks
 *
 * BODY:
 *   addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
 *   PartPose.offset(0.0F, 0.0F, 0.0F)
 *   → Model Y: 0 to 12 (12 pixels tall)
 *   → Size: 8x12x4 pixels = 0.5x0.75x0.25 blocks
 *
 * RIGHT_LEG / LEFT_LEG:
 *   addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
 *   PartPose.offset(±1.9F, 12.0F, 0.0F)
 *   → Model Y: 12 to 24 (12 pixels tall, offset Y=12)
 *   → Size: 4x12x4 pixels = 0.25x0.75x0.25 blocks
 *
 * RIGHT_ARM / LEFT_ARM:
 *   addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
 *   PartPose.offset(±5.0F, 2.0F, 0.0F)
 *   → Model Y: 0 to 10 (from shoulder at Y=2-2=0 to Y=2+10=12)
 *   → Size: 4x12x4 pixels = 0.25x0.75x0.25 blocks
 *
 * ====== World Coordinate Conversion ======
 *
 * Standing player (height = 1.8 blocks):
 *   Model Y=24 → World Y = player.getY() (feet)
 *   Model Y=0  → World Y = player.getY() + 1.5 (neck)
 *   Model Y=-8 → World Y = player.getY() + 1.8 (head top)
 *
 * Conversion formula: worldOffset = (24 - modelY) / 32 * playerHeight
 *
 * Body part boundaries (standing, height=1.8):
 *   HEAD:  Y = 1.35 to 1.8 (model -8 to 0)   → 75% to 100%
 *   BODY:  Y = 0.675 to 1.35 (model 0 to 12) → 37.5% to 75%
 *   ARMS:  Y = 0.675 to 1.35 (model 0 to 12) → 37.5% to 75%
 *   LEGS:  Y = 0 to 0.675 (model 12 to 24)   → 0% to 37.5%
 *
 * Sneaking player (height = 1.5 blocks):
 *   All proportions scaled by 1.5/1.8 = 0.833
 */
public class BodyPartHitbox {

    public enum BodyPart {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEGS,
        NONE
    }

    // ====== Exact values from HumanoidModel (in pixels, 16 pixels = 1 block) ======

    // Head: addBox(-4, -8, -4, 8, 8, 8) at offset(0, 0, 0)
    private static final float HEAD_MIN_Y = -8.0f;   // Top of head (model coords)
    private static final float HEAD_MAX_Y = 0.0f;    // Bottom of head (neck)
    private static final float HEAD_HALF_WIDTH = 4.0f;  // 8 pixels wide / 2
    private static final float HEAD_HALF_DEPTH = 4.0f;

    // Body: addBox(-4, 0, -2, 8, 12, 4) at offset(0, 0, 0)
    private static final float BODY_MIN_Y = 0.0f;    // Top of body (neck)
    private static final float BODY_MAX_Y = 12.0f;   // Bottom of body
    private static final float BODY_HALF_WIDTH = 4.0f;  // 8 pixels wide / 2
    private static final float BODY_HALF_DEPTH = 2.0f;  // 4 pixels deep / 2

    // Legs: addBox(-2, 0, -2, 4, 12, 4) at offset(±1.9, 12, 0)
    private static final float LEG_OFFSET_Y = 12.0f;
    private static final float LEG_MIN_Y = 12.0f;    // Top of legs
    private static final float LEG_MAX_Y = 24.0f;    // Bottom of legs (feet)
    private static final float LEG_HALF_WIDTH = 2.0f;   // 4 pixels wide / 2
    private static final float LEG_OFFSET_X = 1.9f;     // Leg X offset from center

    // Arms: addBox(-3, -2, -2, 4, 12, 4) at offset(±5, 2, 0)
    private static final float ARM_OFFSET_Y = 2.0f;
    private static final float ARM_MIN_Y = 0.0f;     // -2 + 2 = 0 (shoulder)
    private static final float ARM_MAX_Y = 12.0f;    // 10 + 2 = 12 (but model says 12 tall from -2)
    private static final float ARM_HALF_WIDTH = 2.0f;   // 4 pixels wide / 2
    private static final float ARM_OFFSET_X = 5.0f;     // Arm X offset from center

    // Model total height in pixels
    private static final float MODEL_TOTAL_HEIGHT = 32.0f;  // -8 to 24

    // Model Y coordinate at feet (bottom of legs)
    private static final float MODEL_FEET_Y = 24.0f;

    // Pixels to blocks conversion
    private static final float PIXELS_PER_BLOCK = 16.0f;

    /**
     * Convert model Y coordinate to world Y offset from player's feet.
     * Model Y=-8 (head top) → 1.8 blocks above feet
     * Model Y=24 (feet) → 0 blocks above feet
     *
     * @param modelY The Y coordinate in model space
     * @param playerHeight The player's current bounding box height
     * @return Y offset from player's feet in blocks
     */
    private static double modelYToWorldOffset(float modelY, double playerHeight) {
        // Model coordinate system:
        // - Model Y=-8 is the TOP of the head
        // - Model Y=24 is the FEET (bottom)
        // - Total range: 32 pixels
        //
        // We want to convert to world offset from feet:
        // - Feet (modelY=24) → offset 0
        // - Head top (modelY=-8) → offset = playerHeight (1.8 for standing)
        //
        // Formula: normalizedY = (24 - modelY) / 32
        // This gives: 0 at feet, 1 at head top
        double normalizedY = (MODEL_FEET_Y - modelY) / MODEL_TOTAL_HEIGHT;
        return normalizedY * playerHeight;
    }

    /**
     * Get the AABB for a specific body part of a player, using exact HumanoidModel dimensions.
     */
    public static AABB getBodyPartAABB(Player player, BodyPart part) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        // Scale factor for current player height (sneaking etc.)
        // Standing = 1.8, Sneaking = 1.5, Swimming = 0.6
        double scale = height / 1.8;

        switch (part) {
            case HEAD: {
                // Head: model Y -8 to 0
                double top = py + modelYToWorldOffset(HEAD_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(HEAD_MAX_Y, height);
                double halfW = (HEAD_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                double halfD = (HEAD_HALF_DEPTH / PIXELS_PER_BLOCK) * scale;
                return new AABB(
                    px - halfW, bottom, pz - halfD,
                    px + halfW, top, pz + halfD
                );
            }

            case BODY: {
                // Body: model Y 0 to 12
                double top = py + modelYToWorldOffset(BODY_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(BODY_MAX_Y, height);
                double halfW = (BODY_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                double halfD = (BODY_HALF_DEPTH / PIXELS_PER_BLOCK) * scale;
                return new AABB(
                    px - halfW, bottom, pz - halfD,
                    px + halfW, top, pz + halfD
                );
            }

            case LEFT_ARM: {
                // Left arm: offset X=+5, model Y 0 to 12
                double top = py + modelYToWorldOffset(ARM_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(ARM_MAX_Y, height);
                double offsetX = (ARM_OFFSET_X / PIXELS_PER_BLOCK) * scale;
                double halfW = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                double halfD = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;  // Arms are 4x4 in XZ
                return new AABB(
                    px + offsetX - halfW, bottom, pz - halfD,
                    px + offsetX + halfW, top, pz + halfD
                );
            }

            case RIGHT_ARM: {
                // Right arm: offset X=-5, model Y 0 to 12
                double top = py + modelYToWorldOffset(ARM_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(ARM_MAX_Y, height);
                double offsetX = (ARM_OFFSET_X / PIXELS_PER_BLOCK) * scale;
                double halfW = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                double halfD = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                return new AABB(
                    px - offsetX - halfW, bottom, pz - halfD,
                    px - offsetX + halfW, top, pz + halfD
                );
            }

            case LEGS: {
                // Legs: model Y 12 to 24 (combined both legs for simpler hit detection)
                double top = py + modelYToWorldOffset(LEG_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(LEG_MAX_Y, height);
                // Combined width covers both legs
                double totalWidth = ((LEG_OFFSET_X + LEG_HALF_WIDTH) / PIXELS_PER_BLOCK) * scale;
                double halfD = (LEG_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                return new AABB(
                    px - totalWidth, bottom, pz - halfD,
                    px + totalWidth, top, pz + halfD
                );
            }

            default:
                return player.getBoundingBox();
        }
    }

    /**
     * Get individual leg AABBs for precise hit detection.
     */
    public static AABB getLeftLegAABB(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();
        double scale = height / 1.8;

        double top = py + modelYToWorldOffset(LEG_MIN_Y, height);
        double bottom = py + modelYToWorldOffset(LEG_MAX_Y, height);
        double offsetX = (LEG_OFFSET_X / PIXELS_PER_BLOCK) * scale;
        double halfW = (LEG_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
        double halfD = (LEG_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;

        return new AABB(
            px + offsetX - halfW, bottom, pz - halfD,
            px + offsetX + halfW, top, pz + halfD
        );
    }

    public static AABB getRightLegAABB(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();
        double scale = height / 1.8;

        double top = py + modelYToWorldOffset(LEG_MIN_Y, height);
        double bottom = py + modelYToWorldOffset(LEG_MAX_Y, height);
        double offsetX = (LEG_OFFSET_X / PIXELS_PER_BLOCK) * scale;
        double halfW = (LEG_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
        double halfD = (LEG_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;

        return new AABB(
            px - offsetX - halfW, bottom, pz - halfD,
            px - offsetX + halfW, top, pz + halfD
        );
    }

    /**
     * Determine which body part was hit by a ray from attacker to player.
     * Uses precise hitbox intersection with priority: HEAD > BODY > ARMS > LEGS
     *
     * @param player The player being hit
     * @param attackOrigin The origin point of the attack (attacker's eye position)
     * @param attackDirection The direction of the attack (normalized look vector)
     * @return The body part that was hit, or NONE if no hit
     */
    public static BodyPart getHitBodyPart(Player player, Vec3 attackOrigin, Vec3 attackDirection) {
        Vec3 rayEnd = attackOrigin.add(attackDirection.scale(10));

        // Get all hitboxes
        AABB headBox = getBodyPartAABB(player, BodyPart.HEAD);
        AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY);
        AABB leftArmBox = getBodyPartAABB(player, BodyPart.LEFT_ARM);
        AABB rightArmBox = getBodyPartAABB(player, BodyPart.RIGHT_ARM);
        AABB legsBox = getBodyPartAABB(player, BodyPart.LEGS);

        // Perform ray-box intersection tests
        Optional<Vec3> headHit = headBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> bodyHit = bodyBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> leftArmHit = leftArmBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> rightArmHit = rightArmBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> legsHit = legsBox.clip(attackOrigin, rayEnd);

        // Calculate distances
        double headDist = headHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double bodyDist = bodyHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double leftArmDist = leftArmHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double rightArmDist = rightArmHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double legsDist = legsHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);

        // Find minimum distance and corresponding body part
        double minDist = Double.MAX_VALUE;
        BodyPart result = BodyPart.NONE;

        if (headHit.isPresent() && headDist < minDist) {
            minDist = headDist;
            result = BodyPart.HEAD;
        }
        if (bodyHit.isPresent() && bodyDist < minDist) {
            minDist = bodyDist;
            result = BodyPart.BODY;
        }
        if (leftArmHit.isPresent() && leftArmDist < minDist) {
            minDist = leftArmDist;
            result = BodyPart.LEFT_ARM;
        }
        if (rightArmHit.isPresent() && rightArmDist < minDist) {
            minDist = rightArmDist;
            result = BodyPart.RIGHT_ARM;
        }
        if (legsHit.isPresent() && legsDist < minDist) {
            result = BodyPart.LEGS;
        }

        return result;
    }

    /**
     * Determine which body part was hit by a point (e.g., projectile impact point).
     *
     * @param player The player being hit
     * @param hitPoint The point where the hit occurred
     * @return The body part at that point, or BODY as default
     */
    public static BodyPart getBodyPartAtPoint(Player player, Vec3 hitPoint) {
        // Check each hitbox with small inflation for edge cases
        double inflate = 0.05;

        AABB headBox = getBodyPartAABB(player, BodyPart.HEAD).inflate(inflate);
        if (headBox.contains(hitPoint)) {
            return BodyPart.HEAD;
        }

        AABB leftArmBox = getBodyPartAABB(player, BodyPart.LEFT_ARM).inflate(inflate);
        if (leftArmBox.contains(hitPoint)) {
            return BodyPart.LEFT_ARM;
        }

        AABB rightArmBox = getBodyPartAABB(player, BodyPart.RIGHT_ARM).inflate(inflate);
        if (rightArmBox.contains(hitPoint)) {
            return BodyPart.RIGHT_ARM;
        }

        AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY).inflate(inflate);
        if (bodyBox.contains(hitPoint)) {
            return BodyPart.BODY;
        }

        AABB legsBox = getBodyPartAABB(player, BodyPart.LEGS).inflate(inflate);
        if (legsBox.contains(hitPoint)) {
            return BodyPart.LEGS;
        }

        // Fallback: use Y position relative to player height
        double relativeY = (hitPoint.y - player.getY()) / player.getBbHeight();

        // Based on HumanoidModel proportions (corrected):
        // Head: 75% - 100% (model Y -8 to 0)
        // Body: 37.5% - 75% (model Y 0 to 12)
        // Legs: 0% - 37.5% (model Y 12 to 24)
        if (relativeY >= 0.75) {
            return BodyPart.HEAD;
        } else if (relativeY >= 0.375) {
            return BodyPart.BODY;
        } else {
            return BodyPart.LEGS;
        }
    }

    /**
     * Simplify body part for damage calculation.
     * Converts LEFT_ARM/RIGHT_ARM to BODY since arms are usually treated as body damage.
     */
    public static BodyPart simplifyBodyPart(BodyPart part) {
        return switch (part) {
            case LEFT_ARM, RIGHT_ARM -> BodyPart.BODY;
            default -> part;
        };
    }

    /**
     * Check if player is in a pose that affects hitbox (sneaking, swimming, etc.)
     */
    public static boolean isInAlteredPose(Player player) {
        return player.isCrouching() || player.isSwimming() || player.isFallFlying();
    }

    /**
     * Get debug info about body part boundaries for current player state.
     */
    public static String getDebugInfo(Player player) {
        double height = player.getBbHeight();
        return String.format(
            "Player height: %.2f | HEAD: %.2f-%.2f | BODY: %.2f-%.2f | LEGS: %.2f-%.2f",
            height,
            modelYToWorldOffset(HEAD_MIN_Y, height),
            modelYToWorldOffset(HEAD_MAX_Y, height),
            modelYToWorldOffset(BODY_MIN_Y, height),
            modelYToWorldOffset(BODY_MAX_Y, height),
            modelYToWorldOffset(LEG_MIN_Y, height),
            modelYToWorldOffset(LEG_MAX_Y, height)
        );
    }

    // ====== Pose-Aware Hit Detection using synced ModelPart data ======

    /**
     * Determine which body part was hit using synced model part data for accurate pose detection.
     * Falls back to static calculations if no synced data is available.
     *
     * @param player The player being hit
     * @param attackOrigin The origin point of the attack (attacker's eye position)
     * @param attackDirection The direction of the attack (normalized look vector)
     * @return The body part that was hit, or NONE if no hit
     */
    public static BodyPart getHitBodyPartWithPose(Player player, Vec3 attackOrigin, Vec3 attackDirection) {
        UUID playerUUID = player.getUUID();
        PlayerModelPartCache.CachedPlayerParts cachedParts = PlayerModelPartCache.getPlayerParts(playerUUID);

        // If no synced data, fall back to static calculation
        if (cachedParts == null) {
            return getHitBodyPart(player, attackOrigin, attackDirection);
        }

        Vec3 rayEnd = attackOrigin.add(attackDirection.scale(10));
        double minDist = Double.MAX_VALUE;
        BodyPart result = BodyPart.NONE;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();
        double scale = height / 1.8;

        // Check HEAD with rotation
        PlayerModelPartCache.PartTransform headTransform = cachedParts.getHead();
        AABB headBox = getRotatedBodyPartAABB(player, BodyPart.HEAD, headTransform, scale);
        Optional<Vec3> headHit = headBox.clip(attackOrigin, rayEnd);
        if (headHit.isPresent()) {
            double dist = headHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.HEAD;
            }
        }

        // Check BODY with rotation
        PlayerModelPartCache.PartTransform bodyTransform = cachedParts.getBody();
        AABB bodyBox = getRotatedBodyPartAABB(player, BodyPart.BODY, bodyTransform, scale);
        Optional<Vec3> bodyHit = bodyBox.clip(attackOrigin, rayEnd);
        if (bodyHit.isPresent()) {
            double dist = bodyHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.BODY;
            }
        }

        // Check LEFT ARM with rotation
        PlayerModelPartCache.PartTransform leftArmTransform = cachedParts.getLeftArm();
        AABB leftArmBox = getRotatedBodyPartAABB(player, BodyPart.LEFT_ARM, leftArmTransform, scale);
        Optional<Vec3> leftArmHit = leftArmBox.clip(attackOrigin, rayEnd);
        if (leftArmHit.isPresent()) {
            double dist = leftArmHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.LEFT_ARM;
            }
        }

        // Check RIGHT ARM with rotation
        PlayerModelPartCache.PartTransform rightArmTransform = cachedParts.getRightArm();
        AABB rightArmBox = getRotatedBodyPartAABB(player, BodyPart.RIGHT_ARM, rightArmTransform, scale);
        Optional<Vec3> rightArmHit = rightArmBox.clip(attackOrigin, rayEnd);
        if (rightArmHit.isPresent()) {
            double dist = rightArmHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.RIGHT_ARM;
            }
        }

        // Check LEGS with rotation (combined left and right)
        PlayerModelPartCache.PartTransform leftLegTransform = cachedParts.getLeftLeg();
        PlayerModelPartCache.PartTransform rightLegTransform = cachedParts.getRightLeg();

        // Left leg
        AABB leftLegBox = getRotatedLegAABB(player, true, leftLegTransform, scale);
        Optional<Vec3> leftLegHit = leftLegBox.clip(attackOrigin, rayEnd);
        if (leftLegHit.isPresent()) {
            double dist = leftLegHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.LEGS;
            }
        }

        // Right leg
        AABB rightLegBox = getRotatedLegAABB(player, false, rightLegTransform, scale);
        Optional<Vec3> rightLegHit = rightLegBox.clip(attackOrigin, rayEnd);
        if (rightLegHit.isPresent()) {
            double dist = rightLegHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                result = BodyPart.LEGS;
            }
        }

        return result;
    }

    /**
     * Get a rotated AABB for a body part based on synced transform data.
     * For simplicity, this creates an oriented bounding box approximation.
     *
     * @param player The player
     * @param part The body part
     * @param transform The synced transform data
     * @param scale Height scale factor
     * @return An AABB that approximates the rotated hitbox
     */
    private static AABB getRotatedBodyPartAABB(Player player, BodyPart part,
                                               PlayerModelPartCache.PartTransform transform, double scale) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        // Get base AABB
        AABB baseAABB = getBodyPartAABB(player, part);

        // If no significant rotation, return base AABB
        if (transform == null || !transform.hasSignificantRotation()) {
            // Apply position offset from synced data
            if (transform != null) {
                Vec3 offset = transform.getPositionOffset().scale(scale);
                return baseAABB.move(offset.x, offset.y, offset.z);
            }
            return baseAABB;
        }

        // For rotated parts, we need to expand the AABB to encompass the rotated volume
        // This is an approximation - true OBB would require more complex collision detection
        Vec3 center = baseAABB.getCenter();
        double halfWidth = (baseAABB.maxX - baseAABB.minX) / 2;
        double halfHeight = (baseAABB.maxY - baseAABB.minY) / 2;
        double halfDepth = (baseAABB.maxZ - baseAABB.minZ) / 2;

        // Calculate the diagonal to use as the expanded radius
        double diagonal = Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight + halfDepth * halfDepth);

        // Apply position offset
        Vec3 offset = transform.getPositionOffset().scale(scale);
        center = center.add(offset.x, offset.y, offset.z);

        // Create expanded AABB that can contain the rotated box
        // We use a more conservative expansion based on the rotation angles
        double xRotFactor = Math.abs(Math.sin(transform.xRot));
        double yRotFactor = Math.abs(Math.sin(transform.yRot));
        double zRotFactor = Math.abs(Math.sin(transform.zRot));

        double expandX = halfHeight * xRotFactor + halfDepth * yRotFactor;
        double expandY = halfWidth * zRotFactor + halfDepth * xRotFactor;
        double expandZ = halfWidth * yRotFactor + halfHeight * zRotFactor;

        return new AABB(
            center.x - halfWidth - expandX,
            center.y - halfHeight - expandY,
            center.z - halfDepth - expandZ,
            center.x + halfWidth + expandX,
            center.y + halfHeight + expandY,
            center.z + halfDepth + expandZ
        );
    }

    /**
     * Get a rotated AABB for a leg based on synced transform data.
     */
    private static AABB getRotatedLegAABB(Player player, boolean isLeft,
                                          PlayerModelPartCache.PartTransform transform, double scale) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        // Get base leg AABB
        AABB baseAABB = isLeft ? getLeftLegAABB(player) : getRightLegAABB(player);

        if (transform == null || !transform.hasSignificantRotation()) {
            if (transform != null) {
                Vec3 offset = transform.getPositionOffset().scale(scale);
                return baseAABB.move(offset.x, offset.y, offset.z);
            }
            return baseAABB;
        }

        // Apply rotation expansion (same logic as other parts)
        Vec3 center = baseAABB.getCenter();
        double halfWidth = (baseAABB.maxX - baseAABB.minX) / 2;
        double halfHeight = (baseAABB.maxY - baseAABB.minY) / 2;
        double halfDepth = (baseAABB.maxZ - baseAABB.minZ) / 2;

        Vec3 offset = transform.getPositionOffset().scale(scale);
        center = center.add(offset.x, offset.y, offset.z);

        double xRotFactor = Math.abs(Math.sin(transform.xRot));
        double yRotFactor = Math.abs(Math.sin(transform.yRot));
        double zRotFactor = Math.abs(Math.sin(transform.zRot));

        double expandX = halfHeight * xRotFactor + halfDepth * yRotFactor;
        double expandY = halfWidth * zRotFactor + halfDepth * xRotFactor;
        double expandZ = halfWidth * yRotFactor + halfHeight * zRotFactor;

        return new AABB(
            center.x - halfWidth - expandX,
            center.y - halfHeight - expandY,
            center.z - halfDepth - expandZ,
            center.x + halfWidth + expandX,
            center.y + halfHeight + expandY,
            center.z + halfDepth + expandZ
        );
    }

    /**
     * Determine which body part was hit at a point using synced pose data.
     */
    public static BodyPart getBodyPartAtPointWithPose(Player player, Vec3 hitPoint) {
        UUID playerUUID = player.getUUID();
        PlayerModelPartCache.CachedPlayerParts cachedParts = PlayerModelPartCache.getPlayerParts(playerUUID);

        // If no synced data, fall back to static calculation
        if (cachedParts == null) {
            return getBodyPartAtPoint(player, hitPoint);
        }

        double scale = player.getBbHeight() / 1.8;
        double inflate = 0.05;

        // Check each part with rotation-aware hitboxes
        AABB headBox = getRotatedBodyPartAABB(player, BodyPart.HEAD, cachedParts.getHead(), scale).inflate(inflate);
        if (headBox.contains(hitPoint)) {
            return BodyPart.HEAD;
        }

        AABB leftArmBox = getRotatedBodyPartAABB(player, BodyPart.LEFT_ARM, cachedParts.getLeftArm(), scale).inflate(inflate);
        if (leftArmBox.contains(hitPoint)) {
            return BodyPart.LEFT_ARM;
        }

        AABB rightArmBox = getRotatedBodyPartAABB(player, BodyPart.RIGHT_ARM, cachedParts.getRightArm(), scale).inflate(inflate);
        if (rightArmBox.contains(hitPoint)) {
            return BodyPart.RIGHT_ARM;
        }

        AABB bodyBox = getRotatedBodyPartAABB(player, BodyPart.BODY, cachedParts.getBody(), scale).inflate(inflate);
        if (bodyBox.contains(hitPoint)) {
            return BodyPart.BODY;
        }

        AABB leftLegBox = getRotatedLegAABB(player, true, cachedParts.getLeftLeg(), scale).inflate(inflate);
        if (leftLegBox.contains(hitPoint)) {
            return BodyPart.LEGS;
        }

        AABB rightLegBox = getRotatedLegAABB(player, false, cachedParts.getRightLeg(), scale).inflate(inflate);
        if (rightLegBox.contains(hitPoint)) {
            return BodyPart.LEGS;
        }

        // Fall back to height-based detection
        return getBodyPartAtPoint(player, hitPoint);
    }

    /**
     * Check if synced pose data is available for a player.
     */
    public static boolean hasSyncedPoseData(Player player) {
        return PlayerModelPartCache.hasValidData(player.getUUID());
    }

    /**
     * Get debug info including synced pose status.
     */
    public static String getDebugInfoWithPose(Player player) {
        String baseInfo = getDebugInfo(player);
        boolean hasPose = hasSyncedPoseData(player);
        return baseInfo + " | Pose synced: " + hasPose;
    }
}
