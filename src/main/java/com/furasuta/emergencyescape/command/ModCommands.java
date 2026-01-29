package com.furasuta.emergencyescape.command;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                    .requires(source -> source.hasPermission(2)) // OP権限が必要
                    .executes(ModCommands::reloadConfig))
                .then(Commands.literal("config")
                    .executes(ModCommands::showConfig))
        );

        LOGGER.info("[EmergencyEscape] Commands registered");
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            // Forgeの設定は自動的に同期されるが、明示的にリロードを通知
            // 設定ファイルを強制的に再読み込み
            ModConfig.SPEC.afterReload();

            context.getSource().sendSuccess(() ->
                Component.literal("§a[EmergencyEscape] 設定をリロードしました"), true);

            // 現在の設定値をログに出力
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
