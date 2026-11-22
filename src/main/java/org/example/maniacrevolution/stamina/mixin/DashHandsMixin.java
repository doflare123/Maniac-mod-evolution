package org.example.maniacrevolution.stamina.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.example.maniacrevolution.stamina.client.DashPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class DashHandsMixin {
    @Inject(method = {"renderHandsWithItems", "m_109314_"}, at = @At("HEAD"), remap = false)
    private void maniacrev$dashHands(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                    LocalPlayer player, int light, CallbackInfo ci) {
        pose.pushPose();
        DashPresentation.hands(player, pose, partialTick);
    }

    @Inject(method = {"renderHandsWithItems", "m_109314_"}, at = @At("RETURN"), remap = false)
    private void maniacrev$restoreHandPose(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                           LocalPlayer player, int light, CallbackInfo ci) {
        pose.popPose();
    }
}
