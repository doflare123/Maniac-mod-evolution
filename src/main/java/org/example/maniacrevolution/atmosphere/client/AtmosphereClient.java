package org.example.maniacrevolution.atmosphere.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.PointLight;
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
import org.example.maniacrevolution.ModItems;
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
import org.example.maniacrevolution.warden.client.WardenVisionPrototype;

/** Client-only Veil adapter. Gameplay state remains on the server. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereClient {
    public static final ResourceLocation PIPELINE = Maniacrev.loc("atmosphere");
    private static Profile preview;
    private static boolean disabled;
    private static float previewSanity = 1, previewChase;
    private static float serverChase, corruption, pursuit;
    private static ClientLevel world;
    private static java.util.UUID playerId;
    private static float fogGround, indoor;
    private static boolean testLight;
    private static Vec3 lampPosition;
    private static PointLight lamp;

    private AtmosphereClient() {}

    public static void chase(float value) {
        serverChase = AtmosphereRules.clamp(value);
    }

    public static float cameraCorruption() {
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

    /** The match visibility rule survives disabling optional post-processing. */
    public static boolean hasFogLimit() {
        return viewingWorld() && (matchProfile() != Profile.OFF
                || preview != null && !disabled && AtmosphereConfig.ENABLED.get());
    }

    public static Profile profile() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (disabled || !AtmosphereConfig.ENABLED.get() || !viewingWorld()
                || mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE
                || player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS)
                || player.hasEffect(ModEffects.FORESTER_BLIND.get())) return Profile.OFF;
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
        updateLight(current);
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var manager = VeilRenderSystem.renderer().getPostProcessingManager();
        if (profile() == Profile.OFF) manager.remove(PIPELINE);
        else manager.add(PIPELINE);
    }

    @SubscribeEvent
    public static void uniforms(ForgeVeilPostProcessingEvent.Pre event) {
        if (!PIPELINE.equals(event.getName())) return;
        var mc = Minecraft.getInstance();
        var pipeline = event.getPipeline();
        Profile current = profile();
        if (mc.level == null || mc.player == null) return;
        float time = (mc.level.getGameTime() % 240000 + mc.getFrameTime()) / 20F;
        float comfort = AtmosphereConfig.DISTORTION.get().floatValue();
        pipeline.setFloat("Active", current == Profile.OFF ? 0 : 1);
        pipeline.setFloat("Time", time);
        pipeline.setFloat("Corruption", corruption * comfort);
        pipeline.setFloat("Pursuit", pursuit * comfort);
        pipeline.setFloat("FogGround", fogGround);
        pipeline.setFloat("FogDensity", 0.055F + corruption * 0.012F);
        pipeline.setFloat("Indoor", indoor);
        pipeline.setFloat("SightFadeStart", AtmosphereRules.FOG_FADE_START);
        pipeline.setFloat("SightRange", AtmosphereRules.FOG_END);
        pipeline.setFloat("Hurt", Math.min(1, mc.player.hurtTime / 10F) * comfort);
        pipeline.setFloat("Fear", mc.player.hasEffect(ModEffects.FEAR.get()) ? comfort : 0);
        pipeline.setFloat("Plague", mc.player.hasEffect(ModEffects.PLAGUE.get()) ? 1 : 0);
        pipeline.setInt("FogSamples", AtmosphereConfig.FOG_SAMPLES.get());
        // Do not brighten blackout states or use the point light as wall vision.
        pipeline.setFloat("LampStrength", lampPosition == null || current == Profile.OFF ? 0 : 1);
        Vec3 light = lampPosition == null ? Vec3.ZERO : lampPosition;
        pipeline.setVector("LampPosition", (float) light.x, (float) light.y, (float) light.z);
    }

    private static void updateLight(Profile current) {
        var mc = Minecraft.getInstance();
        boolean lighter = mc.player.isHolding(ModItems.NIGHTMARE_LIGHTER.get());
        if (current == Profile.OFF || !testLight && !lighter) {
            removeLight();
            return;
        }
        if (!testLight || lampPosition == null) lampPosition = mc.player.getEyePosition().add(0, -0.4, 0);
        var deferred = VeilRenderSystem.renderer().getDeferredRenderer();
        if (!deferred.isEnabled()) {
            if (lamp != null) deferred.getLightRenderer().removeLight(lamp);
            lamp = null;
            return;
        }
        if (lamp == null) {
            lamp = new PointLight().setColor(1.0F, 0.55F, 0.22F).setRadius(10).setBrightness(1.3F);
            deferred.getLightRenderer().addLight(lamp);
        }
        lamp.setPosition(lampPosition.x, lampPosition.y, lampPosition.z);
    }

    private static void removeLight() {
        if (lamp != null) VeilRenderSystem.renderer().getDeferredRenderer().getLightRenderer().removeLight(lamp);
        lamp = null;
        lampPosition = null;
    }

    private static void reset() {
        VeilRenderSystem.renderer().getPostProcessingManager().remove(PIPELINE);
        removeLight();
        preview = null;
        disabled = testLight = false;
        previewSanity = 1;
        previewChase = serverChase = corruption = pursuit = indoor = 0;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
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
                            testLight = BoolArgumentType.getBool(c, "enabled");
                            removeLight();
                            var player = Minecraft.getInstance().player;
                            if (testLight && player != null) lampPosition = player.getEyePosition().add(player.getLookAngle().scale(3));
                            return message(testLight ? "Тестовый свет поставлен перед вами. Для освещения поверхностей включите ресурс-пак Veil Deferred (Fast/Fancy, без Embeddium)."
                                    : "Тестовый свет убран.");
                        })))
                .then(Commands.literal("status").executes(c -> {
                    var renderer = VeilRenderSystem.renderer();
                    var pipeline = renderer.getPostProcessingManager().getPipeline(PIPELINE);
                    return message("Veil: профиль=" + profile() + ", шейдер=" + (pipeline != null && pipeline.hasUniform("FogDensity"))
                            + ", свет=" + renderer.getDeferredRenderer().isEnabled()
                            + ", обзор=" + (hasFogLimit() ? AtmosphereRules.FOG_END + " блоков" : "обычный")
                            + ", рассудок=" + Math.round((1 - corruption) * 100) + "%, погоня=" + pursuit);
                })));
    }
}
