package com.ronalonis.illagerblabber;

import net.neoforged.neoforge.common.ModConfigSpec;

public class IllagerBlabberConfig {
    public static final ModConfigSpec SERVER_CONFIG;

    public static ModConfigSpec.IntValue COOLDOWN_PASSIVE_MIN;
    public static ModConfigSpec.IntValue COOLDOWN_PASSIVE_RANDOM;
    public static ModConfigSpec.IntValue COOLDOWN_COMBAT_MIN;
    public static ModConfigSpec.IntValue COOLDOWN_COMBAT_RANDOM;
    public static ModConfigSpec.IntValue COOLDOWN_SPOTTED_MIN;
    public static ModConfigSpec.IntValue COOLDOWN_SPOTTED_RANDOM;
    public static ModConfigSpec.IntValue COOLDOWN_HURT_MIN;
    public static ModConfigSpec.IntValue COOLDOWN_HURT_RANDOM;
    public static ModConfigSpec.IntValue COOLDOWN_VICTORY_MIN;
    public static ModConfigSpec.IntValue COOLDOWN_VICTORY_RANDOM;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("cooldowns");

        COOLDOWN_PASSIVE_MIN = builder
                .translation("config.illagerblabber.cooldowns.passive_min")
                .defineInRange("passive_min", 30, 0, Integer.MAX_VALUE);
        COOLDOWN_PASSIVE_RANDOM = builder
                .translation("config.illagerblabber.cooldowns.passive_random")
                .defineInRange("passive_random", 60, 0, Integer.MAX_VALUE);

        COOLDOWN_COMBAT_MIN = builder
                .translation("config.illagerblabber.cooldowns.combat_min")
                .defineInRange("combat_min", 40, 0, Integer.MAX_VALUE);
        COOLDOWN_COMBAT_RANDOM = builder
                .translation("config.illagerblabber.cooldowns.combat_random")
                .defineInRange("combat_random", 60, 0, Integer.MAX_VALUE);

        COOLDOWN_SPOTTED_MIN = builder
                .translation("config.illagerblabber.cooldowns.spotted_min")
                .defineInRange("spotted_min", 100, 0, Integer.MAX_VALUE);
        COOLDOWN_SPOTTED_RANDOM = builder
                .translation("config.illagerblabber.cooldowns.spotted_random")
                .defineInRange("spotted_random", 40, 0, Integer.MAX_VALUE);

        COOLDOWN_HURT_MIN = builder
                .translation("config.illagerblabber.cooldowns.hurt_min")
                .defineInRange("hurt_min", 20, 0, Integer.MAX_VALUE);
        COOLDOWN_HURT_RANDOM = builder
                .translation("config.illagerblabber.cooldowns.hurt_random")
                .defineInRange("hurt_random", 20, 0, Integer.MAX_VALUE);

        COOLDOWN_VICTORY_MIN = builder
                .translation("config.illagerblabber.cooldowns.victory_min")
                .defineInRange("victory_min", 40, 0, Integer.MAX_VALUE);
        COOLDOWN_VICTORY_RANDOM = builder
                .translation("config.illagerblabber.cooldowns.victory_random")
                .defineInRange("victory_random", 20, 0, Integer.MAX_VALUE);

        builder.pop();

        SERVER_CONFIG = builder.build();
    }
}