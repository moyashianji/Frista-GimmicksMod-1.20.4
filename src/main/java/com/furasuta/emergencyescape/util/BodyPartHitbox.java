package com.furasuta.emergencyescape.util;

import com.furasuta.emergencyescape.config.ModConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * HumanoidModelのCube定義に基づくプレイヤー部位別ヒットボックス。
 *
 * モデル座標系: 16px = 1ブロック、原点は首元(Y=0)、足元はY=24。
 * 部位境界(立位, 高さ1.8):
 *   HEAD: 75%-100%  / BODY: 37.5%-75%  / ARMS: 37.5%-75%  / LEGS: 0%-37.5%
 * スニーク時は高さ1.5に比例スケール。
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

    // --- HumanoidModelの定義値 (単位: ピクセル, 16px = 1ブロック) ---

    // 頭: addBox(-4, -8, -4, 8, 8, 8) offset(0, 0, 0)
    private static final float HEAD_MIN_Y = -8.0f;
    private static final float HEAD_MAX_Y = 0.0f;
    private static final float HEAD_HALF_WIDTH = 4.0f;
    private static final float HEAD_HALF_DEPTH = 4.0f;

    // 胴体: addBox(-4, 0, -2, 8, 12, 4) offset(0, 0, 0)
    private static final float BODY_MIN_Y = 0.0f;
    private static final float BODY_MAX_Y = 12.0f;
    private static final float BODY_HALF_WIDTH = 4.0f;
    private static final float BODY_HALF_DEPTH = 2.0f;

    // 脚: addBox(-2, 0, -2, 4, 12, 4) offset(+-1.9, 12, 0)
    private static final float LEG_OFFSET_Y = 12.0f;
    private static final float LEG_MIN_Y = 12.0f;
    private static final float LEG_MAX_Y = 24.0f;
    private static final float LEG_HALF_WIDTH = 2.0f;
    private static final float LEG_OFFSET_X = 1.9f;

    // 腕: addBox(-3, -2, -2, 4, 12, 4) offset(+-5, 2, 0)
    private static final float ARM_OFFSET_Y = 2.0f;
    private static final float ARM_MIN_Y = 0.0f;
    private static final float ARM_MAX_Y = 12.0f;
    private static final float ARM_HALF_WIDTH = 2.0f;
    private static final float ARM_OFFSET_X = 5.0f;

    private static final float MODEL_TOTAL_HEIGHT = 32.0f;  // -8 ~ 24
    private static final float MODEL_FEET_Y = 24.0f;
    private static final float PIXELS_PER_BLOCK = 16.0f;

    /**
     * モデルY座標を足元からのワールドYオフセットに変換する。
     */
    private static double modelYToWorldOffset(float modelY, double playerHeight) {
        double normalizedY = (MODEL_FEET_Y - modelY) / MODEL_TOTAL_HEIGHT;
        return normalizedY * playerHeight;
    }

    /**
     * 指定部位のAABBを取得する。
     */
    public static AABB getBodyPartAABB(Player player, BodyPart part) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        double scale = height / 1.8;

        switch (part) {
            case HEAD: {
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
                double top = py + modelYToWorldOffset(ARM_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(ARM_MAX_Y, height);
                double offsetX = (ARM_OFFSET_X / PIXELS_PER_BLOCK) * scale;
                double halfW = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                double halfD = (ARM_HALF_WIDTH / PIXELS_PER_BLOCK) * scale;
                return new AABB(
                    px + offsetX - halfW, bottom, pz - halfD,
                    px + offsetX + halfW, top, pz + halfD
                );
            }

            case RIGHT_ARM: {
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
                double top = py + modelYToWorldOffset(LEG_MIN_Y, height);
                double bottom = py + modelYToWorldOffset(LEG_MAX_Y, height);
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

    /** 左脚のAABBを取得する。 */
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

    /** 右脚のAABBを取得する。 */
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
     * レイキャストで被弾部位を判定する。最も近いヒットボックスを返す。
     */
    public static BodyPart getHitBodyPart(Player player, Vec3 attackOrigin, Vec3 attackDirection) {
        Vec3 rayEnd = attackOrigin.add(attackDirection.scale(10));

        AABB headBox = getBodyPartAABB(player, BodyPart.HEAD);
        AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY);
        AABB leftArmBox = getBodyPartAABB(player, BodyPart.LEFT_ARM);
        AABB rightArmBox = getBodyPartAABB(player, BodyPart.RIGHT_ARM);
        AABB legsBox = getBodyPartAABB(player, BodyPart.LEGS);

        Optional<Vec3> headHit = headBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> bodyHit = bodyBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> leftArmHit = leftArmBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> rightArmHit = rightArmBox.clip(attackOrigin, rayEnd);
        Optional<Vec3> legsHit = legsBox.clip(attackOrigin, rayEnd);

        double headDist = headHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double bodyDist = bodyHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double leftArmDist = leftArmHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double rightArmDist = rightArmHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);
        double legsDist = legsHit.map(v -> v.distanceToSqr(attackOrigin)).orElse(Double.MAX_VALUE);

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
     * 座標から被弾部位を判定する（飛び道具の着弾点など）。
     */
    public static BodyPart getBodyPartAtPoint(Player player, Vec3 hitPoint) {
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

        // フォールバック: Y座標の相対位置から判定
        double relativeY = (hitPoint.y - player.getY()) / player.getBbHeight();

        double headThreshold = ModConfig.HEAD_THRESHOLD_PERCENT.get() / 100.0;
        double bodyThreshold = ModConfig.BODY_THRESHOLD_PERCENT.get() / 100.0;

        if (relativeY >= headThreshold) {
            return BodyPart.HEAD;
        } else if (relativeY >= bodyThreshold) {
            return BodyPart.BODY;
        } else {
            return BodyPart.LEGS;
        }
    }

    /**
     * 腕をBODYに統合した簡略部位を返す。
     */
    public static BodyPart simplifyBodyPart(BodyPart part) {
        return switch (part) {
            case LEFT_ARM, RIGHT_ARM -> BodyPart.BODY;
            default -> part;
        };
    }

    /**
     * プレイヤーがスニーク・水泳・エリトラ滑空中かどうかを返す。
     */
    public static boolean isInAlteredPose(Player player) {
        return player.isCrouching() || player.isSwimming() || player.isFallFlying();
    }

    /**
     * 部位境界のデバッグ情報を返す。
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

    // --- ポーズ同期データを使った被弾判定 ---

    /**
     * 同期済みモデルパーツデータを使ったレイキャスト被弾判定。
     * 同期データがない場合は静的計算にフォールバックする。
     */
    public static BodyPart getHitBodyPartWithPose(Player player, Vec3 attackOrigin, Vec3 attackDirection) {
        UUID playerUUID = player.getUUID();
        PlayerModelPartCache.CachedPlayerParts cachedParts = PlayerModelPartCache.getPlayerParts(playerUUID);

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

        PlayerModelPartCache.PartTransform leftLegTransform = cachedParts.getLeftLeg();
        PlayerModelPartCache.PartTransform rightLegTransform = cachedParts.getRightLeg();

        AABB leftLegBox = getRotatedLegAABB(player, true, leftLegTransform, scale);
        Optional<Vec3> leftLegHit = leftLegBox.clip(attackOrigin, rayEnd);
        if (leftLegHit.isPresent()) {
            double dist = leftLegHit.get().distanceToSqr(attackOrigin);
            if (dist < minDist) {
                minDist = dist;
                result = BodyPart.LEGS;
            }
        }

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
     * 回転を考慮した部位AABBを返す。回転が小さい場合はベースAABBを使用する。
     */
    private static AABB getRotatedBodyPartAABB(Player player, BodyPart part,
                                               PlayerModelPartCache.PartTransform transform, double scale) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        AABB baseAABB = getBodyPartAABB(player, part);

        if (transform == null || !transform.hasSignificantRotation()) {
            if (transform != null) {
                Vec3 offset = transform.getPositionOffset().scale(scale);
                return baseAABB.move(offset.x, offset.y, offset.z);
            }
            return baseAABB;
        }

        // 回転体を包含するAABBを近似的に拡張
        Vec3 center = baseAABB.getCenter();
        double halfWidth = (baseAABB.maxX - baseAABB.minX) / 2;
        double halfHeight = (baseAABB.maxY - baseAABB.minY) / 2;
        double halfDepth = (baseAABB.maxZ - baseAABB.minZ) / 2;

        double diagonal = Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight + halfDepth * halfDepth);

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
     * 回転を考慮した脚のAABBを返す。
     */
    private static AABB getRotatedLegAABB(Player player, boolean isLeft,
                                          PlayerModelPartCache.PartTransform transform, double scale) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double height = player.getBbHeight();

        AABB baseAABB = isLeft ? getLeftLegAABB(player) : getRightLegAABB(player);

        if (transform == null || !transform.hasSignificantRotation()) {
            if (transform != null) {
                Vec3 offset = transform.getPositionOffset().scale(scale);
                return baseAABB.move(offset.x, offset.y, offset.z);
            }
            return baseAABB;
        }

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
     * ポーズ同期データを使った座標ベースの被弾判定。
     */
    public static BodyPart getBodyPartAtPointWithPose(Player player, Vec3 hitPoint) {
        UUID playerUUID = player.getUUID();
        PlayerModelPartCache.CachedPlayerParts cachedParts = PlayerModelPartCache.getPlayerParts(playerUUID);

        if (cachedParts == null) {
            return getBodyPartAtPoint(player, hitPoint);
        }

        double scale = player.getBbHeight() / 1.8;
        double inflate = 0.05;

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

        return getBodyPartAtPoint(player, hitPoint);
    }

    /** ポーズ同期データが利用可能かを返す。 */
    public static boolean hasSyncedPoseData(Player player) {
        return PlayerModelPartCache.hasValidData(player.getUUID());
    }

    /** ポーズ同期状態を含むデバッグ情報を返す。 */
    public static String getDebugInfoWithPose(Player player) {
        String baseInfo = getDebugInfo(player);
        boolean hasPose = hasSyncedPoseData(player);
        return baseInfo + " | Pose synced: " + hasPose;
    }
}
