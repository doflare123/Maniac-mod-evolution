package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.RoseColoredGlassesItem;

/** Keeps the bound item visible in inventories, but never renders it in either hand. */
public final class RoseColoredGlassesItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation INVENTORY_MODEL =
            new ResourceLocation(Maniacrev.MODID, "item/rose_colored_glasses_inventory");

    public RoseColoredGlassesItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int combinedLight, int combinedOverlay) {
        if (isOffhandDisplay(stack, displayContext)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(INVENTORY_MODEL);
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                        buffer, renderType, true, stack.hasFoil());
                minecraft.getItemRenderer().renderModelLists(
                        pass, stack, combinedLight, combinedOverlay, poseStack, consumer);
            }
        }
    }

    private static boolean isOffhandDisplay(ItemStack stack, ItemDisplayContext context) {
        boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean rightHand = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (!leftHand && !rightHand) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return true;
        java.util.UUID ownerId = RoseColoredGlassesItem.getOwner(stack);
        Player owner = ownerId == null ? null : minecraft.level.getPlayerByUUID(ownerId);
        if (owner == null) return true;

        HumanoidArm renderedArm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        return renderedArm != owner.getMainArm();
    }
}
