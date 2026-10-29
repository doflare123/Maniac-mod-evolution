package org.example.maniacrevolution.engineer;

public final class EngineerRulesTest {
    private static void check(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
    private static void close(float actual, float expected, String reason) { check(Math.abs(actual - expected) < .0001f, reason + ": " + actual); }
    public static void main(String[] args) {
        check(!EngineerRules.interrupted(.1 * .1, 3, -2), "Accidental movement and mouse drift preserve installation");
        check(!EngineerRules.interrupted(0, -358, 0), "Yaw wrap is not a full turn");
        check(EngineerRules.interrupted(.26 * .26, 0, 0), "Walking away cancels");
        check(EngineerRules.interrupted(0, 13, 0) && EngineerRules.interrupted(0, 0, -13), "Intentional large turns cancel");
        EngineerLedger ledger = new EngineerLedger();
        close(ledger.freshFraction(1, 0, 2), 1, "Initial personal and QTE progress is eligible");
        close(ledger.freshFraction(1, 0, 2), 0, "Rollback cannot pay for the same interval again");
        close(ledger.freshFraction(1, 1, 3), .5f, "Only the previously unseen part pays");
        ledger.observe(1, 5);
        close(ledger.freshFraction(1, 3, 5), 0, "External SberSprout/card progress cannot be farmed");
        close(ledger.freshFraction(2, 0, 2), 1, "Different computer IDs have independent budgets");
        close(ledger.freshFraction(1, Float.NaN, 10), 0, "Invalid floating-point input is ignored");
        close(EngineerRules.addArmor(0), 3, "Each portion gives three protection");
        close(EngineerRules.addArmor(18), 20, "Last portion fills only the remaining capacity");
        close(EngineerRules.addArmor(20), 20, "Protection cannot exceed twenty");
        var small = EngineerRules.absorb(20, 7);
        close(small.armor(), 13, "Small hits spend only the absorbed damage");
        close(small.damage(), 0, "Protection absorbs the entire small hit");
        var large = EngineerRules.absorb(13, 17);
        close(large.armor(), 0, "Large hits exhaust the reserve");
        close(large.damage(), 4, "Only overflow reaches HP");
        close(EngineerRules.absorb(3, .5f).armor(), 2.5f, "Fractional damage is preserved");
        close(EngineerRules.absorb(3, 0).armor(), 3, "Zero damage cannot consume protection");
        close(EngineerRules.clampArmor(Float.NaN), 0, "Invalid protection is ignored");
        System.out.println("Engineer: forgiving input, progress accounting, stacking and damage absorption passed.");
    }
}
