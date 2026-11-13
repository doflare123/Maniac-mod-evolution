package org.example.maniacrevolution.lich.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.lich.*;
import org.example.maniacrevolution.network.ModNetworking;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class LichClient {
    public static CompoundTag army = new CompoundTag();
    private static long received;
    private static long combatUntil;
    private static long noManaUntil;
    private static Object combatLevel,combatPlayer;
    private static void clearCombat() {combatUntil=noManaUntil=0;combatLevel=combatPlayer=null;}
    public static boolean noManaWarning() {
        var mc=Minecraft.getInstance();
        return mc.level!=null&&combatLevel==mc.level&&combatPlayer==mc.player&&mc.level.getGameTime()<noManaUntil;
    }
    public static int combatCooldown() {
        var mc=Minecraft.getInstance();
        return mc.level==null||combatLevel!=mc.level||combatPlayer!=mc.player?0:(int)Math.max(0,combatUntil-mc.level.getGameTime());
    }
    public static boolean active() {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && !mc.player.isCreative() && !mc.player.isSpectator()
                && army.contains("hp") && mc.level.getGameTime()-received < 40;
    }
    public static void request(String action, CompoundTag data) { ModNetworking.sendToServer(new LichRequest(action, data)); }
    public static void request(String action) { request(action, new CompoundTag()); }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void deselect(InputEvent.InteractionKeyMappingTriggered event) {
        var mc=Minecraft.getInstance();
        if(event.isCanceled()||!event.isUseItem()||!active()||mc.screen!=null
                ||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!mc.player.isShiftKeyDown()
                ||!mc.player.getMainHandItem().is(org.example.maniacrevolution.ModItems.LICH_BOOK.get()))return;
        // Consume before block/entity interaction, including adventure's useOn permission check.
        event.setCanceled(true);event.setSwingHand(false);request("none");
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var mc=Minecraft.getInstance();
        if(event.isCanceled()||!event.isAttack()||!active()||mc.screen!=null)return;
        var phylactery=LichReticle.aimedPhylactery();
        if(phylactery!=null) {
            event.setCanceled(true);event.setSwingHand(false);
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(mc.player.getYRot(),mc.player.getXRot(),mc.player.onGround()));
            var data=new CompoundTag();data.putUUID("phylactery",phylactery.getUUID());request("repair",data);return;
        }
        boolean book=mc.player.getMainHandItem().is(org.example.maniacrevolution.ModItems.LICH_BOOK.get());
        // Like Warden, preserve ordinary block mining without the book; consume every melee/miss click.
        if(!book&&mc.hitResult!=null&&mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)return;
        event.setCanceled(true);event.setSwingHand(false);
        if(combatCooldown()>0)return;
        combatLevel=mc.level;combatPlayer=mc.player;combatUntil=mc.level.getGameTime()+LichCombat.COOLDOWN;
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(mc.player.getYRot(),mc.player.getXRot(),mc.player.onGround()));
        request(book?"shoot":"melee");
    }
    public static void accept(CompoundTag tag) {
        if (tag == null) return; var mc = Minecraft.getInstance();
        switch (tag.getString("mode")) {
            case "combat" -> {
                if(!active()||!tag.getString("dimension").equals(mc.level.dimension().location().toString()))return;
                combatLevel=mc.level;combatPlayer=mc.player;
                combatUntil=tag.getLong("tick")+Math.max(0,Math.min(LichCombat.COOLDOWN,tag.getInt("cooldown")));
                if(tag.getString("failure").equals("no_mana"))noManaUntil=mc.level.getGameTime()+30;
                else if(tag.getBoolean("swing"))noManaUntil=0;
                if(tag.getBoolean("swing")) {
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    LichWorldMarkers.swing();
                }
            }
            case "forms" -> LichPlayerForm.accept(tag);
            case "cast" -> LichReticle.feedback(tag);
            case "ui" -> LichAudio.ui(tag.getString("sound"));
            case "cue" -> LichWorldMarkers.accept(tag);
            case "points" -> CorpsePointMarkers.accept(tag);
            case "clear" -> { army = new CompoundTag(); clearCombat(); LichReticle.clear(); if (mc.screen instanceof LichScreen || mc.screen instanceof LichCorpseScreen || mc.screen instanceof LichArmyScreen) mc.setScreen(null); }
            case "army" -> {
                army = tag; if (mc.level != null) received = mc.level.getGameTime();
                if (tag.getBoolean("open")) mc.setScreen(tag.hasUUID("corpse") ? new LichCorpseScreen(tag.getUUID("corpse")) : new LichArmyScreen(tag));
                else if (mc.screen instanceof LichArmyScreen screen) screen.update(tag);
                else if (mc.screen instanceof LichScreen screen && screen.armyMode()) screen.update(tag);
            }
            default -> {
                if (tag.getBoolean("open")) mc.setScreen(new LichScreen(tag));
                else if (mc.screen instanceof LichScreen screen) screen.update(tag);
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { army = new CompoundTag(); clearCombat(); LichReticle.clear(); }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) {
        if (!active() || Minecraft.getInstance().options.hideGui) return;
        var mc = Minecraft.getInstance(); var g = e.getGuiGraphics();
        LichArmyOverlay.render(g,mc.font,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),army);
    }
}
