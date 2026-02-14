package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.event.EmergencyEscapeEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT)
public class HealthBarRenderer {

    // Colors for body health (horizontal bar indicator)
    private static final int BODY_CYAN = 0xFF00D9B8;      // 70%+ - Cyan/Teal
    private static final int BODY_YELLOW = 0xFFFFD700;    // 50-69% - Yellow
    private static final int BODY_ORANGE = 0xFFFF8C00;    // 30-49% - Orange
    private static final int BODY_RED = 0xFFFF0000;       // <30% - Red
    private static final int BODY_BLACK = 0xFF000000;     // 0% - Black (dead)

    // Colors for head health (hexagon indicator) - 4-stage gradient like body
    private static final int HEAD_CYAN = 0xFF00D9B8;      // 70%+ - Cyan/Teal
    private static final int HEAD_YELLOW = 0xFFFFD700;    // 50-69% - Yellow
    private static final int HEAD_ORANGE = 0xFFFF8C00;    // 30-49% - Orange
    private static final int HEAD_RED = 0xFFFF0000;       // <30% - Red
    private static final int HEAD_BLACK = 0xFF000000;     // 0% - Black (dead)

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!EmergencyEscapeEventHandler.hasEmergencyEscapeItem(player)) return;

        // Hide vanilla health bar and hunger bar when escape item is held
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Only render if player has emergency escape item
        if (!EmergencyEscapeEventHandler.hasEmergencyEscapeItem(player)) return;

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // Position above the vanilla health bar
            int centerX = screenWidth / 2;
            int healthBarY = screenHeight - 39; // Vanilla health bar position

            // Draw body health indicator (horizontal bar, LEFT side)
            int bodyX = centerX - 91; // Align with left side of health bar
            int bodyY = healthBarY - 22;
            drawBodyHealthIndicator(guiGraphics, bodyX, bodyY, cap.getBodyHealthPercent());

            // Draw head health indicator (hexagon, RIGHT side)
            int headX = centerX + 50;
            int headY = healthBarY - 30;
            drawHeadHealthIndicator(guiGraphics, headX, headY, cap.getHeadHealthPercent());

            // Draw voluntary escape progress bar if holding key
            float holdProgress = KeyInputHandler.getHoldProgress();
            if (holdProgress > 0) {
                drawEscapeProgress(guiGraphics, centerX, healthBarY - 50, holdProgress);
            }
        });
    }

    private static void drawBodyHealthIndicator(GuiGraphics guiGraphics, int x, int y, float healthPercent) {
        int color;

        // Body health colors based on percentage
        if (healthPercent <= 0) {
            color = BODY_BLACK;
        } else if (healthPercent >= 70) {
            color = BODY_CYAN;
        } else if (healthPercent >= 50) {
            color = BODY_YELLOW;
        } else if (healthPercent >= 30) {
            color = BODY_ORANGE;
        } else {
            color = BODY_RED;
        }

        // Draw horizontal bar for body health
        int barWidth = 82;
        drawHealthBar(guiGraphics, x, y, barWidth, 8, color);
    }

    private static void drawHeadHealthIndicator(GuiGraphics guiGraphics, int x, int y, float healthPercent) {
        int color;

        // Head health colors based on percentage (4-stage gradient like body)
        if (healthPercent <= 0) {
            color = HEAD_BLACK;
        } else if (healthPercent >= 70) {
            color = HEAD_CYAN;
        } else if (healthPercent >= 50) {
            color = HEAD_YELLOW;
        } else if (healthPercent >= 30) {
            color = HEAD_ORANGE;
        } else {
            color = HEAD_RED;
        }

        // Draw hexagon shape for head health
        drawHexagonShape(guiGraphics, x, y + 8, 20, color);
    }

    private static void drawHealthBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Draw filled bar
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);

        // Draw border (black outline)
        int borderColor = 0xFF000000;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);           // Top
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);           // Left
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);  // Right
    }

    private static void drawHexagonShape(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        // Draw a hexagon approximation
        // Top part
        guiGraphics.fill(x + 5, y, x + 15, y + 2, color);
        guiGraphics.fill(x + 3, y + 2, x + 17, y + 4, color);
        guiGraphics.fill(x + 2, y + 4, x + 18, y + 6, color);
        guiGraphics.fill(x + 1, y + 6, x + 19, y + 8, color);
        // Middle part
        guiGraphics.fill(x, y + 8, x + 20, y + 12, color);
        // Bottom part
        guiGraphics.fill(x + 1, y + 12, x + 19, y + 14, color);
        guiGraphics.fill(x + 2, y + 14, x + 18, y + 16, color);
        guiGraphics.fill(x + 3, y + 16, x + 17, y + 18, color);
        guiGraphics.fill(x + 5, y + 18, x + 15, y + 20, color);

        // Draw border (darker outline)
        int borderColor = 0xFF000000;
        // Top edge
        guiGraphics.fill(x + 5, y, x + 15, y + 1, borderColor);
        // Top-left diagonal
        guiGraphics.fill(x + 3, y + 2, x + 5, y + 3, borderColor);
        guiGraphics.fill(x + 2, y + 4, x + 3, y + 5, borderColor);
        guiGraphics.fill(x + 1, y + 6, x + 2, y + 7, borderColor);
        guiGraphics.fill(x, y + 8, x + 1, y + 12, borderColor);
        // Top-right diagonal
        guiGraphics.fill(x + 15, y + 2, x + 17, y + 3, borderColor);
        guiGraphics.fill(x + 17, y + 4, x + 18, y + 5, borderColor);
        guiGraphics.fill(x + 18, y + 6, x + 19, y + 7, borderColor);
        guiGraphics.fill(x + 19, y + 8, x + 20, y + 12, borderColor);
        // Bottom-left diagonal
        guiGraphics.fill(x + 1, y + 12, x + 2, y + 13, borderColor);
        guiGraphics.fill(x + 2, y + 14, x + 3, y + 15, borderColor);
        guiGraphics.fill(x + 3, y + 16, x + 5, y + 17, borderColor);
        // Bottom-right diagonal
        guiGraphics.fill(x + 18, y + 12, x + 19, y + 13, borderColor);
        guiGraphics.fill(x + 17, y + 14, x + 18, y + 15, borderColor);
        guiGraphics.fill(x + 15, y + 16, x + 17, y + 17, borderColor);
        // Bottom edge
        guiGraphics.fill(x + 5, y + 19, x + 15, y + 20, borderColor);
    }

    private static void drawEscapeProgress(GuiGraphics guiGraphics, int centerX, int y, float progress) {
        int barWidth = 60;
        int barHeight = 5;
        int x = centerX - barWidth / 2;

        // Background
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

        // Progress
        int progressWidth = (int) (barWidth * progress);
        guiGraphics.fill(x, y, x + progressWidth, y + barHeight, 0xFF00FF00);

        // Border
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y, 0xFF000000);
        guiGraphics.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
        guiGraphics.fill(x - 1, y, x, y + barHeight, 0xFF000000);
        guiGraphics.fill(x + barWidth, y, x + barWidth + 1, y + barHeight, 0xFF000000);

        // Text
        Minecraft mc = Minecraft.getInstance();
        String text = "脱出中...";
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, centerX - textWidth / 2, y - 10, 0xFFFFFF, true);
    }
}
