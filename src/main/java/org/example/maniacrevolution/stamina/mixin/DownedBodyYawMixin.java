package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.example.maniacrevolution.downed.DownedPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerRenderer.class)
public abstract class DownedBodyYawMixin {
    @ModifyVariable(method = {
            "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            "m_7523_(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private float maniacrev$lockDownedBodyYaw(float yaw, AbstractClientPlayer player) {
        return DownedPresentation.bodyYaw(player, yaw);
    }
}
