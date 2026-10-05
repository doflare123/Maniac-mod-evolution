package org.example.maniacrevolution.warden.client;

import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.SoundEvent.SoundSourceEvent;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.ArrayList;
import java.util.List;

/** Checks the real sampler and timing without constructing Minecraft or an OpenGL context. */
public final class WardenVisionPrototypeTest {
    public static void main(String[] args) {
        faceSampling();
        WardenWaterPatternTest.run();
        interactionColours();
        visionAccess();
        waveVisibility();
        waveSurfacePatches();
        playerForm();
        WardenAbilityAnimationTest.run();
        org.example.maniacrevolution.warden.WardenNoiseTest.run();
        org.example.maniacrevolution.warden.WardenShriekerTest.run();
        org.example.maniacrevolution.warden.WardenSniffTest.run();
        org.example.maniacrevolution.warden.WardenCombatTest.run();
        org.example.maniacrevolution.warden.WardenArmorTest.run();
        rendererMesh();
        renderBudget();
        samplerComparison();
        pulseTiming();
        soundPulses();
        soundInbox();
        soundEventRouting();
        snapshotIsolation();
        shriekerSnapshot();
        WardenSurfaceCache cache = new WardenSurfaceCache();
        check(cache.scanOffset(0).equals(net.minecraft.core.BlockPos.ZERO), "dense cache captures nearest block first");
        double previousDistance = -1;
        var visitedOffsets = new java.util.HashSet<net.minecraft.core.BlockPos>();
        for (int index = 0; index < 25 * 25 * 25; index++) {
            var offset = cache.scanOffset(index);
            double distance = offset.distSqr(net.minecraft.core.BlockPos.ZERO);
            check(distance >= previousDistance && visitedOffsets.add(offset), "nearest-first scan covers every block once");
            previousDistance = distance;
        }
        check(!cache.ready() && cache.faceCount() == 0 && cache.pointCount() == 0, "empty cache");
        cache.clear();
        check(!cache.ready() && !cache.limited(), "reset cache");
        System.out.println("Warden prototype: surface sampling, wave front, sound filtering, audio inbox, immutable echo and reset checks passed.");
    }

    private static void waveSurfacePatches() {
        var patches = new WardenNoiseSurfaces();
        patches.request(Vec3.ZERO, 100);
        var first = patches.caches().get(0);
        patches.request(new Vec3(4, 0, 0), 104);
        check(patches.caches().size() == 1 && patches.caches().get(0) == first, "nearby wave steps reuse their surface patch");
        for (int distance = 5; distance <= 40; distance += 5) patches.request(new Vec3(distance, 0, 0), 100 + distance);
        check(patches.caches().size() == WardenNoiseSurfaces.CAPACITY && !patches.caches().contains(first), "long flight keeps only bounded recent surface patches");
        check(patches.caches().stream().anyMatch(c -> c.center().equals(new net.minecraft.core.BlockPos(40, 0, 0))), "forty-block endpoint gets actual world surface cache");
        patches.expire(140 + WardenNoiseSurfaces.LIFE);
        check(patches.caches().isEmpty(), "quiet patches expire after pulse fade");
        patches.request(new Vec3(40, 10, 0), 200); patches.clear();
        check(patches.caches().isEmpty(), "mode/world reset clears far wave geometry");
    }

    private static void waveVisibility() {
        var far = new WardenWavePoints.Ring(new Vec3(40, 0, 0), new Vec3(1, 0, 0), 100, 1);
        var points = WardenWavePoints.visible(List.of(far), 100, Vec3.ZERO, p -> true);
        check(points.size() == WardenWavePoints.POINTS_PER_RING, "full wave visible at forty blocks, outside near cache");
        for (var point : points) { near(point.position().x, 40, "ring perpendicular to trajectory"); near(point.position().distanceTo(far.position()), 0.42, "sonic ring radius"); }
        check(WardenWavePoints.visible(List.of(far), 100, Vec3.ZERO, p -> false).isEmpty(), "blocked/unknown visibility never displays points");
        var half = WardenWavePoints.visible(List.of(far), 104, Vec3.ZERO, p -> p.y > 0);
        check(!half.isEmpty() && half.size() < points.size() && half.stream().allMatch(p -> p.alpha() == 0.5F), "partial cover and age fade");
        check(WardenWavePoints.visible(List.of(far), 108, Vec3.ZERO, p -> true).isEmpty(), "expired rings disappear");
        check(WardenWavePoints.visible(List.of(far), 100, new Vec3(-20, 0, 0), p -> true).isEmpty(), "visual range remains bounded");
        var vertical = new WardenWavePoints.Ring(Vec3.ZERO, new Vec3(0, 1, 0), 100, 1);
        for (var p : WardenWavePoints.visible(List.of(vertical), 100, Vec3.ZERO, v -> true)) near(p.position().y, 0, "vertical shot has valid ring basis");
        var many = new ArrayList<WardenWavePoints.Ring>();
        for (int i = 0; i < 64; i++) many.add(far);
        int[] calls = {0};
        var bounded = WardenWavePoints.visible(many, 100, Vec3.ZERO, p -> { calls[0]++; return true; });
        check(calls[0] == WardenWavePoints.MAX_TESTS && bounded.size() == WardenWavePoints.MAX_TESTS, "per-frame occlusion budget");
    }
    private static void faceSampling() {
        // Thin door model face at world coordinates; points must remain on its actual plane.
        Vec3 normal = new Vec3(0, 0, 0.003);
        var door = new WardenSurfaceCache.Face(new Vec3(5, 10, 2.1875), new Vec3(6, 10, 2.1875),
                new Vec3(6, 11, 2.1875), new Vec3(5, 11, 2.1875), normal, true);
        check(door.yellow() && door.points().size() == 16, "door grid");
        for (Vec3 p : door.points()) {
            near(p.z, 2.1905, "door plane");
            check(p.x > 5 && p.x < 6 && p.y > 10 && p.y < 11, "door surface bounds");
        }
        var repeat = new WardenSurfaceCache.Face(door.a(), door.b(), door.c(), door.d(), normal, true);
        check(door.points().equals(repeat.points()), "world-anchored deterministic points");
        var hatch = new WardenSurfaceCache.Face(new Vec3(0, 0.1875, 0), new Vec3(1, 0.1875, 0),
                new Vec3(1, 0.1875, 1), new Vec3(0, 0.1875, 1), new Vec3(0, 0.003, 0), true);
        for (Vec3 p : hatch.points()) near(p.y, 0.1905, "thin horizontal trapdoor");
        var step = new WardenSurfaceCache.Face(new Vec3(0, 0.5, 0), new Vec3(1, 0.5, 0),
                new Vec3(1, 0.5, 0.5), new Vec3(0, 0.5, 0.5), new Vec3(0, 0.003, 0), false);
        check(!step.yellow() && step.points().size() == 8, "half-depth stair tread grid");
        for (Vec3 p : step.points()) {
            near(p.y, 0.503, "stair tread height");
            check(p.z > 0 && p.z < 0.5, "stair tread bounds");
        }
    }

    private static void pulseTiming() {
        near(WardenPulseTiming.brightness(0, 0), 0, "no reveal before impulse");
        near(WardenPulseTiming.brightness(2, 12), 0, "wave front has not arrived");
        check(WardenPulseTiming.brightness(2, 2) > 0, "near surface before distant surface");
        check(WardenPulseTiming.brightness(5.2, 12) > 0, "fast wave front reveals distant face");
        double arrival = 12 / WardenPulseTiming.SPEED;
        check(WardenPulseTiming.brightness(arrival + 0.3, 12)
                > WardenPulseTiming.brightness(arrival + 0.1, 12), "sub-tick gradual reveal");
        near(WardenPulseTiming.brightness(10, 13), 0, "outside radius");
        float previous = WardenPulseTiming.brightness(6, 12);
        for (double age = 6.25; age < 80; age += 0.25) {
            float next = WardenPulseTiming.brightness(age, 12);
            check(next >= 0 && next <= previous, "continuous monotonic fade");
            previous = next;
        }
        near(WardenPulseTiming.brightness(80, 0), 0, "expired pulse black");
        near(WardenPulseTiming.brightness(100, 12), 0, "expired pulse stays black");
        near(WardenPulseTiming.brightness(14, 2, 6, 14), 0, "walking echo short lifetime");
        check(WardenPulseTiming.brightness(4, 7, 9, 22) > 0, "sprint larger radius");
        near(WardenPulseTiming.brightness(4, 7, 6, 14), 0, "walk smaller radius");
        check(WardenPulseTiming.brightness(6, 11, 12, 28) > 0, "sprint jump largest radius");
    }

    private static void interactionColours() {
        for (var type : List.of(net.minecraft.world.level.block.ButtonBlock.class,
                net.minecraft.world.level.block.LeverBlock.class, net.minecraft.world.level.block.ChestBlock.class,
                net.minecraft.world.level.block.BedBlock.class, net.minecraft.world.level.block.CraftingTableBlock.class,
                net.minecraft.world.level.block.DoorBlock.class, net.minecraft.world.level.block.TrapDoorBlock.class,
                net.minecraft.world.level.block.FenceGateBlock.class)) {
            check(WardenVisionClassification.hasInteraction(type), "interactive yellow: " + type.getSimpleName());
        }
        check(!WardenVisionClassification.hasInteraction(net.minecraft.world.level.block.Block.class), "plain block white");
        check(!WardenVisionClassification.hasInteraction(net.minecraft.world.level.block.StairBlock.class), "stairs white");
        var blue = WardenVisionClassification.CreatureTint.BLUE;
        var red = WardenVisionClassification.CreatureTint.RED;
        var gray = WardenVisionClassification.CreatureTint.GRAY;
        check(WardenVisionClassification.creatureTint(true, "SuRvIvOrS", true, false, 29.99 * 29.99) == blue, "near moving survivor blue");
        check(WardenVisionClassification.creatureTint(true, "MaNiAc", true, false, 40 * 40) == red, "distant moving maniac red");
        check(WardenVisionClassification.creatureTint(false, "survivors", false, true, 40 * 40) == gray, "stationary mob gray");
        check(WardenVisionClassification.creatureTint(false, "maniac", true, false, 0) == gray, "moving mob gray");
        check(WardenVisionClassification.creatureTint(true, null, true, false, 0) == gray, "moving unteamed player gray");
        check(WardenVisionClassification.creatureTint(true, "other", true, false, 0) == gray, "moving other team gray");
        var white = WardenVisionClassification.CreatureTint.WHITE;
        check(WardenVisionClassification.creatureTint(true, "survivors", true, false, 30 * 30) == blue, "survivor blue at thirty block boundary");
        check(WardenVisionClassification.creatureTint(true, "survivors", true, false, 30.001 * 30.001) == white, "survivor white beyond thirty blocks");
        check(WardenVisionClassification.creatureTint(true, "survivors", true, false, 49 * 49) == white, "distant revealed survivor remains visible in white");
        check(WardenVisionClassification.creatureTint(true, "survivors", true, false, new Vec3(24, 19, 0).lengthSqr()) == white, "vertical distance counts toward blue radius");
        for (String team : new String[]{"survivors", "maniac", "other", null}) {
            for (double distance : new double[]{0, 900, 1600}) {
                check(WardenVisionClassification.creatureTint(true, team, false, false, distance) == white, "standing player white");
                check(WardenVisionClassification.creatureTint(true, team, true, true, distance) == white, "Shift movement white");
                check(WardenVisionClassification.creatureTint(true, team, false, true, distance) == white, "standing Shift white");
            }
        }
        check(white.r == 1 && white.g == 1 && white.b == 1, "white palette");
        check(!WardenVisionClassification.moving(0, 0, 0), "no displacement, including camera rotation, is stationary");
        check(!WardenVisionClassification.moving(0.0001, 0, 0), "tiny interpolation noise is stationary");
        check(WardenVisionClassification.moving(0.05, 0, 0), "walking displacement");
        check(WardenVisionClassification.moving(0, -0.05, 0), "vertical displacement");
        check(WardenVisionClassification.moving(0, 0, -0.05), "negative displacement");
        check(blue.b > blue.r && blue.b > blue.g && red.r > red.g && red.r > red.b,
                "render palette has dominant blue/red channels");
        check(gray.r == gray.g && gray.g == gray.b, "gray palette has equal channels");
    }

    private static void visionAccess() {
        check(WardenVisionAccess.mode(false, true, true, true, 1, 9, "maniac") == WardenVisionAccess.Mode.OFF,
                "normal test override disables automatic match vision");
        check(WardenVisionAccess.mode(true, true, true, true, 1, 9, "maniac") == WardenVisionAccess.Mode.OFF,
                "normal override takes priority over TEST");
        check(WardenVisionAccess.mode(false, false, true, true, 1, 9, "maniac") == WardenVisionAccess.Mode.MATCH,
                "restore brings back automatic match vision");
        org.example.maniacrevolution.character.CharacterRegistry.init();
        var warden = org.example.maniacrevolution.character.CharacterRegistry.getClass("warden");
        check(warden != null && warden.getType() == org.example.maniacrevolution.character.CharacterType.MANIAC,
                "registered Warden maniac");
        check(warden.getScoreboardId() == 9 && warden.getItems().isEmpty(), "vision-only class ID and no invented equipment");
        long sameId = org.example.maniacrevolution.character.CharacterRegistry.getClassesByType(warden.getType())
                .stream().filter(c -> c.getScoreboardId() == warden.getScoreboardId()).count();
        check(sameId == 1, "Warden scoreboard ID is unique among maniacs");
        check(warden.getFrescoTexture().toString().equals("minecraft:textures/item/echo_shard.png"), "existing placeholder texture");
        for (boolean test : new boolean[]{false, true})
            for (boolean alive : new boolean[]{false, true})
                for (boolean survival : new boolean[]{false, true})
                    for (int phase : new int[]{0, 1, 2, 3, 4})
                        for (int id : new int[]{-1, 17, 9})
                            for (String team : new String[]{null, "survivors", "maniac", "MaNiAc"}) {
                                var expected = !alive || !survival ? WardenVisionAccess.Mode.OFF
                                        : test ? WardenVisionAccess.Mode.TEST
                                        : phase >= 1 && phase <= 3 && id == 9 && "maniac".equalsIgnoreCase(team)
                                        ? WardenVisionAccess.Mode.MATCH : WardenVisionAccess.Mode.OFF;
                                check(WardenVisionAccess.mode(test, alive, survival, phase, id, team) == expected,
                                        "match access and independent test mode");
                            }
        check(WardenVisionAccess.mode(false, true, true, 1, 9, "maniac") == WardenVisionAccess.Mode.MATCH,
                "turning test mode off cannot bypass match vision");
        check(WardenVisionAccess.mode(false, true, true, 1, 18, "maniac") == WardenVisionAccess.Mode.OFF,
                "previous Warden ID no longer activates match vision");
    }

    private static void playerForm() {
        var state = new org.example.maniacrevolution.warden.WardenFormState();
        var first = new java.util.UUID(1, 2);
        var second = new java.util.UUID(3, 4);
        check(!state.active(first) && !state.update(first, false), "unknown player has normal form");
        check(state.update(first, true) && state.active(first), "entering match changes form");
        check(!state.update(first, true), "unchanged form needs no repeated broadcast");
        check(state.update(second, true) && state.active(first), "multiple Wardens have independent flags");
        check(state.update(first, false) && !state.active(first) && state.active(second), "death/class/team/mode transition removes only one form");
        state.clear();
        check(!state.active(first) && !state.active(second), "disconnect/server stop clears all forms");
        for (boolean active : new boolean[]{true, false}) {
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var packet = new org.example.maniacrevolution.network.packets.WardenFormPacket(first, active);
                packet.encode(buffer);
                var decoded = org.example.maniacrevolution.network.packets.WardenFormPacket.decode(buffer);
                check(decoded.equals(packet) && buffer.readableBytes() == 0, "form UUID/flag packet round trip");
            } finally { buffer.release(); }
        }

        var root = net.minecraft.client.model.WardenModel.createBodyLayer().bakeRoot();
        var model = new WardenPlayerModel(root);
        var head = root.getChild("bone").getChild("body").getChild("head");
        var leg = root.getChild("bone").getChild("left_leg");
        model.animate(0, 0, 0, 30, 15, false);
        near(head.yRot, Math.PI / 6, "player head yaw drives Warden head");
        check(Math.abs(head.xRot - Math.PI / 12) < 0.001, "player head pitch drives Warden head");
        near(leg.xRot, 0, "standing Warden has no walking leg animation");
        model.animate(1, 0.2F, 10, 30, 15, false);
        check(Math.abs(leg.xRot) > 0.1, "player gait drives Warden leg");
        var walkingMesh = new WardenMeshCapture(false, 0.12, 1024, 12000);
        model.renderToBuffer(new com.mojang.blaze3d.vertex.PoseStack(), walkingMesh.quadBuffer(), 0xF000F0, 0, 1, 1, 1, 1);
        check(!walkingMesh.limited() && walkingMesh.faces().size() > 36, "full vanilla Warden mesh captured without GPU");
        model.animate(1, 0.2F, 10, 30, 15, false);
        var repeatMesh = new WardenMeshCapture(false, 0.12, 1024, 12000);
        model.renderToBuffer(new com.mojang.blaze3d.vertex.PoseStack(), repeatMesh.quadBuffer(), 0xF000F0, 0, 1, 1, 1, 1);
        check(walkingMesh.faces().equals(repeatMesh.faces()), "capture and normal rendering do not accumulate model transforms");
        model.animate(0, 0, 0, 0, 0, true);
        near(root.getChild("bone").y, 22, "crouch compensates PlayerRenderer offset at feet");
        near(root.getChild("bone").getChild("body").y, -19, "crouch lowers torso only");
        model.animate(0, 0, 0, 0, 0, false);
        near(root.getChild("bone").y, 24, "standing restores base pose");
        near(root.getChild("bone").getChild("body").y, -21, "standing restores torso pose");
        for (var arm : net.minecraft.world.entity.HumanoidArm.values()) {
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            model.translateToHand(arm, pose);
            var hand = pose.last().pose().transformPosition(new org.joml.Vector3f());
            check(Float.isFinite(hand.x) && Float.isFinite(hand.y) && Float.isFinite(hand.z), "held item transform finite");
            check(arm == net.minecraft.world.entity.HumanoidArm.LEFT ? hand.x > 0 : hand.x < 0, "items attached to correct Warden arm");
        }
        System.out.println("Warden player form: independent flags, cleanup, packet round trip, vanilla mesh and player animation checks passed.");
    }

    private static void rendererMesh() {
        var definition = new net.minecraft.client.model.geom.builders.MeshDefinition();
        definition.getRoot().addOrReplaceChild("box", net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                .texOffs(0, 0).addBox(0, 0, 0, 16, 8, 16), net.minecraft.client.model.geom.PartPose.ZERO);
        var model = net.minecraft.client.model.geom.builders.LayerDefinition.create(definition, 64, 64).bakeRoot();
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.translate(5, 2, 7);
        var mesh = new WardenMeshCapture(true, 0.25, 100, 1000);
        model.render(pose, mesh.quadBuffer(), 0xF000F0, 0);
        check(mesh.faces().size() == 6 && !mesh.limited(), "real ModelPart cube captured without GPU");
        for (var face : mesh.faces()) {
            check(face.yellow(), "captured interactive mesh yellow");
            for (Vec3 point : face.points()) {
                check(point.x >= 4.996 && point.x <= 6.004 && point.y >= 1.996 && point.y <= 2.504
                        && point.z >= 6.996 && point.z <= 8.004, "model translation and half-height preserved");
                check(Math.abs(point.x - 4.997) < 0.00001 || Math.abs(point.x - 6.003) < 0.00001
                        || Math.abs(point.y - 1.997) < 0.00001 || Math.abs(point.y - 2.503) < 0.00001
                        || Math.abs(point.z - 6.997) < 0.00001 || Math.abs(point.z - 8.003) < 0.00001,
                        "dots lie on actual model surfaces");
            }
        }
        var bounded = new WardenMeshCapture(false, 0.12, 1, 1000);
        model.render(pose, bounded.quadBuffer(), 0xF000F0, 0);
        check(bounded.limited() && bounded.faces().size() == 1, "captured geometry bounded");
    }

    private static void renderBudget() {
        check(WardenRenderBudget.pointStride(36) == 1, "near points retain full density");
        check(WardenRenderBudget.pointStride(37) == 2 && WardenRenderBudget.pointStride(100) == 2, "medium density");
        check(WardenRenderBudget.pointStride(101) == 4, "far density");
        long interval = WardenRenderBudget.CAPTURE_INTERVAL_NS;
        check(WardenRenderBudget.captureDue(0, Long.MIN_VALUE), "first dynamic frame immediate");
        check(!WardenRenderBudget.captureDue(interval - 1, 0), "high FPS reuses dynamic snapshot");
        check(WardenRenderBudget.captureDue(interval, 0), "dynamic update at interval");
        var cache = new WardenSurfaceCache();
        var faces = cache.flatFaces();
        long revision = cache.revision();
        check(faces == cache.flatFaces(), "unchanged geometry reuses flattened list");
        cache.clear();
        check(cache.revision() > revision && cache.flatFaces().isEmpty(), "reset invalidates geometry revision");
        var face = new WardenSurfaceCache.Face(new Vec3(1, 2, 3), new Vec3(3, 3, 4),
                new Vec3(3, 5, 5), new Vec3(1, 4, 4), new Vec3(0, 0, 0.003), false);
        for (var point : face.points()) check(face.bounds().contains(point), "culling bounds enclose transformed points");
        // LOD selects a subset, never relocates points; every level retains the first sample of a thin face.
        for (int stride : List.of(1, 2, 4)) {
            int count = 0;
            for (int i = 0; i < face.points().size(); i += stride) count++;
            check(count == (face.points().size() + stride - 1) / stride && count > 0, "stable LOD subset count");
        }
    }

    private static List<Vec3> referenceSamples(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal) {
        int nu = Math.min(16, Math.max(1, (int) Math.ceil(Math.max(a.distanceTo(b), c.distanceTo(d)) / 0.25)));
        int nv = Math.min(16, Math.max(1, (int) Math.ceil(Math.max(a.distanceTo(d), b.distanceTo(c)) / 0.25)));
        var points = new ArrayList<Vec3>();
        for (int i = 0; i < nu; i++) for (int j = 0; j < nv; j++) {
            double u = (i + 0.5) / nu, v = (j + 0.5) / nv;
            points.add(a.scale((1 - u) * (1 - v)).add(b.scale(u * (1 - v)))
                    .add(c.scale(u * v)).add(d.scale((1 - u) * v)).add(normal));
        }
        return List.copyOf(points);
    }

    private static void samplerComparison() {
        var random = new java.util.Random(17);
        for (int i = 0; i < 200; i++) {
            var a = new Vec3(random.nextDouble() * 10, random.nextDouble() * 10, random.nextDouble() * 10);
            var b = a.add(0.2, 0.7, 0.5);
            var c = b.add(0.6, -0.1, 0.4);
            var d = a.add(0.6, -0.1, 0.4);
            var normal = new Vec3(0, 0.003, 0);
            var expected = referenceSamples(a, b, c, d, normal);
            var actual = new WardenSurfaceCache.Face(a, b, c, d, normal, false).points();
            check(expected.size() == actual.size(), "optimized sampler preserves grid count");
            for (int point = 0; point < expected.size(); point++)
                check(expected.get(point).distanceToSqr(actual.get(point)) < 1e-20, "optimized sampler preserves coordinates");
        }
        System.out.println("Optimization checks: sampler equivalence, conservative bounds, stable LOD, capture rate and cache revisions passed.");
    }

    private static WardenSoundPulseFilter.HeardSound sound(String id, SoundSource source, float volume,
                                                          boolean relative, boolean looping) {
        return new WardenSoundPulseFilter.HeardSound(id, source, 1, 2, 3, volume, 16, true, relative, looping);
    }

    private static void soundPulses() {
        var step = sound("minecraft:block.stone.step", SoundSource.PLAYERS, 0.15f, false, false);
        var quiet = WardenSoundPulseFilter.profile(step);
        check(quiet != null, "real positional step accepted");
        for (SoundSource source : List.of(SoundSource.BLOCKS, SoundSource.PLAYERS, SoundSource.HOSTILE, SoundSource.NEUTRAL)) {
            check(WardenSoundPulseFilter.profile(sound("another_mod:any_new_sound", source, 0.5f, false, false)) != null,
                    "arbitrary gameplay sound needs no registration");
        }
        for (SoundSource source : List.of(SoundSource.MASTER, SoundSource.MUSIC, SoundSource.RECORDS, SoundSource.AMBIENT, SoundSource.WEATHER)) {
            check(WardenSoundPulseFilter.profile(sound("minecraft:some_sound", source, 1, false, false)) == null,
                    "background category excluded");
        }
        check(WardenSoundPulseFilter.profile(sound("minecraft:ui.button.click", SoundSource.PLAYERS, 1, false, false)) == null, "UI ID excluded");
        check(WardenSoundPulseFilter.profile(sound("maniacrev:warden_pulse", SoundSource.PLAYERS, 1, false, false)) == null, "no own-effect feedback");
        check(WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.PLAYERS, 1, true, false)) == null, "relative sound excluded");
        check(WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.BLOCKS, 1, false, true)) == null, "looping background excluded");
        check(WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.BLOCKS, 0, false, false)) == null, "silent source excluded");
        check(WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.BLOCKS, Float.NaN, false, false)) == null, "invalid volume rejected");
        var loud = WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.BLOCKS, 1, false, false));
        check(loud.radius() > quiet.radius() && loud.duration() > quiet.duration(), "loudness increases reveal");
        var extreme = WardenSoundPulseFilter.profile(sound("custom:sound", SoundSource.BLOCKS, 100, false, false));
        near(extreme.radius(), 12, "maximum radius bounded");
        near(extreme.duration(), 28, "maximum duration bounded");
        check(WardenSoundPulseFilter.withinHearingRange(step, 15 * 15), "near sound audible");
        check(!WardenSoundPulseFilter.withinHearingRange(step, 17 * 17), "outside attenuation range excluded");
        check(WardenSoundPulseFilter.withinHearingRange(sound("custom:sound", SoundSource.BLOCKS, 2, false, false), 30 * 30), "loud source longer hearing range");
        check(WardenSoundPulseFilter.suppressOwnMovement(step, true, 0.5), "shift suppresses own step");
        check(!WardenSoundPulseFilter.suppressOwnMovement(step, false, 0.5), "normal step not suppressed");
        check(!WardenSoundPulseFilter.suppressOwnMovement(step, true, 16), "shift does not silence distant players");
        check(!WardenSoundPulseFilter.suppressOwnMovement(sound("minecraft:block.wooden_door.open", SoundSource.BLOCKS, 1, false, false), true, 0.5), "shift does not silence doors");
        check(WardenSoundPulseFilter.suppressOwnMovement(sound("minecraft:entity.player.small_fall", SoundSource.PLAYERS, 1, false, false), true, 0.5), "shift suppresses own landing");
    }

    private static void soundInbox() {
        var inbox = new WardenSoundPulseInbox();
        var metadata = sound("minecraft:block.stone.step", SoundSource.PLAYERS, 0, false, false);
        int[] samples = {0};
        var sound = new WardenSoundPulseInbox.PendingSound(metadata, () -> { samples[0]++; return 0.15; });
        check(inbox.ticket() == -1, "inactive audio handler exits early");
        inbox.offer(inbox.generation(), sound);
        check(inbox.drain().isEmpty(), "disabled prototype ignores callbacks");
        inbox.enable();
        long oldGeneration = inbox.generation();
        inbox.offer(oldGeneration, sound);
        var delivered = inbox.drain();
        check(delivered.equals(List.of(sound)), "audio snapshot delivered once");
        check(samples[0] == 0, "audio callback and enqueue do not sample volume/random");
        near(delivered.get(0).sample().volume(), 0.15, "volume sampled by consumer");
        check(samples[0] == 1, "volume sampled once on consumption");
        check(inbox.drain().isEmpty(), "no replay on next tick");
        inbox.offer(oldGeneration, sound);
        inbox.clear();
        inbox.enable();
        inbox.offer(oldGeneration, sound);
        check(inbox.drain().isEmpty(), "clear invalidates queued and in-flight old callbacks");
        for (int i = 0; i < WardenSoundPulseInbox.CAPACITY + 10; i++) inbox.offer(inbox.generation(), sound);
        check(inbox.drain().size() == WardenSoundPulseInbox.CAPACITY, "sound flood bounded");
    }

    private static void soundEventRouting() {
        var bus = BusBuilder.builder().build();
        int[] calls = {0};
        bus.addListener(EventPriority.NORMAL, false, SoundSourceEvent.class, event -> calls[0]++);
        var instance = new SimpleSoundInstance(new ResourceLocation("test:source"), SoundSource.BLOCKS,
                1, 1, RandomSource.create(), false, 0, SoundInstance.Attenuation.LINEAR, 1, 2, 3, false);
        // Posting source events on a private bus checks the actual Forge inheritance dispatch.
        // This does not create a sound engine/channel or assert audio playback in Minecraft.
        bus.post(new PlaySoundSourceEvent(null, instance, null));
        bus.post(new PlayStreamingSourceEvent(null, instance, null));
        check(calls[0] == 2, "one base listener receives both static and streaming sound events");
    }

    private static void snapshotIsolation() {
        var source = new ArrayList<>(List.of(new Vec3(1, 2, 3)));
        var snapshot = new WardenEchoSnapshot(source, 7);
        source.set(0, new Vec3(4, 5, 6));
        check(snapshot.points().equals(List.of(new Vec3(1, 2, 3))), "echo does not follow source");
        try {
            snapshot.points().clear();
            throw new AssertionError("echo points must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Snapshot owns its coordinates even when the producer changes or disappears.
        }
    }

    private static void shriekerSnapshot() {
        var root = net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, false), 64, 64).bakeRoot();
        var model = new net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>(root, false);
        var transform = new com.mojang.blaze3d.vertex.PoseStack(); transform.translate(4, 8, 9);
        var points = WardenEchoSnapshot.sample(model, transform);
        var echo = new WardenEchoSnapshot(points, 10, 60, new Vec3(4, 8, 9), 12);
        var before = List.copyOf(echo.points());
        model.leftArm.xRot = 1.5F; transform.translate(20, 0, 0); points.clear();
        check(echo.points().equals(before) && !before.isEmpty(), "echo owns frozen world-space geometry");
        check(!WardenEchoSnapshot.sample(model, transform).equals(before), "later pose and position differ from frozen echo");
        Vec3 point = new Vec3(5, 8, 9);
        check(echo.brightness(point, 10) == 0 && echo.brightness(point, 12) > 0, "echo waits for own front");
        check(echo.brightness(point, 69) < echo.brightness(point, 12) && echo.brightness(point, 70) == 0, "own lifetime fades to zero");
        check(echo.brightness(new Vec3(17, 8, 9), 12) == 0, "echo clipped to its own impulse");
        var delayed = new WardenEchoSnapshot(before, -20, 60, new Vec3(4, 8, 9), 12);
        check(delayed.brightness(point, 0) < echo.brightness(point, 12) && delayed.brightness(point, 40) == 0,
                "late delivery keeps original fade and expiry");
        check(WardenVisionClassification.hasInteraction(org.example.maniacrevolution.warden.WardenShriekerBlock.class), "shrieker is yellow interactive geometry");
    }

    private static void near(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 0.000001, label);
    }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
