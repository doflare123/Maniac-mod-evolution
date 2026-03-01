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

public class AddictionCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID =
            new ResourceLocation("maniacrev", "addiction_cap");

    public static final Capability<AddictionCapability> ADDICTION_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final AddictionCapability instance = new AddictionCapability();
    private final LazyOptional<AddictionCapability> optional = LazyOptional.of(() -> instance);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ADDICTION_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT()              { return instance.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt)    { instance.deserializeNBT(nbt); }

    public void invalidate() { optional.invalidate(); }

    /** Удобный хелпер: получить capability игрока или null */
    @Nullable
    public static AddictionCapability get(Player player) {
        return player.getCapability(ADDICTION_CAP).orElse(null);
    }
}