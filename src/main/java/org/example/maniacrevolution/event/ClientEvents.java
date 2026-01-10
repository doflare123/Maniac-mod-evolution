package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.client.particle.NecromancerParticle;
import org.example.maniacrevolution.client.renderer.BloodMarkerRenderer;
import org.example.maniacrevolution.effect.client.FearClientHandler;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.init.ModParticles;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ActivatePerkPacket;
import org.example.maniacrevolution.network.packets.SwitchPerkPacket;
import org.example.maniacrevolution.perk.perks.client.WallhackGlowHandler;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // ФИКС: Убраны проверки на null, т.к. клавиши теперь final
        if (ModKeybinds.OPEN_GUIDE.consumeClick()) {
            mc.setScreen(new GuideScreen());
        }

        if (ModKeybinds.ACTIVATE_PERK.consumeClick()) {
            ModNetworking.CHANNEL.sendToServer(new ActivatePerkPacket());
        }

        if (ModKeybinds.SWITCH_PERK.consumeClick()) {
            ModNetworking.CHANNEL.sendToServer(new SwitchPerkPacket());
        }
    }

//    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//    public class ParticleFactoryRegistry {
//
//        @SubscribeEvent
//        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
//            event.registerSpriteSet(ModParticles.NECROMANCER_SOUL.get(),
//                    NecromancerParticle.Provider::new);
//
//            event.registerSpriteSet(ModParticles.NECROMANCER_PENTAGRAM.get(),
//                    NecromancerParticle.Provider::new);
//
//            event.registerSpriteSet(ModParticles.RESURRECTION_ENERGY.get(),
//                    NecromancerParticle.Provider::new);
//        }
//    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Регистрируем рендерер для BloodMarkerEntity
            event.registerEntityRenderer(ModEntities.BLOOD_MARKER.get(), BloodMarkerRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            ModKeybinds.registerKeyMappings(event);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(org.example.maniacrevolution.client.renderer.WallhackRenderer.class);
        MinecraftForge.EVENT_BUS.register(FearClientHandler.class);
        event.enqueueWork(() -> {
            // Устанавливаем cutout render type для блока соли
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SALT_BLOCK.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        // Очищаем все свечения при выходе
        WallhackGlowHandler.clearAll();
    }
}
