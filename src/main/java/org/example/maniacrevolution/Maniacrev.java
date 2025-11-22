package org.example.maniacrevolution;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.command.ModCommands;
import org.example.maniacrevolution.cosmetic.CosmeticRegistry;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.shop.ShopRegistry;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Maniacrev.MODID)
public class Maniacrev {
    public static final String MODID = "maniacrev";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final java.util.function.Supplier<SoundEvent> PHASE_CHANGE_SOUND =
            SOUNDS.register("phase_change", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MODID, "phase_change")));

    public Maniacrev() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // ========== РЕГИСТРАЦИЯ ВСЕХ DEFERRED REGISTERS ==========
        SOUNDS.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);  // <-- ДОБАВИТЬ ЭТУ СТРОКУ
        // =========================================================

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GameManager());
        MinecraftForge.EVENT_BUS.register(new PlayerDataManager());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            PerkRegistry.init();
            ShopRegistry.init();
            CosmeticRegistry.init();
            LOGGER.info("ManiacRev Mod initialized!");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GameManager.init(event.getServer());
        PlayerDataManager.load(event.getServer());
        LOGGER.info("ManiacRev server data loaded");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PlayerDataManager.save(event.getServer());
        LOGGER.info("ManiacRev server data saved");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModKeybinds.register();
            LOGGER.info("ManiacRev client initialized");
        }
    }
}
