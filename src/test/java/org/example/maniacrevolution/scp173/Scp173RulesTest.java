package org.example.maniacrevolution.scp173;

/** Run with verifyScp173; no game window or live server required. */
public final class Scp173RulesTest {
    private static int checks;
    public static void main(String[] args) {
        check(Scp173Rules.active(true, true, 1, 13, "maniac"), "active class");
        check(!Scp173Rules.active(true, true, 0, 13, "maniac"), "lobby");
        check(!Scp173Rules.active(true, true, 4, 13, "maniac"), "after match");
        check(!Scp173Rules.active(true, false, 1, 13, "maniac"), "creative/spectator statue");
        check(!Scp173Rules.active(true, true, 1, 9, "maniac"), "Warden unaffected");
        check(!Scp173Rules.active(true, true, 1, 13, "survivors"), "team change");
        check(!Scp173Rules.active(false, true, 1, 13, "maniac"), "dead statue");
        check(Scp173Rules.observer(true, true, false, "survivors"), "living survivor");
        check(!Scp173Rules.observer(true, true, true, "survivors"), "downed");
        check(!Scp173Rules.observer(false, true, false, "survivors"), "dead");
        check(!Scp173Rules.observer(true, false, false, "survivors"), "spectator/creative");
        check(!Scp173Rules.observer(true, true, false, "maniac"), "maniac observer");
        check(!Scp173Rules.observer(true, true, false, null), "no team");
        check(view(0, 0, 20, 0, 0), "20-block boundary");
        check(!view(0, 0, 20.001, 0, 0), "outside range");
        check(!view(0, 0, -2, 0, 0), "behind head, regardless of camera");
        check(view(-2, 0, 0, 90, 0), "Minecraft yaw convention");
        check(view(0, 2, 0, 0, -90), "looking straight up");
        check(!view(0, 2, 0, 0, 0), "above vertical FOV");
        check(view(0, 0, 2, 360, 0), "yaw wrapping");
        for (double yaw : new double[]{-179, 0, 179}) {
            for (double pitch : new double[]{-35, 0, 35}) {
                check(atAngles(yaw + 60, pitch + 40, yaw, pitch), "rectangular corner inside");
                check(atAngles(yaw - 60, pitch - 40, yaw, pitch), "opposite corner inside");
                check(!atAngles(yaw + 60.01, pitch, yaw, pitch), "outside horizontal edge");
                check(!atAngles(yaw, pitch + 40.01, yaw, pitch), "outside vertical edge");
            }
        }
        check(!view(Double.NaN, 0, 0, 0, 0), "NaN rejected");
        check(!view(0, 0, 1, Double.POSITIVE_INFINITY, 0), "infinite rotation rejected");
        var blink = new Scp173BlinkState();
        for (int tick = 1; tick < 120; tick++) blink.tick(tick, false);
        check(blink.remaining(119) == 0, "baseline has not expired before six seconds");
        blink.tick(120, false);
        check(blink.remaining(120) == 10 && blink.remaining(129) == 1 && blink.remaining(130) == 0, "automatic blink is half a second");
        check(!blink.blink(121), "spam cannot extend a current blink");
        var pressure = new Scp173BlinkState();
        for (int tick = 1; tick <= 300; tick++) pressure.tick(tick, true);
        check(Math.abs(pressure.pressure() - 1) < 1e-9, "15 seconds to maximum pressure");
        pressure.blink(305);
        check(Math.abs(pressure.pressure() - 1) < 1e-9, "manual blink retains pressure");
        for (int tick = 301; tick <= 450; tick++) pressure.tick(tick, false);
        check(Math.abs(pressure.pressure() - 0.5) < 1e-8, "partial recovery retains residual pressure");
        for (int tick = 451; tick <= 600; tick++) pressure.tick(tick, false);
        check(pressure.pressure() < 1e-8, "15 seconds to recover pressure");
        var first = new Scp173BlinkState(); var second = new Scp173BlinkState();
        first.blink(10); second.blink(12);
        check(first.remaining(19) > 0 && second.remaining(19) > 0 && first.remaining(20) == 0, "only overlapping blink windows release both observers");
        var ability = new Scp173AbilityState();
        check(ability.lightReady() && ability.cost() == 10, "light ready initially at ten mana");
        for (int tick = 0; tick < 100; tick++) ability.tick(2);
        check(Math.abs(ability.observation() - 130) < 1e-7, "two observers add 30 percent discount rate");
        double saved = ability.observation(); ability.tick(0);
        check(ability.observation() == saved, "discount persists without observers");
        for (int tick = 0; tick < 1100; tick++) ability.tick(1);
        check(ability.cost() == 0, "full discount costs zero mana");
        ability.usedLight();
        check(!ability.lightReady() && ability.cost() == 10, "successful activation resets discount and starts cooldown");
        for (int tick = 0; tick < 100; tick++) ability.tick(2);
        check(Math.abs(ability.lightCooldown() - 3460) < 1e-7, "cooldown runs at 1.4 independently of 1.3 discount rate");
        check(ability.melee(0) && !ability.melee(99) && ability.melee(100), "five-second strike cooldown");
        ability.changeClock(60, 1000);
        check(ability.meleeCooldown(1000) == 140
                && ability.cost() < 10, "dimension clock remap preserves cooldown and discount");
        check(Math.abs(Scp173Rules.darknessRange(1, 0) - 3) < 1e-8, "darkness pulse minimum three");
        check(Math.abs(Scp173Rules.darknessRange(1, 40) - 20) < 1e-8, "darkness pulse returns to twenty");
        check(Scp173Rules.darknessRange(0.5, 0) == 11.5, "darkness factor interpolates range");
        check(!Scp173Rules.inView(0, 0, 3.01, 0, 0, 3) && Scp173Rules.inView(0, 0, 3, 0, 0, 3), "blindness range boundary");
        check(!Scp173Rules.inView(0, 0, 1.01, 0, 0, 1), "blackout range boundary");
        for (boolean held : new boolean[]{false, true}) for (boolean target : new boolean[]{false, true}) {
            var pixels = Scp173Presentation.reticle(target, held, false);
            check(pixels.stream().allMatch(p -> pixels.stream().anyMatch(q -> q.x() == -p.x() && q.y() == -p.y() && q.color() == p.color())),
                    "reticle stays pixel-symmetric in each gameplay state");
        }
        check(Scp173Presentation.reach(0) == 0 && Scp173Presentation.reach(8) == 0, "strike begins and ends at rest");
        check(Scp173Presentation.reach(2) == 1 && Scp173Presentation.reach(5) > 0, "strike reaches and returns");
        check(Scp173Presentation.reach(Double.NaN) == 0, "invalid animation time remains at rest");
        check(Scp173Presentation.blinkClosure(10) == 0 && Scp173Presentation.blinkClosure(0) == 0, "blink starts and ends open");
        check(Scp173Presentation.blinkClosure(9) == 0.5F && Scp173Presentation.blinkClosure(1) == 0.5F, "eyelids move symmetrically");
        check(Scp173Presentation.blinkClosure(8) == 1 && Scp173Presentation.blinkClosure(2) == 1, "eyes fully shut during middle six ticks");
        check(Scp173Presentation.blinkClosure(Double.NaN) == 0, "invalid blink timing remains open");
        System.out.println("SCP-173 rules: " + checks + " checks passed.");
    }

    private static boolean atAngles(double targetYaw, double targetPitch, double yaw, double pitch) {
        double horizontal = 10 * Math.cos(Math.toRadians(targetPitch));
        return view(-horizontal * Math.sin(Math.toRadians(targetYaw)), -10 * Math.sin(Math.toRadians(targetPitch)),
                horizontal * Math.cos(Math.toRadians(targetYaw)), yaw, pitch);
    }
    private static boolean view(double x, double y, double z, double yaw, double pitch) {
        return Scp173Rules.inView(x, y, z, yaw, pitch);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
