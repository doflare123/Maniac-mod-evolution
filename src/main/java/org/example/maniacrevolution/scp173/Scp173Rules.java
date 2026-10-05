package org.example.maniacrevolution.scp173;

/** Dependency-free rules shared by the server and focused checks. */
public final class Scp173Rules {
    public static final double RANGE = 20;
    private Scp173Rules() {}

    public static double darknessRange(double factor, double tick) {
        double pulse = Math.max(0, Math.cos(tick * Math.PI * 0.025));
        return 20 - 17 * Math.max(0, Math.min(1, factor)) * pulse;
    }

    public static boolean active(boolean alive, boolean survivalAdventure, int phase, int classId, String team) {
        return alive && survivalAdventure && phase >= 1 && phase <= 3 && classId == 13 && "maniac".equals(team);
    }

    public static boolean observer(boolean alive, boolean survivalAdventure, boolean downed, String team) {
        return alive && survivalAdventure && !downed && "survivors".equals(team);
    }

    /** Minecraft yaw: 0 faces +Z; positive pitch looks down. Axes are checked separately. */
    public static boolean inView(double dx, double dy, double dz, double yaw, double pitch) {
        return inView(dx, dy, dz, yaw, pitch, RANGE);
    }

    public static boolean inView(double dx, double dy, double dz, double yaw, double pitch, double range) {
        if (!Double.isFinite(dx + dy + dz + yaw + pitch)) return false;
        if (!Double.isFinite(range) || range < 0 || dx * dx + dy * dy + dz * dz > range * range + 1.0e-8) return false;
        double horizontal = Math.hypot(dx, dz);
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double yawDifference = Math.IEEEremainder(targetYaw - yaw, 360);
        double targetPitch = -Math.toDegrees(Math.atan2(dy, horizontal));
        return (horizontal < 1.0e-8 || Math.abs(yawDifference) <= 60 + 1.0e-8)
                && Math.abs(targetPitch - pitch) <= 40 + 1.0e-8;
    }
}
