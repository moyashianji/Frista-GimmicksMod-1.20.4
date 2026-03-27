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
 * プレイヤーモデルのパーツデータを定期的にサーバーへ同期するクライアント側ハンドラ。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT)
public class ModelPartSyncHandler {

    /** 同期間隔（5tick = 250ms = 毎秒4回） */
    private static final int SYNC_INTERVAL_TICKS = 5;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.screen != null) return;

        tickCounter++;
        if (tickCounter < SYNC_INTERVAL_TICKS) return;
        tickCounter = 0;

        SyncModelPartPacket packet = buildSyncPacket(mc.player);
        if (packet != null) {
            NetworkHandler.CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
        }
    }

    /**
     * プレイヤーのレンダラーからモデルパーツデータを取得し、同期パケットを構築する。
     */
    private static SyncModelPartPacket buildSyncPacket(Player player) {
        Minecraft mc = Minecraft.getInstance();

        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return null;
        }

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        if (model == null) {
            return null;
        }

        SyncModelPartPacket.Builder builder = new SyncModelPartPacket.Builder();

        ModelPart head = model.head;
        builder.setHead(head.x, head.y, head.z, head.xRot, head.yRot, head.zRot);

        ModelPart body = model.body;
        builder.setBody(body.x, body.y, body.z, body.xRot, body.yRot, body.zRot);

        ModelPart leftArm = model.leftArm;
        builder.setLeftArm(leftArm.x, leftArm.y, leftArm.z, leftArm.xRot, leftArm.yRot, leftArm.zRot);

        ModelPart rightArm = model.rightArm;
        builder.setRightArm(rightArm.x, rightArm.y, rightArm.z, rightArm.xRot, rightArm.yRot, rightArm.zRot);

        ModelPart leftLeg = model.leftLeg;
        builder.setLeftLeg(leftLeg.x, leftLeg.y, leftLeg.z, leftLeg.xRot, leftLeg.yRot, leftLeg.zRot);

        ModelPart rightLeg = model.rightLeg;
        builder.setRightLeg(rightLeg.x, rightLeg.y, rightLeg.z, rightLeg.xRot, rightLeg.yRot, rightLeg.zRot);

        return builder.build();
    }

    /**
     * 指定プレイヤーのモデルパーツデータを配列として取得する。
     * 他プレイヤーのポーズ取得にも使用可能。
     *
     * @return パーツデータ配列（取得不可の場合null）
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

        setPartData(data, 0, model.head);
        setPartData(data, 1, model.body);
        setPartData(data, 2, model.leftArm);
        setPartData(data, 3, model.rightArm);
        setPartData(data, 4, model.leftLeg);
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
