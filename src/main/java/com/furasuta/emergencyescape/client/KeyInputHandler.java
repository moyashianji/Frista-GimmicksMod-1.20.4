package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.event.EmergencyEscapeEventHandler;
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

    // Track camera type to prevent perspective switching
    private static CameraType lastCameraType = CameraType.FIRST_PERSON;

    /**
     * Use ClientTickEvent to continuously check key state while held.
     * InputEvent.Key only fires on state changes, not while held.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // Reset state when not in game
            keyPressStartTime = 0;
            wasKeyDown = false;
            escapeTriggered = false;
            return;
        }

        // Check and reset camera type if player has escape item
        checkAndResetCameraType();

        Player player = mc.player;

        // Check if player has emergency escape item
        if (!EmergencyEscapeEventHandler.hasEmergencyEscapeItem(player)) {
            keyPressStartTime = 0;
            wasKeyDown = false;
            escapeTriggered = false;
            return;
        }

        boolean isKeyDown = ESCAPE_KEY.isDown();

        if (isKeyDown && !wasKeyDown) {
            // Key just pressed
            keyPressStartTime = System.currentTimeMillis();
            escapeTriggered = false;
            LOGGER.debug("[VoluntaryEscape] P key pressed, starting hold timer");
        } else if (isKeyDown && wasKeyDown && !escapeTriggered) {
            // Key is being held
            long holdTime = System.currentTimeMillis() - keyPressStartTime;
            int requiredHoldTime = ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get();

            if (holdTime >= requiredHoldTime) {
                // Send packet to server
                LOGGER.info("[VoluntaryEscape] Hold time reached ({}ms), sending escape request to server", holdTime);
                NetworkHandler.CHANNEL.send(new VoluntaryEscapePacket(), PacketDistributor.SERVER.noArg());
                escapeTriggered = true; // Prevent spam
            }
        } else if (!isKeyDown && wasKeyDown) {
            // Key released
            if (!escapeTriggered) {
                LOGGER.debug("[VoluntaryEscape] P key released before hold time reached");
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
     * Block F5 key (perspective toggle) when player has emergency escape item.
     * Uses high priority to intercept before vanilla processing.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Check if F5 key was pressed (perspective toggle)
        if (event.getKey() == GLFW.GLFW_KEY_F5 && event.getAction() == GLFW.GLFW_PRESS) {
            // Check if player has emergency escape item
            if (EmergencyEscapeEventHandler.hasEmergencyEscapeItem(mc.player)) {
                // Store current camera type before it changes
                lastCameraType = mc.options.getCameraType();
                LOGGER.debug("[EmergencyEscape] F5 pressed with escape item - blocking perspective change");
            }
        }
    }

    /**
     * Additional check to reset camera if it somehow changed while having the escape item.
     * This is called in the tick handler.
     */
    public static void checkAndResetCameraType() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (EmergencyEscapeEventHandler.hasEmergencyEscapeItem(mc.player)) {
            CameraType currentType = mc.options.getCameraType();
            if (currentType != CameraType.FIRST_PERSON) {
                // Force back to first person
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                LOGGER.debug("[EmergencyEscape] Reset camera to FIRST_PERSON (was: {})", currentType);
            }
        }
        // Update last camera type
        lastCameraType = mc.options.getCameraType();
    }
}
