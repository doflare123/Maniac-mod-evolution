package org.example.maniacrevolution.atmosphere.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.atmosphere.client.AtmosphereLights;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both names are explicit because this project does not generate a Mixin refmap. Pinned to 1.20.1. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelLightMixin {
    @Inject(method = {"setBlocksDirty", "m_6550_"}, at = @At("HEAD"), remap = false, require = 1)
    private void maniacrev$lightChanged(BlockPos block, BlockState before, BlockState after, CallbackInfo ci) {
        AtmosphereLights.blockChanged(block, before, after);
    }
}
