package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.network.NetworkHandler;
import com.furasuta.emergencyescape.network.SyncModelPartPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Client-side handler that extracts PlayerModel part data and syncs it to the server.
 * This runs every few ticks to keep the server updated with accurate pose information.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT)
public class ModelPartSyncHandler {

    /**
     * How often to sync model parts (in client ticks).
     * 5 ticks = 250ms = 4 times per second
     */
    private static final int SYNC_INTERVAL_TICKS = 5;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Only sync while in game (not in menus)
        if (mc.screen != null) return;

        tickCounter++;
        if (tickCounter < SYNC_INTERVAL_TICKS) return;
        tickCounter = 0;

        // Get the player model and extract part data
        SyncModelPartPacket packet = buildSyncPacket(mc.player);
        if (packet != null) {
            NetworkHandler.CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
        }
    }

    /**
     * Build a sync packet by extracting model part data from the player's renderer.
     *
     * @param player The local client player
     * @return The packet to send, or null if unable to extract data
     */
    private static SyncModelPartPacket buildSyncPacket(Player player) {
        Minecraft mc = Minecraft.getInstance();

        // Get the player renderer
        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return null;
        }

        // Get the player model
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        if (model == null) {
            return null;
        }

        // Extract part data
        SyncModelPartPacket.Builder builder = new SyncModelPartPacket.Builder();

        // Head
        ModelPart head = model.head;
        builder.setHead(head.x, head.y, head.z, head.xRot, head.yRot, head.zRot);

        // Body
        ModelPart body = model.body;
        builder.setBody(body.x, body.y, body.z, body.xRot, body.yRot, body.zRot);

        // Left Arm
        ModelPart leftArm = model.leftArm;
        builder.setLeftArm(leftArm.x, leftArm.y, leftArm.z, leftArm.xRot, leftArm.yRot, leftArm.zRot);

        // Right Arm
        ModelPart rightArm = model.rightArm;
        builder.setRightArm(rightArm.x, rightArm.y, rightArm.z, rightArm.xRot, rightArm.yRot, rightArm.zRot);

        // Left Leg
        ModelPart leftLeg = model.leftLeg;
        builder.setLeftLeg(leftLeg.x, leftLeg.y, leftLeg.z, leftLeg.xRot, leftLeg.yRot, leftLeg.zRot);

        // Right Leg
        ModelPart rightLeg = model.rightLeg;
        builder.setRightLeg(rightLeg.x, rightLeg.y, rightLeg.z, rightLeg.xRot, rightLeg.yRot, rightLeg.zRot);

        return builder.build();
    }

    /**
     * Extract model part data for a specific player (can be used for other players too).
     * This is useful for syncing other players' poses for accurate hit detection.
     *
     * @param player The player to extract data from
     * @return Array of part data, or null if unable to extract
     */
    public static float[] extractPartData(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();

        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return null;
        }

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        if (model == null) {
            return null;
        }

        float[] data = new float[36];

        // HEAD (index 0)
        setPartData(data, 0, model.head);
        // BODY (index 1)
        setPartData(data, 1, model.body);
        // LEFT_ARM (index 2)
        setPartData(data, 2, model.leftArm);
        // RIGHT_ARM (index 3)
        setPartData(data, 3, model.rightArm);
        // LEFT_LEG (index 4)
        setPartData(data, 4, model.leftLeg);
        // RIGHT_LEG (index 5)
        setPartData(data, 5, model.rightLeg);

        return data;
    }

    private static void setPartData(float[] data, int index, ModelPart part) {
        int offset = index * 6;
        data[offset] = part.x;
        data[offset + 1] = part.y;
        data[offset + 2] = part.z;
        data[offset + 3] = part.xRot;
        data[offset + 4] = part.yRot;
        data[offset + 5] = part.zRot;
    }
}
