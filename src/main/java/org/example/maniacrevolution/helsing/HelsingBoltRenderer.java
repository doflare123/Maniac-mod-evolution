package org.example.maniacrevolution.helsing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class HelsingBoltRenderer extends EntityRenderer<HelsingBoltEntity> {
    private final ItemRenderer items;
    private final java.util.EnumMap<HelsingBoltItem.Kind, ItemStack> models = new java.util.EnumMap<>(HelsingBoltItem.Kind.class);
    public HelsingBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
        for (var kind : HelsingBoltItem.Kind.values()) models.put(kind, new ItemStack(kind.item()));
    }
    @Override public void render(HelsingBoltEntity bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, bolt.yRotO, bolt.getYRot()) - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, bolt.xRotO, bolt.getXRot()) - 90));
        pose.scale(0.8F, 0.8F, 0.8F);
        items.renderStatic(models.get(bolt.kind()), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, bolt.level(), bolt.getId());
        pose.popPose();
        super.render(bolt, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(HelsingBoltEntity bolt) { return TextureAtlas.LOCATION_BLOCKS; }
}
