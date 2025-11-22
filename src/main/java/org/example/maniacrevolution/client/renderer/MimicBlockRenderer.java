package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.entity.MimicBlockEntity;

@OnlyIn(Dist.CLIENT)
public class MimicBlockRenderer extends EntityRenderer<MimicBlockEntity> {

    public MimicBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(MimicBlockEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {

        BlockState blockState = entity.getBlockState();

        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            poseStack.pushPose();

            // Смещаем чтобы блок был по центру позиции энтити
            poseStack.translate(-0.5, 0, -0.5);

            // Рендерим блок стандартным рендерером Minecraft
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    blockState,
                    poseStack,
                    buffer,
                    light,
                    OverlayTexture.NO_OVERLAY
            );

            poseStack.popPose();
        }

        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicBlockEntity entity) {
        // Используем атлас блоков (стандартный)
        return InventoryMenu.BLOCK_ATLAS;
    }
}
