package org.example.maniacrevolution.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FurySwipesCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID =
            new ResourceLocation("maniacrev", "fury_swipes_cap");

    public static final Capability<FurySwipesCapability> FURY_SWIPES =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final FurySwipesCapability instance = new FurySwipesCapability();
    private final LazyOptional<FurySwipesCapability> optional = LazyOptional.of(() -> instance);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == FURY_SWIPES ? optional.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT()           { return instance.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt) { instance.deserializeNBT(nbt); }

    public void invalidate() { optional.invalidate(); }

    @Nullable
    public static FurySwipesCapability get(Player player) {
        return player.getCapability(FURY_SWIPES).orElse(null);
    }
}