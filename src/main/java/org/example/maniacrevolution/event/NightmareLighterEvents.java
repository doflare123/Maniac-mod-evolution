package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.NightmareLighterItem;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NightmareLighterEvents {
    private NightmareLighterEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof NightmareLighterItem) {
            NightmareLighterItem.tickHeld(player, mainHand, EquipmentSlot.MAINHAND);
            return;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof NightmareLighterItem) {
            NightmareLighterItem.tickHeld(player, offHand, EquipmentSlot.OFFHAND);
            return;
        }

        NightmareLighterItem.removeLight(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NightmareLighterItem.removeLight(player);
        }
    }
}
