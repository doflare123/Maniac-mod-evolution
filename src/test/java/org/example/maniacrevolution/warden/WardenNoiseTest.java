package org.example.maniacrevolution.warden;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.network.packets.WardenNoisePacket;

import java.util.UUID;

/** Pure noise policies, movement and wire data; no Minecraft world or GPU is constructed. */
public final class WardenNoiseTest {
    private static final ResourceLocation WORLD = new ResourceLocation("minecraft", "overworld");
    private static final UUID FIRST = new UUID(1, 2), SECOND = new UUID(3, 4);
    public static void run() {
        movement(); profilesAndRouting(); batchesAndThrottle(); packets(); hiddenSources();
        System.out.println("Warden server noise: steps/Shift/jump/landing/teleport, roles, range/expiry, budgets/coalescing and packet checks passed.");
    }
    private static WardenNoise noise(UUID source, Vec3 pos, WardenNoise.Kind kind, float strength, long tick) {
        return new WardenNoise(WORLD, source, pos, kind, strength, 16, tick);
    }
    private static void movement() {
        var walk = new WardenMovementNoise();
        check(walk.sample(Vec3.ZERO, 0, true, false, false) == null, "initial baseline is silent");
        int steps = 0;
        for (int tick = 1; tick <= 20; tick++) {
            var kind = walk.sample(new Vec3(tick * 0.2, 0, 0), tick, true, false, false);
            if (kind != null) { check(kind == WardenNoise.Kind.STEP, "walk produces steps only"); steps++; }
        }
        check(steps == 3, "steps depend on distance rather than one pulse each tick");
        for (int tick = 21; tick <= 40; tick++)
            check(walk.sample(new Vec3(4, 0, 0), tick, true, false, false) == null, "standing silent");
        var sprint = new WardenMovementNoise();
        sprint.sample(Vec3.ZERO, 0, true, true, false);
        int sprintSteps = 0;
        for (int tick = 1; tick <= 20; tick++) {
            var kind = sprint.sample(new Vec3(tick * 0.4, 0, 0), tick, true, true, false);
            if (kind != null) { check(kind == WardenNoise.Kind.SPRINT, "sprint profile"); sprintSteps++; }
        }
        check(sprintSteps > 0 && sprintSteps <= 5, "sprint rate bounded");
        var sneak = new WardenMovementNoise();
        sneak.sample(Vec3.ZERO, 0, true, false, false);
        for (int tick = 1; tick <= 10; tick++)
            check(sneak.sample(new Vec3(tick * 0.2, 0, 0), tick, true, false, true) == null, "Shift steps silent");
        check(sneak.sample(new Vec3(2.2, 0, 0), 11, true, false, false) == null, "no accumulated step after Shift");
        var jump = new WardenMovementNoise();
        jump.sample(Vec3.ZERO, 0, true, false, false);
        check(jump.sample(new Vec3(0, 0.3, 0), 1, false, false, false) == WardenNoise.Kind.JUMP, "jump takeoff");
        check(jump.sample(Vec3.ZERO, 2, true, false, false) == WardenNoise.Kind.LAND, "landing includes final falling displacement");
        check(jump.sample(new Vec3(0, 0.3, 0), 3, false, false, true) == null, "Shift jump silent");
        check(jump.sample(Vec3.ZERO, 4, true, false, true) == null, "Shift landing silent");
        check(jump.sample(new Vec3(40, 10, 0), 5, false, false, false) == null, "teleport does not emit jump");
        check(jump.sample(new Vec3(40, 0, 0), 6, true, false, false) == null, "teleport resets fall accumulator");
        check(jump.sample(new Vec3(41, 0, 0), 10, true, false, false) == null, "missing ticks reset baseline");
        var falling = new WardenMovementNoise();
        falling.sample(Vec3.ZERO, 0, true, false, false);
        check(falling.sample(new Vec3(0, -0.3, 0), 1, false, false, false) == null, "falling off ledge is not a jump");
        check(falling.sample(new Vec3(0, -0.7, 0), 2, true, false, false) == WardenNoise.Kind.LAND, "ledge landing");
    }
    private static void profilesAndRouting() {
        var quiet = noise(FIRST, Vec3.ZERO, WardenNoise.Kind.STEP, 0.25F, 100);
        var loud = noise(FIRST, Vec3.ZERO, WardenNoise.Kind.QTE, 1, 100);
        var distantStep = new WardenNoise(WORLD, FIRST, new Vec3(49.9, 0, 0), WardenNoise.Kind.STEP,
                0.25F, WardenMatchRules.VISION_RANGE, 100);
        check(distantStep.audible(WORLD, Vec3.ZERO) && !distantStep.audible(WORLD, new Vec3(-0.2, 0, 0)),
                "steps delivered at 49.9 blocks, not beyond 50");
        check(distantStep.radius() == quiet.radius(), "longer hearing preserves local noise reveal area");
        check(quiet.valid() && quiet.radius() == 7.5 && quiet.duration() == 18, "existing volume curve for weak noise");
        check(loud.radius() == 12 && loud.duration() == 28, "bounded strong profile fits surface cache");
        check(loud.audible(WORLD, new Vec3(15.9, 0, 0)), "listener in hearing range");
        check(!loud.audible(WORLD, new Vec3(16, 0, 0)), "hearing boundary is exclusive");
        var nether = new ResourceLocation("minecraft", "the_nether");
        check(!loud.audible(nether, Vec3.ZERO) && !loud.fresh(nether, 100), "dimension isolation");
        check(loud.fresh(WORLD, 127) && !loud.fresh(WORLD, 128), "expired packet cannot resurrect noise");
        check(!loud.fresh(WORLD, 79), "far future server time rejected");
        check(!noise(FIRST, Vec3.ZERO, WardenNoise.Kind.SOUND, Float.NaN, 100).valid(), "invalid strength rejected");
        check(!noise(FIRST, new Vec3(Double.NaN, 0, 0), WardenNoise.Kind.SOUND, 1, 100).valid(), "invalid position rejected");
        check(!noise(FIRST, Vec3.ZERO, WardenNoise.Kind.SOUND, 0, 100).valid(), "zero strength silent");
        for (String team : new String[]{"survivors", "maniac", "SuRvIvOrS"}) {
            check(WardenMatchRules.participant(true, true, 1, team), "both teams can create noise");
            check(!WardenMatchRules.participant(false, true, 1, team), "dead sources excluded");
            check(!WardenMatchRules.participant(true, false, 1, team), "creative/spectator sources excluded");
            check(!WardenMatchRules.participant(true, true, 0, team), "lobby silent");
        }
        check(!WardenMatchRules.participant(true, true, 1, null), "unteamed source excluded");
        check(!WardenMatchRules.active(true, true, 1, 9, "survivors"), "saved Warden class on survivor does not receive gameplay noise");
        check(WardenSoundPolicy.eligible("othermod:machine", SoundSource.BLOCKS), "arbitrary background sound accepted");
        check(!WardenSoundPolicy.eligible("minecraft:music.menu", SoundSource.MUSIC), "music excluded");
        check(WardenSoundPolicy.coveredPlayerAction("minecraft:entity.player.hurt", SoundSource.PLAYERS), "damage sound cannot duplicate server damage action");
        check(!WardenSoundPolicy.coveredPlayerAction("minecraft:entity.cow.step", SoundSource.NEUTRAL), "mob steps remain background noise");
    }
    private static void batchesAndThrottle() {
        var batch = new WardenNoiseBatch();
        batch.offer(noise(FIRST, Vec3.ZERO, WardenNoise.Kind.INTERACTION, 0.35F, 100));
        batch.offer(noise(null, new Vec3(0.5, 0, 0), WardenNoise.Kind.SOUND, 1, 100));
        var combined = batch.drain();
        check(combined.size() == 1 && combined.get(0).source().equals(FIRST) && combined.get(0).strength() == 1,
                "interaction and nearby spatial sound coalesce without weakening");
        batch.offer(noise(FIRST, Vec3.ZERO, WardenNoise.Kind.ATTACK, 0.6F, 100));
        batch.offer(noise(SECOND, Vec3.ZERO, WardenNoise.Kind.ATTACK, 0.6F, 100));
        check(batch.drain().size() == 2, "different known sources never merge");
        batch.offer(noise(FIRST, Vec3.ZERO, WardenNoise.Kind.ATTACK, 0.6F, 100));
        batch.offer(noise(FIRST, Vec3.ZERO, WardenNoise.Kind.DAMAGE, 1, 100));
        check(batch.drain().get(0).kind() == WardenNoise.Kind.DAMAGE, "stronger final damage keeps its kind");
        for (int i = 0; i < WardenNoiseBatch.CAPACITY + 10; i++)
            batch.offer(noise(null, new Vec3(i * 3, 0, 0), WardenNoise.Kind.SOUND, 1, 100));
        check(batch.drain().size() == WardenNoiseBatch.CAPACITY && batch.drain().isEmpty(), "bounded batch and one-time drain");
        batch.offer(noise(null, Vec3.ZERO, WardenNoise.Kind.SOUND, 1, 100)); batch.clear();
        check(batch.drain().isEmpty(), "match cleanup drops pending noises");
        var throttle = new WardenNoiseThrottle();
        check(throttle.allow(FIRST, WardenNoise.Kind.QTE, 100), "first valid QTE fail allowed");
        check(!throttle.allow(FIRST, WardenNoise.Kind.QTE, 101), "QTE replay rate limited");
        check(throttle.allow(SECOND, WardenNoise.Kind.QTE, 101), "other player's QTE independent");
        check(throttle.allow(FIRST, WardenNoise.Kind.QTE, 120), "QTE cooldown expires");
        check(throttle.allow(FIRST, WardenNoise.Kind.ATTACK, 120), "different action not blocked by QTE");
        throttle.remove(FIRST);
        check(throttle.allow(FIRST, WardenNoise.Kind.QTE, 121), "class/role/logout clears source budget");
        throttle.clear();
        check(throttle.allow(SECOND, WardenNoise.Kind.QTE, 102), "new match clears all budgets");
    }
    private static void packets() {
        for (var kind : WardenNoise.Kind.values()) for (UUID source : new UUID[]{null, FIRST}) {
            var packet = new WardenNoisePacket(noise(source, new Vec3(1.25, 64, -2.5), kind, 0.6F, 100));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.encode(buffer);
                check(packet.equals(WardenNoisePacket.decode(buffer)) && buffer.readableBytes() == 0, "noise wire round trip");
            } finally { buffer.release(); }
        }
    }
    private static void hiddenSources() {
        check(WardenNoiseVisibility.echoInRange(Vec3.ZERO, new Vec3(10, 0, 0)), "hidden echo includes ten block boundary");
        check(!WardenNoiseVisibility.echoInRange(Vec3.ZERO, new Vec3(10.001, 0, 0)), "hidden player beyond ten blocks stays hidden");
        check(!WardenNoiseVisibility.echoInRange(Vec3.ZERO, new Vec3(8, 7, 0)), "hidden range uses full three-dimensional distance");
        for (var kind : WardenNoise.Kind.values()) {
            check(WardenNoiseVisibility.reveal(true, false, kind) == WardenNoiseVisibility.Reveal.PULSE, "visible background source remains a pulse");
            check(WardenNoiseVisibility.reveal(false, false, kind) == WardenNoiseVisibility.Reveal.NONE, "hidden background source cannot reveal walls");
            check(WardenNoiseVisibility.reveal(false, true, kind) == (kind == WardenNoise.Kind.WAVE
                    ? WardenNoiseVisibility.Reveal.NONE : WardenNoiseVisibility.Reveal.ECHO), "hidden player echo excludes travelling wave owner");
        }
        var throttle = new WardenNoiseEchoThrottle(); var third = new UUID(5, 6);
        check(throttle.allow(FIRST, SECOND, 100) && !throttle.allow(FIRST, SECOND, 119), "repeated footsteps do not continuously refresh hidden player");
        check(throttle.allow(third, SECOND, 119), "second Warden receives its own echo");
        check(throttle.allow(FIRST, SECOND, 120), "one-second echo cooldown boundary");
        throttle.clear(); check(throttle.allow(FIRST, SECOND, 120), "match reset clears echo throttle");
        var noise = noise(SECOND, Vec3.ZERO, WardenNoise.Kind.STEP, 0.25F, 100);
        var pose = new WardenEchoPose(new Vec3(1, 0, 0), 20, 30, 0, 1, 0.2F, 0, 0, 100, net.minecraft.world.entity.Pose.STANDING, false, false);
        var echo = new org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket(WORLD, Vec3.ZERO, 100, pose);
        var packet = new WardenNoisePacket(noise, echo); var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { packet.encode(buffer); check(packet.valid() && packet.equals(WardenNoisePacket.decode(buffer)) && buffer.readableBytes() == 0, "short frozen echo wire round trip"); }
        finally { buffer.release(); }
        check(!new WardenNoisePacket(noise(null, Vec3.ZERO, WardenNoise.Kind.SOUND, 1, 100), echo).valid(), "background noise cannot carry a player snapshot");
        check(!new WardenNoisePacket(noise(SECOND, Vec3.ZERO, WardenNoise.Kind.WAVE, 1, 100), echo).valid(), "wave cannot carry its owner's snapshot");
        check(!new WardenNoisePacket(noise, new org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket(WORLD, Vec3.ZERO, 101, pose)).valid(), "pose metadata must match the noise event");
        var batch = new WardenNoiseBatch();
        batch.offer(noise); batch.offer(noise(SECOND, Vec3.ZERO, WardenNoise.Kind.WAVE, 0.5F, 100));
        check(batch.drain().size() == 2, "wave and body noise never merge or change echo eligibility");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
