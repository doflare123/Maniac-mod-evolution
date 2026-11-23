package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.example.maniacrevolution.downed.DownedCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DownedSwimMixin {
    @Inject(method = {"getSwimAmount", "m_20998_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void maniacrev$skipDownedSwim(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof AbstractClientPlayer player && DownedCapability.isDowned(player)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = {"isVisuallySwimming", "m_6067_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void maniacrev$decaySwimBlendWhileDowned(CallbackInfoReturnable<Boolean> cir) {
        // The collision pose stays low without building up a swim blend for the revival frame.
        if ((Object) this instanceof AbstractClientPlayer player && DownedCapability.isDowned(player)) {
            cir.setReturnValue(false);
        }
    }
}
