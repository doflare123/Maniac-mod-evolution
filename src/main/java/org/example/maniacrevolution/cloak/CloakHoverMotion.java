package org.example.maniacrevolution.cloak;

/** Motion shared by the client controller and regression checks, in blocks per tick. */
public final class CloakHoverMotion {
    private CloakHoverMotion() {}
    public record Motion(double x, double y, double z) {}

    public static Motion calculate(float left, float forward, float yaw, double movementAttribute,
                                   double currentHeight, double targetHeight) {
        double length = Math.max(1, Math.hypot(left, forward));
        double speed = Math.max(0, movementAttribute) * 2.1585;
        double angle = Math.toRadians(yaw);
        return new Motion((left * Math.cos(angle) - forward * Math.sin(angle)) / length * speed,
                Math.max(-0.1, Math.min(0.1, targetHeight - currentHeight)),
                (forward * Math.cos(angle) + left * Math.sin(angle)) / length * speed);
    }
}
