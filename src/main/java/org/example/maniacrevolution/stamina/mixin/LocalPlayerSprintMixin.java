package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.example.maniacrevolution.stamina.client.ClientStamina;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSprintMixin {
    @Inject(method = {"canStartSprinting", "m_264082_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void maniacrev$requireHeldSprint(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !ClientStamina.trySprint((LocalPlayer) (Object) this)) cir.setReturnValue(false);
    }
}
