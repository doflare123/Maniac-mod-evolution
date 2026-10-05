package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.*;
import org.example.maniacrevolution.warden.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenCombatClient {
    private static WardenCombatStatePacket state;
    private static Object level, player;
    private static int sequence, gesture;
    private static boolean captured, waitRelease;
    private static long strikeTick = -1;
    private static boolean strikeHit;
    private WardenCombatClient() {}
    public static boolean eligible() { return WardenSniffClient.eligible(); }
    private static boolean hands() { var p = Minecraft.getInstance().player; return p != null && p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty(); }
    private static void send(WardenCombatInputPacket.Action action, int token) { ModNetworking.sendToServer(new WardenCombatInputPacket(action, token)); }
    private static long age() { var l = Minecraft.getInstance().level; return state == null || l == null ? 0 : Math.max(0, l.getGameTime() - state.tick()); }
    public static int meleeCooldown() { return state == null ? 0 : (int) Math.max(0, state.melee() - age()); }
    public static int waveCooldown() { return state == null ? 0 : (int) Math.max(0, state.wave() - age()); }
    public static float charge(float partial) { return state == null || !state.charging() || !captured ? 0 : Math.min(1, (state.charge() + age() + partial) / WardenCombatRules.MAX_CHARGE); }
    public static float strike(float partial) {
        var l = Minecraft.getInstance().level;
        return strikeTick < 0 || l == null ? 0 : (float) Math.max(0, 1 - Math.max(0, l.getGameTime() + partial - strikeTick) / 8);
    }
    public static boolean strikeHit() { return strikeHit; }
    public static void acceptFeedback(WardenMeleeFeedbackPacket packet) {
        var mc = Minecraft.getInstance();
        if (!eligible() || mc.level == null || !packet.valid() || !packet.dimension().equals(mc.level.dimension().location())
                || packet.tick() > mc.level.getGameTime() + 20 || mc.level.getGameTime() - packet.tick() >= 8) return;
        if (level != mc.level || player != mc.player) { reset(); level = mc.level; player = mc.player; }
        if (packet.tick() >= strikeTick) { strikeTick = packet.tick(); strikeHit = packet.hit(); }
    }
    public static void accept(WardenCombatStatePacket packet) {
        var mc = Minecraft.getInstance();
        if (!eligible() || mc.level == null || !packet.valid() || !packet.dimension().equals(mc.level.dimension().location())
                || packet.tick() > mc.level.getGameTime() + 20 || mc.level.getGameTime() - packet.tick() > WardenCombatRules.WAVE_CD) return;
        if (level != mc.level || player != mc.player) { reset(); level = mc.level; player = mc.player; }
        if (state != null && packet.tick() < state.tick()) return;
        state = packet;
        if (captured && packet.token() == gesture && !packet.charging()) captured = false;
    }
    private static boolean interaction() {
        var mc = Minecraft.getInstance();
        if (mc.hitResult instanceof EntityHitResult hit) return WardenInteractionPolicy.interactive(hit.getEntity());
        return mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && WardenInteractionPolicy.interactive(mc.level.getBlockState(hit.getBlockPos()), mc.level, hit.getBlockPos());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void input(InputEvent.InteractionKeyMappingTriggered e) {
        var mc = Minecraft.getInstance();
        if (!eligible() || mc.screen != null || e.isCanceled()) return;
        // Keep vanilla block mining, but every entity/miss attack belongs to the fixed melee cycle.
        if (e.isAttack() && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK)) {
            e.setCanceled(true); e.setSwingHand(false);
            if (meleeCooldown() == 0 && !captured) {
                // Input precedes LocalPlayer's normal movement tick. Deliver the current aim before
                // the attack so a quick turn doesn't raycast using the previous tick's rotation.
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                        mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
                send(WardenCombatInputPacket.Action.MELEE, 0);
            }
        }
        if (!e.isUseItem()) return;
        if (captured) { e.setCanceled(true); e.setSwingHand(false); return; }
        if (waitRelease) return;
        if (!hands() || interaction()) { waitRelease = true; return; }
        // An offhand event must never create a second gesture.
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        e.setCanceled(true); e.setSwingHand(false); waitRelease = true;
        if (waveCooldown() > 0) return;
        if (level != mc.level || player != mc.player) { state = null; level = mc.level; player = mc.player; }
        gesture = sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        captured = true; send(WardenCombatInputPacket.Action.START, gesture);
    }
    private static void reset() { state = null; strikeTick = -1; strikeHit = false; captured = waitRelease = false; level = player = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { reset(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player || !eligible()) {
            if (captured && mc.player != null) send(WardenCombatInputPacket.Action.CANCEL, gesture);
            reset(); return;
        }
        if (mc.isPaused()) return;
        if (captured && (mc.screen != null || !hands())) { send(WardenCombatInputPacket.Action.CANCEL, gesture); captured = false; }
        if (captured && mc.options.keyUse.isDown() && mc.level.getGameTime() % 20 == 0) send(WardenCombatInputPacket.Action.KEEP_ALIVE, gesture);
        if (!mc.options.keyUse.isDown()) {
            if (captured) { send(WardenCombatInputPacket.Action.RELEASE, gesture); captured = false; }
            waitRelease = false;
        }
    }
}
