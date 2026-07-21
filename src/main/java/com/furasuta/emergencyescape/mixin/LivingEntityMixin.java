package com.furasuta.emergencyescape.mixin;

import com.furasuta.emergencyescape.util.BodyPartHitbox;
import com.furasuta.emergencyescape.util.HitPositionTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmergencyEscape-HitDetection");

    /**
     * ダメージ適用前にヒット位置と被弾部位を記録する。
     */
    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        Vec3 hitPosition = null;
        Vec3 attackOrigin = null;
        Vec3 attackDirection = null;
        BodyPartHitbox.BodyPart bodyPart = null;
        String hitSource = "unknown";

        // 飛び道具によるヒット - 軌道とプレイヤーBBの交点でポイントベース判定
        if (source.getDirectEntity() instanceof Projectile projectile) {
            Vec3 projPos = projectile.position();
            Vec3 projMotion = projectile.getDeltaMovement();

            // DeltaMovementから前tick位置を逆算し、軌道がプレイヤーBBに
            // 入った交点（実際の着弾位置）を求めてポイントベースで部位判定する。
            // レイキャスト方式だと上からの角度で頭が優先されるため使わない。
            hitPosition = projPos;
            hitSource = "projectile:" + projectile.getType().toShortString();

            if (projMotion.lengthSqr() > 0.001) {
                // このtickで弾が通った線分（掃過軌道）を復元する。
                // 高速弾は1tickで数十ブロック進むうえ、着弾tickでは弾がまだ動いていない
                // （position()が発射位置のまま）ことがあるため、
                //  ・始点は「1tick前の位置」
                //  ・レイ長は「1tickの移動距離」を必ず超える長さ
                // にしないと軌道がプレイヤーに届かず、発射位置の高さで誤判定する。
                double motionLen = projMotion.length();
                Vec3 trajectoryDir = projMotion.normalize();
                Vec3 rayStart = projPos.subtract(projMotion);
                double rayLen = motionLen * 2.0 + 8.0; // 弾が未移動/通過済みのどちらでも届く長さ
                attackOrigin = rayStart;
                attackDirection = trajectoryDir;

                // 軌道レイを「各部位のヒットボックス」に直接当てて、最も近い部位を採用する。
                // （膨張させた全身BBとの交点→点内包判定、だと近似フォールバックに落ちやすい）
                bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, rayStart, trajectoryDir, rayLen);
                hitSource = "projectile_trajectory:" + projectile.getType().toShortString();

                // 着弾座標も軌道から求めておく（ログ/フォールバック用）
                net.minecraft.world.phys.AABB playerBox = player.getBoundingBox().inflate(0.1);
                java.util.Optional<Vec3> intersection =
                        playerBox.clip(rayStart, rayStart.add(trajectoryDir.scale(rayLen)));
                if (intersection.isPresent()) {
                    hitPosition = intersection.get();
                }
            } else {
                attackOrigin = projPos;
                attackDirection = new Vec3(0, 0, 0);
                bodyPart = BodyPartHitbox.getBodyPartAtPointWithPose(player, projPos);
                hitSource = "projectile_static:" + projectile.getType().toShortString();
            }

            if (bodyPart == null || bodyPart == BodyPartHitbox.BodyPart.NONE) {
                // フォールバック: 求めた着弾座標で点判定
                bodyPart = BodyPartHitbox.getBodyPartAtPointWithPose(player, hitPosition);
                hitSource = "projectile_point_fallback:" + projectile.getType().toShortString();
            }

            double playerY = player.getY();
            double playerHeight = player.getBbHeight();
            double relativeY = (hitPosition.y - playerY) / playerHeight;

            LOGGER.debug("[HitDetect] Projectile hit: HitY={}, ProjY={}, PlayerY={}, Height={}, RelativeY={}%, BodyPart={}",
                String.format("%.2f", hitPosition.y),
                String.format("%.2f", projPos.y),
                String.format("%.2f", playerY),
                String.format("%.2f", playerHeight),
                String.format("%.1f", relativeY * 100),
                bodyPart);

            if (BodyPartHitbox.hasSyncedPoseData(player)) {
                hitSource += "_pose_synced";
            }
        }
        // 近接攻撃：攻撃者の視線方向でレイキャスト
        else if (source.getEntity() != null) {
            var attacker = source.getEntity();
            attackOrigin = attacker.getEyePosition();
            attackDirection = attacker.getLookAngle();
            hitSource = "melee:" + attacker.getType().toShortString();

            bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, attackOrigin, attackDirection);

            if (bodyPart == BodyPartHitbox.BodyPart.NONE) {
                // フォールバック：距離と高さから推定
                double distance = attackOrigin.distanceTo(player.position().add(0, player.getBbHeight() / 2, 0));
                hitPosition = attackOrigin.add(attackDirection.scale(Math.min(distance, 5)));

                double minY = player.getY();
                double maxY = player.getY() + player.getBbHeight();
                if (hitPosition.y < minY) {
                    hitPosition = new Vec3(hitPosition.x, minY, hitPosition.z);
                } else if (hitPosition.y > maxY) {
                    hitPosition = new Vec3(hitPosition.x, maxY, hitPosition.z);
                }

                bodyPart = BodyPartHitbox.getBodyPartAtPointWithPose(player, hitPosition);
                hitSource = "melee_fallback:" + attacker.getType().toShortString();
            } else {
                hitPosition = attackOrigin.add(attackDirection.scale(
                    attackOrigin.distanceTo(player.position().add(0, player.getBbHeight() / 2, 0))
                ));
                if (BodyPartHitbox.hasSyncedPoseData(player)) {
                    hitSource += "_pose_synced";
                }
            }
        }
        // 発生源座標があるダメージ（爆発など）
        else if (source.getSourcePosition() != null) {
            Vec3 sourcePos = source.getSourcePosition();
            hitSource = "source_position:" + source.type().msgId();

            Vec3 playerCenter = player.position().add(0, player.getBbHeight() / 2, 0);
            attackDirection = playerCenter.subtract(sourcePos).normalize();
            attackOrigin = sourcePos;

            bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, attackOrigin, attackDirection);

            if (bodyPart == BodyPartHitbox.BodyPart.NONE) {
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "explosion_default:" + source.type().msgId();
            } else if (BodyPartHitbox.hasSyncedPoseData(player)) {
                hitSource += "_pose_synced";
            }

            hitPosition = playerCenter;
        }
        // 発生源座標のないダメージ（落下、窒息、奈落など）
        else {
            hitSource = "environmental:" + source.type().msgId();
            Vec3 playerCenter = player.position().add(0, player.getBbHeight() / 2, 0);
            hitPosition = playerCenter;

            String damageType = source.type().msgId();
            if (damageType.contains("fall") || damageType.contains("flyIntoWall")) {
                bodyPart = BodyPartHitbox.BodyPart.LEGS;
                hitSource = "fall_damage:" + damageType;
            } else if (damageType.contains("drown") || damageType.contains("suffocate") || damageType.contains("inWall")) {
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "suffocation:" + damageType;
            } else if (damageType.contains("lava") || damageType.contains("inFire") || damageType.contains("onFire")
                    || damageType.contains("hotFloor") || damageType.contains("freeze")) {
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "temperature:" + damageType;
            } else if (damageType.contains("cactus") || damageType.contains("sweetBerry") || damageType.contains("stalagmite")) {
                bodyPart = BodyPartHitbox.BodyPart.LEGS;
                hitSource = "ground_hazard:" + damageType;
            } else if (damageType.contains("anvil") || damageType.contains("fallingBlock") || damageType.contains("fallingStalactite")) {
                bodyPart = BodyPartHitbox.BodyPart.HEAD;
                hitSource = "falling_object:" + damageType;
            } else if (damageType.contains("lightningBolt")) {
                bodyPart = BodyPartHitbox.BodyPart.HEAD;
                hitSource = "lightning:" + damageType;
            } else if (damageType.contains("starve") || damageType.contains("wither") || damageType.contains("magic")
                    || damageType.contains("poison") || damageType.contains("generic") || damageType.contains("outOfWorld")) {
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "status_effect:" + damageType;
            } else {
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "unknown_environmental:" + damageType;
            }
        }

        HitPositionTracker.setLastHitInfo(
            player.getUUID(),
            new HitPositionTracker.HitInfo(hitPosition, attackOrigin, attackDirection, bodyPart, hitSource)
        );
    }
}
