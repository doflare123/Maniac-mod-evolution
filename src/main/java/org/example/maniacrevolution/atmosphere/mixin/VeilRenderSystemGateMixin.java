package org.example.maniacrevolution.atmosphere.mixin;

import org.example.maniacrevolution.atmosphere.client.VeilRenderControl;
import org.example.maniacrevolution.atmosphere.client.VeilPostOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public abstract class VeilRenderSystemGateMixin {
    @Inject(method = {"beginFrame", "endFrame"}, at = @At("HEAD"), cancellable = true, require = 1)
    private static void maniacrev$benchmarkGate(CallbackInfo ci) {
        if (VeilRenderControl.skipRenderCall(1)) ci.cancel();
    }

    @Inject(method = "renderPost", at = @At("HEAD"), cancellable = true, require = 1)
    private static void maniacrev$postOrder(CallbackInfo ci) {
        if (VeilRenderControl.skipRenderCall(1) || VeilPostOrder.skipLatePass()) ci.cancel();
    }
}
