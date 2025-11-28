package org.example.maniacrevolution.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.perk.perks.survivor.MimicPerk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для изменения размеров игрока при использовании перка Mimic.
 */
@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;

        // Если игрок использует перк Mimic, возвращаем размер блока
        if (MimicPerk.isPlayerMimicking(player.getUUID())) {
            cir.setReturnValue(EntityDimensions.fixed(0.98f, 0.98f));
        }
    }
}