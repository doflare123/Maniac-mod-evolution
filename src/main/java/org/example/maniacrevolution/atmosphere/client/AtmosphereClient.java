package org.example.maniacrevolution.atmosphere.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.NightmareLighterItem;
import org.example.maniacrevolution.atmosphere.AtmosphereConfig;
import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.atmosphere.AtmosphereRules.Profile;
import org.example.maniacrevolution.character.maniac.WardenClass;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.nightmare.ClientNightmareData;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.util.PlayerModeUtil;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.example.maniacrevolution.warden.client.WardenVisionPrototype;

/** Client-only Veil adapter. Gameplay state remains on the server. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereClient {
    public static final ResourceLocation PIPELINE = Maniacrev.loc("atmosphere");
    public static final ResourceLocation LIGHT_PIPELINE = Maniacrev.loc("atmosphere_light");
    private static Profile preview;
    private static boolean disabled;
    private static float previewSanity = 1, previewChase;
    private static float serverChase, corruption, pursuit;
    private static ClientLevel world;
    private static java.util.UUID playerId;
    private static float fogGround, indoor;
    private static boolean testLight;
    private static Vec3 lampPosition;

    private AtmosphereClient() {}

    public static GraphicsMode graphicsMode() {
        GraphicsMode benchmark = AtmosphereBenchmark.graphicsOverride();
        return benchmark == null ? ClientGameSettings.getGraphicsMode() : benchmark;
    }

    public static void chase(float value) {
        serverChase = AtmosphereRules.clamp(value);
    }

    public static float cameraCorruption() {
        if (AtmosphereBenchmark.forcingProfile()) return 0;
        return profile() == Profile.SURVIVOR ? corruption : 0;
    }

    private static boolean viewingWorld() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        return player != null && mc.level != null && player.isAlive()
                && PlayerModeUtil.isSurvivalOrAdventure(player) && mc.getCameraEntity() == player
                && !WardenVisionPrototype.inMatch();
    }

    private static Profile matchProfile() {
        var player = Minecraft.getInstance().player;
        return AtmosphereRules.profile(true, true, ClientGameState.getPhase(),
                player.getTeam() == null ? null : player.getTeam().getName(),
                ClientPlayerData.getManiacClassId() == NightmareConfig.KEEPER_CLASS_ID,
                ClientPlayerData.getManiacClassId() == WardenClass.SCOREBOARD_ID);
    }

    /** Local post-processing toggles keep the sight limit; the host's OFF mode removes it. */
    public static boolean hasFogLimit() {
        return !VeilRenderControl.isBypassed() && graphicsMode().enabled() && viewingWorld() && (AtmosphereBenchmark.forcingProfile() || matchProfile() != Profile.OFF
                || preview != null && !disabled && AtmosphereConfig.ENABLED.get());
    }

    public static Profile profile() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (VeilRenderControl.isBypassed() || !graphicsMode().enabled() || (!AtmosphereBenchmark.forcingProfile() && (disabled || !AtmosphereConfig.ENABLED.get())) || !viewingWorld()
                || mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE
                || player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS)
                || player.hasEffect(ModEffects.FORESTER_BLIND.get())) return Profile.OFF;
        if (AtmosphereBenchmark.forcingProfile()) return Profile.SURVIVOR;
        if (preview != null) return preview;
        return matchProfile();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (world != mc.level || mc.player == null || !mc.player.getUUID().equals(playerId)) {
            reset();
            world = mc.level;
            playerId = mc.player == null ? null : mc.player.getUUID();
            if (mc.player != null) fogGround = (float) mc.player.getY();
        }
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        Profile current = profile();
        float sanity = preview != null ? previewSanity : ClientNightmareData.getSanityPercent();
        float target = AtmosphereRules.corruption(current, preview != null || ClientNightmareData.isActive(), sanity);
        corruption += (target - corruption) * 0.12F;
        pursuit += ((current == Profile.OFF ? 0 : preview != null ? previewChase : serverChase) - pursuit) * 0.2F;
        fogGround += ((float) mc.player.getY() - fogGround) * 0.05F;
        if (mc.player.tickCount % 10 == 0) {
            float roof = mc.level.canSeeSky(mc.player.blockPosition().above()) ? 0 : 1;
            indoor += (roof - indoor) * 0.4F;
        }
        if (current == Profile.OFF) AtmosphereLights.suspend();
        else AtmosphereLights.tick(graphicsMode(), testLight && lampPosition != null || holdingBurningLighter());
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var manager = VeilRenderSystem.renderer().getPostProcessingManager();
        ResourceLocation selected = graphicsMode() == GraphicsMode.LIGHT ? LIGHT_PIPELINE : PIPELINE;
        manager.remove(selected.equals(PIPELINE) ? LIGHT_PIPELINE : PIPELINE);
        if (profile() == Profile.OFF) {
            manager.remove(selected);
            removeLight();
        } else {
            manager.add(selected);
        }
    }

    @SubscribeEvent
    public static void uniforms(ForgeVeilPostProcessingEvent.Pre event) {
        if (!PIPELINE.equals(event.getName()) && !LIGHT_PIPELINE.equals(event.getName())) return;
        var mc = Minecraft.getInstance();
        var pipeline = event.getPipeline();
        Profile current = profile();
        if (mc.level == null || mc.player == null) return;
        float time = (mc.level.getGameTime() % 240000 + mc.getFrameTime()) / 20F;
        float comfort = AtmosphereConfig.DISTORTION.get().floatValue();
        float lightingCorruption = current == Profile.SURVIVOR ? corruption : 0;
        pipeline.setFloat("Active", current == Profile.OFF ? 0 : 1);
        pipeline.setFloat("Time", time);
        pipeline.setFloat("Corruption", lightingCorruption * comfort);
        pipeline.setFloat("LightCorruption", lightingCorruption);
        pipeline.setFloat("Comfort", comfort);
        pipeline.setFloat("Pursuit", pursuit * comfort);
        pipeline.setFloat("FogGround", fogGround);
        pipeline.setFloat("FogDensity", 0.055F + lightingCorruption * 0.012F);
        pipeline.setFloat("Indoor", indoor);
        pipeline.setFloat("SightFadeStart", AtmosphereRules.FOG_FADE_START);
        pipeline.setFloat("SightRange", AtmosphereRules.FOG_END);
        pipeline.setFloat("Hurt", Math.min(1, mc.player.hurtTime / 10F) * comfort);
        pipeline.setFloat("Fear", mc.player.hasEffect(ModEffects.FEAR.get()) ? comfort : 0);
        pipeline.setFloat("Plague", mc.player.hasEffect(ModEffects.PLAGUE.get()) ? 1 : 0);
        pipeline.setInt("FogSamples", graphicsMode().fogSamples());
        Vec3 light = testLight ? lampPosition : holdingBurningLighter()
                ? mc.player.getEyePosition(mc.getFrameTime()).add(0, -0.3, 0) : null;
        AtmosphereLights.upload(pipeline, current == Profile.OFF ? GraphicsMode.OFF : graphicsMode(),
                light, testLight, mc.getFrameTime());
        AtmosphereBenchmark.configureUniforms(pipeline);
    }

    private static boolean holdingBurningLighter() {
        var player = Minecraft.getInstance().player;
        return player != null && (NightmareLighterItem.isLit(player.getMainHandItem())
                || NightmareLighterItem.isLit(player.getOffhandItem()));
    }

    private static void removeLight() {
        lampPosition = null;
        AtmosphereLights.suspend();
    }

    private static void reset() {
        VeilRenderSystem.renderer().getPostProcessingManager().remove(PIPELINE);
        VeilRenderSystem.renderer().getPostProcessingManager().remove(LIGHT_PIPELINE);
        removeLight();
        AtmosphereLights.clear();
        preview = null;
        disabled = testLight = false;
        previewSanity = 1;
        previewChase = serverChase = corruption = pursuit = indoor = 0;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        ClientGameSettings.resetGraphicsMode();
        world = null;
        playerId = null;
    }

    private static int message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), false);
        return 1;
    }

    private static int preview(Profile value) {
        preview = value;
        disabled = false;
        return message("Атмосфера: " + value + ". Просмотр работает в survival/adventure; /mrvisual auto — вернуть матч.");
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mrvisual")
                .then(Commands.literal("auto").executes(c -> {
                    preview = null; disabled = false; previewSanity = 1; previewChase = 0;
                    testLight = false; removeLight();
                    return message("Атмосфера определяется ролью и состоянием матча.");
                }))
                .then(Commands.literal("off").executes(c -> {
                    disabled = true; testLight = false; removeLight();
                    return message("Постобработка выключена; предел обзора в матче остаётся. /mrvisual auto — включить эффекты.");
                }))
                .then(Commands.literal("preview")
                        .then(Commands.literal("survivor").executes(c -> preview(Profile.SURVIVOR)))
                        .then(Commands.literal("maniac").executes(c -> preview(Profile.MANIAC)))
                        .then(Commands.literal("keeper").executes(c -> preview(Profile.KEEPER))))
                .then(Commands.literal("sanity").then(Commands.argument("percent", FloatArgumentType.floatArg(0, 100))
                        .executes(c -> {
                            if (preview != Profile.SURVIVOR) return message("Сначала /mrvisual preview survivor.");
                            previewSanity = FloatArgumentType.getFloat(c, "percent") / 100F;
                            return message("Тестовый рассудок: " + Math.round(previewSanity * 100) + "% (серверный рассудок не изменён).");
                        })))
                .then(Commands.literal("chase").then(Commands.argument("intensity", FloatArgumentType.floatArg(0, 1))
                        .executes(c -> {
                            if (preview == null) return message("Сначала /mrvisual preview survivor|maniac|keeper.");
                            previewChase = FloatArgumentType.getFloat(c, "intensity");
                            return message("Тестовая погоня: " + previewChase);
                        })))
                .then(Commands.literal("light").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(c -> {
                            if (BoolArgumentType.getBool(c, "enabled") && !graphicsMode().enabled())
                                return message("Сначала включите один из режимов графики в настройках матча.");
                            testLight = BoolArgumentType.getBool(c, "enabled");
                            removeLight();
                            var player = Minecraft.getInstance().player;
                            if (testLight && player != null) lampPosition = player.getEyePosition().add(player.getLookAngle().scale(3));
                            return message(testLight ? "Тёплый тестовый свет поставлен перед вами. Он освещает поверхности во всех включённых режимах."
                                    : "Тестовый свет убран.");
                        })))
                .then(Commands.literal("status").executes(c -> {
                    var renderer = VeilRenderSystem.renderer();
                    var pipeline = renderer.getPostProcessingManager().getPipeline(
                            graphicsMode() == GraphicsMode.LIGHT ? LIGHT_PIPELINE : PIPELINE);
                    return message("Veil: профиль=" + profile() + ", шейдер=" + (pipeline != null && pipeline.hasUniform("FogDensity"))
                            + ", графика=" + graphicsMode().displayName()
                            + ", источники=" + AtmosphereLights.sourceCount() + "/" + AtmosphereLights.MAX_SOURCES
                            + ", группы=" + AtmosphereLights.groupCount() + ", секции=" + AtmosphereLights.cachedSections()
                            + ", сканирования=" + AtmosphereLights.sectionScans()
                            + ", обзор=" + (hasFogLimit() ? AtmosphereRules.FOG_END + " блоков" : "обычный")
                            + ", рассудок=" + Math.round((1 - corruption) * 100) + "%, погоня=" + pursuit);
                })));
    }
}
