package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.Maniacrev;

public class ClarityItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation MODEL_3D = new ResourceLocation(Maniacrev.MODID, "block/clarity_3d");
    private static final ResourceLocation MODEL_2D = new ResourceLocation(Maniacrev.MODID, "item/clarity");

    public ClarityItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int combinedLight, int combinedOverlay) {

        Minecraft mc = Minecraft.getInstance();

        // Используем 2D модель в GUI (инвентарь, слоты)
        if (displayContext == ItemDisplayContext.GUI) {
            BakedModel model2D = mc.getModelManager().getModel(MODEL_2D);
            mc.getItemRenderer().render(stack, displayContext, false, poseStack,
                    buffer, combinedLight, combinedOverlay, model2D);
        }
        // Используем 3D модель в мире/руке
        else {
            BakedModel model3D = mc.getModelManager().getModel(MODEL_3D);

            // Масштабируем и позиционируем модель
            poseStack.pushPose();

            // Настройка в зависимости от контекста
            switch (displayContext) {
                case FIRST_PERSON_LEFT_HAND:
                case FIRST_PERSON_RIGHT_HAND:
                    poseStack.translate(0.5, 0.5, 0.5);
                    poseStack.scale(1.0f, 1.0f, 1.0f);
                    break;
                case THIRD_PERSON_LEFT_HAND:
                case THIRD_PERSON_RIGHT_HAND:
                    poseStack.translate(0.5, 0.5, 0.5);
                    poseStack.scale(0.8f, 0.8f, 0.8f);
                    break;
                case GROUND:
                    poseStack.translate(0.5, 0, 0.5);
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    break;
                case HEAD:
                    poseStack.translate(0.5, 0, 0.5);
                    break;
                default:
                    poseStack.translate(0.5, 0.5, 0.5);
                    break;
            }

            mc.getItemRenderer().render(stack, displayContext, false, poseStack,
                    buffer, combinedLight, combinedOverlay, model3D);

            poseStack.popPose();
        }
    }
}