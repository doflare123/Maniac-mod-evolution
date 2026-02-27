package org.example.maniacrevolution.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlagueCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    // Замените "maniacrev" на ваш modid
    public static final ResourceLocation ID =
            new ResourceLocation("maniacrev", "plague_cap");

    public static final Capability<PlagueCapability> PLAGUE_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlagueCapability instance = new PlagueCapability();
    private final LazyOptional<PlagueCapability> optional = LazyOptional.of(() -> instance);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAGUE_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    // ─── Хелпер для получения capability с игрока ────────────────────────────

    /**
     * Получить PlagueCapability с игрока.
     * Возвращает null если capability не зарегистрирована (не должно происходить).
     */
    @Nullable
    public static PlagueCapability get(net.minecraft.world.entity.player.Player player) {
        return player.getCapability(PLAGUE_CAP).orElse(null);
    }
}