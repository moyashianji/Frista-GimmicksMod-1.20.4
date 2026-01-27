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
     * Captures the hit position and determines the body part when an entity takes damage.
     * This runs before the actual damage is applied, so we can track where the hit occurred.
     */
    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only track for players
        if (!(self instanceof Player player)) {
            return;
        }

        Vec3 hitPosition = null;
        Vec3 attackOrigin = null;
        Vec3 attackDirection = null;
        BodyPartHitbox.BodyPart bodyPart = null;
        String hitSource = "unknown";

        // Try to get hit position from projectile
        if (source.getDirectEntity() instanceof Projectile projectile) {
            // Use projectile's current position as hit location
            // This is the ACTUAL position where the arrow/projectile hit the player
            // This is more accurate than raycast from shooter because:
            // 1. The shooter may have moved/rotated since firing
            // 2. Projectiles follow curved paths (gravity)
            // 3. The player may have moved since the projectile was fired
            hitPosition = projectile.position();
            hitSource = "projectile_position:" + projectile.getType().toShortString();

            // Calculate body part from projectile's actual position (use pose-aware method)
            bodyPart = BodyPartHitbox.getBodyPartAtPointWithPose(player, hitPosition);

            // Log projectile position and player bounds for debugging
            double playerY = player.getY();
            double playerHeight = player.getBbHeight();
            double relativeY = (hitPosition.y - playerY) / playerHeight;

            LOGGER.debug("[HitDetect] Projectile hit: Y={}, PlayerY={}, Height={}, RelativeY={}%, BodyPart={}",
                String.format("%.2f", hitPosition.y),
                String.format("%.2f", playerY),
                String.format("%.2f", playerHeight),
                String.format("%.1f", relativeY * 100),
                bodyPart);

            // Store attack info for reference (but DON'T override bodyPart with raycast)
            if (source.getEntity() != null) {
                attackOrigin = source.getEntity().getEyePosition();
                attackDirection = projectile.getDeltaMovement().normalize();
            }

            if (BodyPartHitbox.hasSyncedPoseData(player)) {
                hitSource += "_pose_synced";
            }
        }
        // For melee attacks, use attacker's look vector for raycast
        else if (source.getEntity() != null) {
            var attacker = source.getEntity();
            attackOrigin = attacker.getEyePosition();
            attackDirection = attacker.getLookAngle();
            hitSource = "melee:" + attacker.getType().toShortString();

            // Perform raycast to find which body part was hit (use pose-aware method)
            bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, attackOrigin, attackDirection);

            if (bodyPart == BodyPartHitbox.BodyPart.NONE) {
                // Fallback: estimate based on distance and height
                double distance = attackOrigin.distanceTo(player.position().add(0, player.getBbHeight() / 2, 0));
                hitPosition = attackOrigin.add(attackDirection.scale(Math.min(distance, 5)));

                // Clamp to player bounds
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
                // Calculate approximate hit position from raycast
                hitPosition = attackOrigin.add(attackDirection.scale(
                    attackOrigin.distanceTo(player.position().add(0, player.getBbHeight() / 2, 0))
                ));
                if (BodyPartHitbox.hasSyncedPoseData(player)) {
                    hitSource += "_pose_synced";
                }
            }
        }
        // For damage without a direct source (explosion, environment, etc.)
        else if (source.getSourcePosition() != null) {
            // Use source position for explosions, etc.
            Vec3 sourcePos = source.getSourcePosition();
            hitSource = "source_position:" + source.type().msgId();

            // Direction from source to player center
            Vec3 playerCenter = player.position().add(0, player.getBbHeight() / 2, 0);
            attackDirection = playerCenter.subtract(sourcePos).normalize();
            attackOrigin = sourcePos;

            // Raycast from source to player (use pose-aware method)
            bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, attackOrigin, attackDirection);

            if (bodyPart == BodyPartHitbox.BodyPart.NONE) {
                // Default to body for explosions
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "explosion_default:" + source.type().msgId();
            } else if (BodyPartHitbox.hasSyncedPoseData(player)) {
                hitSource += "_pose_synced";
            }

            hitPosition = playerCenter;
        }
        // Fallback for damage types without source position (fall damage, suffocation, void, etc.)
        else {
            hitSource = "environmental:" + source.type().msgId();
            Vec3 playerCenter = player.position().add(0, player.getBbHeight() / 2, 0);
            hitPosition = playerCenter;

            // Determine body part based on damage type
            String damageType = source.type().msgId();
            if (damageType.contains("fall") || damageType.contains("flyIntoWall")) {
                // Fall damage affects legs primarily
                bodyPart = BodyPartHitbox.BodyPart.LEGS;
                hitSource = "fall_damage:" + damageType;
            } else if (damageType.contains("drown") || damageType.contains("suffocate") || damageType.contains("inWall")) {
                // Suffocation/drowning affects body
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "suffocation:" + damageType;
            } else if (damageType.contains("lava") || damageType.contains("inFire") || damageType.contains("onFire")
                    || damageType.contains("hotFloor") || damageType.contains("freeze")) {
                // Fire/temperature damage affects body
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "temperature:" + damageType;
            } else if (damageType.contains("cactus") || damageType.contains("sweetBerry") || damageType.contains("stalagmite")) {
                // Ground hazards affect legs
                bodyPart = BodyPartHitbox.BodyPart.LEGS;
                hitSource = "ground_hazard:" + damageType;
            } else if (damageType.contains("anvil") || damageType.contains("fallingBlock") || damageType.contains("fallingStalactite")) {
                // Falling objects affect head
                bodyPart = BodyPartHitbox.BodyPart.HEAD;
                hitSource = "falling_object:" + damageType;
            } else if (damageType.contains("lightningBolt")) {
                // Lightning affects head
                bodyPart = BodyPartHitbox.BodyPart.HEAD;
                hitSource = "lightning:" + damageType;
            } else if (damageType.contains("starve") || damageType.contains("wither") || damageType.contains("magic")
                    || damageType.contains("poison") || damageType.contains("generic") || damageType.contains("outOfWorld")) {
                // Status effects, generic, and void damage affect body
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "status_effect:" + damageType;
            } else {
                // Default to body for unknown damage types
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "unknown_environmental:" + damageType;
            }
        }

        // Store the hit info (bodyPart is always set now)
        HitPositionTracker.setLastHitInfo(
            player.getUUID(),
            new HitPositionTracker.HitInfo(hitPosition, attackOrigin, attackDirection, bodyPart, hitSource)
        );
    }
}
