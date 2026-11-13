package org.example.maniacrevolution.lich.client;

import java.util.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class LichPlayerForm {
    private static final Map<UUID,String> CASTS = new HashMap<>();
    private static final Map<AbstractClientPlayer,Boolean> PREVIEWS = new WeakHashMap<>();
    public static void preview(AbstractClientPlayer player,boolean lich){PREVIEWS.put(player,lich);}
    private static Renderer renderer;
    public static void accept(CompoundTag tag) {
        LichAudio.forms(tag);
        CASTS.clear();for(var value:tag.getList("rows",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;CASTS.put(row.getUUID("id"),row.getString("cast"));}
    }
    private static boolean active(AbstractClientPlayer p) {return PREVIEWS.containsKey(p)?PREVIEWS.get(p):p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&CASTS.containsKey(p.getUUID());}
    public static final class Body implements GeoReplacedEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        private final Map<AbstractClientPlayer,Action> actions=new WeakHashMap<>();
        private static final class Action {String clip="";double until;boolean swinging;int swing,hurt;}
        @Override public EntityType<?> getReplacingEntityType(){return EntityType.PLAYER;}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
            c.add(new AnimationController<>(this,"lich",0,s->{
                Entity e=s.getData(DataTickets.ENTITY);String clip="idle";
                if(e instanceof AbstractClientPlayer p){
                    String cast=CASTS.getOrDefault(p.getUUID(),"");
                    var action=actions.computeIfAbsent(p,k->new Action());double age=p.tickCount+s.getPartialTick();
                    if(p.hurtTime>action.hurt){action.clip="hurt";action.until=age+7;s.resetCurrentAnimation();}
                    else if(p.swinging&&(!action.swinging||p.swingTime<action.swing)){action.clip="attack";action.until=age+15;s.resetCurrentAnimation();}
                    action.hurt=p.hurtTime;action.swing=p.swingTime;action.swinging=p.swinging;
                    boolean book=p.getMainHandItem().is(ModItems.LICH_BOOK.get());
                    clip=!cast.isEmpty()?"cast_"+cast:age<action.until?action.clip:locomotion(p.isSwimming(),p.isCrouching(),p.isSprinting(),p.walkAnimation.speed()>.03F);
                    if(book&&!clip.startsWith("cast_"))clip="book_"+clip;
                }
                boolean loop=!clip.contains("cast_")&&!clip.endsWith("hurt")&&!clip.endsWith("attack");
                s.getController().transitionLength(clip.startsWith("cast_")?0:loop?2:1);
                return s.setAndContinue(RawAnimation.begin().then("animation.lich."+clip,
                        loop?Animation.LoopType.LOOP:Animation.LoopType.PLAY_ONCE));
            }));
        }
        @Override public double getTick(Object o){return o instanceof Entity e?e.tickCount:GeoReplacedEntity.super.getTick(o);}
    }
    public static String locomotion(boolean swimming,boolean crouching,boolean sprinting,boolean moving){
        return swimming?"swim":crouching?(moving?"crouch_move":"crouch_idle"):moving?(sprinting?"sprint":"move"):"idle";
    }
    private static final class Renderer extends GeoReplacedEntityRenderer<AbstractClientPlayer,Body>{
        Renderer(EntityRendererProvider.Context c){
            super(c,new LichRenderers.Model<>(b->"lich"),new Body());addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
            addRenderLayer(new LichBookLayer(this,()->currentEntity));
            addRenderLayer(new LichHeldItemLayer(this,()->currentEntity));
        }
    }
    @SubscribeEvent public static void render(RenderPlayerEvent.Pre e){
        if(renderer==null||!(e.getEntity() instanceof AbstractClientPlayer p)||!active(p))return;
        e.setCanceled(true);var pose=e.getPoseStack();pose.pushPose();
        try{
            var offset=e.getRenderer().getRenderOffset(p,e.getPartialTick());pose.translate(-offset.x,-offset.y,-offset.z);
            if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<>(p,e.getRenderer(),e.getPartialTick(),pose,e.getMultiBufferSource(),e.getPackedLight())))return;
            renderer.render(p,p.getYRot(),e.getPartialTick(),pose,e.getMultiBufferSource(),e.getPackedLight());
            String cast=CASTS.getOrDefault(p.getUUID(),"");
            if(!cast.isEmpty()){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-p.yBodyRot));pose.translate(0,1.25,-.625);pose.scale(.4F,.4F,.4F);LichMagic.render(p,"cast_"+cast,pose,e.getMultiBufferSource(),e.getPackedLight());pose.popPose();}
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(p,e.getRenderer(),e.getPartialTick(),pose,e.getMultiBufferSource(),e.getPackedLight()));
        }finally{pose.popPose();}
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();if(mc.player==null||!active(mc.player)||!mc.player.getMainHandItem().is(ModItems.LICH_BOOK.get()))return;
        if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        e.setCanceled(true);
        var pose=e.getPoseStack();pose.pushPose();try{
            pose.translate(0,-.28,-.58);pose.scale(.8F,.8F,.8F);
            mc.getItemRenderer().renderStatic(mc.player.getMainHandItem(),ItemDisplayContext.GROUND,e.getPackedLight(),0,pose,e.getMultiBufferSource(),mc.level,0);
        }finally{pose.popPose();}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){CASTS.clear();PREVIEWS.clear();}
    @Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup{
        @SubscribeEvent public static void setup(EntityRenderersEvent.AddLayers e){renderer=new Renderer(e.getContext());}
    }
}
