package org.example.maniacrevolution.techies;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.techies.client.*;
import org.joml.Matrix4f;

/** Opt-in offscreen Minecraft-engine capture; never shipped or active in normal clients. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class TechiesClientRenderProbe {
    private static boolean done;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("maniacrev.techies.renderProbe") || done || e.phase!=TickEvent.Phase.END
                || mc.level==null || mc.player==null || mc.player.tickCount<20) return;
        done=true; var target=new TextureTarget(900,600,true,Minecraft.ON_OSX);
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack(); view.pushPose();
        try {
            target.setClearColor(.04f,.06f,.09f,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,900,600,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            view.setIdentity(); view.translate(0,0,-10000); RenderSystem.applyModelViewMatrix();
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            var player=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"TechiesProbe"));
            player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.TECHIES_HELMET.get()));
            player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.TECHIES_REMOTE.get()));
            InventoryScreen.renderEntityInInventoryFollowsAngle(g,145,480,175,.1f,.03f,player);
            g.drawCenteredString(mc.font,"Vanilla player + decorative helmet",150,520,0xFFFFFF);
            for(MineType type:MineType.values()) {
                var v=new TechiesVisual("techies_mine_"+type.color); float x=370+type.ordinal()*185;
                draw(g,v,x,260,false,false);
                g.drawCenteredString(mc.font,type.color,(int)x,315,0xFFFFFF);
                g.pose().pushPose(); g.pose().translate(x-16,390,100); g.pose().scale(2,2,2); g.renderItem(new ItemStack(type.item()),0,0); g.pose().popPose();
            }
            g.flush(); save(target,"techies-model-probe.png");
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            var screen=new TechiesRadialScreen(); screen.init(mc,900,600); screen.render(g,525,275,0); g.flush(); save(target,"techies-radial-probe.png");
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            for(MineType type:MineType.values()) for(int phase=0;phase<3;phase++) {
                var v=new TechiesVisual("techies_mine_"+type.color); v.clip="activate"; v.seconds=phase*type.activation/40.0;
                draw(g,v,180+type.ordinal()*280,150+phase*155,false,false);
            }
            g.flush(); save(target,"techies-activation-probe.png");
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            for(MineType type:MineType.values()) for(int allowed=0;allowed<2;allowed++) {
                var v=new TechiesVisual("techies_mine_"+type.color); v.red=allowed==1?.1f:1; v.green=allowed==1?1:.1f; v.blue=.1f; v.alpha=.55f;
                draw(g,v,180+type.ordinal()*280,220+allowed*260,true,false);
            }
            g.flush(); save(target,"techies-projection-probe.png");
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            for(MineType type:MineType.values()) {
                var technician=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"HandsProbe"));
                technician.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.TECHIES_HELMET.get()));
                technician.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
                TechiesHands.accept(new TechiesPosePacket(technician.getUUID(),mc.level.getGameTime(),type.ordinal(),mc.level.getGameTime()-type.install/2,-100));
                InventoryScreen.renderEntityInInventoryFollowsAngle(g,160+type.ordinal()*290,500,175,0,0,technician);
            }
            g.flush(); save(target,"techies-hands-third-person-probe.png");
            // Exercise the actual RenderHandEvent path without changing the server match or inventory.
            var scoreboard=mc.level.getScoreboard(); var team=scoreboard.getPlayerTeam("survivors"); if(team==null) team=scoreboard.addPlayerTeam("survivors");
            scoreboard.addPlayerToTeam(mc.player.getScoreboardName(),team);
            org.example.maniacrevolution.data.ClientPlayerData.setSurvivorClass(14);
            org.example.maniacrevolution.data.ClientGameState.update(3,0,0,false);
            TechiesClient.clear();
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            var abilitySlot=org.example.maniacrevolution.hud.CustomHud.class.getDeclaredMethod("renderAbilitySlot",GuiGraphics.class,int.class,int.class,
                    org.example.maniacrevolution.item.IItemWithAbility.class,net.minecraft.world.entity.player.Player.class);
            abilitySlot.setAccessible(true);
            g.pose().pushPose(); g.pose().translate(70,60,0); g.pose().scale(3,3,1);
            abilitySlot.invoke(org.example.maniacrevolution.hud.CustomHud.INSTANCE,g,0,0,TechiesAbilityHud.INSTANCE,mc.player); g.pose().popPose();
            for(int phase=0;phase<3;phase++) TechiesHud.installation(g,310+phase*180,105,MineType.values()[phase],phase*.5f);
            var status=org.example.maniacrevolution.hud.CustomHud.class.getDeclaredMethod("renderMineStatus",GuiGraphics.class,MineType.class,int.class,int.class);
            status.setAccessible(true);
            long readyClock=(long)Math.ceil(TechiesClient.time());
            for(MineType type:MineType.values()) {
                TechiesClient.cooldowns[type.ordinal()]=0;
                if(!TechiesHud.stateKey(type).endsWith(".ready")) throw new AssertionError("Zero cooldown must select ready state");
                g.pose().pushPose(); g.pose().translate(220+type.ordinal()*160,220,0); g.pose().scale(3,3,1);
                status.invoke(org.example.maniacrevolution.hud.CustomHud.INSTANCE,g,type,0,0); g.pose().popPose();
                TechiesClient.cooldowns[type.ordinal()]=readyClock+type.cooldown/2;
                if(!TechiesHud.stateKey(type).endsWith(".recharging") || TechiesHud.remaining(type)<=0) throw new AssertionError("Positive cooldown must select recharging state");
                g.pose().pushPose(); g.pose().translate(220+type.ordinal()*160,310,0); g.pose().scale(3,3,1);
                status.invoke(org.example.maniacrevolution.hud.CustomHud.INSTANCE,g,type,0,0); g.pose().popPose();
            }
            var grid=org.example.maniacrevolution.hud.CustomHud.class.getDeclaredMethod("renderTimedEffects",GuiGraphics.class,float.class,int.class,int.class,net.minecraft.world.entity.player.Player.class);
            grid.setAccessible(true);
            int section=(int)grid.invoke(org.example.maniacrevolution.hud.CustomHud.INSTANCE,g,0f,900,470,mc.player);
            if(section!=org.example.maniacrevolution.hud.CustomHud.getTimedEffectSectionHeight(mc.player))
                throw new AssertionError("Readiness cards must participate in the same effect-grid height calculation");
            if(mc.getResourceManager().getResource(TechiesAbilityHud.INSTANCE.getAbilityIcon()).isEmpty()) throw new AssertionError("Drawn ability icon missing");
            g.flush(); save(target,"techies-hud-states-probe.png"); TechiesClient.clear();
            Maniacrev.LOGGER.info("TECHIES_HUD_STATES_PROBE_PASS: six readiness states, shared effect grid, drawn ability icon, segmented crosshair installation");
            mc.getSingleplayerServer().submit(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); p.setInvulnerable(false);
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.server.level.ServerPlayer.class,p,0,"f_8921_");
                var sb=p.getScoreboard(); var phase=sb.getObjective("phaseGame");
                sb.getOrCreatePlayerScore("game",phase).setScore(1);
                var maniacs=sb.getPlayerTeam("maniac"); if(maniacs==null) maniacs=sb.addPlayerTeam("maniac");
                sb.addPlayerToTeam(p.getScoreboardName(),maniacs);
                var support=p.blockPosition().below(); p.serverLevel().setBlockAndUpdate(support,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                p.setPos(support.getX()+.5,support.getY()+1,support.getZ()+.5); p.setOnGround(true);
                for(var slot:EquipmentSlot.values()) if(slot.getType()==EquipmentSlot.Type.ARMOR) p.setItemSlot(slot,ItemStack.EMPTY);
                p.setHealth(20); p.setAbsorptionAmount(0); p.invulnerableTime=0; p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                TechiesManager.explode(new TechiesManager.Mine(88001,p,support,MineType.RED),p.server,java.util.List.of(p));
                if(p.getHealth()!=17 || p.getDeltaMovement().horizontalDistanceSqr()==0)
                    throw new AssertionError("Real ServerPlayer in maniac phase one must take 3 HP and receive red impulse");
                p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                TechiesManager.explode(new TechiesManager.Mine(88002,p,support,MineType.BLUE),p.server,java.util.List.of(p));
                if(p.getHealth()!=17 || p.getDeltaMovement().horizontalDistanceSqr()==0)
                    throw new AssertionError("Real ServerPlayer must receive blue impulse without damage");
                p.setInvulnerable(true); p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                TechiesManager.explode(new TechiesManager.Mine(88003,p,support,MineType.RED),p.server,java.util.List.of(p));
                if(p.getHealth()!=17 || p.getDeltaMovement().lengthSqr()!=0)
                    throw new AssertionError("Actual ServerPlayer invulnerability must remain effective");
                p.setInvulnerable(false);
                Maniacrev.LOGGER.info("TECHIES_REAL_PLAYER_PROBE_PASS: phase-one red 3 HP and red/blue impulses; actual invulnerability respected");
                return null;
            }).get(10,java.util.concurrent.TimeUnit.SECONDS);
            scoreboard.addPlayerToTeam(mc.player.getScoreboardName(),team);
            TechiesClient.clear();
            mc.setScreen(null); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true);
            TechiesClient.openMenu();
            mc.player.input.tick(false,1);
            if(!TechiesClient.menuOpen() || mc.screen!=null || !mc.options.keyUp.isDown()
                    || !mc.options.keySprint.isDown() || mc.player.input.forwardImpulse<=0)
                throw new AssertionError("Radial overlay must preserve movement/sprint without installing a Screen");
            var outsideLeft=new net.minecraftforge.client.event.InputEvent.MouseButton.Pre(0,1,0);
            TechiesClient.mouse(outsideLeft);
            if(TechiesClient.menuOpen() || !outsideLeft.isCanceled() || mc.options.keyAttack.isDown())
                throw new AssertionError("Left click in center/outside must close without attacking or placing");
            TechiesClient.mouse(new net.minecraftforge.client.event.InputEvent.MouseButton.Pre(0,0,0));
            TechiesClient.openMenu();
            var right=new net.minecraftforge.client.event.InputEvent.MouseButton.Pre(1,1,0); TechiesClient.mouse(right);
            if(TechiesClient.menuOpen() || !right.isCanceled() || mc.options.keyUse.isDown())
                throw new AssertionError("Right click anywhere must close without using held item");
            TechiesClient.mouse(new net.minecraftforge.client.event.InputEvent.MouseButton.Pre(1,0,0));
            mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false);
            var find=org.example.maniacrevolution.hud.CustomHud.class.getDeclaredMethod("findItemWithAbility",net.minecraft.world.entity.player.Player.class);
            find.setAccessible(true);
            if(find.invoke(new org.example.maniacrevolution.hud.CustomHud(),mc.player)!=TechiesAbilityHud.INSTANCE)
                throw new AssertionError("Techies must occupy the existing class ability slot");
            Maniacrev.LOGGER.info("TECHIES_OVERLAY_PROBE_PASS: movement/sprint preserved, left/right close without world actions, ability slot selected");
            for(MineType type:MineType.values()) {
                TechiesClient.selected=type.ordinal(); TechiesClient.installStart=(long)TechiesClient.time()-type.install/2;
                target.clear(Minecraft.ON_OSX); target.bindWrite(true);
                RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(70),1.5f,.05f,20),VertexSorting.DISTANCE_TO_ORIGIN);
                view.setIdentity(); RenderSystem.applyModelViewMatrix();
                var handPose=new com.mojang.blaze3d.vertex.PoseStack(); var buffers=mc.renderBuffers().bufferSource();
                TechiesClient.hand(new net.minecraftforge.client.event.RenderHandEvent(net.minecraft.world.InteractionHand.MAIN_HAND,handPose,buffers,LightTexture.FULL_BRIGHT,0,0,0,0,mc.player.getMainHandItem()));
                TechiesClient.hand(new net.minecraftforge.client.event.RenderHandEvent(net.minecraft.world.InteractionHand.OFF_HAND,handPose,buffers,LightTexture.FULL_BRIGHT,0,0,0,0,mc.player.getOffhandItem()));
                buffers.endBatch(); save(target,"techies-hands-first-person-"+type.color+"-probe.png");
                if(type==MineType.RED) save(target,"techies-hands-first-person-probe.png");
            }
            TechiesClient.clear();
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            var far=new TechiesVisual("techies_mine_green"); far.alpha=.65f;
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(70),1.5f,.05f,64),VertexSorting.DISTANCE_TO_ORIGIN);
            var renderAt=TechiesClient.class.getDeclaredMethod("renderAt",com.mojang.blaze3d.vertex.PoseStack.class,
                    net.minecraft.client.renderer.MultiBufferSource.class,TechiesVisual.class,net.minecraft.world.phys.Vec3.class,boolean.class,boolean.class);
            renderAt.setAccessible(true);
            renderAt.invoke(null,new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),far,new net.minecraft.world.phys.Vec3(0,0,-1024),false,true);
            mc.renderBuffers().bufferSource().endBatch(); save(target,"techies-target-far-probe.png");
            try(var image=Screenshot.takeScreenshot(target)) {
                int green=0;
                for(int y=260;y<330;y++) for(int x=420;x<480;x++) {
                    int color=image.getPixelRGBA(x,y),r=color&255,gc=(color>>8)&255,b=(color>>16)&255;
                    if(gc>r+10 && gc>b+10) green++;
                }
                if(green<10) throw new AssertionError("Selected mine beyond far plane must produce visible green indicator pixels");
            }
            Maniacrev.LOGGER.info("TECHIES_FAR_TARGET_PROBE_PASS: 1024-block target visible with a 64-block far plane");
            var context=TechiesClient.class.getDeclaredField("connection"); context.setAccessible(true); context.set(null,null);
            long clock=mc.level.getGameTime();
            TechiesHands.accept(new TechiesPosePacket(player.getUUID(),clock,0,clock-10,-100));
            TechiesClient.accept(new TechiesStatePacket(1,mc.level.dimension().location(),clock,99999,0,net.minecraft.core.BlockPos.ZERO,-1,0,0,0,0));
            TechiesClient.accept(new TechiesStatePacket(3,mc.level.dimension().location(),clock,0,-1,net.minecraft.core.BlockPos.ZERO,-1,0,clock+100,0,0));
            TechiesClient.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            if(!TechiesClient.MINES.containsKey(99999) || TechiesClient.cooldowns[0]!=clock+100 || TechiesHands.pose(player)==null)
                throw new AssertionError("First packets must survive the first client tick, including pose-before-state ordering");
            TechiesClient.clear();
            Maniacrev.LOGGER.info("TECHIES_LIFECYCLE_PROBE_PASS: first pose/state packets survive client context initialization");
            Maniacrev.LOGGER.info("TECHIES_RENDER_PROBE_PASS: real GeckoLib models, helmet, radial menu, activation and neutral projection captured");
        } catch(Exception error) { Maniacrev.LOGGER.error("TECHIES_RENDER_PROBE_FAIL",error); }
        finally {
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting);
            mc.getMainRenderTarget().bindWrite(true); target.destroyBuffers(); mc.stop();
        }
    }
    private static void draw(GuiGraphics g,TechiesVisual v,float x,float y,boolean neutral,boolean through) {
        g.pose().pushPose(); g.pose().translate(x,y,150); g.pose().scale(115,-115,115);
        g.pose().mulPose(Axis.XP.rotationDegrees(20)); g.pose().mulPose(Axis.YP.rotationDegrees(-20));
        var texture=neutral?TechiesClient.WHITE:Maniacrev.loc("textures/item/"+v.stem+".png");
        var type=neutral?RenderType.entityTranslucent(texture):RenderType.entityCutoutNoCull(texture);
        TechiesClient.RENDERER.render(g.pose(),v,g.bufferSource(),type,null,LightTexture.FULL_BRIGHT);
        g.pose().popPose();
    }
    private static void save(TextureTarget target,String name) throws java.io.IOException {
        try(var image=Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of(name)); }
    }
}
