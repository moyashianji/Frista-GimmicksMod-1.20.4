package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * 他プレイヤーのガスパーティクルをクライアントに送信するパケット。
 * 自分のガスは見えない、他プレイヤーのガスは見えるようにする。
 */
public class SpawnGasParticlesPacket {
    private final int entityId;
    private final int gasType;
    private final int bonusLevel; // 足デバフ加算レベル（0/1/2）色分け用
    private final double x;
    private final double y;
    private final double z;

    public SpawnGasParticlesPacket(int entityId, int gasType, int bonusLevel, double x, double y, double z) {
        this.entityId = entityId;
        this.gasType = gasType;
        this.bonusLevel = bonusLevel;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SpawnGasParticlesPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeInt(packet.gasType);
        buf.writeInt(packet.bonusLevel);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
    }

    public static SpawnGasParticlesPacket decode(FriendlyByteBuf buf) {
        return new SpawnGasParticlesPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(SpawnGasParticlesPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientPacketHandler.handleSpawnGasParticles(
                        packet.entityId, packet.gasType, packet.bonusLevel, packet.x, packet.y, packet.z);
            });
        });
        ctx.setPacketHandled(true);
    }
}
