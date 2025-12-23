package com.furasuta.emergencyescape.mixin;

import com.furasuta.emergencyescape.util.BodyPartHitbox;
import com.furasuta.emergencyescape.util.HitPositionTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

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
            hitPosition = projectile.position();
            hitSource = "projectile:" + projectile.getType().toShortString();

            // Calculate body part from projectile position
            bodyPart = BodyPartHitbox.getBodyPartAtPoint(player, hitPosition);

            // If we have the shooter, use raycast for more accuracy
            if (source.getEntity() != null) {
                attackOrigin = source.getEntity().getEyePosition();
                attackDirection = projectile.getDeltaMovement().normalize();

                // Try raycast-based detection
                BodyPartHitbox.BodyPart raycastResult = BodyPartHitbox.getHitBodyPart(player, attackOrigin, attackDirection);
                if (raycastResult != BodyPartHitbox.BodyPart.NONE) {
                    bodyPart = raycastResult;
                    hitSource = "projectile_raycast:" + projectile.getType().toShortString();
                }
            }
        }
        // For melee attacks, use attacker's look vector for raycast
        else if (source.getEntity() != null) {
            var attacker = source.getEntity();
            attackOrigin = attacker.getEyePosition();
            attackDirection = attacker.getLookAngle();
            hitSource = "melee:" + attacker.getType().toShortString();

            // Perform raycast to find which body part was hit
            bodyPart = BodyPartHitbox.getHitBodyPart(player, attackOrigin, attackDirection);

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

                bodyPart = BodyPartHitbox.getBodyPartAtPoint(player, hitPosition);
                hitSource = "melee_fallback:" + attacker.getType().toShortString();
            } else {
                // Calculate approximate hit position from raycast
                hitPosition = attackOrigin.add(attackDirection.scale(
                    attackOrigin.distanceTo(player.position().add(0, player.getBbHeight() / 2, 0))
                ));
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

            // Raycast from source to player
            bodyPart = BodyPartHitbox.getHitBodyPart(player, attackOrigin, attackDirection);

            if (bodyPart == BodyPartHitbox.BodyPart.NONE) {
                // Default to body for explosions
                bodyPart = BodyPartHitbox.BodyPart.BODY;
                hitSource = "explosion_default:" + source.type().msgId();
            }

            hitPosition = playerCenter;
        }

        // Store the hit info
        if (bodyPart != null) {
            HitPositionTracker.setLastHitInfo(
                player.getUUID(),
                new HitPositionTracker.HitInfo(hitPosition, attackOrigin, attackDirection, bodyPart, hitSource)
            );
        }
    }
}
