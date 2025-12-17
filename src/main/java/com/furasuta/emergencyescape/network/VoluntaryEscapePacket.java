package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.event.EmergencyEscapeEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.List;

public class VoluntaryEscapePacket {

    public VoluntaryEscapePacket() {
    }

    public static void encode(VoluntaryEscapePacket packet, FriendlyByteBuf buf) {
        // No data needed
    }

    public static VoluntaryEscapePacket decode(FriendlyByteBuf buf) {
        return new VoluntaryEscapePacket();
    }

    public static void handle(VoluntaryEscapePacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Check if player has emergency escape item
            if (!EmergencyEscapeEventHandler.hasEmergencyEscapeItem(player)) return;

            // Check if player is already escaping
            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.isEscaping()) return;

                // Check if enemy players are nearby
                if (isEnemyPlayerNearby(player)) return;

                // Trigger voluntary escape
                EmergencyEscapeEventHandler.triggerEmergencyEscape(player);
            });
        });
        ctx.setPacketHandled(true);
    }

    private static boolean isEnemyPlayerNearby(ServerPlayer player) {
        double radius = ModConfig.VOLUNTARY_ESCAPE_RADIUS.get();
        PlayerTeam playerTeam = player.getTeam();

        List<ServerPlayer> nearbyPlayers = player.serverLevel().getPlayers(p -> {
            if (p == player) return false;
            double distance = p.distanceTo(player);
            return distance <= radius;
        });

        for (ServerPlayer nearbyPlayer : nearbyPlayers) {
            PlayerTeam nearbyTeam = nearbyPlayer.getTeam();

            // If either player has no team, or they are on different teams
            if (playerTeam == null || nearbyTeam == null || !playerTeam.equals(nearbyTeam)) {
                return true;
            }
        }

        return false;
    }
}
