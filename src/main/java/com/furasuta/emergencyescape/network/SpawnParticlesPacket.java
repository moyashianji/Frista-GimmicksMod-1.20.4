package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class SpawnParticlesPacket {
    private final int entityId; // 対象プレイヤー。クライアント側で実位置に追従させるために使う
    private final double x;
    private final double y;
    private final double z;

    public SpawnParticlesPacket(int entityId, double x, double y, double z) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SpawnParticlesPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
    }

    public static SpawnParticlesPacket decode(FriendlyByteBuf buf) {
        return new SpawnParticlesPacket(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(SpawnParticlesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientPacketHandler.handleSpawnParticles(packet.entityId, packet.x, packet.y, packet.z);
            });
        });
        ctx.setPacketHandled(true);
    }
}
