package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.util.PlayerModelPartCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/**
 * Packet to sync PlayerModel part positions and rotations from client to server.
 * This allows the server to have accurate hitbox data based on the actual model pose.
 *
 * Synced parts: HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
 * Each part has: x, y, z position offset and xRot, yRot, zRot rotation
 */
public class SyncModelPartPacket {

    // Part data arrays (6 parts × 6 values each = 36 floats total)
    // Order: HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
    // Each part: [x, y, z, xRot, yRot, zRot]
    private final float[] partData;

    public SyncModelPartPacket(float[] partData) {
        this.partData = partData;
    }

    public static void encode(SyncModelPartPacket packet, FriendlyByteBuf buf) {
        // Write all 36 floats
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

            // Store the part data in the cache
            PlayerModelPartCache.updatePlayerParts(player.getUUID(), packet.partData);
        });
        ctx.setPacketHandled(true);
    }

    public float[] getPartData() {
        return partData;
    }

    /**
     * Helper class to build the packet data from individual part values.
     */
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
