package org.example.maniacrevolution.mana;

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

public class ManaProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<ManaData> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    private ManaData manaData = null;
    private final LazyOptional<ManaData> optional = LazyOptional.of(this::createManaData);

    private ManaData createManaData() {
        if (this.manaData == null) {
            this.manaData = new ManaData();
        }
        return this.manaData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MANA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createManaData().saveNBTData().getAllKeys().forEach(key ->
                tag.put(key, createManaData().saveNBTData().get(key))
        );
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createManaData().loadNBTData(tag);
    }
}