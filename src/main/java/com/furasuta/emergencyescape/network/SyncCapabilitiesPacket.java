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
    private final boolean systemActive; // コマンドで有効化されたシステムの状態
    private final float legDamageAccum; // 足ダメージの累積（加算デバフの表示用）
    private final boolean leaking; // 経験値漏出中か（漏出アイコン表示用）

    public SyncCapabilitiesPacket(float headHealth, float bodyHealth, int maxHeadHealth, int maxBodyHealth,
                                   boolean isActive, boolean isEscaping, int escapeTicksRemaining, boolean systemActive,
                                   float legDamageAccum, boolean leaking) {
        this.headHealth = headHealth;
        this.bodyHealth = bodyHealth;
        this.maxHeadHealth = maxHeadHealth;
        this.maxBodyHealth = maxBodyHealth;
        this.isActive = isActive;
        this.isEscaping = isEscaping;
        this.escapeTicksRemaining = escapeTicksRemaining;
        this.systemActive = systemActive;
        this.legDamageAccum = legDamageAccum;
        this.leaking = leaking;
    }

    public static void encode(SyncCapabilitiesPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.headHealth);
        buf.writeFloat(packet.bodyHealth);
        buf.writeInt(packet.maxHeadHealth);
        buf.writeInt(packet.maxBodyHealth);
        buf.writeBoolean(packet.isActive);
        buf.writeBoolean(packet.isEscaping);
        buf.writeInt(packet.escapeTicksRemaining);
        buf.writeBoolean(packet.systemActive);
        buf.writeFloat(packet.legDamageAccum);
        buf.writeBoolean(packet.leaking);
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
                buf.readFloat(),
                buf.readBoolean()
        );
    }

    public static void handle(SyncCapabilitiesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientPacketHandler.handleSyncCapabilities(
                        packet.headHealth, packet.bodyHealth,
                        packet.maxHeadHealth, packet.maxBodyHealth,
                        packet.isActive, packet.isEscaping,
                        packet.escapeTicksRemaining, packet.systemActive,
                        packet.legDamageAccum, packet.leaking
                );
            });
        });
        ctx.setPacketHandled(true);
    }
}
