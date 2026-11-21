package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.client.KeyMapping;
import org.example.maniacrevolution.stamina.client.ClientStamina;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class SprintKeyMappingMixin {
    @Inject(method = {"isDown", "m_90857_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void maniacrev$readHeldSprintKey(CallbackInfoReturnable<Boolean> cir) {
        if (ClientStamina.usesHeldSprintKey((KeyMapping) (Object) this)) {
            cir.setReturnValue(ClientStamina.sprintKeyHeld());
        }
    }
}
