package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class SyncCapabilitiesPacket {
    private final float headHealth;
    private final float bodyHealth;
    private final int maxHeadHealth;
    private final int maxBodyHealth;
    private final boolean isActive;
    private final boolean isEscaping;
    private final int escapeTicksRemaining;
    private final boolean hasItem;
    private final int gasParticleType; // 0=none, 1=heavy(large instant), 2=light(other consumption)

    public SyncCapabilitiesPacket(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                   boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean hasItem,
                                   int gasParticleType) {
        this.headHealth = headHealth;
        this.bodyHealth = bodyHealth;
        this.maxHeadHealth = maxHeadHealth;
        this.maxBodyHealth = maxBodyHealth;
        this.isActive = isActive;
        this.isEscaping = isEscaping;
        this.escapeTicksRemaining = escapeTicksRemaining;
        this.hasItem = hasItem;
        this.gasParticleType = gasParticleType;
    }

    public static void encode(SyncCapabilitiesPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.headHealth);
        buf.writeFloat(packet.bodyHealth);
        buf.writeInt(packet.maxHeadHealth);
        buf.writeInt(packet.maxBodyHealth);
        buf.writeBoolean(packet.isActive);
        buf.writeBoolean(packet.isEscaping);
        buf.writeInt(packet.escapeTicksRemaining);
        buf.writeBoolean(packet.hasItem);
        buf.writeInt(packet.gasParticleType);
    }

    public static SyncCapabilitiesPacket decode(FriendlyByteBuf buf) {
        return new SyncCapabilitiesPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    public static void handle(SyncCapabilitiesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientPacketHandler.handleSyncCapabilities(
                        packet.headHealth, packet.bodyHealth,
                        packet.maxHeadHealth, packet.maxBodyHealth,
                        packet.isActive, packet.isEscaping,
                        packet.escapeTicksRemaining, packet.hasItem,
                        packet.gasParticleType
                );
            });
        });
        ctx.setPacketHandled(true);
    }
}
