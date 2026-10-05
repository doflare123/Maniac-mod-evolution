package org.example.maniacrevolution.scp173.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.packets.Scp173HoldPacket;
import org.example.maniacrevolution.util.PlayerModeUtil;

/** Input/camera feedback; the server gate independently enforces the same restriction. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class Scp173Client {
    private static Scp173HoldPacket state;
    private static Object player, level;
    private Scp173Client() {}

    public static void accept(Scp173HoldPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.getUUID().equals(packet.player())
                || !mc.level.dimension().location().equals(packet.dimension())) return;
        if (!packet.held()) { reset(); return; }
        state = packet; player = mc.player; level = mc.level;
        mc.player.setPos(packet.position());
        mc.player.setDeltaMovement(packet.velocity());
        rotation();
    }

    private static boolean held() {
        var mc = Minecraft.getInstance();
        if (state == null) return false;
        if (player != mc.player || level != mc.level || !PlayerModeUtil.isSurvivalOrAdventure(mc.player)
                || !mc.player.isAlive() || ClientPlayerData.getManiacClassId() != 13
                || mc.player.getTeam() == null || !"maniac".equals(mc.player.getTeam().getName())) {
            reset(); return false;
        }
        return true;
    }

    private static void rotation() {
        if (!held()) return;
        var p = Minecraft.getInstance().player;
        p.setYRot(state.yaw()); p.setXRot(state.pitch());
        p.setYHeadRot(state.yaw()); p.setYBodyRot(state.yaw());
        p.yRotO = state.yaw(); p.xRotO = state.pitch();
        p.yHeadRotO = state.yaw(); p.yBodyRotO = state.yaw();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent e) {
        if (!held()) return;
        var input = e.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
        rotation();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
        var hit = Minecraft.getInstance().hitResult;
        if (held() && e.isAttack() && (hit == null || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK)) {
            e.setCanceled(true); e.setSwingHand(false);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) { rotation(); }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e) { rotation(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { reset(); }
    private static void reset() { state = null; player = level = null; }
}
