package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.example.maniacrevolution.downed.DownedPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class DownedTurnMixin {
    @Inject(method = {"turn", "m_19884_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void maniacrev$limitDownedLook(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && DownedPresentation.turn(player, yaw, pitch)) ci.cancel();
    }
}
