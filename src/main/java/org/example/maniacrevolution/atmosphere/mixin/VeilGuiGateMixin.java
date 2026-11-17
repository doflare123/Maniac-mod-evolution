package org.example.maniacrevolution.atmosphere.mixin;

import org.example.maniacrevolution.atmosphere.client.VeilRenderControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.GuiInfo", remap = false)
public abstract class VeilGuiGateMixin {
    @Inject(method = {"update", "unbind"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void maniacrev$benchmarkGate(CallbackInfo ci) {
        if (VeilRenderControl.skipRenderCall(8)) ci.cancel();
    }
}
