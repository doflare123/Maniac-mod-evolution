package org.example.maniacrevolution.engineer.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.data.*;
import org.example.maniacrevolution.engineer.*;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.PlayerModeUtil;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class EngineerClient {
    public static EngineerStatePacket state;
    private static Object connection;
    private static long received;
    private static boolean awaitingSelection;
    private static int heldType = -1;
    private static BlockPos heldPos;
    private static long lastHold;
    private static boolean wasUseDown;
    private EngineerClient() {}
    public static boolean eligible() {
        var p = Minecraft.getInstance().player;
        return p != null && p.isAlive() && !p.isSpectator() && (state != null && state.active()
                || p.getInventory().items.stream().anyMatch(s -> !s.isEmpty() && s.getItem() instanceof EngineerItem)
                || !p.getOffhandItem().isEmpty() && p.getOffhandItem().getItem() instanceof EngineerItem
                || EngineerRules.phase(ClientGameState.getPhase()) && ClientPlayerData.isSurvivorClass(EngineerRules.CLASS_ID)
                && p.getTeam() != null && "survivors".equals(p.getTeam().getName()));
    }
    public static double time() {
        var mc = Minecraft.getInstance(); return state == null || mc.level == null ? 0 : state.tick() + mc.level.getGameTime() - received + mc.getFrameTime();
    }
    public static float armor() { return state == null || !state.active() ? 0 : EngineerRules.clampArmor(state.armor()); }
    public static String armorText(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
    public static void accept(EngineerStatePacket packet) {
        ensureConnection(); state = packet;
        var level = Minecraft.getInstance().level; received = level == null ? 0 : level.getGameTime();
        if (awaitingSelection && (packet.selected() >= 0 || packet.work() >= 0)) {
            awaitingSelection = false;
            if (Minecraft.getInstance().screen instanceof EngineerScreen) Minecraft.getInstance().setScreen(null);
        }
    }
    public static void request(EngineerActionPacket.Action action, BlockPos pos, int value) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (action == EngineerActionPacket.Action.PLACE || action == EngineerActionPacket.Action.BREAK || action == EngineerActionPacket.Action.COLLECT || action == EngineerActionPacket.Action.USE_HELD)
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        ModNetworking.sendToServer(new EngineerActionPacket(action, pos == null ? BlockPos.ZERO : pos, value));
    }
    public static void select(int type) { awaitingSelection = true; request(EngineerActionPacket.Action.SELECT, BlockPos.ZERO, type); }
    public static void useHeld(EngineerItem.Kind kind, net.minecraft.world.InteractionHand hand) {
        var mc = Minecraft.getInstance(); if (!gameplay()) return;
        if (kind == EngineerItem.Kind.TABLET || kind == EngineerItem.Kind.REMOTE) { open(kind == EngineerItem.Kind.REMOTE); return; }
        if (kind == EngineerItem.Kind.SCRAP && !mc.player.isShiftKeyDown()) {
            if (mc.player.isUsingItem()) return;
            mc.player.startUsingItem(hand);
        }
        request(EngineerActionPacket.Action.USE_HELD, EngineerManager.placement(mc.player), hand.ordinal());
    }
    public static void open(boolean remote) {
        var mc = Minecraft.getInstance();
        if (!eligible() || mc.screen != null || state != null && (state.work() >= 0 || state.selected() >= 0)
                || org.example.maniacrevolution.client.QTEClientHandler.isQTEActive()) return;
        if (remote && !mc.player.getMainHandItem().is(ModItems.ENGINEER_REMOTE.get()) && !mc.player.getOffhandItem().is(ModItems.ENGINEER_REMOTE.get())) return;
        awaitingSelection = false;
        request(remote ? EngineerActionPacket.Action.OPEN_REMOTE : EngineerActionPacket.Action.OPEN_WORKSHOP, BlockPos.ZERO, 0);
        mc.setScreen(new EngineerScreen(remote));
    }
    public static void ensureConnection() {
        var current = Minecraft.getInstance().getConnection();
        if (current != connection) { state = null; heldType = -1; heldPos = null; awaitingSelection = false; wasUseDown = false; connection = current; }
    }
    private static boolean gameplay() {
        var mc = Minecraft.getInstance(); return mc.player != null && mc.level != null && mc.player.isAlive()
                && !mc.player.isSpectator();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ensureConnection(); var mc = Minecraft.getInstance();
        wasUseDown = mc.options.keyUse.isDown();
        if (mc.screen instanceof EngineerScreen screen && (!eligible() || org.example.maniacrevolution.client.QTEClientHandler.isQTEActive()
                || screen.remote && !mc.player.getMainHandItem().is(ModItems.ENGINEER_REMOTE.get()) && !mc.player.getOffhandItem().is(ModItems.ENGINEER_REMOTE.get()))) mc.setScreen(null);
        if (!gameplay() || mc.screen != null) { stopHolding(); return; }
        if (state != null && (state.work() == EngineerManager.BUILD || state.work() == EngineerManager.ARM_PLATE)) return;
        if (state != null && state.selected() >= 0) return;
        BlockPos pos = EngineerManager.target(mc.player);
        String team = mc.player.getTeam() == null ? "" : mc.player.getTeam().getName();
        boolean outsideMatch = !EngineerRules.phase(ClientGameState.getPhase());
        int action = pos != null && !mc.player.isCreative() && mc.options.keyAttack.isDown() && (outsideMatch || team.equals("maniac")) ? EngineerManager.BREAK
                : pos != null && mc.options.keyUse.isDown() && mc.player.isShiftKeyDown() && (outsideMatch || team.equals("survivors") || eligible()) ? EngineerManager.COLLECT : -1;
        if (action < 0) { stopHolding(); return; }
        if (heldType != action || !pos.equals(heldPos)) {
            stopHolding(); heldType = action; heldPos = pos; lastHold = mc.level.getGameTime();
            request(action == EngineerManager.BREAK ? EngineerActionPacket.Action.BREAK : EngineerActionPacket.Action.COLLECT, pos, action);
        } else if (mc.level.getGameTime() - lastHold >= 4) {
            lastHold = mc.level.getGameTime(); request(EngineerActionPacket.Action.KEEP_WORKING, pos, action);
        }
    }
    private static void stopHolding() {
        if (heldType >= 0) request(EngineerActionPacket.Action.CANCEL, heldPos, 0);
        heldType = -1; heldPos = null;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void input(InputEvent.InteractionKeyMappingTriggered e) {
        if (!gameplay()) return;
        var mc = Minecraft.getInstance();
        if (state != null && (state.selected() >= 0 || state.work() == EngineerManager.BUILD || state.work() == EngineerManager.ARM_PLATE)) {
            e.setCanceled(true); e.setSwingHand(false);
            if (e.isUseItem() && !wasUseDown) request(EngineerActionPacket.Action.CANCEL, BlockPos.ZERO, 0);
            else if (e.isAttack() && state.work() < 0) request(EngineerActionPacket.Action.PLACE, EngineerManager.placement(mc.player), 0);
            return;
        }
        if (e.isUseItem() && mc.player.getItemInHand(e.getHand()).getItem() instanceof EngineerItem item) {
            e.setCanceled(true); e.setSwingHand(false);
            if (!wasUseDown || item.kind == EngineerItem.Kind.SCRAP && !mc.player.isUsingItem()) useHeld(item.kind, e.getHand());
            return;
        }
        if (EngineerManager.target(mc.player) != null && !(e.isAttack() && mc.player.isCreative())) { e.setCanceled(true); e.setSwingHand(e.isAttack()); }
    }
    @SubscribeEvent public static void key(InputEvent.Key e) {
        if (e.getKey() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && e.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && state != null && (state.selected() >= 0 || state.work() >= 0)) request(EngineerActionPacket.Action.CANCEL, BlockPos.ZERO, 0);
    }
    public static void resourceHud(GuiGraphics g, int right, int bottom) {
        if (!eligible() || state == null || !state.active()) return;
        var mc = Minecraft.getInstance();
        String text = state.scrap() + "/" + EngineerRules.MAX_SCRAP;
        int width = 23 + mc.font.width(text), x = Math.max(4, right - width), y = bottom - 22;
        g.fill(x, y, x + width, y + 20, 0xD0181B20); g.renderOutline(x, y, width, 20, armor() > 0 ? 0xFFA0A8B0 : 0xFFC99C54);
        g.renderItem(new net.minecraft.world.item.ItemStack(ModItems.ENGINEER_SCRAP.get()), x + 2, y + 2);
        g.drawString(mc.font, text, x + 21, y + 6, 0xFFFFD38B, false);
        if (armor() > 0) g.fill(x + 1, y + 18, x + width - 1, y + 19, 0xFFA0A8B0);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance(); if (!gameplay() || mc.options.hideGui) return;
        int cx = mc.getWindow().getGuiScaledWidth() / 2, cy = mc.getWindow().getGuiScaledHeight() / 2;
        var g = event.getGuiGraphics();
        if (mc.player.isCreative() || !org.example.maniacrevolution.config.HudConfig.isCustomHudEnabled()) resourceHud(g, mc.getWindow().getGuiScaledWidth() - 4, mc.getWindow().getGuiScaledHeight() - 40);
        if (state != null && state.work() >= 0) {
            int duration = state.work() == EngineerManager.BREAK ? EngineerRules.BREAK_TICKS : state.work() == EngineerManager.COLLECT ? EngineerRules.COLLECT_TICKS
                    : state.work() == EngineerManager.ARM_PLATE ? EngineerRules.PLATE_TICKS : EngineerRules.INSTALL_TICKS;
            float progress = (float) Math.max(0, Math.min(1, (time() - state.started()) / duration));
            g.fill(cx - 60, cy + 31, cx + 60, cy + 36, 0xD0181B20);
            g.fill(cx - 59, cy + 32, cx - 59 + Math.round(118 * progress), cy + 35, 0xFFFFC857);
            g.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.engineer.work." + state.work()), cx, cy + 42, 0xFFFFD38B);
        } else if (eligible() && state != null && state.selected() >= 0) {
            g.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.engineer.place"), cx, cy + 35, 0xFFFFD38B);
        } else if (mc.screen == null && EngineerManager.target(mc.player) != null) {
            String team = mc.player.getTeam() == null ? "" : mc.player.getTeam().getName();
            g.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.engineer." + (team.equals("maniac") || !EngineerRules.phase(ClientGameState.getPhase()) ? "break" : "collect")), cx, cy + 35, 0xFFCFD8DE);
        }
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !eligible() || state == null || state.selected() < 0) return;
        var mc = Minecraft.getInstance(); BlockPos pos = state.work() == EngineerManager.BUILD ? state.workPos() : EngineerManager.placement(mc.player);
        if (pos == null) return;
        boolean valid = EngineerManager.validPlacement(mc.level, pos, state.selected(), mc.player.getDirection());
        Vec3 camera = event.getCamera().getPosition(); var pose = event.getPoseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        var buffer = mc.renderBuffers().bufferSource();
        AABB bounds = state.selected() == 0 ? new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1) : new AABB(pos).deflate(.15);
        LevelRenderer.renderLineBox(pose, buffer.getBuffer(RenderType.lines()), bounds, valid ? .35f : 1f, valid ? 1f : .2f, .25f, .85f);
        pose.popPose(); buffer.endBatch(RenderType.lines());
    }
}
