package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ActivateArmorAbilityPacket;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class ArmorAbilityKeyHandler {

    private static boolean wasPressed = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isPressed = ModKeybinds.ACTIVATE_ARMOR_ABILITY.isDown();

        // Обработка нажатия (один раз при нажатии)
        if (isPressed && !wasPressed) {
            handleArmorAbilityActivation(mc.player);
        }

        wasPressed = isPressed;
    }

    private static void handleArmorAbilityActivation(LocalPlayer player) {
        // Проверяем броню игрока
        IActivatableArmor activatableArmor = null;
        EquipmentSlot slot = null;

        // Проверяем все слоты брони
        for (EquipmentSlot armorSlot : EquipmentSlot.values()) {
            if (armorSlot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack armorPiece = player.getItemBySlot(armorSlot);
            if (armorPiece.getItem() instanceof IActivatableArmor armor) {
                activatableArmor = armor;
                slot = armorSlot;
                break; // Берем первую найденную броню со способностью
            }
        }

        if (activatableArmor == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cНаденьте броню со способностью!"),
                    true
            );
            return;
        }

        // Отправляем пакет на сервер
        ModNetworking.sendToServer(new ActivateArmorAbilityPacket(slot));
    }
}