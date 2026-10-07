package org.example.maniacrevolution.strange.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cloak.CloakManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.strange.*;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class StrangeClient {
    private static final Map<AbstractClientPlayer, Pose> POSES = new IdentityHashMap<>();
    private static final class Pose {
        final ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        int kind = -1;
        int effectId = -1;
        Pose(AbstractClientPlayer player) { PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(2600, layer); }
    }
    public static StrangeEffectEntity effect(AbstractClientPlayer player) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (var entity : mc.level.entitiesForRendering())
            if (entity instanceof StrangeEffectEntity effect && effect.ownerId() == player.getId()
                    && effect.kind() != StrangeEffectEntity.PORTAL) return effect;
        return null;
    }
    @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !CloakManager.equipped(mc.player)
                || mc.player.isCreative() || mc.player.isSpectator()) return;
        var effect = effect(mc.player);
        if (effect!=null && effect.casting()) {
            if(event.isAttack() || event.isUseItem()) { event.setCanceled(true); event.setSwingHand(false); }
            return;
        }
        if (mc.player.getMainHandItem().is(org.example.maniacrevolution.ModItems.SPACE_AMULET.get())
                || mc.player.getOffhandItem().is(org.example.maniacrevolution.ModItems.SPACE_AMULET.get())) return;
        if (event.isUseItem()) {
            // Only an empty-hand cast aimed at a maniac consumes RMB. Blocks and held items keep vanilla use.
            if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                    || !mc.player.getMainHandItem().isEmpty() || !mc.player.getOffhandItem().isEmpty()
                    || mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    || !aimingAtManiac()) return;
            ModNetworking.sendToServer(new StrangeActionPacket(StrangeActionPacket.Action.CAPTURE));
            event.setCanceled(true); event.setSwingHand(false);
        } else if (event.isAttack() && effect != null && effect.kind() != StrangeEffectEntity.DEFENSE) {
            if (!mc.player.getMainHandItem().isEmpty() || !mc.player.getOffhandItem().isEmpty()
                    || mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return;
            ModNetworking.sendToServer(new StrangeActionPacket(StrangeActionPacket.Action.WHIP));
            event.setCanceled(true); event.setSwingHand(false);
        }
    }
    private static boolean aimingAtManiac() {
        var mc = Minecraft.getInstance();
        var start = mc.player.getEyePosition();
        var end = start.add(mc.player.getLookAngle().scale(CloakManager.THROW_RANGE));
        for (var target : mc.level.players()) {
            if (target == mc.player || target.getTeam() == null || !"maniac".equals(target.getTeam().getName())) continue;
            var hit = target.getBoundingBox().inflate(.35).clip(start, end);
            if (hit.isPresent() && mc.player.hasLineOfSight(target)) return true;
        }
        return false;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PortalVisual.cleanup();
        var mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        Set<AbstractClientPlayer> active = Collections.newSetFromMap(new IdentityHashMap<>());
        if (mc.level != null) for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof StrangeEffectEntity effect) || effect.kind() == StrangeEffectEntity.PORTAL
                    || effect.kind() == StrangeEffectEntity.COMBAT) continue;
            if (!(mc.level.getEntity(effect.ownerId()) instanceof AbstractClientPlayer player)) continue;
            if (effect.kind() == StrangeEffectEntity.WHIP || effect.casting()) {
                player.yBodyRot = effect.getYRot(); player.yBodyRotO = effect.getYRot();
            }
            active.add(player);
            Pose pose = POSES.computeIfAbsent(player, Pose::new);
            if (pose.kind != effect.kind() || pose.effectId != effect.getId()) {
                pose.kind = effect.kind(); pose.effectId = effect.getId();
                String name = effect.casting() ? (effect.kind()==StrangeEffectEntity.PORTAL_OPEN?"portal_open":"portal_close")
                        +(effect.drawingLeft()?"_left":"") : effect.kind() == StrangeEffectEntity.WHIP ? "whip" : "defense";
                var animation = PlayerAnimationRegistry.getAnimation(Maniacrev.loc("animation.strange_player." + name));
                if (animation != null) pose.layer.setAnimation(new KeyframeAnimationPlayer(animation, (int)effect.age()));
            }
        }
        var iterator = POSES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!active.contains(entry.getKey())) {
                entry.getValue().layer.setAnimation(null); entry.getValue().kind = -1;
                if (mc.level == null || !mc.level.players().contains(entry.getKey())) iterator.remove();
            }
        }
    }
}
