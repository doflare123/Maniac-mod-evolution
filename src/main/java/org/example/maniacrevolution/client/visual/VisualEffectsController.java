package org.example.maniacrevolution.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.config.VisualEffectsConfig;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.nightmare.ClientNightmareData;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class VisualEffectsController {
    private static final VisualEffectState STATE = new VisualEffectState();
    private static LocalPlayer trackedPlayer;
    private static Object trackedLevel;
    private static boolean wasGrounded;
    private static double lastVerticalSpeed;
    private static int ticks;
    private static int previewTicks;
    private static String preview = "off";

    private VisualEffectsController() {}

    private static boolean eligiblePlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.player.isAlive()
                && !mc.player.isCreative() && !mc.player.isSpectator();
    }

    private static boolean active() {
        return eligiblePlayer() && VisualEffectsConfig.ENABLED.get() && VisualEffectsConfig.INTENSITY.get() > 0
                && (previewTicks > 0
                || (ClientGameState.isGameRunning() && ClientPlayerData.getActiveType() != null));
    }

    private static boolean firstPersonView() {
        Minecraft mc = Minecraft.getInstance();
        return active() && mc.options.getCameraType().isFirstPerson()
                && mc.getCameraEntity() == mc.player;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != trackedPlayer || mc.level != trackedLevel) {
            reset();
            trackedPlayer = mc.player;
            trackedLevel = mc.level;
            wasGrounded = mc.player != null && mc.player.onGround();
        }
        if (!active()) {
            STATE.reset();
            ticks = 0;
            lastVerticalSpeed = 0;
            wasGrounded = mc.player != null && mc.player.onGround();
            previewTicks = 0;
            VisualPostProcessor.close();
            return;
        }
        if (mc.isPaused()) return;
        LocalPlayer player = mc.player;
        boolean survivor = ClientPlayerData.getActiveType() == CharacterType.SURVIVOR;
        boolean maniac = ClientPlayerData.getActiveType() == CharacterType.MANIAC;
        float health = player.getHealth();
        if (previewTicks > 0) {
            survivor = !preview.equals("maniac");
            maniac = !survivor;
            if (preview.equals("survivor")) health = player.getMaxHealth() * 0.15f;
        }
        boolean grounded = player.onGround();
        boolean canMoveCamera = !player.isPassenger() && !player.isSleeping()
                && !player.isInWaterOrBubble() && !player.isFallFlying();
        float landing = canMoveCamera && grounded && !wasGrounded
                ? Mth.clamp((float) (-lastVerticalSpeed - 0.2) * 1.5f, 0, 1) : 0;
        float strafe = canMoveCamera && player.input != null
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.0001
                ? player.input.leftImpulse : 0;
        STATE.tick(health, player.getMaxHealth(), player.getHealth() + player.getAbsorptionAmount(),
                player.hurtTime > 0, survivor, maniac, strafe, landing);
        wasGrounded = grounded;
        lastVerticalSpeed = player.getDeltaMovement().y;
        ticks++;
        if (previewTicks > 0) {
            if (preview.equals("damage") && previewTicks % 40 == 0) STATE.pulseDamage();
            previewTicks--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!firstPersonView() || !VisualEffectsConfig.CAMERA.get() || mc.screen != null
                || mc.player.isSleeping() || mc.player.isPassenger()) return;
        // Fear owns actual aim; severe nightmare already supplies its own camera motion.
        if (mc.player.hasEffect(ModEffects.FEAR.get())
                || (ClientNightmareData.isVisible() && ClientNightmareData.getSanityPercent() < 0.18f)) return;
        float partial = (float) event.getPartialTick();
        float intensity = VisualEffectsConfig.INTENSITY.get().floatValue();
        event.setRoll(event.getRoll() + STATE.roll(partial) * intensity);
        event.setPitch(event.getPitch()
                + (STATE.landing(partial) * 0.65f + STATE.damage(partial) * 0.22f) * intensity);
    }

    @SubscribeEvent
    public static void beforeHud(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!firstPersonView() || mc.screen != null || !VisualEffectsConfig.POST_PROCESSING.get()) return;
        float partial = event.getPartialTick();
        float intensity = VisualEffectsConfig.INTENSITY.get().floatValue();
        // Flush queued GUI geometry before changing framebuffers. HUD is drawn after this event.
        event.getGuiGraphics().flush();
        VisualPostProcessor.render(partial, (ticks + partial) / 20.0f,
                STATE.injury(partial) * intensity, STATE.damage(partial) * intensity,
                STATE.maniac(partial) * intensity, VisualEffectsConfig.DISTORTION.get() ? 1 : 0);
    }

    /** Local ten-second preview: no health/role changes and no network packets. */
    static boolean preview(String mode) {
        if (mode.equals("off")) {
            reset();
            return true;
        }
        if (!eligiblePlayer() || !VisualEffectsConfig.ENABLED.get()
                || VisualEffectsConfig.INTENSITY.get() <= 0) return false;
        reset();
        Minecraft mc = Minecraft.getInstance();
        trackedPlayer = mc.player;
        trackedLevel = mc.level;
        wasGrounded = mc.player.onGround();
        preview = mode;
        previewTicks = 200;
        return true;
    }

    private static void reset() {
        STATE.reset();
        trackedPlayer = null;
        trackedLevel = null;
        lastVerticalSpeed = 0;
        wasGrounded = false;
        ticks = previewTicks = 0;
        preview = "off";
        VisualPostProcessor.close();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        VisualPostProcessor.reload();
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ReloadRegistration {
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                if (RenderSystem.isOnRenderThread()) VisualPostProcessor.reload();
                else RenderSystem.recordRenderCall(VisualPostProcessor::reload);
            });
        }
    }
}
