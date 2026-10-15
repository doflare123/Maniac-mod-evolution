package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundEvent.SoundSourceEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.warden.WardenNoise;
import org.example.maniacrevolution.util.PlayerModeUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Warden match vision with server noise/echoes and a separate opt-in visual test mode. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenVisionPrototype {
    private static final KeyMapping TOGGLE = key("key.maniacrev.warden_vision", GLFW.GLFW_KEY_F8);
    private static final KeyMapping PULSE = key("key.maniacrev.warden_pulse", GLFW.GLFW_KEY_F9);
    private static WardenSurfaceCache surfaces = new WardenSurfaceCache();
    private static WardenSurfaceCache nextSurfaces;
    private static final WardenDynamicScene DYNAMIC = new WardenDynamicScene();
    private static final WardenSceneBuffers BUFFERS = new WardenSceneBuffers();
    private static final WardenWorldDepth WORLD_DEPTH = new WardenWorldDepth();
    private static final WardenNoiseSurfaces DISTANT_SURFACES = new WardenNoiseSurfaces();
    private static WardenSurfaceCache scentOwner;
    private static long scentRevision = -1, scentDynamicRevision = -1;
    private static net.minecraft.client.renderer.culling.Frustum frustum;
    private static int drawnPoints;
    private static final WardenSoundPulseInbox SOUNDS = new WardenSoundPulseInbox();
    private static final List<Pulse> PULSES = new ArrayList<>();
    private static final List<WardenEchoSnapshot> ECHOES = new ArrayList<>();
    private static final List<WardenEchoSnapshot> NOISE_ECHOES = new ArrayList<>();
    private static final List<WardenWavePoints.Ring> RINGS = new ArrayList<>();
    private static final double DURATION = WardenPulseTiming.DURATION;
    private static boolean enabled;
    private static boolean normalForTests;
    private static ClientLevel level;
    private static AbstractClientPlayer sessionPlayer;
    private static WardenVisionAccess.Mode mode = WardenVisionAccess.Mode.OFF;
    private static Vec3 origin;
    private static double echoBorn;
    private static long clock;
    private static Matrix4f view;
    private static Matrix4f projection;
    private static Vec3 camera;
    private static Vector3f right;
    private static Vector3f up;

    private record Pulse(Vec3 origin, double born, double radius, double duration) {}

    private WardenVisionPrototype() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, "key.categories.maniacrev");
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
            event.register(PULSE);
        }
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == level && mc.player == sessionPlayer && mode != WardenVisionAccess.Mode.OFF
                && currentMode() == mode;
    }

    private static WardenVisionAccess.Mode currentMode() {
        var player = Minecraft.getInstance().player;
        return WardenVisionAccess.mode(enabled, normalForTests, player != null && player.isAlive(),
                PlayerModeUtil.isSurvivalOrAdventure(player), ClientGameState.getPhase(),
                ClientPlayerData.getManiacClassId(), player == null || player.getTeam() == null
                        ? null : player.getTeam().getName());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        enabled = false;
        normalForTests = false;
        mode = WardenVisionAccess.Mode.OFF;
        level = null;
        sessionPlayer = null;
        view = null;
        clearPulse();
        // These server mirrors must not activate vision using the previous connection's state.
        ClientPlayerData.reset();
        ClientGameState.update(0, 0, 0, false);
    }

    private static void clearPulse() {
        WardenSniffClient.clearVisuals();
        origin = null;
        ECHOES.clear();
        NOISE_ECHOES.clear();
        PULSES.clear();
        RINGS.clear();
        surfaces.clear();
        nextSurfaces = null;
        SOUNDS.clear();
        DYNAMIC.clear();
        BUFFERS.clear();
        WORLD_DEPTH.clear();
        DISTANT_SURFACES.clear();
    }
    public static boolean inMatch() { return currentMode() == WardenVisionAccess.Mode.MATCH; }
    static boolean renderingVision() { return active(); }
    static void clearScentBuffers() {
        BUFFERS.clear(WardenSceneBuffers.Layer.SCENT); BUFFERS.clear(WardenSceneBuffers.Layer.SCENT_DEPTH);
        scentOwner = null; scentRevision = scentDynamicRevision = -1;
    }

    private static int setEnabled(boolean value) {
        Minecraft mc = Minecraft.getInstance();
        clearPulse();
        enabled = value;
        if (value) normalForTests = false;
        WardenVisionPerformance.reset();
        level = mc.level;
        sessionPlayer = mc.player;
        mode = currentMode();
        if (value && mc.player != null) surfaces.start(mc.player.blockPosition());
        message(value ? "Варден: прототип включён. F9 — импульс; /wardenvision echo <имя|self> — тестовое эхо."
                : mode == WardenVisionAccess.Mode.MATCH ? "Тестовый режим выключен; зрение Вардена в матче остаётся активным."
                : "Варден: обычное зрение восстановлено.");
        return 1;
    }

    private static int pulse() {
        if (!enabled || !active()) { message("Тестовый импульс требует включения прототипа и режима survival/adventure."); return 0; }
        Minecraft mc = Minecraft.getInstance();
        origin = mc.player.position();
        echoBorn = clock;
        ECHOES.clear();
        PULSES.clear();
        addPulse(origin, WardenSurfaceCache.RADIUS, DURATION);
        message("Тестовый импульс: 12 блоков, 4 секунды. Стоящие/Shift белые, движущиеся выжившие синие; эхо — отдельная команда.");
        return 1;
    }

    private static int echo(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || !active() || origin == null || clock - echoBorn >= DURATION) {
            message("Сначала вызовите тестовый импульс."); return 0;
        }
        AbstractClientPlayer target = "self".equalsIgnoreCase(name) ? mc.player : mc.level.players().stream()
                .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null || target.isSpectator() || !target.isAlive()
                || target.position().distanceToSqr(origin) > WardenSurfaceCache.RADIUS * WardenSurfaceCache.RADIUS) {
            message("Игрок не загружен или вне области импульса."); return 0;
        }
        if (ECHOES.size() == 8) ECHOES.remove(0);
        ECHOES.add(WardenEchoSnapshot.capture(target, clock));
        message("Сохранено неподвижное тестовое эхо: " + target.getGameProfile().getName());
        return 1;
    }

    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), false);
    }
    private static int normalForTests(boolean value) {
        clearPulse(); view = null; enabled = false; normalForTests = value;
        var mc = Minecraft.getInstance(); level = mc.level; sessionPlayer = mc.player; mode = currentMode();
        message(value ? "Зрение Вардена отключено для тестов. Бой и форма сохранены. /wardenvision restore — вернуть зрение."
                : "Тестовое отключение отменено. Зрение снова определяется классом и матчем.");
        return 1;
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wardenvision")
                .then(Commands.literal("normal").executes(c -> normalForTests(true)))
                .then(Commands.literal("restore").executes(c -> normalForTests(false)))
                .then(Commands.literal("perf").executes(c -> { message(WardenVisionPerformance.report()); return 1; }))
                .then(Commands.literal("perfreset").executes(c -> { WardenVisionPerformance.reset(); return 1; }))
                .then(Commands.literal("on").executes(c -> setEnabled(true)))
                .then(Commands.literal("off").executes(c -> setEnabled(false)))
                .then(Commands.literal("pulse").executes(c -> pulse()))
                .then(Commands.literal("echo").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> echo(StringArgumentType.getString(c, "player")))))
                .then(Commands.literal("status").executes(c -> {
                    message("Варден: test=" + enabled + ", normalForTests=" + normalForTests + ", mode=" + currentMode() + ", active=" + active() + ", faces=" + surfaces.faceCount()
                            + ", points=" + surfaces.pointCount() + ", ready=" + surfaces.ready() + ", limited=" + surfaces.limited()
                            + ", pulses=" + PULSES.size() + ", dynamicLimited=" + DYNAMIC.limited()
                            + ", dynamicFaces=" + (DYNAMIC.blocks().size() + DYNAMIC.creatures().size())
                            + ", worldDepth=" + WORLD_DEPTH.ready() + ", scentPoints=" + WardenSniffClient.points().size()
                            + ", scentReady=" + WardenSniffClient.renderable() + ", noiseCaches=" + DISTANT_SURFACES.caches().size()
                            + ", noiseEchoes=" + NOISE_ECHOES.size());
                    return 1;
                })));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || mc.player != sessionPlayer || mc.player == null || !mc.player.isAlive()) {
            enabled = false;
            level = mc.level;
            sessionPlayer = mc.player;
            clearPulse();
            view = null;
        }
        while (TOGGLE.consumeClick()) if (mc.player != null) setEnabled(!enabled);
        var nextMode = currentMode();
        if (nextMode != mode) {
            clearPulse();
            view = null;
            mode = nextMode;
        }
        while (PULSE.consumeClick()) pulse();
        if (mc.isPaused()) { SOUNDS.clear(); return; }
        clock++;
        if (!active()) { clearPulse(); return; }
        if (origin != null && clock - echoBorn >= DURATION) { origin = null; ECHOES.clear(); }
        PULSES.removeIf(p -> p.born >= 0 && clock - p.born >= p.duration);
        ECHOES.removeIf(e -> clock - e.born() >= e.duration());
        NOISE_ECHOES.removeIf(e -> clock - e.born() >= e.duration());
        RINGS.removeIf(r -> clock - r.born() >= 8);
        long cacheStart = System.nanoTime();
        updateSurfaces(mc);
        DISTANT_SURFACES.tick(mc, clock);
        WardenVisionPerformance.record(WardenVisionPerformance.Stage.CACHE, cacheStart);
        if (mode == WardenVisionAccess.Mode.TEST) SOUNDS.enable(); else SOUNDS.clear();
        boolean sneaking = mc.player.isCrouching() || mc.options.keyShift.isDown();
        Vec3 listener = mc.gameRenderer.getMainCamera().getPosition();
        for (var pending : SOUNDS.drain()) {
            // AbstractSoundInstance.getVolume() may sample its RandomSource. Never call it on the audio thread.
            var sound = pending.sample();
            var profile = WardenSoundPulseFilter.profile(sound);
            if (profile == null) continue;
            Vec3 position = new Vec3(sound.x(), sound.y(), sound.z());
            if (!WardenSoundPulseFilter.withinHearingRange(sound, position.distanceToSqr(listener))) continue;
            if (WardenSoundPulseFilter.suppressOwnMovement(sound, sneaking, position.distanceToSqr(mc.player.position()))) continue;
            addPulse(position, profile.radius(), profile.duration());
        }
    }

    @SubscribeEvent
    public static void soundStarted(SoundSourceEvent event) {
        // Both static and streaming source events reach this single handler, after Channel.play().
        // Position is frozen now; volume sampling and all renderer mutations happen on the client thread.
        long generation = SOUNDS.ticket();
        if (generation < 0) return;
        var instance = event.getSound();
        var file = instance.getSound();
        var sound = new WardenSoundPulseFilter.HeardSound(instance.getLocation().toString(), instance.getSource(),
                instance.getX(), instance.getY(), instance.getZ(), 0,
                file == null ? 16 : file.getAttenuationDistance(),
                instance.getAttenuation() == net.minecraft.client.resources.sounds.SoundInstance.Attenuation.LINEAR,
                instance.isRelative(), instance.isLooping());
        if (WardenSoundPulseFilter.eligible(sound))
            SOUNDS.offer(generation, new WardenSoundPulseInbox.PendingSound(sound, instance::getVolume));
    }

    private static void addPulse(Vec3 position, double radius, double duration) {
        if (PULSES.size() == 8) PULSES.remove(0);
        PULSES.add(new Pulse(position, clock, radius, duration));
    }

    public static void acceptServerNoise(WardenNoise noise, org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket echo) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || currentMode() != WardenVisionAccess.Mode.MATCH || !noise.valid() || mc.isPaused()) return;
        double age = Math.max(0, mc.level.getGameTime() - noise.tick());
        if (!noise.dimension().equals(mc.level.dimension().location()) || noise.tick() > mc.level.getGameTime() + 20
                || age >= (echo == null ? noise.duration() : org.example.maniacrevolution.warden.WardenNoiseVisibility.ECHO_TICKS)) return;
        // A class/phase packet can enable MATCH before its first client tick. Initialize once without losing that first noise.
        if (mc.level != level || mc.player != sessionPlayer || mode != WardenVisionAccess.Mode.MATCH) {
            clearPulse(); level = mc.level; sessionPlayer = mc.player;
            mode = WardenVisionAccess.Mode.MATCH; view = null;
        }
        if (echo != null) {
            var snapshot = WardenEchoSnapshot.captureServer(echo.pose(), echo.origin(), clock - age,
                    org.example.maniacrevolution.warden.WardenNoiseVisibility.ECHO_TICKS);
            if (NOISE_ECHOES.size() == 16) NOISE_ECHOES.remove(0);
            // Immediate standalone fade, independent of the shrieker pulse/echo clocks.
            NOISE_ECHOES.add(new WardenEchoSnapshot(snapshot.points(), snapshot.born(), snapshot.duration(), null, snapshot.radius()));
            return;
        }
        if (!visibleNoise(noise.position(), noise.kind() == WardenNoise.Kind.WAVE ? null : noise.source())) return;
        addPulse(noise.position(), noise.radius(), noise.duration() - age);
        DISTANT_SURFACES.request(noise.position(), clock - (long) age);
    }

    private static boolean visibleNoise(Vec3 position) {
        return visibleNoise(position, null);
    }
    private static boolean visibleNoise(Vec3 position, java.util.UUID source) {
        var mc = Minecraft.getInstance();
        var eye = mc.gameRenderer.getMainCamera().getPosition();
        var actor = source == null ? null : mc.level.getPlayerByUUID(source);
        var sightTarget = actor != null && actor.position().distanceToSqr(position) <= 12 * 12 ? actor.getEyePosition() : position;
        return position.distanceToSqr(mc.player.position()) <= org.example.maniacrevolution.warden.WardenMatchRules.VISION_RANGE
                * org.example.maniacrevolution.warden.WardenMatchRules.VISION_RANGE
                && org.example.maniacrevolution.warden.WardenNoiseVisibility.visible(mc.level, mc.player, eye, sightTarget);
    }

    public static void acceptWave(org.example.maniacrevolution.network.packets.WardenWavePacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || currentMode() != WardenVisionAccess.Mode.MATCH || mc.isPaused()
                || !packet.valid() || !packet.dimension().equals(mc.level.dimension().location())) return;
        double age = Math.max(0, mc.level.getGameTime() - packet.tick());
        if (age >= 8 || packet.tick() > mc.level.getGameTime() + 20) return;
        if (!visibleNoise(packet.position())) return;
        if (mc.level != level || mc.player != sessionPlayer || mode != WardenVisionAccess.Mode.MATCH) {
            clearPulse(); level = mc.level; sessionPlayer = mc.player; mode = WardenVisionAccess.Mode.MATCH; view = null;
        }
        if (RINGS.size() >= 64) RINGS.remove(0);
        RINGS.add(new WardenWavePoints.Ring(packet.position(), packet.direction(), clock - age, packet.strength()));
        DISTANT_SURFACES.request(packet.position(), clock - (long) age);
    }

    public static void acceptShriekerEcho(org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || currentMode() != WardenVisionAccess.Mode.MATCH || mc.isPaused()
                || !packet.valid() || !packet.dimension().equals(mc.level.dimension().location())
                || !org.example.maniacrevolution.warden.WardenShriekerRules.fresh(packet.tick(), mc.level.getGameTime())) return;
        if (mc.level != level || mc.player != sessionPlayer || mode != WardenVisionAccess.Mode.MATCH) {
            clearPulse(); level = mc.level; sessionPlayer = mc.player; mode = WardenVisionAccess.Mode.MATCH; view = null;
        }
        double age = Math.max(0, mc.level.getGameTime() - packet.tick());
        double duration = org.example.maniacrevolution.warden.WardenShriekerRules.PULSE_TICKS - age;
        if (duration > 0 && visibleNoise(packet.origin())) {
            addPulse(packet.origin(), org.example.maniacrevolution.warden.WardenShriekerRules.RADIUS, duration);
            DISTANT_SURFACES.request(packet.origin(), clock - (long) age);
        }
        if (ECHOES.size() == 8) ECHOES.remove(0);
        // The sensor can relay a distant player's vibration: reveal the complete pose at its own location.
        ECHOES.add(WardenEchoSnapshot.captureServer(packet.pose(), packet.pose().position(), clock - age,
                org.example.maniacrevolution.warden.WardenShriekerRules.ECHO_TICKS));
    }

    static Matrix4f echoProjection(Matrix4f projection) {
        // Infinite far plane for depth-free echoes; native render distance must not clip shrieker poses.
        float near = projection.m32() / (projection.m22() - 1);
        return new Matrix4f(projection).m22(-1).m32(-2 * near);
    }

    private static void updateSurfaces(Minecraft mc) {
        if (surfaces.center() == null) surfaces.start(mc.player.blockPosition());
        surfaces.tick(mc);
        var center = surfaces.center();
        if (nextSurfaces == null && (Math.abs(mc.player.getX() - center.getX()) > 3
                || Math.abs(mc.player.getY() - center.getY()) > 3 || Math.abs(mc.player.getZ() - center.getZ()) > 3)) {
            nextSurfaces = new WardenSurfaceCache();
            nextSurfaces.start(mc.player.blockPosition());
        }
        if (nextSurfaces != null) {
            nextSurfaces.tick(mc);
            if (nextSurfaces.ready() || nextSurfaces.limited()) {
                surfaces = nextSurfaces;
                nextSurfaces = null;
            }
        }
        if (surfaces.ready()) {
            for (int i = 0; i < PULSES.size(); i++) {
                Pulse p = PULSES.get(i);
                if (p.born < 0) PULSES.set(i, new Pulse(p.origin, clock, p.radius, p.duration));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void world(RenderLevelStageEvent event) {
        if (!active()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // AFTER_LEVEL in Forge 1.20.1 supplies the projection PoseStack, not the world view.
            view = new Matrix4f(event.getPoseStack().last().pose());
            projection = new Matrix4f(event.getProjectionMatrix());
            camera = event.getCamera().getPosition();
            right = event.getCamera().rotation().transform(new Vector3f(1, 0, 0));
            up = event.getCamera().rotation().transform(new Vector3f(0, 1, 0));
            WORLD_DEPTH.capture(Minecraft.getInstance().getMainRenderTarget());
            frustum = new net.minecraft.client.renderer.culling.Frustum(view, projection);
            frustum.prepare(camera.x, camera.y, camera.z);
            if (!PULSES.isEmpty() || !RINGS.isEmpty())
                DYNAMIC.capture(Minecraft.getInstance(), surfaces, event.getPartialTick());
            else DYNAMIC.clear();
            if (WardenSniffClient.readyForCapture())
                WardenSniffClient.scene().capture(Minecraft.getInstance(), WardenSniffClient.cache(), event.getPartialTick());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            render(event.getPartialTick(), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (active() && !DYNAMIC.capturing() && !WardenSniffClient.capturing() && !WardenPlayerForm.isPreview(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideCapturedName(RenderNameTagEvent event) {
        if (DYNAMIC.capturing() || WardenSniffClient.capturing()) event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeHud(RenderGuiEvent.Pre event) {
        // Erase post-world effects, outlines, screen effects and all existing world markers before HUD.
        if (!active()) return;
        try {
            render(event.getPartialTick(), false);
            clearHudDepth();
            WardenFirstPersonArms.renderAfterVision();
        } finally { clearHudDepth(); }
    }
    private static void clearHudDepth() {
        // Neither world nor first-person arms may become HUD occluders, including early returns.
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.depthMask(true); RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX); RenderSystem.depthMask(mask);
    }

    private static void render(float partialTick, boolean prepare) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        RenderSystem.clearColor(0, 0, 0, 1);
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        RenderSystem.depthMask(oldDepthMask);
        if (view == null || (PULSES.isEmpty() && ECHOES.isEmpty() && NOISE_ECHOES.isEmpty() && RINGS.isEmpty() && !WardenSniffClient.renderable())
                || !WORLD_DEPTH.restore(mc.getMainRenderTarget())) return;
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSorting = RenderSystem.getVertexSorting();
        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        boolean oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        var oldColorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, oldColorMask);
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int oldBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int oldBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int oldBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int oldBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        Matrix4f matrix = new Matrix4f(view).translate((float) -camera.x, (float) -camera.y, (float) -camera.z);
        long drawStart;
        try {
            if (prepare) prepareBuffers(mc.isPaused() ? clock : clock + partialTick);
            drawStart = System.nanoTime();
            BUFFERS.draw(WardenSceneBuffers.Layer.BLOCK_DEPTH, matrix, projection);
            BUFFERS.draw(WardenSceneBuffers.Layer.DYNAMIC_DEPTH, matrix, projection);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            BUFFERS.draw(WardenSceneBuffers.Layer.POINTS, matrix, projection);
            BUFFERS.draw(WardenSceneBuffers.Layer.WAVE, matrix, projection);
            if (WardenSniffClient.renderable()) {
                RenderSystem.depthMask(true); RenderSystem.disableBlend();
                RenderSystem.colorMask(false, false, false, false);
                BUFFERS.draw(WardenSceneBuffers.Layer.SCENT_DEPTH, matrix, projection);
                RenderSystem.colorMask(oldColorMask.get(0) != 0, oldColorMask.get(1) != 0, oldColorMask.get(2) != 0, oldColorMask.get(3) != 0);
                RenderSystem.depthMask(false); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                BUFFERS.draw(WardenSceneBuffers.Layer.SCENT, matrix, projection);
            }
            RenderSystem.disableDepthTest(); // Only immutable echoes bypass occlusion.
            BUFFERS.draw(WardenSceneBuffers.Layer.ECHOES, matrix, echoProjection(projection));
            WardenVisionPerformance.record(WardenVisionPerformance.Stage.DRAW, drawStart);
        } finally {
            RenderSystem.colorMask(oldColorMask.get(0) != 0, oldColorMask.get(1) != 0, oldColorMask.get(2) != 0, oldColorMask.get(3) != 0);
            RenderSystem.depthMask(oldDepthMask);
            RenderSystem.depthFunc(oldDepthFunc);
            if (oldDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (oldCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(oldBlendSrcRgb, oldBlendDstRgb, oldBlendSrcAlpha, oldBlendDstAlpha);
            if (oldBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void prepareBuffers(double now) {
        long start = System.nanoTime();
        drawnPoints = 0;
        if (BUFFERS.blocksChanged(surfaces)) BUFFERS.upload(WardenSceneBuffers.Layer.BLOCK_DEPTH,
                buffer -> depthFaces(buffer, surfaces.flatFaces()));
        if (BUFFERS.dynamicChanged(DYNAMIC.revision())) BUFFERS.upload(WardenSceneBuffers.Layer.DYNAMIC_DEPTH, buffer -> {
            depthFaces(buffer, DYNAMIC.blocks());
            depthFaces(buffer, DYNAMIC.creatures());
        });
        if (WardenSniffClient.renderable()) {
            var scentCache = WardenSniffClient.cache(); var scene = WardenSniffClient.scene();
            if (scentOwner != scentCache || scentRevision != scentCache.revision() || scentDynamicRevision != scene.revision()) {
                BUFFERS.upload(WardenSceneBuffers.Layer.SCENT_DEPTH, buffer -> {
                    depthFaces(buffer, scentCache.flatFaces()); depthFaces(buffer, scene.blocks()); depthFaces(buffer, scene.creatures());
                });
                scentOwner = scentCache; scentRevision = scentCache.revision(); scentDynamicRevision = scene.revision();
            }
            BUFFERS.upload(WardenSceneBuffers.Layer.SCENT, buffer -> {
                double serverTime = Minecraft.getInstance().level.getGameTime() + (Minecraft.getInstance().isPaused() ? 0 : now - clock);
                for (var point : WardenSniffClient.points()) {
                    var p = point.position();
                    if (p.distanceToSqr(camera) >= 16 * 16 || Math.abs(p.x - scentCache.center().getX()) > 19
                            || Math.abs(p.y - scentCache.center().getY()) > 19 || Math.abs(p.z - scentCache.center().getZ()) > 19) continue;
                    float alpha = point.brightness(serverTime);
                    if (alpha > 0.01F) dot(buffer, p, 0.2F, 1, 0.85F, alpha, 0.12);
                }
            });
        }
        BUFFERS.upload(WardenSceneBuffers.Layer.POINTS, buffer -> {
            pointFaces(buffer, surfaces.flatFaces(), now, null);
            pointFaces(buffer, DYNAMIC.blocks(), now, null);
            // Native world depth occludes these distant patches; never expand the main cache cube.
            for (var cache : DISTANT_SURFACES.caches()) pointFaces(buffer, cache.flatFaces(), now, null);
            for (var creature : DYNAMIC.creatureMeshes())
                pointFaces(buffer, creature.faces(), now, creature.tint());
        });
        BUFFERS.upload(WardenSceneBuffers.Layer.WAVE, buffer -> {
            var mc = Minecraft.getInstance(); var knownChunks = new java.util.HashMap<Long, Boolean>();
            for (var point : WardenWavePoints.visible(RINGS, now, camera, p -> {
                int ax = net.minecraft.core.BlockPos.containing(camera).getX() >> 4, az = net.minecraft.core.BlockPos.containing(camera).getZ() >> 4;
                int bx = net.minecraft.core.BlockPos.containing(p).getX() >> 4, bz = net.minecraft.core.BlockPos.containing(p).getZ() >> 4;
                // Unknown chunks are not empty space. This conservative rectangle contains the complete ray.
                for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
                    final int chunkX = x, chunkZ = z;
                    if (!knownChunks.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x, z), key -> mc.level.hasChunk(chunkX, chunkZ))) return false;
                }
                return mc.level.clip(new net.minecraft.world.level.ClipContext(camera, p,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player)).getType()
                        == net.minecraft.world.phys.HitResult.Type.MISS;
            })) dot(buffer, point.position(), 0.2F, 0.95F, 0.85F, point.alpha());
        });
        BUFFERS.upload(WardenSceneBuffers.Layer.ECHOES, buffer -> {
            for (var echo : NOISE_ECHOES) {
                float alpha = echo.brightness(Vec3.ZERO, now);
                if (alpha > 0.01F) for (var point : echo.points()) dot(buffer, point, 0.15F, 0.45F, 1, alpha);
            }
            for (var echo : ECHOES) {
                double echoAge = now - echo.born();
                // A late test snapshot must fade before its owning pulse expires, not vanish brightly.
                double fade = Math.max(0, 1 - echoAge / echo.duration());
                if (echo.areaOrigin() == null) fade = Math.min(fade, Math.max(0, 1 - (now - echoBorn) / DURATION));
                float alpha = (float) (fade * fade);
                for (Vec3 point : echo.points()) {
                    Vec3 area = echo.areaOrigin() == null ? origin : echo.areaOrigin();
                    if (area != null && point.distanceToSqr(area) <= echo.radius() * echo.radius())
                        dot(buffer, point, 0.15f, 0.45f, 1, echo.areaOrigin() == null ? alpha : echo.brightness(point, now));
                }
            }
        });
        WardenVisionPerformance.points(drawnPoints);
        WardenVisionPerformance.record(WardenVisionPerformance.Stage.BUILD, start);
    }

    private static void depthFaces(BufferBuilder buffer, List<WardenSurfaceCache.Face> faces) {
        for (var face : faces) {
            if (face.water()) continue; // Transparent water must not become an opaque depth wall.
            vertex(buffer, face.a()); vertex(buffer, face.b()); vertex(buffer, face.c()); vertex(buffer, face.d());
        }
    }

    private static void pointFaces(BufferBuilder buffer, List<WardenSurfaceCache.Face> faces, double now,
                                   WardenVisionClassification.CreatureTint tint) {
        for (var face : faces) {
            if (frustum != null && !frustum.isVisible(face.bounds())) continue;
            boolean reached = false;
            for (var pulse : PULSES) {
                if (pulse.born < 0 || now <= pulse.born) continue;
                double range = Math.min(pulse.radius, (now - pulse.born) * WardenPulseTiming.SPEED);
                if (face.bounds().distanceToSqr(pulse.origin) <= range * range) { reached = true; break; }
            }
            if (!reached) continue;
            int stride = WardenRenderBudget.pointStride(face.a().distanceToSqr(camera));
            if (face.water()) {
                // Keep each sparse ripple continuous at distance instead of thinning individual segments.
                int bands = stride >= 4 ? 1 : WardenWaterPattern.BANDS;
                for (int i = 0; i < bands * WardenWaterPattern.SEGMENTS; i++) {
                    var ribbon = WardenWaterPattern.ribbon(face, i, now);
                    float alpha = brightness(ribbon.center(), now);
                    if (alpha <= 0.01F) continue;
                    drawnPoints++;
                    waterVertex(buffer, ribbon.a(), alpha); waterVertex(buffer, ribbon.b(), alpha);
                    waterVertex(buffer, ribbon.c(), alpha); waterVertex(buffer, ribbon.d(), alpha);
                }
                continue;
            }
            for (int i = 0; i < face.points().size(); i += stride) {
                Vec3 point = face.points().get(i);
                float alpha = brightness(point, now);
                if (alpha > 0.01f) dot(buffer, point, tint == null ? 1 : tint.r, tint == null ? 1 : tint.g,
                        tint == null ? (face.yellow() ? 0.05f : 1) : tint.b, alpha);
            }
        }
    }

    private static void waterVertex(BufferBuilder buffer, Vec3 p, float alpha) {
        buffer.vertex(p.x, p.y, p.z).color(WardenWaterPattern.RED, WardenWaterPattern.GREEN, WardenWaterPattern.BLUE, alpha).endVertex();
    }

    private static float brightness(Vec3 point, double now) {
        float alpha = 0;
        for (Pulse pulse : PULSES) {
            if (pulse.born < 0) continue;
            double squared = point.distanceToSqr(pulse.origin);
            if (squared > pulse.radius * pulse.radius) continue;
            alpha = Math.max(alpha, WardenPulseTiming.brightness(now - pulse.born,
                    Math.sqrt(squared), pulse.radius, pulse.duration));
        }
        return alpha;
    }

    private static void dot(BufferBuilder buffer, Vec3 p, float r, float g, float b, float alpha) {
        double size = Math.max(0.014, Math.min(0.035, p.distanceTo(camera) * 0.002));
        dot(buffer, p, r, g, b, alpha, size);
    }
    private static void dot(BufferBuilder buffer, Vec3 p, float r, float g, float b, float alpha, double size) {
        drawnPoints++;
        for (int corner = 0; corner < 4; corner++) {
            double x = (corner == 0 || corner == 3 ? -size : size);
            double y = (corner < 2 ? -size : size);
            buffer.vertex((float) (p.x + right.x * x + up.x * y),
                    (float) (p.y + right.y * x + up.y * y), (float) (p.z + right.z * x + up.z * y))
                    .color(r, g, b, alpha).endVertex();
        }
    }

    private static void vertex(BufferBuilder buffer, Vec3 p) {
        buffer.vertex(p.x, p.y, p.z).color(0, 0, 0, 255).endVertex();
    }
}
