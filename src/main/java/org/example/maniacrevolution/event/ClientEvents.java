package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ActivatePerkPacket;
import org.example.maniacrevolution.network.packets.SwitchPerkPacket;

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

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            ModKeybinds.registerKeyMappings(event);
        }
    }
}
