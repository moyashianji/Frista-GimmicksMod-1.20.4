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
    public static final ForgeConfigSpec.IntValue EXPLOSION_SOUND_RANGE;

    // 部位体力設定
    public static final ForgeConfigSpec.IntValue HEAD_MAX_HEALTH;
    public static final ForgeConfigSpec.IntValue BODY_MAX_HEALTH;

    // 部位判定の高さ閾値設定
    public static final ForgeConfigSpec.IntValue HEAD_THRESHOLD_PERCENT;
    public static final ForgeConfigSpec.IntValue BODY_THRESHOLD_PERCENT;

    // アラート設定
    public static final ForgeConfigSpec.DoubleValue ALERT_VOLUME;

    // リスポーン設定
    public static final ForgeConfigSpec.IntValue RESPAWN_LEVEL;

    // 足ダメージ加算デバフ設定
    public static final ForgeConfigSpec.BooleanValue LEG_BONUS_ENABLED;
    public static final ForgeConfigSpec.IntValue LEG_BONUS1_THRESHOLD;
    public static final ForgeConfigSpec.IntValue LEG_BONUS2_THRESHOLD;
    public static final ForgeConfigSpec.IntValue LEG_SPEED_SAMPLE_INTERVAL;
    public static final ForgeConfigSpec.IntValue LEG_BONUS_CONSUME_INTERVAL;
    public static final ForgeConfigSpec.IntValue LEG_RANK_HOLD_TICKS;
    public static final ForgeConfigSpec.DoubleValue LEG_LARGE_FALL_DISTANCE;
    public static final ForgeConfigSpec.IntValue LEG_EFFECT_INTERVAL;
    public static final ForgeConfigSpec.IntValue LEG_EFFECT_COUNT;
    public static final ForgeConfigSpec.DoubleValue LEG_SPEED_T1;
    public static final ForgeConfigSpec.DoubleValue LEG_SPEED_T2;
    public static final ForgeConfigSpec.DoubleValue LEG_SPEED_T3;
    public static final ForgeConfigSpec.DoubleValue LEG_SPEED_T4;
    public static final ForgeConfigSpec.IntValue LEG_R1_AMOUNT1;
    public static final ForgeConfigSpec.IntValue LEG_R2_AMOUNT1;
    public static final ForgeConfigSpec.IntValue LEG_R3_AMOUNT1;
    public static final ForgeConfigSpec.IntValue LEG_R4_AMOUNT1;
    public static final ForgeConfigSpec.IntValue LEG_R5_AMOUNT1;
    public static final ForgeConfigSpec.IntValue LEG_R1_AMOUNT2;
    public static final ForgeConfigSpec.IntValue LEG_R2_AMOUNT2;
    public static final ForgeConfigSpec.IntValue LEG_R3_AMOUNT2;
    public static final ForgeConfigSpec.IntValue LEG_R4_AMOUNT2;
    public static final ForgeConfigSpec.IntValue LEG_R5_AMOUNT2;

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
        EXPLOSION_SOUND_RANGE = BUILDER
                .comment("緊急脱出の爆発音が聞こえる範囲（ブロック）")
                .defineInRange("explosionSoundRange", 30, 1, 200);
        BUILDER.pop();

        BUILDER.comment("部位体力設定").push("bodyPartHealth");
        HEAD_MAX_HEALTH = BUILDER
                .comment("頭の最大体力")
                .defineInRange("headMaxHealth", 10, 1, 1000);
        BODY_MAX_HEALTH = BUILDER
                .comment("胴体の最大体力")
                .defineInRange("bodyMaxHealth", 40, 1, 1000);
        BUILDER.pop();

        BUILDER.comment("部位判定の高さ閾値設定（プレイヤーの身長に対する%）").push("bodyPartThreshold");
        HEAD_THRESHOLD_PERCENT = BUILDER
                .comment("頭の判定開始位置（%）- この高さより上が頭判定")
                .defineInRange("headThresholdPercent", 75, 1, 100);
        BODY_THRESHOLD_PERCENT = BUILDER
                .comment("胴体の判定開始位置（%）- この高さより上が胴体判定（頭の閾値より下の部分）")
                .defineInRange("bodyThresholdPercent", 38, 1, 100);
        BUILDER.pop();

        BUILDER.comment("アラート設定").push("alert");
        ALERT_VOLUME = BUILDER
                .comment("部位体力アラートの音量（0.0〜2.0）")
                .defineInRange("alertVolume", 1.0, 0.0, 2.0);
        BUILDER.pop();

        BUILDER.comment("リスポーン設定").push("respawn");
        RESPAWN_LEVEL = BUILDER
                .comment("リスポーン時に付与される経験値レベル")
                .defineInRange("respawnLevel", 1000, 0, 21863);
        BUILDER.pop();

        BUILDER.comment("足ダメージ加算デバフ設定（足への累積ダメージで負傷し、移動速度に応じてレベルが余計に消費される）").push("legDamage");
        LEG_BONUS_ENABLED = BUILDER
                .comment("足ダメージ加算デバフを有効にする")
                .define("enabled", true);
        LEG_BONUS1_THRESHOLD = BUILDER
                .comment("加算1になる足の累積ダメージ")
                .defineInRange("bonus1Threshold", 5, 1, 1000);
        LEG_BONUS2_THRESHOLD = BUILDER
                .comment("加算2になる足の累積ダメージ（この値で頭打ち。以降は蓄積しない）")
                .defineInRange("bonus2Threshold", 10, 1, 1000);
        LEG_SPEED_SAMPLE_INTERVAL = BUILDER
                .comment("移動速度をサンプリングする間隔（tick）")
                .defineInRange("speedSampleIntervalTicks", 3, 1, 20);
        LEG_BONUS_CONSUME_INTERVAL = BUILDER
                .comment("加算消費を行う間隔（tick）")
                .defineInRange("consumeIntervalTicks", 7, 1, 100);
        LEG_RANK_HOLD_TICKS = BUILDER
                .comment("速度ランクを維持する時間（tick）。一瞬の速度変化で消費がガタつかないようにする")
                .defineInRange("rankHoldTicks", 40, 1, 200);
        LEG_LARGE_FALL_DISTANCE = BUILDER
                .comment("この落下距離（ブロック）を超える落下のみ、鉛直速度を速度判定に加える（階段等の誤検知防止）")
                .defineInRange("largeFallDistance", 3.0, 0.0, 256.0);
        LEG_EFFECT_INTERVAL = BUILDER
                .comment("加算エフェクト（白/紫ガス）を出す間隔（tick）。20tick=1秒。※黒ガス(経験値消費)が出ている時のみ発生")
                .defineInRange("effectIntervalTicks", 50, 5, 600);
        LEG_EFFECT_COUNT = BUILDER
                .comment("加算エフェクト1回あたりの粒の数（白/紫それぞれ）")
                .defineInRange("effectCount", 1, 1, 20);

        BUILDER.comment("速度ランクの区切り（blocks/tick）。①<T1 / T1<=②<T2 / T2<=③<T3 / T3<=④<T4 / ⑤>=T4").push("speedBands");
        LEG_SPEED_T1 = BUILDER.comment("ランク①(静止)とランク②(歩行)の境界").defineInRange("speedT1", 0.05, 0.0, 100.0);
        LEG_SPEED_T2 = BUILDER.comment("ランク②とランク③の境界").defineInRange("speedT2", 0.15, 0.0, 100.0);
        LEG_SPEED_T3 = BUILDER.comment("ランク③とランク④の境界").defineInRange("speedT3", 0.22, 0.0, 100.0);
        LEG_SPEED_T4 = BUILDER.comment("ランク④とランク⑤(走行/大落下)の境界").defineInRange("speedT4", 0.30, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("加算1時の速度ランク別・消費レベル（consumeIntervalTicks毎）").push("consumeBonus1");
        LEG_R1_AMOUNT1 = BUILDER.comment("ランク①(静止)").defineInRange("rank1", 0, 0, 1000);
        LEG_R2_AMOUNT1 = BUILDER.comment("ランク②(歩行)").defineInRange("rank2", 1, 0, 1000);
        LEG_R3_AMOUNT1 = BUILDER.comment("ランク③").defineInRange("rank3", 3, 0, 1000);
        LEG_R4_AMOUNT1 = BUILDER.comment("ランク④").defineInRange("rank4", 4, 0, 1000);
        LEG_R5_AMOUNT1 = BUILDER.comment("ランク⑤(走行/大落下)").defineInRange("rank5", 6, 0, 1000);
        BUILDER.pop();

        BUILDER.comment("加算2時の速度ランク別・消費レベル（consumeIntervalTicks毎）").push("consumeBonus2");
        LEG_R1_AMOUNT2 = BUILDER.comment("ランク①(静止)").defineInRange("rank1", 0, 0, 1000);
        LEG_R2_AMOUNT2 = BUILDER.comment("ランク②(歩行)").defineInRange("rank2", 2, 0, 1000);
        LEG_R3_AMOUNT2 = BUILDER.comment("ランク③").defineInRange("rank3", 5, 0, 1000);
        LEG_R4_AMOUNT2 = BUILDER.comment("ランク④").defineInRange("rank4", 6, 0, 1000);
        LEG_R5_AMOUNT2 = BUILDER.comment("ランク⑤(走行/大落下)").defineInRange("rank5", 8, 0, 1000);
        BUILDER.pop();

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
