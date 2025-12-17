package com.furasuta.emergencyescape.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Damage threshold
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_THRESHOLD;

    // Large damage - Instant consumption
    public static final ForgeConfigSpec.BooleanValue LARGE_DAMAGE_INSTANT_ENABLED;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_INSTANT_DURATION;
    public static final ForgeConfigSpec.DoubleValue LARGE_DAMAGE_INSTANT_INTERVAL;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_INSTANT_AMOUNT;

    // Large damage - Sustained consumption
    public static final ForgeConfigSpec.BooleanValue LARGE_DAMAGE_SUSTAINED_ENABLED;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_DURATION;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_INTERVAL;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_SUSTAINED_AMOUNT;

    // Small damage - Instant consumption
    public static final ForgeConfigSpec.BooleanValue SMALL_DAMAGE_INSTANT_ENABLED;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_INSTANT_DURATION;
    public static final ForgeConfigSpec.DoubleValue SMALL_DAMAGE_INSTANT_INTERVAL;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_INSTANT_AMOUNT;

    // Small damage - Sustained consumption
    public static final ForgeConfigSpec.BooleanValue SMALL_DAMAGE_SUSTAINED_ENABLED;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_DURATION;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_INTERVAL;
    public static final ForgeConfigSpec.IntValue SMALL_DAMAGE_SUSTAINED_AMOUNT;

    // Emergency escape settings
    public static final ForgeConfigSpec.IntValue ESCAPE_DEATH_DELAY;
    public static final ForgeConfigSpec.IntValue VOLUNTARY_ESCAPE_RADIUS;
    public static final ForgeConfigSpec.IntValue VOLUNTARY_ESCAPE_HOLD_TIME;

    // Body part health
    public static final ForgeConfigSpec.IntValue HEAD_MAX_HEALTH;
    public static final ForgeConfigSpec.IntValue BODY_MAX_HEALTH;

    static {
        BUILDER.comment("Emergency Escape Mod Configuration").push("general");

        BUILDER.comment("Damage Settings").push("damage");
        LARGE_DAMAGE_THRESHOLD = BUILDER
                .comment("Damage amount threshold for large damage (damage >= this value is large)")
                .defineInRange("largeDamageThreshold", 5, 1, 100);
        BUILDER.pop();

        BUILDER.comment("Large Damage Experience Consumption Settings").push("largeDamage");

        BUILDER.comment("Instant Consumption").push("instant");
        LARGE_DAMAGE_INSTANT_ENABLED = BUILDER
                .comment("Enable instant experience consumption for large damage")
                .define("enabled", true);
        LARGE_DAMAGE_INSTANT_DURATION = BUILDER
                .comment("Duration in seconds for instant consumption")
                .defineInRange("duration", 6, 1, 60);
        LARGE_DAMAGE_INSTANT_INTERVAL = BUILDER
                .comment("Interval in seconds between each consumption")
                .defineInRange("interval", 0.5, 0.1, 10.0);
        LARGE_DAMAGE_INSTANT_AMOUNT = BUILDER
                .comment("Experience points consumed each time")
                .defineInRange("amount", 10, 1, 1000);
        BUILDER.pop();

        BUILDER.comment("Sustained Consumption").push("sustained");
        LARGE_DAMAGE_SUSTAINED_ENABLED = BUILDER
                .comment("Enable sustained experience consumption for large damage")
                .define("enabled", true);
        LARGE_DAMAGE_SUSTAINED_DURATION = BUILDER
                .comment("Duration in seconds for sustained consumption")
                .defineInRange("duration", 90, 1, 600);
        LARGE_DAMAGE_SUSTAINED_INTERVAL = BUILDER
                .comment("Interval in seconds between each consumption")
                .defineInRange("interval", 1, 1, 60);
        LARGE_DAMAGE_SUSTAINED_AMOUNT = BUILDER
                .comment("Experience points consumed each time")
                .defineInRange("amount", 1, 1, 1000);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Small Damage Experience Consumption Settings").push("smallDamage");

        BUILDER.comment("Instant Consumption").push("instant");
        SMALL_DAMAGE_INSTANT_ENABLED = BUILDER
                .comment("Enable instant experience consumption for small damage")
                .define("enabled", true);
        SMALL_DAMAGE_INSTANT_DURATION = BUILDER
                .comment("Duration in seconds for instant consumption")
                .defineInRange("duration", 6, 1, 60);
        SMALL_DAMAGE_INSTANT_INTERVAL = BUILDER
                .comment("Interval in seconds between each consumption")
                .defineInRange("interval", 0.5, 0.1, 10.0);
        SMALL_DAMAGE_INSTANT_AMOUNT = BUILDER
                .comment("Experience points consumed each time")
                .defineInRange("amount", 1, 1, 1000);
        BUILDER.pop();

        BUILDER.comment("Sustained Consumption").push("sustained");
        SMALL_DAMAGE_SUSTAINED_ENABLED = BUILDER
                .comment("Enable sustained experience consumption for small damage")
                .define("enabled", true);
        SMALL_DAMAGE_SUSTAINED_DURATION = BUILDER
                .comment("Duration in seconds for sustained consumption")
                .defineInRange("duration", 30, 1, 600);
        SMALL_DAMAGE_SUSTAINED_INTERVAL = BUILDER
                .comment("Interval in seconds between each consumption")
                .defineInRange("interval", 2, 1, 60);
        SMALL_DAMAGE_SUSTAINED_AMOUNT = BUILDER
                .comment("Experience points consumed each time")
                .defineInRange("amount", 1, 1, 1000);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Emergency Escape Settings").push("emergencyEscape");
        ESCAPE_DEATH_DELAY = BUILDER
                .comment("Seconds until death after emergency escape activates")
                .defineInRange("deathDelay", 4, 1, 30);
        VOLUNTARY_ESCAPE_RADIUS = BUILDER
                .comment("Radius in blocks - cannot voluntary escape if enemy player is within this range")
                .defineInRange("voluntaryEscapeRadius", 30, 1, 200);
        VOLUNTARY_ESCAPE_HOLD_TIME = BUILDER
                .comment("Time in milliseconds to hold P key to trigger voluntary escape")
                .defineInRange("voluntaryEscapeHoldTime", 1000, 100, 5000);
        BUILDER.pop();

        BUILDER.comment("Body Part Health Settings").push("bodyPartHealth");
        HEAD_MAX_HEALTH = BUILDER
                .comment("Maximum head health")
                .defineInRange("headMaxHealth", 10, 1, 100);
        BODY_MAX_HEALTH = BUILDER
                .comment("Maximum body (torso) health")
                .defineInRange("bodyMaxHealth", 40, 1, 200);
        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
