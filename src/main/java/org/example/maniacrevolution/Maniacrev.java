package org.example.maniacrevolution;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.block.entity.ModBlockEntities;
import org.example.maniacrevolution.client.model.HookModel;
import org.example.maniacrevolution.client.model.TotemModel;
import org.example.maniacrevolution.client.renderer.HookRenderer;
import org.example.maniacrevolution.client.renderer.TotemRenderer;
import org.example.maniacrevolution.command.*;
import org.example.maniacrevolution.cosmetic.CosmeticRegistry;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedEventHandler;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.entity.TotemEntity;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.HackCommands;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.hack.ModHackRegistry;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.pregame.PreGameReadyManager;
import org.example.maniacrevolution.shop.ShopRegistry;
import org.example.maniacrevolution.potion.ModPotions;
import org.example.maniacrevolution.brewing.ModBrewingRecipes;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.readiness.ReadinessManager;
import org.example.maniacrevolution.sound.ModSounds;
import org.example.maniacrevolution.stats.StatsManager;
import org.example.maniacrevolution.system.Agent47ShopConfig;
import org.example.maniacrevolution.map.MapVotingManager;
import org.example.maniacrevolution.command.SettingsCommand;
import org.example.maniacrevolution.command.HpBoostCommand;
import org.example.maniacrevolution.command.ApplySettingsCommand;
import org.slf4j.Logger;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

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
        ModPotions.POTIONS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        DownedCapability.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModHackRegistry.BLOCKS.register(modEventBus);
        ModHackRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModHackRegistry.ITEMS.register(modEventBus);
        // =========================================================

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new GameManager());
        MinecraftForge.EVENT_BUS.register(new PlayerDataManager());
        MinecraftForge.EVENT_BUS.register(new DownedEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        StatsManager.initDriver(event);
        event.enqueueWork(() -> {
            CapabilityManager.get(new CapabilityToken<DownedData>() {});
            ModNetworking.register();
            ModBrewingRecipes.register();
            PerkRegistry.init();
            ShopRegistry.init();
            CosmeticRegistry.init();
            CharacterRegistry.init();
            Agent47ShopConfig.init();
            MapRegistry.init(); // Добавлено для карт
            LOGGER.info("ManiacRev Mod initialized!");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GameManager.init(event.getServer());
        PlayerDataManager.load(event.getServer());
        ReadinessManager.setServer(event.getServer());
        PreGameReadyManager.setServer(event.getServer());
        HackManager.reset();
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ReadinessManager.tick(event.getServer());
            MapVotingManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
        QTECommand.register(event.getDispatcher());
        ClearSaltCommand.register(event.getDispatcher());
        ClearAttributesCommand.register(event.getDispatcher());
        CharacterMenuCommand.register(event.getDispatcher());
        TestGlowCommand.register(event.getDispatcher());
        VoteMapCommand.register(event.getDispatcher());
        ResourcePackCommand.register(event.getDispatcher());
        GeneratorCommand.register(event.getDispatcher());
        ShamanCommands.register(event.getDispatcher());
        HackCommands.register(event.getDispatcher());
        StatsCommand.register(event.getDispatcher());

        SettingsCommand.register(event.getDispatcher());
        HpBoostCommand.register(event.getDispatcher());
        ApplySettingsCommand.register(event.getDispatcher());
        PreGameReadyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.isTimerRunning()) return;

        player.setGameMode(GameType.SPECTATOR);
        player.sendSystemMessage(Component.literal("§7Игра уже идёт. Вы переведены в режим наблюдателя."));
        LOGGER.info("Player {} joined during active game — set to spectator", player.getName().getString());
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
            event.registerEntityRenderer(ModEntities.SHAMAN_TOTEM.get(), TotemRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(HookModel.LAYER_LOCATION, HookModel::createBodyLayer);
            event.registerLayerDefinition(TotemModel.LAYER_LOCATION, TotemModel::createBodyLayer);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEntityEvents {

        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            // RageBee — расширяет ванильную Bee, нужны её атрибуты
            event.put(ModEntities.RAGE_BEE.get(),
                    net.minecraft.world.entity.animal.Bee.createAttributes().build());

            // Тотем шамана
            event.put(ModEntities.SHAMAN_TOTEM.get(),
                    TotemEntity.createAttributes().build());
        }
    }
}