package com.furasuta.emergencyescape.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ダメージ閾値
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_THRESHOLD;

    // 大ダメージ - 即時消費
    public static final ForgeConfigSpec.BooleanValue LARGE_DAMAGE_INSTANT_ENABLED;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_INSTANT_DURATION;
    public static final ForgeConfigSpec.DoubleValue LARGE_DAMAGE_INSTANT_INTERVAL;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_INSTANT_AMOUNT;

    // 大ダメージ - 持続消費
    public static final ForgeConfigSpec.BooleanValue LARGE_DAMAGE_SUSTAINED_ENABLED;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_DURATION;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_INTERVAL;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_AMOUNT;

    // 小ダメージ - 即時消費
    public static final ForgeConfigSpec.BooleanValue SMALL_DAMAGE_INSTANT_ENABLED;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_INSTANT_DURATION;
    public static final ForgeConfigSpec.DoubleValue SMALL_DAMAGE_INSTANT_INTERVAL;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_INSTANT_AMOUNT;

    // 小ダメージ - 持続消費
    public static final ForgeConfigSpec.BooleanValue SMALL_DAMAGE_SUSTAINED_ENABLED;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_DURATION;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_INTERVAL;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_AMOUNT;

    // 緊急脱出設定
    public static final ForgeConfigSpec.IntValue ESCAPE_DEATH_DELAY;
    public static final ForgeConfigSpec.IntValue VOLUNTARY_ESCAPE_RADIUS;
    public static final ForgeConfigSpec.IntValue VOLUNTARY_ESCAPE_HOLD_TIME;

    // 部位体力設定
    public static final ForgeConfigSpec.IntValue HEAD_MAX_HEALTH;
    public static final ForgeConfigSpec.IntValue BODY_MAX_HEALTH;

    // リスポーン設定
    public static final ForgeConfigSpec.IntValue RESPAWN_LEVEL;

    // デバッグ設定
    public static final ForgeConfigSpec.BooleanValue DEBUG_MODE;

    static {
        BUILDER.comment("緊急脱出MOD設定").push("general");

        BUILDER.comment("ダメージ設定").push("damage");
        LARGE_DAMAGE_THRESHOLD = BUILDER
                .comment("大ダメージの閾値（この値以上のダメージは大ダメージとして扱われる）")
                .defineInRange("largeDamageThreshold", 5, 1, 100);
        BUILDER.pop();

        BUILDER.comment("大ダメージ時のレベル消費設定（経験値ではなくレベル単位）").push("largeDamage");

        BUILDER.comment("即時消費").push("instant");
        LARGE_DAMAGE_INSTANT_ENABLED = BUILDER
                .comment("大ダメージ時の即時レベル消費を有効にする")
                .define("enabled", true);
        LARGE_DAMAGE_INSTANT_DURATION = BUILDER
                .comment("即時消費の持続時間（秒）")
                .defineInRange("duration", 6, 1, 60);
        LARGE_DAMAGE_INSTANT_INTERVAL = BUILDER
                .comment("消費の間隔（秒）")
                .defineInRange("interval", 0.5, 0.1, 10.0);
        LARGE_DAMAGE_INSTANT_AMOUNT = BUILDER
                .comment("1回あたりの消費レベル数")
                .defineInRange("amount", 2, 1, 100);
        BUILDER.pop();

        BUILDER.comment("持続消費").push("sustained");
        LARGE_DAMAGE_SUSTAINED_ENABLED = BUILDER
                .comment("大ダメージ時の持続レベル消費を有効にする")
                .define("enabled", true);
        LARGE_DAMAGE_SUSTAINED_DURATION = BUILDER
                .comment("持続消費の持続時間（秒）")
                .defineInRange("duration", 90, 1, 600);
        LARGE_DAMAGE_SUSTAINED_INTERVAL = BUILDER
                .comment("消費の間隔（秒）")
                .defineInRange("interval", 3, 1, 60);
        LARGE_DAMAGE_SUSTAINED_AMOUNT = BUILDER
                .comment("1回あたりの消費レベル数")
                .defineInRange("amount", 1, 1, 100);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("小ダメージ時のレベル消費設定（経験値ではなくレベル単位）").push("smallDamage");

        BUILDER.comment("即時消費").push("instant");
        SMALL_DAMAGE_INSTANT_ENABLED = BUILDER
                .comment("小ダメージ時の即時レベル消費を有効にする")
                .define("enabled", true);
        SMALL_DAMAGE_INSTANT_DURATION = BUILDER
                .comment("即時消費の持続時間（秒）")
                .defineInRange("duration", 6, 1, 60);
        SMALL_DAMAGE_INSTANT_INTERVAL = BUILDER
                .comment("消費の間隔（秒）")
                .defineInRange("interval", 1.0, 0.1, 10.0);
        SMALL_DAMAGE_INSTANT_AMOUNT = BUILDER
                .comment("1回あたりの消費レベル数")
                .defineInRange("amount", 1, 1, 100);
        BUILDER.pop();

        BUILDER.comment("持続消費").push("sustained");
        SMALL_DAMAGE_SUSTAINED_ENABLED = BUILDER
                .comment("小ダメージ時の持続レベル消費を有効にする")
                .define("enabled", true);
        SMALL_DAMAGE_SUSTAINED_DURATION = BUILDER
                .comment("持続消費の持続時間（秒）")
                .defineInRange("duration", 30, 1, 600);
        SMALL_DAMAGE_SUSTAINED_INTERVAL = BUILDER
                .comment("消費の間隔（秒）")
                .defineInRange("interval", 5, 1, 60);
        SMALL_DAMAGE_SUSTAINED_AMOUNT = BUILDER
                .comment("1回あたりの消費レベル数")
                .defineInRange("amount", 1, 1, 100);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("緊急脱出設定").push("emergencyEscape");
        ESCAPE_DEATH_DELAY = BUILDER
                .comment("緊急脱出発動後、死亡するまでの時間（秒）")
                .defineInRange("deathDelay", 4, 1, 30);
        VOLUNTARY_ESCAPE_RADIUS = BUILDER
                .comment("任意脱出の敵プレイヤー検知半径（ブロック）- この範囲内に敵がいると任意脱出不可")
                .defineInRange("voluntaryEscapeRadius", 30, 1, 200);
        VOLUNTARY_ESCAPE_HOLD_TIME = BUILDER
                .comment("任意脱出を発動するためのPキー長押し時間（ミリ秒）")
                .defineInRange("voluntaryEscapeHoldTime", 1000, 100, 5000);
        BUILDER.pop();

        BUILDER.comment("部位体力設定").push("bodyPartHealth");
        HEAD_MAX_HEALTH = BUILDER
                .comment("頭の最大体力")
                .defineInRange("headMaxHealth", 10, 1, 100);
        BODY_MAX_HEALTH = BUILDER
                .comment("胴体の最大体力")
                .defineInRange("bodyMaxHealth", 40, 1, 200);
        BUILDER.pop();

        BUILDER.comment("リスポーン設定").push("respawn");
        RESPAWN_LEVEL = BUILDER
                .comment("リスポーン時に付与される経験値レベル")
                .defineInRange("respawnLevel", 1000, 0, 21863);
        BUILDER.pop();

        BUILDER.comment("デバッグ設定").push("debug");
        DEBUG_MODE = BUILDER
                .comment("デバッグモード - ヒット検知情報をチャットとコンソールに表示")
                .define("debugMode", false);
        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
