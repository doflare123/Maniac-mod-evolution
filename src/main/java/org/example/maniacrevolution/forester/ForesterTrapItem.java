package org.example.maniacrevolution.forester;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.example.maniacrevolution.block.ModBlocks;

public final class ForesterTrapItem extends BlockItem {
    public ForesterTrapItem() { super(ModBlocks.FORESTER_TRAP.get(), new Properties().stacksTo(16)); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm, net.minecraft.world.item.ItemStack stack, float partial, float equip, float swing) {
                pose.translate(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? .45 : -.45, -.32 - equip * .4, -.85);
                return true;
            }
        });
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        if (context.getPlayer() == null || (!context.getPlayer().isCreative() && !ForesterManager.canUse(context.getPlayer()))) return InteractionResult.FAIL;
        return super.place(context);
    }
}
