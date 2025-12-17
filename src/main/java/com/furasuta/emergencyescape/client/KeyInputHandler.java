package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.event.EmergencyEscapeEventHandler;
import com.furasuta.emergencyescape.network.NetworkHandler;
import com.furasuta.emergencyescape.network.VoluntaryEscapePacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    public static final KeyMapping ESCAPE_KEY = new KeyMapping(
            "key.emergencyescape.voluntary_escape",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.emergencyescape"
    );

    private static long keyPressStartTime = 0;
    private static boolean wasKeyDown = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Player player = mc.player;

        // Check if player has emergency escape item
        if (!EmergencyEscapeEventHandler.hasEmergencyEscapeItem(player)) {
            keyPressStartTime = 0;
            wasKeyDown = false;
            return;
        }

        boolean isKeyDown = ESCAPE_KEY.isDown();

        if (isKeyDown && !wasKeyDown) {
            // Key just pressed
            keyPressStartTime = System.currentTimeMillis();
        } else if (isKeyDown && wasKeyDown) {
            // Key is being held
            long holdTime = System.currentTimeMillis() - keyPressStartTime;
            int requiredHoldTime = ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get();

            if (holdTime >= requiredHoldTime) {
                // Send packet to server
                NetworkHandler.CHANNEL.send(new VoluntaryEscapePacket(), PacketDistributor.SERVER.noArg());
                keyPressStartTime = System.currentTimeMillis() + 10000; // Prevent spam
            }
        } else if (!isKeyDown && wasKeyDown) {
            // Key released
            keyPressStartTime = 0;
        }

        wasKeyDown = isKeyDown;
    }

    public static float getHoldProgress() {
        if (keyPressStartTime == 0 || !wasKeyDown) return 0;

        long holdTime = System.currentTimeMillis() - keyPressStartTime;
        int requiredHoldTime = ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get();

        return Math.min(1.0f, (float) holdTime / requiredHoldTime);
    }
}
