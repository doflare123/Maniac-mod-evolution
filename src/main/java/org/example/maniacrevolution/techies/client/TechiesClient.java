package org.example.maniacrevolution.techies.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.techies.*;
import org.example.maniacrevolution.util.PlayerModeUtil;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class TechiesClient {
    public static final class View {
        public final int id; public final MineType type; public final BlockPos support; public final TechiesVisual visual;
        public long started; public SimpleSoundInstance warning; public boolean placed=true;
        public double recovery=-100,recoverySeconds;
        View(TechiesStatePacket p) { id=p.id(); type=MineType.values()[p.type()]; support=p.pos(); started=p.started(); visual=new TechiesVisual("techies_mine_"+type.color); }
    }
    public static final Map<Integer,View> MINES=new HashMap<>();
    public static final TechiesWorldRenderer RENDERER=new TechiesWorldRenderer();
    public static final net.minecraft.resources.ResourceLocation WHITE=Maniacrev.loc("textures/item/techies_projection.png");
    public static int selected=-1,slot=-1,target;
    public static long installStart=-1;
    public static double remotePress=-100;
    public static BlockPos installation=BlockPos.ZERO;
    public static final long[] cooldowns=new long[3];
    private static long serverTick,clientTick;
    private static TechiesVisual hand;
    private static TechiesVisual projectionVisual;
    private static double recoveryStart=-1,recoverySeconds;
    private static int recoveryType=-1;
    private static Object connection;
    private static TechiesRadialScreen menu;
    private static int swallowedButtons;
    public static boolean menuOpen() { return menu!=null; }
    private TechiesClient() {}
    public static double time() {
        var mc=Minecraft.getInstance(); return mc.level==null?serverTick:serverTick+mc.level.getGameTime()-clientTick+mc.getFrameTime();
    }
    public static boolean eligible() {
        var p=Minecraft.getInstance().player;
        return p!=null && p.isAlive() && PlayerModeUtil.isSurvivalOrAdventure(p) && ClientPlayerData.isSurvivorClass(org.example.maniacrevolution.character.survivor.TechiesClass.SCOREBOARD_ID)
                && org.example.maniacrevolution.data.ClientGameState.getPhase()>=1 && org.example.maniacrevolution.data.ClientGameState.getPhase()<=3
                && p.getTeam()!=null && "survivors".equals(p.getTeam().getName());
    }
    public static void openMenu() {
        var mc=Minecraft.getInstance();
        if(menu!=null) { closeMenu(); return; }
        if(mc.screen==null && selected<0 && installStart<0 && !org.example.maniacrevolution.client.QTEClientHandler.isQTEActive()) {
            menu=new TechiesRadialScreen(); menu.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            mc.mouseHandler.releaseMouse();
        }
    }
    public static void closeMenu() {
        if(menu==null) return;
        menu=null; var mc=Minecraft.getInstance(); if(mc.screen==null) mc.mouseHandler.grabMouse();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre e) {
        if(e.getButton()<0 || e.getButton()>1) return;
        int bit=1<<e.getButton();
        if((swallowedButtons&bit)!=0) {
            e.setCanceled(true); if(e.getAction()==0) swallowedButtons&=~bit; suppressClicks(); return;
        }
        if(menu==null || e.getAction()!=1) return;
        var mc=Minecraft.getInstance();
        double x=mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth();
        double y=mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
        int choice=menu.hover(x,y);
        e.setCanceled(true); swallowedButtons|=bit; closeMenu(); suppressClicks();
        if(e.getButton()==0 && choice>=0) request(TechiesActionPacket.SELECT,choice);
    }
    private static void suppressClicks() {
        var mc=Minecraft.getInstance();
        if((swallowedButtons&1)!=0) { mc.options.keyAttack.setDown(false); while(mc.options.keyAttack.consumeClick()) {} }
        if((swallowedButtons&2)!=0) { mc.options.keyUse.setDown(false); while(mc.options.keyUse.consumeClick()) {} }
    }
    public static void request(int action,int value) {
        var mc=Minecraft.getInstance();
        if(mc.player!=null && mc.getConnection()!=null) {
            if(action==TechiesActionPacket.PLACE)
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(mc.player.getX(),mc.player.getY(),mc.player.getZ(),mc.player.getYRot(),mc.player.getXRot(),mc.player.onGround()));
            else if(action==TechiesActionPacket.HIT || action==TechiesActionPacket.REMOTE)
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(mc.player.getYRot(),mc.player.getXRot(),mc.player.onGround()));
        }
        ModNetworking.sendToServer(new TechiesActionPacket(action,value));
    }
    public static void remote() { if(eligible()) request(TechiesActionPacket.REMOTE,target); }
    public static void cancel() { if(installStart<0) { request(TechiesActionPacket.CANCEL,0); selected=-1; hand=null; } }
    private static void stop(View v) { if(v.warning!=null) { Minecraft.getInstance().getSoundManager().stop(v.warning); v.warning=null; } }
    public static void clear() { closeMenu(); swallowedButtons=0; MINES.values().forEach(TechiesClient::stop); MINES.clear(); selected=slot=-1; installStart=-1; hand=null; target=0; recoveryStart=-1; TechiesHands.clear(); }
    public static void ensureConnection() {
        var current=Minecraft.getInstance().getConnection();
        if(current!=connection) { clear(); Arrays.fill(cooldowns,0); connection=current; }
    }
    public static void accept(TechiesStatePacket p) {
        ensureConnection();
        var mc=Minecraft.getInstance(); if(mc.level==null || !mc.level.dimension().location().equals(p.dimension())) return;
        serverTick=p.tick(); clientTick=mc.level.getGameTime();
        switch(p.operation()) {
            case 0 -> clear();
            case 1,4 -> {
                if(p.type()<0 || p.type()>2) return;
                View v=MINES.get(p.id()); if(v==null) { v=new View(p); MINES.put(p.id(),v); }
                boolean restart=v.started!=p.started();
                if(v.started>=0 && p.started()<0) { v.recovery=time(); v.recoverySeconds=(time()-v.started)/20; }
                stop(v); v.started=p.started(); v.placed=p.operation()==1;
                if(v.started>=0 && p.tick()-v.started<=1) {
                    v.warning=new SimpleSoundInstance(TechiesSounds.warning(v.type),SoundSource.PLAYERS,.8f,1,RandomSource.create(),v.support.above());
                    mc.getSoundManager().play(v.warning);
                }
                if(v.type==MineType.GREEN && v.id==target && v.started>=0 && restart) remotePress=time();
            }
            case 2 -> { View v=MINES.remove(p.id()); if(v!=null) stop(v); }
            case 3 -> {
                if(installStart>=0 && p.started()<0 && selected>=0) {
                    recoveryStart=time(); recoverySeconds=(time()-installStart)/20; recoveryType=selected;
                }
                selected=p.type(); installStart=p.started(); installation=p.pos(); slot=p.selected(); target=p.id();
                cooldowns[0]=p.red(); cooldowns[1]=p.green(); cooldowns[2]=p.blue();
                if(selected>=0 && (hand==null || !hand.stem.equals("techies_mine_"+MineType.values()[selected].color))) hand=new TechiesVisual("techies_mine_"+MineType.values()[selected].color);
            }
            default -> {}
        }
    }
    public static BlockHitResult projection() {
        var mc=Minecraft.getInstance(); if(mc.player==null || mc.level==null) return null;
        Vec3 eye=mc.player.getEyePosition();
        return mc.level.clip(new ClipContext(eye,eye.add(mc.player.getLookAngle().scale(4)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
    }
    public static boolean valid(BlockHitResult hit) {
        var mc=Minecraft.getInstance(); if(hit==null || hit.getType()!=HitResult.Type.BLOCK || hit.getDirection()!=Direction.UP) return false;
        BlockPos pos=hit.getBlockPos();
        if(org.example.maniacrevolution.event.PenaltySlotManager.isInPenaltySlot(mc.player)) return false;
        if(selected>=0 && (cooldowns[selected]>time() || org.example.maniacrevolution.mana.ClientManaData.getMana()<MineType.values()[selected].cost)) return false;
        if(!mc.level.getBlockState(pos).isFaceSturdy(mc.level,pos,Direction.UP)
                || !mc.level.getBlockState(pos.above()).getCollisionShape(mc.level,pos.above()).isEmpty()
                || mc.player.getEyePosition().distanceToSqr(TechiesManager.center(pos))>16) return false;
        for(View v:MINES.values()) if(!TechiesRules.spaced(TechiesManager.center(pos).distanceToSqr(TechiesManager.center(v.support)))) return false;
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        ensureConnection();
        suppressClicks();
        if(menu!=null && (!eligible() || mc.screen!=null || org.example.maniacrevolution.client.QTEClientHandler.isQTEActive())) closeMenu();
        if(selected>=0 && (!eligible() || mc.player==null)) { clear(); return; }
        if(selected>=0 && installStart<0 && mc.player.getInventory().selected!=slot) cancel();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void input(InputEvent.InteractionKeyMappingTriggered e) {
        var mc=Minecraft.getInstance(); if(mc.player==null || !PlayerModeUtil.isSurvivalOrAdventure(mc.player)) return;
        if(selected>=0) {
            e.setCanceled(true); e.setSwingHand(false);
            if(e.isAttack() && installStart<0) request(TechiesActionPacket.PLACE,0);
            if(e.isUseItem() && installStart<0) cancel();
            return;
        }
        if(e.isUseItem() && eligible() && (mc.player.getMainHandItem().is(ModItems.TECHIES_REMOTE.get()) || mc.player.getOffhandItem().is(ModItems.TECHIES_REMOTE.get()))) {
            e.setCanceled(true); e.setSwingHand(false); remote(); return;
        }
        if(e.isAttack() && mc.player.getTeam()!=null && "maniac".equals(mc.player.getTeam().getName())) {
            double reach=mc.player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
            if(ClientPlayerData.isManiacClass(org.example.maniacrevolution.character.maniac.Scp173Class.SCOREBOARD_ID)) reach=Math.min(reach,1.5);
            if(ClientPlayerData.isManiacClass(org.example.maniacrevolution.character.maniac.WardenClass.SCOREBOARD_ID)) reach=Math.min(reach,3);
            Vec3 eye=mc.player.getEyePosition(),end=eye.add(mc.player.getLookAngle().scale(reach));
            View best=null; double distance=reach*reach;
            if(mc.hitResult!=null && mc.hitResult.getType()!=HitResult.Type.MISS)
                distance=Math.min(distance,eye.distanceToSqr(mc.hitResult.getLocation()));
            for(View v:MINES.values()) if(v.placed && v.type!=MineType.GREEN && v.started>=0) {
                Vec3 c=TechiesManager.center(v.support); var contact=new AABB(c.add(-.45,0,-.45),c.add(.45,.8,.45)).clip(eye,end);
                if(contact.isPresent() && eye.distanceToSqr(contact.get())<distance) { best=v; distance=eye.distanceToSqr(contact.get()); }
            }
            if(best!=null) { e.setCanceled(true); e.setSwingHand(true); request(TechiesActionPacket.HIT,best.id); }
        }
    }
    @SubscribeEvent public static void key(InputEvent.Key e) {
        if(e.getKey()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && e.getAction()==org.lwjgl.glfw.GLFW.GLFW_PRESS && menu!=null) {
            var mc=Minecraft.getInstance(); if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
            closeMenu(); return;
        }
        if(e.getKey()==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && e.getAction()==org.lwjgl.glfw.GLFW.GLFW_PRESS && selected>=0 && installStart<0) cancel();
    }
    private static void renderAt(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,TechiesVisual v,Vec3 at,boolean neutral,boolean through) {
        // Depth-test bypass alone cannot cross the projection far plane. The selected-object
        // indicator stays on its exact camera ray at a drawable depth; gameplay/world position stays authoritative.
        if(through && at.lengthSqr()>24*24) at=at.normalize().scale(24);
        pose.pushPose(); pose.translate(at.x,at.y,at.z);
        var texture=neutral?WHITE:Maniacrev.loc("textures/item/"+v.stem+".png");
        RenderType type=through?TechiesRenderTypes.throughWall(texture):neutral?RenderType.entityTranslucent(texture):RenderType.entityCutoutNoCull(texture);
        RENDERER.render(pose,v,buffer,type,buffer.getBuffer(type),LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=Minecraft.getInstance(); if(mc.player==null || !PlayerModeUtil.isSurvivalOrAdventure(mc.player)) return;
        var buffer=mc.renderBuffers().bufferSource(); Vec3 camera=e.getCamera().getPosition();
        for(View v:MINES.values()) if(v.placed) {
            Vec3 c=TechiesManager.center(v.support);
            if(v.id!=target && !e.getFrustum().isVisible(new AABB(c.add(-.6,0,-.6),c.add(.6,1.6,.6)))) continue;
            double recovery=1-(time()-v.recovery)/2;
            v.visual.clip=v.started>=0 || recovery>0?"activate":"";
            v.visual.seconds=v.started>=0?(time()-v.started)/20:v.recoverySeconds;
            v.visual.weight=v.started>=0?1:(float)Math.max(0,recovery);
            v.visual.red=v.visual.green=v.visual.blue=v.visual.alpha=1;
            if(v.id==target) { v.visual.red=.35f; v.visual.green=1; v.visual.blue=.65f; v.visual.alpha=.45f; }
            renderAt(e.getPoseStack(),buffer,v.visual,TechiesManager.center(v.support).subtract(camera),false,v.id==target);
        }
        if(selected>=0) {
            BlockHitResult hit=projection(); BlockPos support=installStart>=0?installation:hit==null?null:hit.getBlockPos();
            if(support!=null && (installStart>=0 || hit.getType()==HitResult.Type.BLOCK)) {
                String stem="techies_mine_"+MineType.values()[selected].color;
                if(projectionVisual==null || !projectionVisual.stem.equals(stem)) projectionVisual=new TechiesVisual(stem);
                TechiesVisual ghost=projectionVisual;
                boolean allowed=installStart>=0 || valid(hit); ghost.red=allowed?.1f:1; ghost.green=allowed?1:.08f; ghost.blue=.1f; ghost.alpha=.5f;
                renderAt(e.getPoseStack(),buffer,ghost,TechiesManager.center(support).subtract(camera),true,false);
            }
        }
        buffer.endBatch();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) {
        if(!eligible()) return;
        var mc=Minecraft.getInstance(); if(mc.options.hideGui) return;
        if(menu!=null && mc.screen==null) {
            menu.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            menu.render(e.getGuiGraphics(),(int)(mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth()),
                    (int)(mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight()),mc.getFrameTime());
        }
        var g=e.getGuiGraphics(); boolean compact=mc.getWindow().getGuiScaledWidth()<420;
        int cx=mc.getWindow().getGuiScaledWidth()/2,cy=mc.getWindow().getGuiScaledHeight()/2;
        int width=compact?100:140,x=cx-width/2,y=cy+42;
        if(installStart>=0 && selected>=0) {
            float progress=(float)Math.min(1,(time()-installStart)/MineType.values()[selected].install);
            TechiesHud.installation(g,cx,cy,MineType.values()[selected],progress);
        } else if(selected>=0) {
            g.drawString(mc.font,compact?"ЛКМ — мина":"ЛКМ — установить",x,y,0xEEEEEE,false);
            g.drawString(mc.font,"ПКМ — отмена",x,y+11,0xEEEEEE,false);
        } else if(target>0) {
            View chosen=MINES.get(target);
            String label=compact?"Пульт: цель":"Пульт: зелёная выбрана";
            if(chosen!=null) {
                Vec3 delta=TechiesManager.center(chosen.support).subtract(mc.player.getEyePosition());
                label=mc.player.getLookAngle().dot(delta)<0?"Пульт: позади":"Пульт: "+Math.round(delta.length())+" м";
            }
            g.drawString(mc.font,mc.font.plainSubstrByWidth(label,width),x,y,0xA5E3B4,false);
        }
    }
    @SubscribeEvent public static void hand(RenderHandEvent e) {
        if(!eligible()) return;
        double recovery=recoveryStart>=0?1-(time()-recoveryStart)/3:0;
        if(selected<0 && recovery<=0) return;
        int type=selected>=0?selected:recoveryType; if(type<0) return;
        if(e.getHand()==InteractionHand.OFF_HAND) {
            e.setCanceled(true); var mc=Minecraft.getInstance(); var pose=e.getPoseStack();
            boolean left=mc.player.getMainArm()!=HumanoidArm.LEFT;
            firstPersonArm(pose,e.getMultiBufferSource(),e.getPackedLight(),left,true);
            return;
        }
        var mc=Minecraft.getInstance(); e.setCanceled(true); PoseStack pose=e.getPoseStack(); pose.pushPose();
        boolean left=mc.player.getMainArm()==HumanoidArm.LEFT;
        var motion=TechiesHands.pose(mc.player); float[] translation=TechiesHands.sample(motion,"fp_main_translation"), rotation=TechiesHands.sample(motion,"fp_main_rotation_deg");
        pose.translate((left?-.35:.35)+(left?-translation[0]:translation[0]),-.4+translation[1],-1+translation[2]);
        pose.mulPose(Axis.XP.rotationDegrees(rotation[0])); pose.mulPose(Axis.YP.rotationDegrees(rotation[1]*(left?-1:1))); pose.mulPose(Axis.ZP.rotationDegrees(rotation[2]*(left?-1:1)));
        pose.mulPose(Axis.XP.rotationDegrees(10));
        pose.scale(.8f,.8f,.8f);
        TechiesVisual v=hand==null?new TechiesVisual("techies_mine_"+MineType.values()[type].color):hand;
        v.clip=installStart>=0 || recovery>0?"install":""; v.seconds=installStart>=0?(time()-installStart)/20:recoverySeconds;
        v.weight=selected>=0?1:(float)Math.max(0,recovery);
        RENDERER.render(pose,v,e.getMultiBufferSource(),RenderType.entityCutoutNoCull(Maniacrev.loc("textures/item/"+v.stem+".png")),null,e.getPackedLight());
        pose.popPose();
        // Real skin arms, no replacement player mesh. The item only overrides rendering, never inventory.
        firstPersonArm(pose,e.getMultiBufferSource(),e.getPackedLight(),left,false);
    }
    private static void firstPersonArm(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light,boolean left,boolean support) {
        var mc=Minecraft.getInstance();
        if(!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer)) return;
        float side=left?-1:1;
        var motion=TechiesHands.pose(mc.player); float[] translate=TechiesHands.sample(motion,"fp_main_translation"),rotate=TechiesHands.sample(motion,"fp_main_rotation_deg");
        float strength=support?.5f:1;
        pose.pushPose(); pose.translate(side*(.64+translate[0]*strength),-.6+translate[1]*strength,-.72+translate[2]*strength);
        pose.mulPose(Axis.YP.rotationDegrees(side*45));
        pose.mulPose(Axis.XP.rotationDegrees(rotate[0]*strength)); pose.mulPose(Axis.ZP.rotationDegrees(side*rotate[2]*strength));
        // Vanilla ItemInHandRenderer.renderPlayerArm orientation, then the supplied additive motion.
        pose.translate(side*-1,3.6,3.5); pose.mulPose(Axis.ZP.rotationDegrees(side*120));
        pose.mulPose(Axis.XP.rotationDegrees(200)); pose.mulPose(Axis.YP.rotationDegrees(side*-135)); pose.translate(side*5.6,0,0);
        if(left) renderer.renderLeftHand(pose,buffer,light,mc.player); else renderer.renderRightHand(pose,buffer,light,mc.player);
        pose.popPose();
    }
}
