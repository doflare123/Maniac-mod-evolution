package org.example.maniacrevolution.necromancer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NecromancerProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<NecromancerData> NECROMANCER = CapabilityManager.get(new CapabilityToken<>() {});

    private NecromancerData necromancerData = null;
    private final LazyOptional<NecromancerData> optional = LazyOptional.of(this::createNecromancerData);

    private NecromancerData createNecromancerData() {
        if (this.necromancerData == null) {
            this.necromancerData = new NecromancerData();
        }
        return this.necromancerData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == NECROMANCER) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createNecromancerData().saveNBTData();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createNecromancerData().loadNBTData(tag);
    }
}