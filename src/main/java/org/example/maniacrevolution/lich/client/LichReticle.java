package org.example.maniacrevolution.lich.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.hack.ComputerBlock;
import org.example.maniacrevolution.lich.*;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class LichReticle {
    record Hint(String label,int color,boolean focus,Entity entity,BlockHitResult block,boolean volume) {
        Hint(String label,int color,boolean focus) {this(label,color,focus,null,null,false);}
        Hint entity(Entity target) {return focus?new Hint(label,color,true,target,null,false):this;}
        Hint block(BlockHitResult hit,boolean whole) {return focus?new Hint(label,color,true,null,hit,whole):this;}
    }
    private static final Hint IDLE=new Hint("",LichReticleLayout.MUTED,false);
    private static Hint hint=IDLE;
    static Hint aimHint() {return hint;}
    public static final LichRitualProgress PROGRESS=new LichRitualProgress();
    private LichReticle() {}
    public static boolean visible() {
        var mc=Minecraft.getInstance();
        return LichClient.active()&&!mc.options.hideGui&&mc.options.getCameraType().isFirstPerson()&&mc.screen==null;
    }
    public static void clear() {PROGRESS.clear();hint=IDLE;LichWorldMarkers.clear();}
    public static void feedback(CompoundTag tag) {PROGRESS.accept(tag);}
    @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
        if(visible()&&event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()))event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(!visible()) {hint=IDLE;return;}
        var mc=Minecraft.getInstance();
        var phylactery=aimedPhylactery();
        boolean book=mc.player.getMainHandItem().is(ModItems.LICH_BOOK.get());
        hint=book&&mc.player.isShiftKeyDown()?action("Снять весь выбор",LichReticleLayout.JADE,false)
                :phylactery!=null?new Hint("",LichReticleLayout.JADE,true).entity(phylactery):book?aim():IDLE;
        var frame=PROGRESS.frame(mc.level.getGameTime());
        if(frame.visible()&&frame.state().equals("emerge")) {
            var entity=mc.level.getEntity(frame.entity());
            if(entity instanceof LichUnit unit&&unit.isDeadOrDying())PROGRESS.lostEmergingUnit(mc.level.getGameTime(),true);
            else if(entity==null&&mc.level.getGameTime()>PROGRESS.start()+10)PROGRESS.lostEmergingUnit(mc.level.getGameTime(),false);
        }
    }
    private static boolean clearSight(Vec3 target) {
        var mc=Minecraft.getInstance();Vec3 eye=mc.player.getEyePosition();
        int steps=Math.max(1,(int)Math.ceil(eye.distanceTo(target)/8));
        for(int i=0;i<=steps;i++)if(!mc.level.hasChunkAt(net.minecraft.core.BlockPos.containing(eye.lerp(target,(double)i/steps))))return false;
        return mc.level.clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;
    }
    public static LichObject aimedPhylactery() {
        var mc=Minecraft.getInstance();
        if(!LichClient.active()||!(mc.hitResult instanceof EntityHitResult hit)
                ||!(hit.getEntity() instanceof LichObject object)||!object.phylactery()||object.isRemoved()
                ||!object.syncedOwner().filter(mc.player.getUUID()::equals).isPresent()
                ||mc.player.distanceToSqr(object)>9||!clearSight(hit.getLocation()))return null;
        return object;
    }
    private static boolean survivor(Player player) {
        var downed=DownedCapability.get(player);
        return player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.getTeam()!=null
                &&player.getTeam().getName().equals("survivors")&&(downed==null||downed.getState()!=DownedState.DOWNED);
    }
    private static Hint action(String label,int color,boolean requiresSelection) {
        if(requiresSelection&&LichClient.army.getInt("selected")==0)return new Hint("Сначала выберите нежить",LichReticleLayout.MUTED,false);
        return new Hint("ПКМ · "+label,color,true);
    }
    private static Hint aim() {
        var mc=Minecraft.getInstance();Vec3 eye=mc.player.getEyePosition(),look=mc.player.getLookAngle();
        Entity best=null;double nearest=Double.POSITIVE_INFINITY;
        for(var entity:mc.level.entitiesForRendering()) {
            if(entity==mc.player||entity.isRemoved())continue;
            boolean candidate=entity instanceof LichUnit unit&&unit.isAlive()&&unit.syncedOwner().filter(mc.player.getUUID()::equals).isPresent()
                    ||entity instanceof Player player&&survivor(player)
                    ||entity instanceof LichObject corpse&&!corpse.phylactery()&&mc.player.distanceToSqr(corpse)<=9;
            if(!candidate)continue;
            double distance=LichReticleLayout.rayDistance(eye,look,entity.getBoundingBox());
            if(distance>=nearest||!Double.isFinite(distance))continue;
            var hit=entity.getBoundingBox().inflate(.15).clip(eye,eye.add(look.scale(eye.distanceTo(entity.getBoundingBox().getCenter())+3)));
            if(hit.isPresent()&&clearSight(hit.get())) {best=entity;nearest=distance;}
        }
        if(best instanceof LichUnit unit)return action(unit.selected()?"Снять выбор":"Выбрать · "+unit.kind().title,LichReticleLayout.JADE,false).entity(unit);
        if(best instanceof Player)return action("Преследовать",LichReticleLayout.RED,true).entity(best);
        if(best instanceof LichObject corpse)return corpse.visualAction().isEmpty()
                ?action("Воскресить / поглотить",LichReticleLayout.GOLD,false).entity(corpse):new Hint("Труп занят ритуалом",LichReticleLayout.RED,false);
        var hit=(BlockHitResult)mc.player.pick(128,1,false);
        if(hit.getType()==HitResult.Type.BLOCK&&mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof ComputerBlock)
            return action("Охранять ПК",LichReticleLayout.GOLD,true).block(hit,true);
        if(LichClient.army.getInt("selected")==0)return IDLE;
        if(mc.player.getXRot()<-65)return action("↑ Вернуться",LichReticleLayout.JADE,true);
        if(mc.player.getXRot()>65)return action("↓ Следовать",LichReticleLayout.JADE,true);
        return hit.getType()==HitResult.Type.BLOCK?action("Перейти",LichReticleLayout.JADE,true).block(hit,false):IDLE;
    }
    public static final IGuiOverlay INSTANCE=(gui,g,partial,width,height)->render(g,partial,width,height);
    private static void render(GuiGraphics g,float partial,int width,int height) {
        if(!visible())return;
        var mc=Minecraft.getInstance();int cx=width/2,cy=height/2;
        var progress=PROGRESS.frame(mc.level.getGameTime()+partial);boolean ritual=progress.showAtReticle();
        boolean noMana=LichClient.noManaWarning();
        int color=noMana?LichReticleLayout.RED:ritual?(progress.failed()?LichReticleLayout.RED:progress.state().equals("emerge")?LichReticleLayout.GOLD:LichReticleLayout.JADE)
                :LichWorldMarkers.swingFlash()?0xffd6ffed:hint.color();
        boolean book=mc.player.getMainHandItem().is(ModItems.LICH_BOOK.get());
        for(var pixel:LichReticleLayout.build(book,color,hint.focus(),ritual,progress.progress(),mc.level.getGameTime()+partial))
            g.fill(cx+pixel.x(),cy+pixel.y(),cx+pixel.x()+pixel.width(),cy+pixel.y()+pixel.height(),pixel.color());
        int cooldown=LichClient.combatCooldown();
        if(cooldown>0) {
            for(var pixel:LichReticleLayout.cooldown(cooldown))
                g.fill(cx+pixel.x(),cy+pixel.y(),cx+pixel.x()+pixel.width(),cy+pixel.y()+pixel.height(),pixel.color());
            String text=LichReticleLayout.cooldownText(cooldown);
            g.pose().pushPose();g.pose().translate(cx,cy-26,0);g.pose().scale(.65f,.65f,1);
            g.drawString(mc.font,text,-mc.font.width(text)/2,0,LichReticleLayout.JADE,true);g.pose().popPose();
        }
        if(noMana)caption(g,"Недостаточно маны · нужно 1",cx,cy,LichReticleLayout.RED,false);
        else if(!ritual&&aimedPhylactery()!=null)phylacteryHint(g,cx,cy);
        else if(!ritual&&!hint.label().isEmpty())caption(g,hint.label().replaceFirst("^ПКМ · ",""),cx,cy,color,hint.focus());
        if(!noMana&&!ritual) {
            String status=LichWorldMarkers.status();
            if(!status.isEmpty())small(g,status,cx,cy-14,0xffd6ffed,true);
        }
        if(ritual&&!noMana) {
            String label=switch(progress.state()) {
                case "cancelled"->"Ритуал прерван";
                case "failed"->progress.action().equals("emerge")?"Нежить погибла":"Ритуал не удался";
                case "complete"->progress.action().equals("absorb")?"Поглощено · +20 маны":"Филактерия · +1 HP";
                default->switch(progress.action()) {case "absorb"->"Поглощение";case "repair"->"Ремонт филактерии";default->"Воскрешение";};
            };
            if(progress.casting())label+=" · "+(int)(progress.progress()*100)+"%";
            caption(g,label,cx,cy,color,false);
            if(progress.casting())small(g,"Не двигайтесь",cx,cy+31,LichReticleLayout.MUTED,true);
        }
        if(mc.player.getMainHandItem().is(ModItems.LICH_BOOK.get())&&LichClient.army.getInt("selected")>0) {
            String count=Integer.toString(LichClient.army.getInt("selected"));
            var plate=LichReticleLayout.counter((int)Math.ceil(mc.font.width(count)*.65));
            g.fill(cx+8,cy+1,cx+plate.x(),cy+2,0xff44685b);
            plate(g,cx,cy,plate,LichReticleLayout.JADE);
            g.fill(cx+plate.x()+2,cy+plate.y()+4,cx+plate.x()+3,cy+plate.y()+6,LichReticleLayout.JADE);
            g.pose().pushPose();g.pose().translate(cx+plate.x()+5,cy+plate.y()+2,0);g.pose().scale(.65f,.65f,1);
            g.drawString(mc.font,count,0,0,LichReticleLayout.JADE,false);g.pose().popPose();
        }
    }
    private static void phylacteryHint(GuiGraphics g,int cx,int cy) {
        var mc=Minecraft.getInstance();var font=mc.font;
        boolean deselect=mc.player.isShiftKeyDown()&&mc.player.getMainHandItem().is(ModItems.LICH_BOOK.get());
        String right=deselect?"Снять выбор":"Взять";
        var p=LichReticleLayout.phylacteryHint((int)Math.ceil(Math.max(font.width("Починить"),font.width(right))*.7));
        plate(g,cx,cy,p,LichReticleLayout.JADE);
        for(int row=0;row<2;row++) {
            int x=cx+p.x(),y=cy+p.y()+row*11;
            int color=row==0?(LichClient.army.getInt("hp")>=10?LichReticleLayout.MUTED:
                    org.example.maniacrevolution.mana.ClientManaData.getMana()<5?LichReticleLayout.RED:LichReticleLayout.JADE):LichReticleLayout.JADE;
            g.fill(x+3,y+2,x+18,y+11,0xff183027);
            g.pose().pushPose();g.pose().translate(x+5,y+4,0);g.pose().scale(.5f,.5f,1);
            g.drawString(font,row==0?"ЛКМ":"ПКМ",0,0,color,false);g.pose().popPose();
            g.pose().pushPose();g.pose().translate(x+21,y+3,0);g.pose().scale(.7f,.7f,1);
            g.drawString(font,row==0?"Починить":right,0,0,color,false);g.pose().popPose();
        }
    }
    private static void plate(GuiGraphics g,int cx,int cy,LichReticleLayout.Plate p,int color) {
        int x=cx+p.x(),y=cy+p.y();
        g.fill(x,y,x+p.width(),y+p.height(),0xd50a1614);
        g.fill(x+2,y,x+p.width()-2,y+1,0xff3b5e50);
        g.fill(x+2,y+p.height()-1,x+p.width()-2,y+p.height(),0xff3b5e50);
        g.fill(x,y+2,x+1,y+p.height()-2,color);g.fill(x+p.width()-1,y+2,x+p.width(),y+p.height()-2,color);
    }
    private static void caption(GuiGraphics g,String label,int cx,int cy,int color,boolean rightClick) {
        var font=Minecraft.getInstance().font;
        label=font.plainSubstrByWidth(label,250);
        var p=LichReticleLayout.caption((int)Math.ceil(font.width(label)*.7));
        g.fill(cx,cy+10,cx+1,cy+p.y(),0xff3b5e50);
        plate(g,cx,cy,p,color);
        int x=cx+p.x(),y=cy+p.y();
        g.fill(x+2,y+2,x+16,y+10,0xff183027);
        if(rightClick) {
            g.pose().pushPose();g.pose().translate(x+4,y+4,0);g.pose().scale(.5f,.5f,1);
            g.drawString(font,"ПКМ",0,0,color,false);g.pose().popPose();
        } else {
            g.fill(x+8,y+4,x+9,y+9,color);g.fill(x+6,y+6,x+11,y+7,color);
        }
        g.pose().pushPose();g.pose().translate(x+20,y+3,0);g.pose().scale(.7f,.7f,1);
        g.drawString(font,label,0,0,color,false);g.pose().popPose();
    }
    private static void small(GuiGraphics g,String text,int x,int y,int color,boolean centered) {
        var font=Minecraft.getInstance().font;g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.8f,.8f,1);
        g.drawString(font,text,centered?-font.width(text)/2:0,0,color,true);g.pose().popPose();
    }
}
