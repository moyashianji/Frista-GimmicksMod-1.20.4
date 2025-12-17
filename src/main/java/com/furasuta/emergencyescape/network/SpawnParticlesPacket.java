package com.furasuta.emergencyescape.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Random;

public class SpawnParticlesPacket {
    private final double x;
    private final double y;
    private final double z;

    public SpawnParticlesPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SpawnParticlesPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
    }

    public static SpawnParticlesPacket decode(FriendlyByteBuf buf) {
        return new SpawnParticlesPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(SpawnParticlesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                Random random = new Random();

                // Explosion particles
                for (int i = 0; i < 50; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 2;
                    double offsetY = (random.nextDouble() - 0.5) * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * 2;
                    level.addParticle(ParticleTypes.EXPLOSION, packet.x + offsetX, packet.y + offsetY + 1, packet.z + offsetZ, 0, 0, 0);
                }

                // White glowing particles
                for (int i = 0; i < 100; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 3;
                    double offsetY = random.nextDouble() * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * 3;
                    double speedX = (random.nextDouble() - 0.5) * 0.5;
                    double speedY = random.nextDouble() * 0.5;
                    double speedZ = (random.nextDouble() - 0.5) * 0.5;
                    level.addParticle(ParticleTypes.END_ROD, packet.x + offsetX, packet.y + offsetY, packet.z + offsetZ, speedX, speedY, speedZ);
                }

                // White beam going up (vertical line effect)
                for (int i = 0; i < 100; i++) {
                    double height = i * 0.5;
                    level.addParticle(ParticleTypes.END_ROD, packet.x, packet.y + height, packet.z, 0, 0.1, 0);
                }

                // Flash effect
                for (int i = 0; i < 30; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 1;
                    double offsetZ = (random.nextDouble() - 0.5) * 1;
                    level.addParticle(ParticleTypes.FLASH, packet.x + offsetX, packet.y + 1, packet.z + offsetZ, 0, 0, 0);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
