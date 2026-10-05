package org.example.maniacrevolution.warden.client;

/** Stable subsets of an existing surface grid; depth geometry is never thinned. */
final class WardenRenderBudget {
    static final long CAPTURE_INTERVAL_NS = 33_333_333L;
    private WardenRenderBudget() {}
    static int pointStride(double distanceSquared) {
        return distanceSquared > 100 ? 4 : distanceSquared > 36 ? 2 : 1;
    }
    static boolean captureDue(long now, long previous) {
        return previous == Long.MIN_VALUE || now - previous >= CAPTURE_INTERVAL_NS;
    }
}
