package org.example.maniacrevolution.downed;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DownedCapability {

    public static final Capability<DownedData> DOWNED = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation KEY = new ResourceLocation(Maniacrev.MODID, "downed_data");

    public static void register(IEventBus modBus) {
        modBus.addListener(DownedCapability::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DownedData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new Provider());
        }
    }

    /**
     * Безопасный геттер — возвращает null если capability ещё не прикреплена.
     * Всегда проверяй результат на null перед использованием!
     */
    @Nullable
    public static DownedData get(Player player) {
        return player.getCapability(DOWNED).orElse(null);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final DownedData data = new DownedData();
        private final LazyOptional<DownedData> optional = LazyOptional.of(() -> data);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return DOWNED.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }
    }
}
