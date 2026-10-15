package org.example.maniacrevolution.client.screen.radial;

public final class RadialSelectionMathTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (int count : new int[]{3, 4, 5, 8}) {
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2 / count;
                check(RadialSelectionMath.sector(Math.sin(angle)*100, -Math.cos(angle)*100, 40, count) == i,
                        "Sector centres agree with rendering for " + count + " entries");
                for (double offset : new double[]{-0.49, 0.49}) {
                    double edge = angle + offset * Math.PI * 2 / count;
                    check(RadialSelectionMath.sector(Math.sin(edge)*100, -Math.cos(edge)*100, 40, count) == i,
                            "Both sides of a sector boundary select the expected entry");
                }
            }
            check(RadialSelectionMath.sector(10, 10, 40, count) == -1, "Centre preserves the current choice");
            check(RadialSelectionMath.sector(Double.NaN, -100, 40, count) == -1, "NaN cannot change selection");
            check(RadialSelectionMath.sector(0, Double.POSITIVE_INFINITY, 40, count) == -1, "Infinity cannot change selection");
        }
        check(RadialSelectionMath.sector(0, -100, 40, 4) == 0, "Helsing iron stays at top");
        check(RadialSelectionMath.sector(100, 0, 40, 4) == 1, "Helsing rune stays right");
        check(RadialSelectionMath.sector(0, 100, 40, 4) == 2, "Helsing silver stays bottom");
        check(RadialSelectionMath.sector(-100, 0, 40, 4) == 3, "Helsing enchanted stays left");
        check(!org.example.maniacrevolution.techies.TechiesRules.active(true,true,1,14,"survivors"),
                "Helsing cannot activate Techies mechanics");
        check(org.example.maniacrevolution.techies.TechiesRules.active(true,true,1,15,"survivors"),
                "Techies has a distinct active class ID");
        org.example.maniacrevolution.character.CharacterRegistry.init();
        var ids = new java.util.HashSet<String>();
        for (var character : org.example.maniacrevolution.character.CharacterRegistry.getAllClasses())
            check(ids.add(character.getType() + ":" + character.getScoreboardId()), "Duplicate class ID: " + character.getId());
        System.out.println("Shared wheel: sector boundaries, 3/4/5/8 options and unique class IDs passed.");
    }
}
