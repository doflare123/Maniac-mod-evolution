package org.example.maniacrevolution.scp173.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Scp173InputPacket;
import org.example.maniacrevolution.network.packets.Scp173InputPacket.Action;
import org.example.maniacrevolution.network.packets.Scp173StatusPacket;
import org.example.maniacrevolution.scp173.Scp173CombatGeometry;
import org.example.maniacrevolution.scp173.Scp173Rules;
import org.example.maniacrevolution.util.PlayerModeUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class Scp173GameplayClient {
    private static final KeyMapping BLINK = new KeyMapping("key.maniacrev.scp173_blink", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.maniacrev");
    private static Scp173StatusPacket state;
    private static Object player, level;
    private static long strikeTick = -1;
    private Scp173GameplayClient() {}
    public static void accept(Scp173StatusPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !packet.dimension().equals(mc.level.dimension().location())
                || packet.tick() > mc.level.getGameTime() + 20 || !Double.isFinite(packet.meter() + packet.pressure() + packet.light() + packet.cost())) return;
        if (player != mc.player || level != mc.level) reset();
        if (state != null && packet.tick() < state.tick()) return;
        if (state != null && packet.statue() && packet.melee() > state.melee() && packet.melee() <= 100)
            strikeTick = packet.tick() - (100 - packet.melee());
        state = packet; player = mc.player; level = mc.level;
        Scp173PlayerForm.update(mc.player.getUUID(), packet.statue());
    }
    public static boolean statue() {
        var mc = Minecraft.getInstance();
        return valid() && state.statue() && mc.player.getTeam() != null
                && "maniac".equals(mc.player.getTeam().getName())
                && org.example.maniacrevolution.data.ClientPlayerData.getManiacClassId() == 13;
    }
    private static boolean valid() {
        var mc = Minecraft.getInstance();
        return state != null && player == mc.player && level == mc.level && mc.player != null
                && mc.player.isAlive() && PlayerModeUtil.isSurvivalOrAdventure(mc.player);
    }
    private static boolean survivor() {
        var p = Minecraft.getInstance().player;
        return valid() && p.getTeam() != null && "survivors".equals(p.getTeam().getName());
    }
    private static int age() { return valid() ? (int) Math.max(0, Minecraft.getInstance().level.getGameTime() - state.tick()) : 0; }
    public static int meleeCooldown() { return statue() ? Math.max(0, state.melee() - age()) : 0; }
    public static double lightCooldown() { return statue() ? Math.max(0, state.light()) : 0; }
    public static int lightDuration() { return statue() ? Math.max(0, state.lightDuration() - age()) : 0; }
    public static float lightCost() { return statue() ? state.cost() : 10; }
    public static float strikeReach(float partial) {
        return statue() && !state.held() && strikeTick >= 0
                ? org.example.maniacrevolution.scp173.Scp173Presentation.reach(Minecraft.getInstance().level.getGameTime() + partial - strikeTick) : 0;
    }
    public static void light() { if (statue()) ModNetworking.sendToServer(new Scp173InputPacket(Action.LIGHT)); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (player != mc.player || level != mc.level) reset();
        while (BLINK.consumeClick()) if (survivor() && state.blinkEnabled() && mc.screen == null)
            ModNetworking.sendToServer(new Scp173InputPacket(Action.BLINK));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
        var mc = Minecraft.getInstance();
        if (!statue() || mc.screen != null || e.isCanceled() || !e.isAttack()
                || mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        e.setCanceled(true); e.setSwingHand(false);
        if (!state.held() && state.melee() <= age()) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
            ModNetworking.sendToServer(new Scp173InputPacket(Action.MELEE));
        }
    }
    @SubscribeEvent
    public static void crosshair(RenderGuiOverlayEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (statue() && !mc.options.hideGui && mc.options.getCameraType().isFirstPerson()
                && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gui(RenderGuiEvent.Post e) {
        if (!valid()) return;
        var mc = Minecraft.getInstance(); var g = e.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        // Eye closure also covers third person; the status panel is rendered above it.
        Scp173BlinkOverlay.mask(g, width, height, closure(e.getPartialTick()));
        if (!mc.options.hideGui && mc.screen == null) {
            if (survivor() && state.blinkEnabled()) {
                Scp173BlinkOverlay.panel(g, width, state.meter(), state.pressure(), BLINK.getTranslatedKeyMessage());
            }
            if (statue()) {
                int x = width / 2, y = height / 2;
                if (mc.options.getCameraType().isFirstPerson()) {
                    boolean target = Scp173CombatGeometry.target(mc.player) != null;
                    for (var pixel : org.example.maniacrevolution.scp173.Scp173Presentation.reticle(target, state.held(), meleeCooldown() > 0))
                        g.fill(x + pixel.x(), y + pixel.y(), x + pixel.x() + 1, y + pixel.y() + 1, pixel.color());
                    if (state.held()) {
                        g.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.maniacrev.scp173.held"), x, y + 30, 0xFF7474);
                    }
                    if (meleeCooldown() > 0)
                        g.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable("hud.maniacrev.scp173.melee", (meleeCooldown() + 19) / 20), x, y + 18, 0xB6B0A1);
                    if (state.impact() > age()) {
                        for (int dx : new int[]{-1, 1}) for (int dy : new int[]{-1, 1})
                            g.fill(x + dx * 9, y + dy * 9, x + dx * 9 + 1, y + dy * 9 + 1, 0xFFFFFFFF);
                    }
                }
            }
        }
    }
    private static float closure(float partial) {
        return survivor() ? org.example.maniacrevolution.scp173.Scp173Presentation.blinkClosure(state.blink() - age() - partial) : 0;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hiddenGuiBlink(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !mc.options.hideGui
                || mc.screen != null || !survivor() || state.blink() <= age()) return;
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader(); var color = RenderSystem.getShaderColor().clone();
        int texture = RenderSystem.getShaderTexture(0);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
                blend = GL11.glIsEnabled(GL11.GL_BLEND), mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int function = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        var view = RenderSystem.getModelViewStack(); view.pushPose();
        try {
            view.setIdentity(); RenderSystem.applyModelViewMatrix();
            int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            Scp173BlinkOverlay.mask(graphics, width, height, closure(event.getPartialTick()));
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.setShaderTexture(0, texture); RenderSystem.depthMask(mask); RenderSystem.depthFunc(function);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fog(ViewportEvent.RenderFog event) {
        if (!survivor() || !state.blinkEnabled() && state.blackout() <= age()) return;
        var p = Minecraft.getInstance().player;
        double range = 20;
        if (state.blackout() > age()) range = 1;
        else if (p.hasEffect(MobEffects.BLINDNESS)) range = 3;
        else if (p.hasEffect(MobEffects.DARKNESS)) {
            var effect = p.getEffect(MobEffects.DARKNESS);
            double factor = effect.getFactorData().map(f -> (double) f.getFactor(p, (float) event.getPartialTick())).orElse(0.0);
            range = Scp173Rules.darknessRange(factor, p.level().getGameTime() + event.getPartialTick());
        } else return;
        event.setNearPlaneDistance((float) Math.min(event.getNearPlaneDistance(), range * 0.2));
        event.setFarPlaneDistance((float) Math.min(event.getFarPlaneDistance(), range));
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE); event.setCanceled(true);
    }
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (survivor() && state.blackout() > age()) { event.setRed(0); event.setGreen(0); event.setBlue(0); }
    }
    private static void reset() { state = null; player = level = null; strikeTick = -1; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() {}
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent e) { e.register(BLINK); }
    }
}
