package org.example.maniacrevolution.stamina.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.example.maniacrevolution.stamina.StaminaManager;
import org.example.maniacrevolution.stamina.client.ClientStamina;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerJumpMixin {
    @Inject(method = {"jumpFromGround", "m_6135_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void maniacrev$spendStaminaOnJump(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player instanceof ServerPlayer serverPlayer) {
            if (!StaminaManager.tryJump(serverPlayer)) ci.cancel();
        } else if (player.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (!ClientStamina.tryLocalJump(player)) ci.cancel();
            });
        }
    }
}
