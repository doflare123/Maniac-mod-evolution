package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.network.ModNetworking;

import java.util.function.Supplier;

import static org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager.getRemainingCooldown;

public class ActivateArmorAbilityPacket {
    private final EquipmentSlot slot;

    public ActivateArmorAbilityPacket(EquipmentSlot slot) {
        this.slot = slot;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(slot);
    }

    public static ActivateArmorAbilityPacket decode(FriendlyByteBuf buffer) {
        return new ActivateArmorAbilityPacket(buffer.readEnum(EquipmentSlot.class));
    }

    public static void syncToClient(ServerPlayer player, Item item, int remainingDuration) {
        int remaining = getRemainingCooldown(player, item);

        if (item instanceof IItemWithAbility ability) {
            int maxCooldown = ability.getMaxCooldownSeconds();
            ModNetworking.sendToPlayer(
                    new SyncAbilityCooldownPacket(item, remaining / 20, maxCooldown, remainingDuration),
                    player
            );
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();

        if (player == null) {
            return false;
        }

        context.enqueueWork(() -> {
            // Проверяем броню в указанном слоте
            ItemStack armorPiece = player.getItemBySlot(slot);

            if (armorPiece.getItem() instanceof IActivatableArmor armor) {
                // Активируем способность
                armor.activateAbility(player);
            }
        });

        context.setPacketHandled(true);
        return true;
    }
}