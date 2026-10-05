package org.example.maniacrevolution.warden;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket;
import org.example.maniacrevolution.network.packets.WardenShriekSoundPacket;

public final class WardenShriekerTest {
    public static void run() {
        check(!new WardenShriekerData().enabled(), "disabled by default");
        var data = new WardenShriekerData(); data.setEnabled(true);
        check(data.isDirty() && WardenShriekerData.load(data.save(new CompoundTag())).enabled(), "enabled flag persists");
        data.setEnabled(false); check(!WardenShriekerData.load(data.save(new CompoundTag())).enabled(), "disabled flag persists");
        for (boolean enabled : new boolean[]{false, true}) for (boolean alive : new boolean[]{false, true})
            for (boolean mode : new boolean[]{false, true}) for (boolean cooling : new boolean[]{false, true})
                for (int phase = 0; phase < 5; phase++) for (String team : new String[]{null, "survivors", "maniac", "other"})
                    check(WardenShriekerRules.trigger(enabled, alive, mode, phase, team, cooling)
                            == (enabled && alive && mode && !cooling && phase >= 1 && phase <= 3 && "survivors".equals(team)), "trigger matrix");
        check(WardenShriekerRules.fresh(100, 259) && !WardenShriekerRules.fresh(100, 260), "8 second expiry");
        check(!WardenShriekerRules.fresh(121, 100) && !WardenShriekerRules.fresh(-1, 100), "invalid timestamp");
        var cycle = new WardenShriekerCycle(0);
        cycle.start(100);
        check(cycle.shrieking(189) && !cycle.shrieking(190), "vanilla length scream");
        check(cycle.cooling(199) && !cycle.cooling(200), "five second cooldown");
        cycle.stop(); check(!cycle.shrieking(110) && cycle.cooling(110), "disable stops scream without skipping cooldown");
        var loaded = new WardenShriekerCycle(cycle.savedCooldown());
        check(loaded.cooling(199) && !loaded.cooling(200) && !loaded.shrieking(110), "reload preserves cooldown, never restarts scream");
        cycle.start(200);
        check(cycle.shrieking(289) && cycle.cooling(299), "new activation after cooldown restarts scream");
        var dimension = new ResourceLocation("minecraft", "overworld");
        for (var pose : Pose.values()) {
            var frozen = new WardenEchoPose(new Vec3(1, 2, 3), 12, 45, -17, 9, 0.7F, 0.4F, 0.5F, 400, pose, true, true);
            var packet = new WardenShriekerEchoPacket(dimension, new Vec3(1, 2, 2), 100, frozen);
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            try { packet.encode(buf); check(packet.equals(WardenShriekerEchoPacket.decode(buf)) && packet.valid(), "frozen pose round trip"); }
            finally { buf.release(); }
        }
        for (boolean start : new boolean[]{false, true}) {
            var packet = new WardenShriekSoundPacket(dimension, new BlockPos(-2, 50, 9), start);
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            try { packet.encode(buf); check(packet.equals(WardenShriekSoundPacket.decode(buf)), "independent sound start/stop"); }
            finally { buf.release(); }
        }
        var bad = new WardenEchoPose(new Vec3(Double.NaN, 0, 0), 0, 0, 0, 0, 0, 0, 0, 1, Pose.STANDING, false, false);
        check(!bad.valid(), "invalid pose rejected");
        var far = new WardenEchoPose(new Vec3(100, 0, 0), 0, 0, 0, 0, 0, 0, 0, 1, Pose.STANDING, false, false);
        check(new WardenShriekerEchoPacket(dimension, Vec3.ZERO, 100, far).valid(), "sensor-relayed pose outside local impulse stays valid");
        check(WardenShriekerRules.LISTENER_RADIUS == 8, "vanilla shrieker sensor vibration radius");
        System.out.println("Warden shrieker: eligibility, saved enable flag, expiry, frozen pose and sound packet checks passed.");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
