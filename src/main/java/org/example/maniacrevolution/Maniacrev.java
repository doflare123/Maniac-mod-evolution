package org.example.maniacrevolution;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.client.model.HookModel;
import org.example.maniacrevolution.client.renderer.HookRenderer;
import org.example.maniacrevolution.command.*;
import org.example.maniacrevolution.cosmetic.CosmeticRegistry;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.shop.ShopRegistry;
import org.example.maniacrevolution.potion.ModPotions;
import org.example.maniacrevolution.brewing.ModBrewingRecipes;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.readiness.ReadinessManager;
import org.slf4j.Logger;

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
        ModBlocks.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus); // Добавь эту строку
        // =========================================================

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GameManager());
        MinecraftForge.EVENT_BUS.register(new PlayerDataManager());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            ModBrewingRecipes.register(); // Добавь эту строку
            PerkRegistry.init();
            ShopRegistry.init();
            CosmeticRegistry.init();
            CharacterRegistry.init();
            LOGGER.info("ManiacRev Mod initialized!");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GameManager.init(event.getServer());
        PlayerDataManager.load(event.getServer());
        ReadinessManager.setServer(event.getServer());
        LOGGER.info("Character system initialized");
        LOGGER.info("ManiacRev server data loaded");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PlayerDataManager.save(event.getServer());
        ReadinessManager.clear();
        LOGGER.info("ManiacRev server data saved");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
        QTECommand.register(event.getDispatcher());
        ClearSaltCommand.register(event.getDispatcher());
        ClearAttributesCommand.register(event.getDispatcher());
        CharacterMenuCommand.register(event.getDispatcher());
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

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.HOOK.get(), HookRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(HookModel.LAYER_LOCATION, HookModel::createBodyLayer);
        }
    }
}