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

    // Current gas particle type synced from server (0=none, 1=heavy, 2=light)
    private static int currentGasParticleType = 0;

    public static int getCurrentGasParticleType() {
        return currentGasParticleType;
    }

    public static void handleSyncCapabilities(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                               boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean hasItem,
                                               int gasParticleType) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
                // Use syncFromServer to avoid resetting health values
                cap.syncFromServer(headHealth, bodyHealth, maxHeadHealth, maxBodyHealth, isActive);
            });

            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                cap.setHasItem(hasItem);
            });

            // Update gas particle type
            currentGasParticleType = gasParticleType;

            // Spawn gas particles on client based on type
            if (gasParticleType > 0) {
                spawnGasParticles(player, gasParticleType);
            }
        }
    }

    private static void spawnGasParticles(Player player, int type) {
        Level level = player.level();
        if (level == null) return;

        Random random = new Random();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (type == 1) {
            // Heavy gas - 噴射 (jet/spray) effect for large damage instant consumption
            // Dense black smoke spraying outward from body
            for (int i = 0; i < 5; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.6;
                double offsetY = random.nextDouble() * 1.5 + 0.3;
                double offsetZ = (random.nextDouble() - 0.5) * 0.6;
                // LARGE_SMOKE for heavy black gas effect
                double speedX = (random.nextDouble() - 0.5) * 0.15;
                double speedY = random.nextDouble() * 0.05 + 0.02;
                double speedZ = (random.nextDouble() - 0.5) * 0.15;
                level.addParticle(ParticleTypes.LARGE_SMOKE, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
            // Extra squid ink particles for heavier effect
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.4;
                double offsetY = random.nextDouble() * 1.2 + 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.4;
                double speedX = (random.nextDouble() - 0.5) * 0.2;
                double speedY = -0.02;
                double speedZ = (random.nextDouble() - 0.5) * 0.2;
                level.addParticle(ParticleTypes.SQUID_INK, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
        } else if (type == 2) {
            // Light gas - ふわふわ (floating) effect for sustained/small consumption
            // Gentle floating black wisps
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = random.nextDouble() * 1.5 + 0.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;
                // SMOKE for lighter gas effect
                double speedX = (random.nextDouble() - 0.5) * 0.03;
                double speedY = random.nextDouble() * 0.05 + 0.01;
                double speedZ = (random.nextDouble() - 0.5) * 0.03;
                level.addParticle(ParticleTypes.SMOKE, x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
            }
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

            // White beam going up to build limit (vertical line effect)
            double maxHeight = 320 - y;
            for (double height = 0; height < maxHeight; height += 0.5) {
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
