package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.util.PlayerModelPartCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/**
 * プレイヤーモデルの各部位の位置・回転をクライアントからサーバーへ同期するパケット。
 * 部位: HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
 * 各部位: x, y, z, xRot, yRot, zRot (6部位 x 6値 = 36 float)
 */
public class SyncModelPartPacket {

    private final float[] partData;

    public SyncModelPartPacket(float[] partData) {
        this.partData = partData;
    }

    public static void encode(SyncModelPartPacket packet, FriendlyByteBuf buf) {
        for (float value : packet.partData) {
            buf.writeFloat(value);
        }
    }

    public static SyncModelPartPacket decode(FriendlyByteBuf buf) {
        float[] partData = new float[36];
        for (int i = 0; i < 36; i++) {
            partData[i] = buf.readFloat();
        }
        return new SyncModelPartPacket(partData);
    }

    public static void handle(SyncModelPartPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            PlayerModelPartCache.updatePlayerParts(player.getUUID(), packet.partData);
        });
        ctx.setPacketHandled(true);
    }

    public float[] getPartData() {
        return partData;
    }

    /** 各部位の値からパケットデータを組み立てるビルダー */
    public static class Builder {
        private final float[] data = new float[36];

        public Builder setHead(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(0, x, y, z, xRot, yRot, zRot);
            return this;
        }

        public Builder setBody(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(1, x, y, z, xRot, yRot, zRot);
            return this;
        }

        public Builder setLeftArm(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(2, x, y, z, xRot, yRot, zRot);
            return this;
        }

        public Builder setRightArm(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(3, x, y, z, xRot, yRot, zRot);
            return this;
        }

        public Builder setLeftLeg(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(4, x, y, z, xRot, yRot, zRot);
            return this;
        }

        public Builder setRightLeg(float x, float y, float z, float xRot, float yRot, float zRot) {
            setPart(5, x, y, z, xRot, yRot, zRot);
            return this;
        }

        private void setPart(int index, float x, float y, float z, float xRot, float yRot, float zRot) {
            int offset = index * 6;
            data[offset] = x;
            data[offset + 1] = y;
            data[offset + 2] = z;
            data[offset + 3] = xRot;
            data[offset + 4] = yRot;
            data[offset + 5] = zRot;
        }

        public SyncModelPartPacket build() {
            return new SyncModelPartPacket(data);
        }
    }
}
