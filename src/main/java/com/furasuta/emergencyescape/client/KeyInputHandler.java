package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.network.NetworkHandler;
import com.furasuta.emergencyescape.network.VoluntaryEscapePacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyInputHandler.class);

    public static final KeyMapping ESCAPE_KEY = new KeyMapping(
            "key.emergencyescape.voluntary_escape",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.emergencyescape"
    );

    private static long keyPressStartTime = 0;
    private static boolean wasKeyDown = false;
    private static boolean escapeTriggered = false;

    private static boolean isClientSystemActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return mc.player.getCapability(BodyPartHealthCapability.CAPABILITY)
                .map(BodyPartHealthCapability::isActive).orElse(false);
    }

    /**
     * キー長押し判定のためClientTickEventで毎tick状態を確認する。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            keyPressStartTime = 0;
            wasKeyDown = false;
            escapeTriggered = false;
            return;
        }

        // システム有効時はF5(視点切替)をブロック
        if (isClientSystemActive()) {
            checkAndResetCameraType();
        }

        if (!isClientSystemActive()) {
            keyPressStartTime = 0;
            wasKeyDown = false;
            escapeTriggered = false;
            return;
        }

        boolean isKeyDown = ESCAPE_KEY.isDown();

        if (isKeyDown && !wasKeyDown) {
            keyPressStartTime = System.currentTimeMillis();
            escapeTriggered = false;
            LOGGER.debug("[任意脱出] Pキー押下、長押しタイマー開始");
        } else if (isKeyDown && wasKeyDown && !escapeTriggered) {
            long holdTime = System.currentTimeMillis() - keyPressStartTime;
            int requiredHoldTime = ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get();

            if (holdTime >= requiredHoldTime) {
                LOGGER.info("[任意脱出] 長押し時間到達({}ms)、サーバーへ脱出リクエスト送信", holdTime);
                NetworkHandler.CHANNEL.send(new VoluntaryEscapePacket(), PacketDistributor.SERVER.noArg());
                escapeTriggered = true;
            }
        } else if (!isKeyDown && wasKeyDown) {
            if (!escapeTriggered) {
                LOGGER.debug("[任意脱出] 長押し時間到達前にPキーが離された");
            }
            keyPressStartTime = 0;
            escapeTriggered = false;
        }

        wasKeyDown = isKeyDown;
    }

    public static float getHoldProgress() {
        if (keyPressStartTime == 0 || !wasKeyDown || escapeTriggered) return 0;

        long holdTime = System.currentTimeMillis() - keyPressStartTime;
        int requiredHoldTime = ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get();

        return Math.min(1.0f, (float) holdTime / requiredHoldTime);
    }

    /**
     * システム有効時にF5キー（視点切り替え）をブロックする。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_F5 && event.getAction() == GLFW.GLFW_PRESS) {
            if (isClientSystemActive()) {
                LOGGER.debug("[脱出] F5押下 - 視点切り替えをブロック");
            }
        }
    }

    /**
     * システム有効中にカメラが一人称以外に変わった場合、強制的にリセットする。
     */
    private static void checkAndResetCameraType() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CameraType currentType = mc.options.getCameraType();
        if (currentType != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            LOGGER.debug("[脱出] カメラを一人称に強制リセット (変更前: {})", currentType);
        }
    }
}
