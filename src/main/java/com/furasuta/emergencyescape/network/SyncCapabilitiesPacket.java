package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SyncCapabilitiesPacket {
    private final float headHealth;
    private final float bodyHealth;
    private final int maxHeadHealth;
    private final int maxBodyHealth;
    private final boolean isActive;
    private final boolean isEscaping;
    private final int escapeTicksRemaining;
    private final boolean hasItem;

    public SyncCapabilitiesPacket(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                   boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean hasItem) {
        this.headHealth = headHealth;
        this.bodyHealth = bodyHealth;
        this.maxHeadHealth = maxHeadHealth;
        this.maxBodyHealth = maxBodyHealth;
        this.isActive = isActive;
        this.isEscaping = isEscaping;
        this.escapeTicksRemaining = escapeTicksRemaining;
        this.hasItem = hasItem;
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
                buf.readBoolean()
        );
    }

    public static void handle(SyncCapabilitiesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
                    cap.setHeadHealth(packet.headHealth);
                    cap.setBodyHealth(packet.bodyHealth);
                    cap.setActive(packet.isActive);
                });

                player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                    cap.setHasItem(packet.hasItem);
                });
            }
        });
        ctx.setPacketHandled(true);
    }
}
