package org.example.maniacrevolution.warden;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.network.packets.WardenScentPacket;
import org.example.maniacrevolution.network.packets.WardenSniffPacket;
import java.util.List;

public final class WardenSniffTest {
    public static void run() {
        var cycle = new WardenSniffCycle();
        var other = new WardenSniffCycle();
        check(cycle.activate(100) && cycle.preparation(100) == 10 && cycle.duration(100) == 0, "half second preparation");
        check(!cycle.activate(101) && other.activate(101), "repeat blocked, Wardens independent");
        check(cycle.preparation(109) == 1 && cycle.duration(110) == 100, "exact activation boundary");
        check(cycle.duration(209) == 1 && cycle.duration(210) == 0, "five second action boundary");
        check(cycle.cooldown(299) == 1 && !cycle.activate(299) && cycle.activate(300), "ten second cooldown from request");
        var point = new WardenScentPoint(new Vec3(1, 0.06, 2), 1, 100);
        var quiet = new WardenScentPoint(point.position(), 0.25F, 100);
        check(point.brightness(100) == 1 && quiet.brightness(100) == 0.25F, "Shift weakens scent without hiding it");
        check(point.brightness(300) == 0.5F && point.brightness(499) > 0 && point.brightness(500) == 0, "brighter twenty second fade and expiry");
        check(!new WardenScentPoint(new Vec3(Double.NaN, 0, 0), 1, 100).valid() && point.brightness(79) == 0, "invalid/far future points");
        int young = 0, old = 0;
        for (int tick = 100; tick < 140; tick += 4) {
            var p = new WardenScentPoint(Vec3.ZERO, 1, tick);
            if (p.keep(150)) young++; if (p.keep(490)) old++;
        }
        check(young == 10 && old > 0 && old < young, "old traces become sparse");
        var trail = new WardenScentTrail(); trail.sample(Vec3.ZERO, 0, false);
        for (int tick = 1; tick <= 20; tick++) trail.sample(new Vec3(tick * 0.2, 0, 0), tick, false);
        var frozen = trail.nearby(Vec3.ZERO, 20);
        check(frozen.size() > 0 && frozen.size() <= 5, "distance sampling rate bounded");
        var positions = List.copyOf(frozen);
        for (int tick = 21; tick <= 30; tick++) trail.sample(new Vec3(4, 0, 0), tick, false);
        check(trail.nearby(Vec3.ZERO, 30).equals(positions), "standing does not create a continuous target marker");
        check(trail.nearby(new Vec3(100, 0, 0), 30).isEmpty(), "local radius only");
        for (int tick = 31; tick <= 80; tick++) trail.sample(new Vec3(4 + (tick - 30) * 0.2, 0, 0), tick, true);
        check(trail.nearby(new Vec3(10, 0, 0), 80).stream().anyMatch(p -> p.strength() == 0.25F), "Shift still leaves weaker traces");
        check(frozen.equals(positions), "later movement never modifies previous packet data");
        trail.sample(new Vec3(100, 0, 0), 81, false);
        check(trail.nearby(new Vec3(100, 0, 0), 81).isEmpty(), "teleport clears old trail, no connecting points");
        trail.sample(new Vec3(101, 0, 0), 90, false);
        check(trail.nearby(new Vec3(100, 0, 0), 90).isEmpty(), "tick gap resets sampler");
        var bounded = new WardenScentTrail(); bounded.sample(Vec3.ZERO, 0, false);
        for (int tick = 1; tick <= 1000; tick++) bounded.sample(new Vec3(tick * 0.8, 0, 0), tick, false);
        check(bounded.nearby(new Vec3(800, 0, 0), 1000).size() <= WardenScentTrail.MAX_POINTS, "long movement remains bounded");
        var longTrail = new WardenScentTrail(); longTrail.sample(Vec3.ZERO, 0, false);
        for (int tick = 1; tick <= 400; tick++) longTrail.sample(new Vec3(5 * Math.sin(tick * 0.1), 0, 5 * Math.cos(tick * 0.1)), tick, false);
        check(longTrail.nearby(Vec3.ZERO, 400).stream().anyMatch(p -> 400 - p.tick() > 200), "extended trail retains positions older than ten seconds");
        check(longTrail.nearby(Vec3.ZERO, 400).size() > 64, "larger source budget retains a longer local trail");
        var dimension = new ResourceLocation("minecraft", "overworld");
        var packet = new WardenScentPacket(dimension, 150, 0, 90, 180, List.of(point, quiet));
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try { packet.encode(buf); check(packet.valid() && packet.equals(WardenScentPacket.decode(buf)), "wire snapshot/timers round trip"); }
        finally { buf.release(); }
        check(!new WardenScentPacket(dimension, 150, 10, 0, 200, List.of(point)).valid(), "no traces during preparation");
        check(new WardenScentPacket(dimension, 300, 0, 50, 100, List.of(point)).valid(), "ten second old trail still transmitted");
        check(!new WardenScentPacket(dimension, 500, 0, 50, 100, List.of(point)).valid(), "twenty second old snapshot rejected");
        check(new WardenScentPacket(dimension, 150, 0, 0, 0, List.of()).valid(), "clear packet valid");
        var request = new FriendlyByteBuf(Unpooled.buffer());
        try { new WardenSniffPacket().encode(request); check(request.readableBytes() == 0 && WardenSniffPacket.decode(request) != null, "request carries no client-supplied coordinates or time"); }
        finally { request.release(); }
        var badSize = new FriendlyByteBuf(Unpooled.buffer());
        try {
            badSize.writeResourceLocation(dimension); badSize.writeLong(150); badSize.writeVarInt(0); badSize.writeVarInt(90); badSize.writeVarInt(180);
            badSize.writeVarInt(WardenScentPacket.MAX_POINTS + 1);
            try { WardenScentPacket.decode(badSize); throw new AssertionError("over-budget packet accepted"); }
            catch (IllegalArgumentException expected) {}
        } finally { badSize.release(); }
        System.out.println("Warden sniff: preparation/action/cooldown, independent users, movement/Shift/teleport, fading/thinning/range and bounded packet checks passed.");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
