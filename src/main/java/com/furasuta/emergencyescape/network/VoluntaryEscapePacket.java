package com.furasuta.emergencyescape.network;

import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.event.EmergencyEscapeEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VoluntaryEscapePacket {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoluntaryEscapePacket.class);

    public VoluntaryEscapePacket() {
    }

    public static void encode(VoluntaryEscapePacket packet, FriendlyByteBuf buf) {
    }

    public static VoluntaryEscapePacket decode(FriendlyByteBuf buf) {
        return new VoluntaryEscapePacket();
    }

    public static void handle(VoluntaryEscapePacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                LOGGER.warn("[VoluntaryEscape] Received packet but sender is null");
                return;
            }

            LOGGER.info("[VoluntaryEscape] Received escape request from player: {}", player.getName().getString());

            if (!EmergencyEscapeEventHandler.isSystemActive(player)) {
                LOGGER.info("[VoluntaryEscape] Denied: System is not active for player");
                player.sendSystemMessage(Component.literal("§c緊急脱出失敗: 特殊体力システムが有効化されていません"));
                return;
            }

            player.getCapability(EmergencyEscapeCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.isEscaping()) {
                    LOGGER.info("[VoluntaryEscape] Denied: Player is already escaping");
                    player.sendSystemMessage(Component.literal("§c緊急脱出失敗: すでに緊急脱出中です"));
                    return;
                }

                if (isEnemyPlayerNearby(player)) {
                    int radius = ModConfig.VOLUNTARY_ESCAPE_RADIUS.get();
                    LOGGER.info("[VoluntaryEscape] Denied: Enemy player is nearby (within {} blocks)", radius);
                    player.sendSystemMessage(Component.literal("§c緊急脱出失敗: 敵プレイヤーから" + radius + "ブロック以上離れてください"));
                    return;
                }

                LOGGER.info("[VoluntaryEscape] Triggering voluntary escape for player: {}", player.getName().getString());
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

            // チームなし、または別チームなら敵とみなす
            if (playerTeam == null || nearbyTeam == null || !playerTeam.equals(nearbyTeam)) {
                return true;
            }
        }

        return false;
    }
}
