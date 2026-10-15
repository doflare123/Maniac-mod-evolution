package org.example.maniacrevolution.client.screen.radial;

public final class RadialSelectionMath {
    public static int sector(double x, double y, double deadZone, int count) {
        if (count < 1) throw new IllegalArgumentException("A wheel needs at least one entry");
        if (!Double.isFinite(x) || !Double.isFinite(y) || x * x + y * y < deadZone * deadZone) return -1;
        double step = Math.PI * 2 / count;
        return Math.floorMod((int) Math.floor((Math.atan2(x, -y) + step / 2) / step), count);
    }
    private RadialSelectionMath() {}
}
