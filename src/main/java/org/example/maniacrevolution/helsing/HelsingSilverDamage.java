package org.example.maniacrevolution.helsing;

/** Silver's multipliers use the same base damage as the other bolts. */
final class HelsingSilverDamage {
    static boolean undead(int maniacClass) {
        return switch (maniacClass) {
            case 10, 8, 5, 6 -> true; // Death, Ghost, Pudge, Plague Doctor.
            default -> false;
        };
    }

    static float amount(double base, boolean reversal, int maniacClass) {
        return reversal ? (float) (base * (undead(maniacClass) ? 2.0 : 1.5)) : 1.0F;
    }

    private HelsingSilverDamage() {}
}
