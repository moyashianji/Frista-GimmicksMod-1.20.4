package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class NetworkHandler {
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(new ResourceLocation(EmergencyEscapeMod.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncCapabilitiesPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCapabilitiesPacket::encode)
                .decoder(SyncCapabilitiesPacket::decode)
                .consumerMainThread(SyncCapabilitiesPacket::handle)
                .add();

        CHANNEL.messageBuilder(SpawnParticlesPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnParticlesPacket::encode)
                .decoder(SpawnParticlesPacket::decode)
                .consumerMainThread(SpawnParticlesPacket::handle)
                .add();

        CHANNEL.messageBuilder(VoluntaryEscapePacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VoluntaryEscapePacket::encode)
                .decoder(VoluntaryEscapePacket::decode)
                .consumerMainThread(VoluntaryEscapePacket::handle)
                .add();
    }
}
