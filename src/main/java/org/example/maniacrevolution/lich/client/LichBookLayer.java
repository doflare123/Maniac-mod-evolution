package org.example.maniacrevolution.lich.client;

import java.util.function.Supplier;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.ItemDisplayContext;
import org.example.maniacrevolution.ModItems;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/** An item nested in a bone must restore both the pose and the parent's buffer. */
public class LichBookLayer extends GeoRenderLayer<LichPlayerForm.Body> {
    private final Supplier<AbstractClientPlayer> player;
    public LichBookLayer(GeoRenderer<LichPlayerForm.Body> renderer,Supplier<AbstractClientPlayer> player){super(renderer);this.player=player;}
    protected boolean hasBook(){var p=player.get();return p!=null&&p.getMainHandItem().is(ModItems.LICH_BOOK.get());}
    protected void renderBook(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var p=player.get();
        Minecraft.getInstance().getItemRenderer().renderStatic(p.getMainHandItem(),ItemDisplayContext.GROUND,light,overlay,pose,buffers,p.level(),p.getId());
    }
    @Override public final void renderForBone(PoseStack pose,LichPlayerForm.Body body,GeoBone bone,RenderType type,
            MultiBufferSource buffers,VertexConsumer vertices,float partial,int light,int overlay){
        if(!bone.getName().equals("book_socket")||!hasBook())return;
        pose.pushPose();try{
            RenderUtils.translateToPivotPoint(pose,bone);renderBook(pose,buffers,light,overlay);
        }finally{
            pose.popPose();
            // Like GeckoLib's BlockAndItemGeoLayer: the item can flush/reuse the
            // shared BufferBuilder, so following bones must resume the body pass.
            buffers.getBuffer(type);
        }
    }
}
