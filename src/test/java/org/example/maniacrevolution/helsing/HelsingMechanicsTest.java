package org.example.maniacrevolution.helsing;

import org.example.maniacrevolution.mana.ManaData;
import java.util.UUID;

public final class HelsingMechanicsTest {
    private static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    public static void main(String[] args) {
        checkConfigLifecycle();
        checkCharge();
        checkSilverDamage();
        ManaData mana = new ManaData(100, 0);
        double[] multiplier = {1.5}; mana.setCostMultiplier(() -> multiplier[0]);
        mana.setMana(29);
        check(!mana.hasEnoughMana(20) && !mana.consumeMana(20) && mana.getMana() == 29, "Rune must reject unaffordable activation without spending mana");
        mana.setMana(30);
        check(mana.consumeMana(20) && mana.getMana() == 0, "Rune charges exactly the adjusted price");
        mana.setMana(50); check(mana.consumeRawMana(10) && mana.getMana() == 40, "External drain does not pay rune surcharge");
        multiplier[0] = 1;
        check(mana.consumeMana(20) && mana.getMana() == 20, "Expiry restores normal cost");

        long start = 1000, duration = 3000;
        check(HelsingTiming.pauseStart(start, duration, 2050, 50) == 1050, "Wall cooldown remains stationary");
        check(HelsingTiming.pauseStart(start, duration, 9000, 50) == start, "An already ready ability stays ready");
        check(HelsingTiming.pauseStart(2030, duration, 2050, 50) == 2050, "New cooldown freezes only elapsed time after activation");
        for (int i = 0; i < 80; i++) start = HelsingTiming.pauseStart(start, duration, 2050 + i * 50L, 50);
        check(start == 5000, "Four-second freeze pauses exactly four seconds of wall time");
        check(HelsingTiming.pauseDeadline(110, 100) == 111, "Tick cooldown freezes");
        check(HelsingTiming.pauseDeadline(90, 100) == 90 && HelsingTiming.pauseDeadline(0, 100) == 0, "Ready tick cooldowns stay ready");

        HelsingStudy<String> study = new HelsingStudy<>(); UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int tick = 1; tick <= 59; tick++) study.observe(a, tick);
        check(study.held == 59 && study.intel == null, "No early disclosure");
        study.lose(60, 40); study.observe(a, 61);
        check(study.held == 1, "Interrupted acquisition restarts");
        for (int tick = 62; tick <= 120; tick++) study.observe(a, tick);
        check(study.held == 60, "Continuous acquisition succeeds"); study.intel = "known";
        study.lose(160, 40); check(study.intel != null, "Grace keeps last-known information");
        study.lose(161, 40); check(study.target == null && study.intel == null, "Grace expiry clears information");
        study.observe(a, 200); study.intel = "a"; study.observe(b, 201);
        check(study.held == 1 && study.intel == null && study.target.equals(b), "Changing prey cannot reuse another target's information");
        System.out.println("Helsing: config lifecycle, persistent crossbow charge, rune costs, cooldown freeze and prey study checks passed.");
    }
    private static void checkSilverDamage() {
        var undead = java.util.List.of(
                new org.example.maniacrevolution.character.maniac.DeathClass(),
                new org.example.maniacrevolution.character.maniac.GhostClass(),
                new org.example.maniacrevolution.character.maniac.PudgeClass(),
                new org.example.maniacrevolution.character.maniac.PlagueDoctorClass());
        for (var character : undead) {
            int id = character.getScoreboardId();
            check(HelsingSilverDamage.undead(id), "Silver recognizes " + character.getId());
            check(HelsingSilverDamage.amount(3, true, id) == 6, "Undead take double base damage in Reversal");
            check(HelsingSilverDamage.amount(3, false, id) == 1, "Outside Reversal all undead take one HP");
        }
        for (int id : new int[]{-1, 0, 1, 4, 7, 9, 11}) {
            check(!HelsingSilverDamage.undead(id), "Other classes are not undead");
            check(HelsingSilverDamage.amount(3, true, id) == 4.5F, "Other classes take 1.5 times base damage");
            check(HelsingSilverDamage.amount(100, false, id) == 1, "Non-Reversal damage ignores configured base");
        }
        check(HelsingSilverDamage.amount(8, true, 10) == 16 && HelsingSilverDamage.amount(8, true, -1) == 12,
                "Multipliers follow configured boltDamage");
    }
    private static void checkCharge() {
        var tag = new net.minecraft.nbt.CompoundTag();
        int use = HelsingCrossbowState.USE_DURATION;
        check(!HelsingCrossbowState.shouldLoad(tag, use - 29, 30), "No early loading");
        check(HelsingCrossbowState.shouldLoad(tag, use - 30, 30), "Load exactly at configured duration");
        HelsingCrossbowState.load(tag, 2);
        check(!HelsingCrossbowState.shouldLoad(tag, use - 200, 30), "Holding use cannot consume a second bolt");
        tag = tag.copy();
        tag.putInt("BoltKind", 1);
        HelsingCrossbowState.load(tag, 1);
        check(HelsingCrossbowState.loaded(tag) && tag.getInt("LoadedBolt") == 2, "Charge survives save and selecting another bolt");
        HelsingCrossbowState.fired(tag);
        check(!HelsingCrossbowState.loaded(tag) && tag.getInt("VisualBolt") == 2 && tag.getInt("BoltKind") == 1,
                "Shot consumes the loaded bolt and preserves the next selection");
    }
    private static void checkConfigLifecycle() {
        check(!HelsingConfig.SPEC.isLoaded(), "Test starts outside a world");
        check(HelsingConfig.COST.get() == 30.0 && HelsingConfig.PROTECTION.get() == 300
                && HelsingConfig.RELOAD.get() == 30, "Tooltip and crossbow defaults are readable before server config load");
        var values = java.util.List.of(HelsingConfig.PROTECTION, HelsingConfig.WEAKNESS, HelsingConfig.FREEZE,
                HelsingConfig.COOLDOWN, HelsingConfig.IRON_DURATION, HelsingConfig.RUNE_DURATION,
                HelsingConfig.BOLT_WEAKNESS_DURATION, HelsingConfig.STUDY, HelsingConfig.MEMORY,
                HelsingConfig.RELOAD, HelsingConfig.BLOOD_HITS, HelsingConfig.COST, HelsingConfig.DAMAGE_REDUCTION,
                HelsingConfig.RUNE_MULTIPLIER, HelsingConfig.BOLT_DAMAGE,
                HelsingConfig.ALLY_RANGE, HelsingConfig.STUDY_RANGE);
        for (var value : values) check(value.get() != null, "All balance values are safe outside a world");
        HelsingConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        try {
            HelsingConfig.COST.value().set(42.0);
            HelsingConfig.RELOAD.value().set(50);
            check(HelsingConfig.COST.get() == 42.0 && HelsingConfig.RELOAD.get() == 50,
                    "Loaded server values override defaults");
        } finally {
            HelsingConfig.SPEC.setConfig(null);
        }
        check(HelsingConfig.COST.get() == 30.0 && HelsingConfig.RELOAD.get() == 30,
                "After unload, resource reload uses defaults instead of cached server values");
        for (var value : values) check(value.get() != null, "All values remain safe after unload");
    }
}
