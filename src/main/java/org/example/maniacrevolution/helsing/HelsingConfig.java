package org.example.maniacrevolution.helsing;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server configuration; both clients and server receive the same balance values. */
public final class HelsingConfig {
    public static final int CLASS_ID = 14;
    public static final ForgeConfigSpec SPEC;
    public static final BalanceValue<Integer> PROTECTION, WEAKNESS, FREEZE, COOLDOWN,
            IRON_DURATION, RUNE_DURATION, BOLT_WEAKNESS_DURATION, STUDY, MEMORY, RELOAD, BLOOD_HITS;
    public static final BalanceValue<Double> COST, DAMAGE_REDUCTION, RUNE_MULTIPLIER,
            BOLT_DAMAGE, ALLY_RANGE, STUDY_RANGE;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("helsing");
        PROTECTION = new BalanceValue<>(b.defineInRange("protectionTicks", 300, 1, 72000));
        WEAKNESS = new BalanceValue<>(b.defineInRange("curseWeaknessTicks", 120, 1, 72000));
        FREEZE = new BalanceValue<>(b.defineInRange("cooldownFreezeTicks", 80, 1, 72000));
        COOLDOWN = new BalanceValue<>(b.defineInRange("abilityCooldownTicks", 900, 1, 72000));
        COST = new BalanceValue<>(b.defineInRange("manaCost", 30.0, 0.0, 10000.0));
        DAMAGE_REDUCTION = new BalanceValue<>(b.defineInRange("attackReduction", 0.25, 0.0, 1.0));
        ALLY_RANGE = new BalanceValue<>(b.defineInRange("allyRange", 5.0, 1.0, 32.0));
        STUDY_RANGE = new BalanceValue<>(b.defineInRange("studyRange", 24.0, 1.0, 128.0));
        STUDY = new BalanceValue<>(b.defineInRange("studyTicks", 60, 1, 72000));
        MEMORY = new BalanceValue<>(b.defineInRange("studyMemoryTicks", 40, 0, 1200));
        RELOAD = new BalanceValue<>(b.defineInRange("crossbowLoadTicks", 30, 5, 200));
        BOLT_DAMAGE = new BalanceValue<>(b.defineInRange("boltDamage", 3.0, 0.0, 100.0));
        IRON_DURATION = new BalanceValue<>(b.defineInRange("ironSlowTicks", 80, 1, 72000));
        RUNE_DURATION = new BalanceValue<>(b.defineInRange("runeDurationTicks", 160, 1, 72000));
        RUNE_MULTIPLIER = new BalanceValue<>(b.defineInRange("runeCostMultiplier", 1.5, 1.0, 10.0));
        BOLT_WEAKNESS_DURATION = new BalanceValue<>(b.defineInRange("enchantedWeaknessTicks", 120, 1, 72000));
        BLOOD_HITS = new BalanceValue<>(b.defineInRange("draculaReducedBloodHits", 3, 1, 100));
        b.pop();
        SPEC = b.build();
    }
    /** Search-tree tooltips are built before joining a world and after its server config unloads. */
    public record BalanceValue<T>(ForgeConfigSpec.ConfigValue<T> value) implements java.util.function.Supplier<T> {
        @Override public T get() { return SPEC.isLoaded() ? value.get() : value.getDefault(); }
    }
    private HelsingConfig() {}
}
