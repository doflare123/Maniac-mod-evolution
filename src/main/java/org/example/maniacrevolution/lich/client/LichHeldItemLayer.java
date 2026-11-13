package org.example.maniacrevolution.lich.client;

import java.util.function.Supplier;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.example.maniacrevolution.ModItems;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/** Ordinary held items follow the animated palms; the main-hand book has its own layer. */
public class LichHeldItemLayer extends GeoRenderLayer<LichPlayerForm.Body> {
    public record Held(ItemStack stack,boolean left) {}
    private final Supplier<AbstractClientPlayer> player;
    public LichHeldItemLayer(GeoRenderer<LichPlayerForm.Body> renderer,Supplier<AbstractClientPlayer> player){super(renderer);this.player=player;}
    public static Held held(String bone,HumanoidArm mainArm,ItemStack main,ItemStack off,boolean mainBook){
        if(!bone.equals("right_palm_socket")&&!bone.equals("left_palm_socket"))return new Held(ItemStack.EMPTY,false);
        boolean left=bone.equals("left_palm_socket");
        boolean primary=(mainArm==HumanoidArm.LEFT)==left;
        var stack=primary?main:off;
        if(primary&&mainBook)stack=ItemStack.EMPTY;
        return new Held(stack,left);
    }
    protected Held held(String bone){
        var p=player.get();return p==null?new Held(ItemStack.EMPTY,false):held(bone,p.getMainArm(),p.getMainHandItem(),p.getOffhandItem(),p.getMainHandItem().is(ModItems.LICH_BOOK.get()));
    }
    protected void renderItem(Held item,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var p=player.get();
        Minecraft.getInstance().getItemRenderer().renderStatic(p,item.stack(),item.left()?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                item.left(),pose,buffers,p.level(),light,overlay,p.getId());
    }
    @Override public final void renderForBone(PoseStack pose,LichPlayerForm.Body body,GeoBone bone,RenderType type,
            MultiBufferSource buffers,VertexConsumer vertices,float partial,int light,int overlay){
        var item=held(bone.getName());if(item.stack().isEmpty())return;
        pose.pushPose();try{
            RenderUtils.translateToPivotPoint(pose,bone);
            // Geo's local axes already account for the vanilla humanoid X/Y flip.
            // Adding vanilla ItemInHandLayer's extra Y rotation would invert the item.
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            renderItem(item,pose,buffers,light,overlay);
        }finally{pose.popPose();buffers.getBuffer(type);}
    }
}
