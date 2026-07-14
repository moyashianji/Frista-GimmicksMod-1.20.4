package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.DamageConsumptionCapability;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
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

    // 胴体HP色（横バー）
    private static final int BODY_CYAN = 0xFF00D9B8;
    private static final int BODY_YELLOW = 0xFFFFD700;
    private static final int BODY_ORANGE = 0xFFFF8C00;
    private static final int BODY_RED = 0xFFFF0000;
    private static final int BODY_BLACK = 0xFF000000;

    // 足デバフ（加算）アイコン
    private static final ResourceLocation ICON_BONUS1 =
            new ResourceLocation(EmergencyEscapeMod.MODID, "textures/gui/leg_bonus1.png");
    private static final ResourceLocation ICON_BONUS2 =
            new ResourceLocation(EmergencyEscapeMod.MODID, "textures/gui/leg_bonus2.png");
    // 経験値漏出中インジケーター
    private static final ResourceLocation ICON_LEAK =
            new ResourceLocation(EmergencyEscapeMod.MODID, "textures/gui/leak_icon.png");

    // 頭部HP色（六角形）
    private static final int HEAD_CYAN = 0xFF00D9B8;
    private static final int HEAD_YELLOW = 0xFFFFD700;
    private static final int HEAD_ORANGE = 0xFFFF8C00;
    private static final int HEAD_RED = 0xFFFF0000;
    private static final int HEAD_BLACK = 0xFF000000;

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean isActive = player.getCapability(BodyPartHealthCapability.CAPABILITY)
                .map(BodyPartHealthCapability::isActive).orElse(false);
        if (!isActive) return;

        // システム有効時はバニラの体力・空腹バーを非表示
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

        GuiGraphics guiGraphics = event.getGuiGraphics();

        // クリエイティブ時：速度バンド調整用に現在の水平速度を表示
        if (player.isCreative()) {
            String speedText = String.format("速度: %.3f blocks/tick", KeyInputHandler.clientHorizontalSpeed);
            guiGraphics.drawString(mc.font, speedText, 4, 4, 0xFFFFFF00, true);
        }

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            int centerX = screenWidth / 2;
            int healthBarY = screenHeight - 39;

            // 胴体HPバー（左側）- 経験値数字と被らない位置に下げた
            int bodyX = centerX - 91;
            int bodyY = healthBarY - 10;
            drawBodyHealthIndicator(guiGraphics, bodyX, bodyY, cap.getBodyHealthPercent());

            // 頭部HP六角形（右側）
            int headX = centerX + 50;
            int headY = healthBarY - 22;
            drawHeadHealthIndicator(guiGraphics, headX, headY, cap.getHeadHealthPercent());

            // 任意脱出の長押しプログレスバー
            float holdProgress = KeyInputHandler.getHoldProgress();
            if (holdProgress > 0) {
                drawEscapeProgress(guiGraphics, centerX, healthBarY - 42, holdProgress);
            }

            // 足デバフ（加算）インジケーター
            int bonusLevel = player.getCapability(DamageConsumptionCapability.CAPABILITY)
                    .map(DamageConsumptionCapability::getBonusLevel).orElse(0);
            if (bonusLevel > 0) {
                drawLegBonusIndicator(guiGraphics, bodyX, bodyY - 20, bonusLevel);
            }

            // 経験値漏出中インジケーター（経験値レベルの上・漏出が止まれば消える）
            boolean leaking = player.getCapability(DamageConsumptionCapability.CAPABILITY)
                    .map(DamageConsumptionCapability::isClientLeaking).orElse(false);
            if (leaking) {
                RenderSystem.enableBlend();
                guiGraphics.blit(ICON_LEAK, centerX - 8, healthBarY - 13, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
                RenderSystem.disableBlend();
            }
        });
    }

    /**
     * 足デバフ（加算）状態のアイコン。加算1=白ふわ / 加算2=紫ふわ（16x16テクスチャ）。
     */
    private static void drawLegBonusIndicator(GuiGraphics guiGraphics, int x, int y, int level) {
        ResourceLocation icon = level >= 2 ? ICON_BONUS2 : ICON_BONUS1;
        RenderSystem.enableBlend();
        guiGraphics.blit(icon, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }

    private static void drawBodyHealthIndicator(GuiGraphics guiGraphics, int x, int y, float healthPercent) {
        int color;

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

        int barWidth = 82;
        drawHealthBar(guiGraphics, x, y, barWidth, 8, color);
    }

    private static void drawHeadHealthIndicator(GuiGraphics guiGraphics, int x, int y, float healthPercent) {
        int color;

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

        drawHexagonShape(guiGraphics, x, y + 8, 20, color);
    }

    private static void drawHealthBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);

        int borderColor = 0xFF000000;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawHexagonShape(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        // 六角形の塗りつぶし
        guiGraphics.fill(x + 5, y, x + 15, y + 2, color);
        guiGraphics.fill(x + 3, y + 2, x + 17, y + 4, color);
        guiGraphics.fill(x + 2, y + 4, x + 18, y + 6, color);
        guiGraphics.fill(x + 1, y + 6, x + 19, y + 8, color);
        guiGraphics.fill(x, y + 8, x + 20, y + 12, color);
        guiGraphics.fill(x + 1, y + 12, x + 19, y + 14, color);
        guiGraphics.fill(x + 2, y + 14, x + 18, y + 16, color);
        guiGraphics.fill(x + 3, y + 16, x + 17, y + 18, color);
        guiGraphics.fill(x + 5, y + 18, x + 15, y + 20, color);

        // 六角形の枠線
        int borderColor = 0xFF000000;
        guiGraphics.fill(x + 5, y, x + 15, y + 1, borderColor);
        guiGraphics.fill(x + 3, y + 2, x + 5, y + 3, borderColor);
        guiGraphics.fill(x + 2, y + 4, x + 3, y + 5, borderColor);
        guiGraphics.fill(x + 1, y + 6, x + 2, y + 7, borderColor);
        guiGraphics.fill(x, y + 8, x + 1, y + 12, borderColor);
        guiGraphics.fill(x + 15, y + 2, x + 17, y + 3, borderColor);
        guiGraphics.fill(x + 17, y + 4, x + 18, y + 5, borderColor);
        guiGraphics.fill(x + 18, y + 6, x + 19, y + 7, borderColor);
        guiGraphics.fill(x + 19, y + 8, x + 20, y + 12, borderColor);
        guiGraphics.fill(x + 1, y + 12, x + 2, y + 13, borderColor);
        guiGraphics.fill(x + 2, y + 14, x + 3, y + 15, borderColor);
        guiGraphics.fill(x + 3, y + 16, x + 5, y + 17, borderColor);
        guiGraphics.fill(x + 18, y + 12, x + 19, y + 13, borderColor);
        guiGraphics.fill(x + 17, y + 14, x + 18, y + 15, borderColor);
        guiGraphics.fill(x + 15, y + 16, x + 17, y + 17, borderColor);
        guiGraphics.fill(x + 5, y + 19, x + 15, y + 20, borderColor);
    }

    private static void drawEscapeProgress(GuiGraphics guiGraphics, int centerX, int y, float progress) {
        int barWidth = 60;
        int barHeight = 5;
        int x = centerX - barWidth / 2;

        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

        int progressWidth = (int) (barWidth * progress);
        guiGraphics.fill(x, y, x + progressWidth, y + barHeight, 0xFF00FF00);

        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y, 0xFF000000);
        guiGraphics.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
        guiGraphics.fill(x - 1, y, x, y + barHeight, 0xFF000000);
        guiGraphics.fill(x + barWidth, y, x + barWidth + 1, y + barHeight, 0xFF000000);

        Minecraft mc = Minecraft.getInstance();
        String text = "脱出中...";
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, centerX - textWidth / 2, y - 10, 0xFFFFFF, true);
    }
}
