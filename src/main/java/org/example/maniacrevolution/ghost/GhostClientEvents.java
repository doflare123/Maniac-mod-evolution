package org.example.maniacrevolution.ghost;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.GhostActionPacket;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class GhostClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GhostPossessionClientState.tick(mc);

        if (!GhostPossessionClientState.isVictimControlled() || mc.player == null) {
            return;
        }

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GhostPossessionClientState.clear();
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!GhostPossessionClientState.isControllerActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isAttack()) {
            if (mc.hitResult instanceof EntityHitResult entityHit) {
                ModNetworking.sendToServer(GhostActionPacket.attackEntity(entityHit.getEntity().getId()));
            }
            return;
        }

        if (!event.isUseItem()) {
            return;
        }

        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            ModNetworking.sendToServer(GhostActionPacket.useEntity(
                    event.getHand(),
                    entity.getId(),
                    entityHit.getLocation().subtract(entity.position())
            ));
            return;
        }

        if (mc.hitResult instanceof BlockHitResult blockHit) {
            ModNetworking.sendToServer(GhostActionPacket.useBlock(
                    event.getHand(),
                    blockHit.getBlockPos(),
                    blockHit.getDirection(),
                    blockHit.getLocation()
            ));
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
            ModNetworking.sendToServer(GhostActionPacket.useItem(event.getHand()));
        }
    }
}
