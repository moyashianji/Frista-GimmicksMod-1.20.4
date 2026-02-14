package com.furasuta.emergencyescape.command;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID)
public class ModCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModCommands.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("emergencyescape")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(ModCommands::reloadConfig))
                .then(Commands.literal("config")
                    .executes(ModCommands::showConfig))
                .then(Commands.literal("testcolor")
                    .requires(source -> source.hasPermission(2))
                    .executes(ModCommands::showColorThresholds)
                    .then(Commands.literal("head")
                        .then(Commands.literal("cyan").executes(ctx -> setHealthForColor(ctx, "head", "cyan")))
                        .then(Commands.literal("yellow").executes(ctx -> setHealthForColor(ctx, "head", "yellow")))
                        .then(Commands.literal("orange").executes(ctx -> setHealthForColor(ctx, "head", "orange")))
                        .then(Commands.literal("red").executes(ctx -> setHealthForColor(ctx, "head", "red")))
                        .then(Commands.literal("black").executes(ctx -> setHealthForColor(ctx, "head", "black"))))
                    .then(Commands.literal("body")
                        .then(Commands.literal("cyan").executes(ctx -> setHealthForColor(ctx, "body", "cyan")))
                        .then(Commands.literal("yellow").executes(ctx -> setHealthForColor(ctx, "body", "yellow")))
                        .then(Commands.literal("orange").executes(ctx -> setHealthForColor(ctx, "body", "orange")))
                        .then(Commands.literal("red").executes(ctx -> setHealthForColor(ctx, "body", "red")))
                        .then(Commands.literal("black").executes(ctx -> setHealthForColor(ctx, "body", "black"))))
                    .then(Commands.literal("reset").executes(ModCommands::resetHealth)))
        );

        LOGGER.info("[EmergencyEscape] Commands registered");
    }

    private static int showColorThresholds(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cプレイヤーのみ使用可能"));
            return 0;
        }

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            int headMax = cap.getMaxHeadHealth();
            int bodyMax = cap.getMaxBodyHealth();
            float headNow = cap.getHeadHealth();
            float bodyNow = cap.getBodyHealth();

            source.sendSuccess(() -> Component.literal("§6=== ゲージ色テスト 閾値一覧 ==="), false);

            // Head thresholds
            source.sendSuccess(() -> Component.literal("§e【頭】§f 現在: " + String.format("%.0f", headNow) + "/" + headMax
                + " (" + String.format("%.1f", cap.getHeadHealthPercent()) + "%)"), false);
            source.sendSuccess(() -> Component.literal("  §b■ シアン §7: " + (int)(headMax * 0.70) + "～" + headMax + " (70%+)"), false);
            source.sendSuccess(() -> Component.literal("  §e■ イエロー §7: " + (int)(headMax * 0.50) + "～" + (int)(headMax * 0.70 - 1) + " (50～69%)"), false);
            source.sendSuccess(() -> Component.literal("  §6■ オレンジ §7: " + (int)(headMax * 0.30) + "～" + (int)(headMax * 0.50 - 1) + " (30～49%)"), false);
            source.sendSuccess(() -> Component.literal("  §c■ レッド §7: 1～" + (int)(headMax * 0.30 - 1) + " (1～29%)"), false);
            source.sendSuccess(() -> Component.literal("  §0■ ブラック §7: 0 (0%)"), false);

            // Body thresholds
            source.sendSuccess(() -> Component.literal("§e【胴】§f 現在: " + String.format("%.0f", bodyNow) + "/" + bodyMax
                + " (" + String.format("%.1f", cap.getBodyHealthPercent()) + "%)"), false);
            source.sendSuccess(() -> Component.literal("  §b■ シアン §7: " + (int)(bodyMax * 0.70) + "～" + bodyMax + " (70%+)"), false);
            source.sendSuccess(() -> Component.literal("  §e■ イエロー §7: " + (int)(bodyMax * 0.50) + "～" + (int)(bodyMax * 0.70 - 1) + " (50～69%)"), false);
            source.sendSuccess(() -> Component.literal("  §6■ オレンジ §7: " + (int)(bodyMax * 0.30) + "～" + (int)(bodyMax * 0.50 - 1) + " (30～49%)"), false);
            source.sendSuccess(() -> Component.literal("  §c■ レッド §7: 1～" + (int)(bodyMax * 0.30 - 1) + " (1～29%)"), false);
            source.sendSuccess(() -> Component.literal("  §0■ ブラック §7: 0 (0%) ※緊急脱出が発動します"), false);

            source.sendSuccess(() -> Component.literal("§a使い方: /emergencyescape testcolor <head|body> <cyan|yellow|orange|red|black>"), false);
            source.sendSuccess(() -> Component.literal("§a全回復: /emergencyescape testcolor reset"), false);
        });

        return 1;
    }

    private static int setHealthForColor(CommandContext<CommandSourceStack> context, String part, String color) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cプレイヤーのみ使用可能"));
            return 0;
        }

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.isActive()) {
                source.sendFailure(Component.literal("§c緊急脱出アイテムをインベントリに入れてください"));
                return;
            }

            int maxHealth = part.equals("head") ? cap.getMaxHeadHealth() : cap.getMaxBodyHealth();
            String partName = part.equals("head") ? "頭" : "胴";

            // Calculate target health for each color (middle of the range)
            float targetHealth;
            String colorName;
            String colorCode;
            switch (color) {
                case "cyan":
                    targetHealth = maxHealth * 0.85f; // 85% - middle of 70-100%
                    colorName = "シアン";
                    colorCode = "§b";
                    break;
                case "yellow":
                    targetHealth = maxHealth * 0.60f; // 60% - middle of 50-69%
                    colorName = "イエロー";
                    colorCode = "§e";
                    break;
                case "orange":
                    targetHealth = maxHealth * 0.40f; // 40% - middle of 30-49%
                    colorName = "オレンジ";
                    colorCode = "§6";
                    break;
                case "red":
                    targetHealth = maxHealth * 0.15f; // 15% - middle of 1-29%
                    targetHealth = Math.max(1, targetHealth); // At least 1 to avoid triggering escape
                    colorName = "レッド";
                    colorCode = "§c";
                    break;
                case "black":
                    targetHealth = 0;
                    colorName = "ブラック";
                    colorCode = "§0";
                    break;
                default:
                    source.sendFailure(Component.literal("§c不明な色: " + color));
                    return;
            }

            // Set the health
            if (part.equals("head")) {
                cap.setHeadHealth(targetHealth);
            } else {
                cap.setBodyHealth(targetHealth);
            }

            float percent = maxHealth > 0 ? (targetHealth / maxHealth) * 100f : 0;
            final float finalTarget = targetHealth;
            source.sendSuccess(() -> Component.literal(
                "§a" + partName + "の体力を " + colorCode + colorName + "§a の範囲にセット: "
                + String.format("%.0f", finalTarget) + "/" + maxHealth
                + " (" + String.format("%.1f", percent) + "%)"
            ), false);

            if (color.equals("black")) {
                source.sendSuccess(() -> Component.literal("§c※ 体力0のため緊急脱出が発動する可能性があります"), false);
            }
        });

        return 1;
    }

    private static int resetHealth(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cプレイヤーのみ使用可能"));
            return 0;
        }

        player.getCapability(BodyPartHealthCapability.CAPABILITY).ifPresent(cap -> {
            cap.reset();
            source.sendSuccess(() -> Component.literal(
                "§a頭・胴の体力をリセットしました: 頭=" + cap.getMaxHeadHealth() + " 胴=" + cap.getMaxBodyHealth()
            ), false);
        });

        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            ModConfig.SPEC.afterReload();

            context.getSource().sendSuccess(() ->
                Component.literal("§a[EmergencyEscape] 設定をリロードしました"), true);

            LOGGER.info("[EmergencyEscape] Config reloaded:");
            LOGGER.info("  - LARGE_DAMAGE_THRESHOLD: {}", ModConfig.LARGE_DAMAGE_THRESHOLD.get());
            LOGGER.info("  - ESCAPE_DEATH_DELAY: {}", ModConfig.ESCAPE_DEATH_DELAY.get());
            LOGGER.info("  - VOLUNTARY_ESCAPE_RADIUS: {}", ModConfig.VOLUNTARY_ESCAPE_RADIUS.get());
            LOGGER.info("  - HEAD_MAX_HEALTH: {}", ModConfig.HEAD_MAX_HEALTH.get());
            LOGGER.info("  - BODY_MAX_HEALTH: {}", ModConfig.BODY_MAX_HEALTH.get());
            LOGGER.info("  - RESPAWN_LEVEL: {}", ModConfig.RESPAWN_LEVEL.get());
            LOGGER.info("  - DEBUG_MODE: {}", ModConfig.DEBUG_MODE.get());

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[EmergencyEscape] 設定のリロードに失敗しました: " + e.getMessage()));
            LOGGER.error("[EmergencyEscape] Config reload failed", e);
            return 0;
        }
    }

    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6=== Emergency Escape 設定 ==="), false);
        source.sendSuccess(() -> Component.literal("§7ダメージ閾値: §f" + ModConfig.LARGE_DAMAGE_THRESHOLD.get()), false);
        source.sendSuccess(() -> Component.literal("§7脱出遅延: §f" + ModConfig.ESCAPE_DEATH_DELAY.get() + "秒"), false);
        source.sendSuccess(() -> Component.literal("§7任意脱出半径: §f" + ModConfig.VOLUNTARY_ESCAPE_RADIUS.get() + "ブロック"), false);
        source.sendSuccess(() -> Component.literal("§7任意脱出長押し: §f" + ModConfig.VOLUNTARY_ESCAPE_HOLD_TIME.get() + "ms"), false);
        source.sendSuccess(() -> Component.literal("§7頭の最大体力: §f" + ModConfig.HEAD_MAX_HEALTH.get()), false);
        source.sendSuccess(() -> Component.literal("§7胴体の最大体力: §f" + ModConfig.BODY_MAX_HEALTH.get()), false);
        source.sendSuccess(() -> Component.literal("§7リスポーンレベル: §f" + ModConfig.RESPAWN_LEVEL.get()), false);
        source.sendSuccess(() -> Component.literal("§7デバッグモード: §f" + (ModConfig.DEBUG_MODE.get() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("§7爆発音範囲: §f" + ModConfig.EXPLOSION_SOUND_RANGE.get() + "ブロック"), false);
        source.sendSuccess(() -> Component.literal("§7アラート音量: §f" + ModConfig.ALERT_VOLUME.get()), false);

        source.sendSuccess(() -> Component.literal("§6--- 部位判定の高さ閾値 ---"), false);
        source.sendSuccess(() -> Component.literal("§7頭の判定開始位置: §f" + ModConfig.HEAD_THRESHOLD_PERCENT.get() + "% §7(この高さより上が頭)"), false);
        source.sendSuccess(() -> Component.literal("§7胴の判定開始位置: §f" + ModConfig.BODY_THRESHOLD_PERCENT.get() + "% §7(この高さより上が胴)"), false);

        source.sendSuccess(() -> Component.literal("§6--- 大ダメージ設定 ---"), false);
        source.sendSuccess(() -> Component.literal("§7即時消費: §f" + (ModConfig.LARGE_DAMAGE_INSTANT_ENABLED.get() ? "ON" : "OFF") +
            " (時間:" + ModConfig.LARGE_DAMAGE_INSTANT_DURATION.get() + "s, 間隔:" + ModConfig.LARGE_DAMAGE_INSTANT_INTERVAL.get() + "s, 量:" + ModConfig.LARGE_DAMAGE_INSTANT_AMOUNT.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7持続消費: §f" + (ModConfig.LARGE_DAMAGE_SUSTAINED_ENABLED.get() ? "ON" : "OFF") +
            " (時間:" + ModConfig.LARGE_DAMAGE_SUSTAINED_DURATION.get() + "s, 間隔:" + ModConfig.LARGE_DAMAGE_SUSTAINED_INTERVAL.get() + "s, 量:" + ModConfig.LARGE_DAMAGE_SUSTAINED_AMOUNT.get() + ")"), false);

        source.sendSuccess(() -> Component.literal("§6--- 小ダメージ設定 ---"), false);
        source.sendSuccess(() -> Component.literal("§7即時消費: §f" + (ModConfig.SMALL_DAMAGE_INSTANT_ENABLED.get() ? "ON" : "OFF") +
            " (時間:" + ModConfig.SMALL_DAMAGE_INSTANT_DURATION.get() + "s, 間隔:" + ModConfig.SMALL_DAMAGE_INSTANT_INTERVAL.get() + "s, 量:" + ModConfig.SMALL_DAMAGE_INSTANT_AMOUNT.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7持続消費: §f" + (ModConfig.SMALL_DAMAGE_SUSTAINED_ENABLED.get() ? "ON" : "OFF") +
            " (時間:" + ModConfig.SMALL_DAMAGE_SUSTAINED_DURATION.get() + "s, 間隔:" + ModConfig.SMALL_DAMAGE_SUSTAINED_INTERVAL.get() + "s, 量:" + ModConfig.SMALL_DAMAGE_SUSTAINED_AMOUNT.get() + ")"), false);

        return 1;
    }
}
