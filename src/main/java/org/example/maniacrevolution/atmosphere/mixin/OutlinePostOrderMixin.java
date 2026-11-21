package org.example.maniacrevolution.atmosphere.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.example.maniacrevolution.atmosphere.client.VeilPostOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class OutlinePostOrderMixin {
    // Explicit Mojang/SRG names match the other pinned 1.20.1 hooks; no refmap is generated.
    @Inject(method = {"doEntityOutline", "m_109769_"}, at = @At("HEAD"), remap = false, require = 1)
    private void maniacrev$postBeforeOutlines(CallbackInfo ci) {
        VeilPostOrder.beforeOutlines();
    }
}
