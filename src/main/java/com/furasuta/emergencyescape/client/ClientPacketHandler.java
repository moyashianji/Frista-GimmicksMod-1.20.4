package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * Client-side packet handler. This class should only be loaded on the client.
 */
public class ClientPacketHandler {

    public static void handleSyncCapabilities(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                               boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean hasItem) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
                cap.setHeadHealth(headHealth);
                cap.setBodyHealth(bodyHealth);
                cap.setActive(isActive);
            });

            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                cap.setHasItem(hasItem);
            });
        }
    }

    public static void handleSpawnParticles(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null) {
            Random random = new Random();

            // Explosion particles
            for (int i = 0; i < 50; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 2;
                double offsetY = (random.nextDouble() - 0.5) * 2;
                double offsetZ = (random.nextDouble() - 0.5) * 2;
                level.addParticle(ParticleTypes.EXPLOSION, x + offsetX, y + offsetY + 1, z + offsetZ, 0, 0, 0);
            }

            // White glowing particles
            for (int i = 0; i < 100; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 3;
                double offsetY = random.nextDouble() * 2;
                double offsetZ = (random.nextDouble() - 0.5) * 3;
                double speedX = (random.nextDouble() - 0.5) * 0.5;
                double speedY = random.nextDouble() * 0.5;
                double speedZ = (random.nextDouble() - 0.5) * 0.5;
                level.addParticle(ParticleTypes.END_ROD, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }

            // White beam going up (vertical line effect)
            for (int i = 0; i < 100; i++) {
                double height = i * 0.5;
                level.addParticle(ParticleTypes.END_ROD, x, y + height, z, 0, 0.1, 0);
            }

            // Flash effect
            for (int i = 0; i < 30; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 1;
                double offsetZ = (random.nextDouble() - 0.5) * 1;
                level.addParticle(ParticleTypes.FLASH, x + offsetX, y + 1, z + offsetZ, 0, 0, 0);
            }
        }
    }
}
