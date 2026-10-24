package org.example.maniacrevolution.forester;

public final class ForesterRulesTest {
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    public static void main(String[] args) {
        check(!ForesterRules.hit(29, 0), "No input before the 1.5-second delay");
        check(!ForesterRules.expired(100) && !ForesterRules.expired(129) && ForesterRules.expired(130),
                "The game lasts five whole seconds after the delay, not five seconds in total");
        check(!ForesterRules.hit(130, ForesterRules.needle(130)), "No success at or after the deadline");
        for (float target : new float[]{.2F, .35F, .5F, .65F, .8F}) {
            int hits = 0, last = -100;
            for (int age = 30; age < 130; age++) {
                float needle = ForesterRules.needle(age);
                check(needle >= 0 && needle <= 1, "Needle stays within the bar");
                if (ForesterRules.hit(age, target) && age - last >= 8) { hits++; last = age; }
            }
            check(hits >= 2, "Every target permits two separate successful presses within the game");
        }
        check(ForesterRules.REARM == 50 && ForesterRules.COLLECT == 30, "Approved interaction timings");
        check(ForesterRules.PEPPER_BURN == 300 && ForesterRules.PEPPER_EFFECT == 100, "Smoke and lingering duration are independent");
        check(ForesterRules.FLASH_DURATION == 60 && ForesterRules.FLASH_BLIND == 40, "Beam and whiteout durations are independent");
        System.out.println("Forester: delay, full game window, reachable targets, interaction and effect timing checks passed.");
    }
}
