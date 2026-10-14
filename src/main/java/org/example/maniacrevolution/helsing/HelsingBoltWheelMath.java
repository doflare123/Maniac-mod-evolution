package org.example.maniacrevolution.helsing;

/** Clockwise sectors: iron at the top, rune right, silver bottom, enchanted left. */
final class HelsingBoltWheelMath {
    static int sector(double x, double y, double deadZone) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x * x + y * y < deadZone * deadZone) return -1;
        double angle = Math.atan2(x, -y);
        return Math.floorMod((int) Math.floor((angle + Math.PI / 4) / (Math.PI / 2)), 4);
    }
    private HelsingBoltWheelMath() {}
}
