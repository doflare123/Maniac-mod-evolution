package org.example.maniacrevolution.item.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.necromancer.NecromancerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Броня некроманта, которая предоставляет пассивную защиту от смерти
 * При использовании пассивки броня может получить урон или сломаться
 */
public class NecromancerArmorItem extends ArmorItem {

    public NecromancerArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Проверяем, носит ли игрок полный сет
            if (hasFullSet(player)) {
                player.getCapability(NecromancerProvider.NECROMANCER).ifPresent(necroData -> {
                    // Проверяем, можно ли восстановить пассивку
                    if (necroData.canRestorePassive() && !necroData.hasPassiveProtection()) {
                        necroData.restorePassiveProtection();
                        player.displayClientMessage(
                                Component.literal("§5✦ Некромантская защита восстановлена ✦"),
                                true
                        );
                    }
                });
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("§5Часть доспехов некроманта"));
        tooltipComponents.add(Component.literal("§7Полный сет: Защита от смерти"));

        int durability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();

        if (durability < maxDurability * 0.2) {
            tooltipComponents.add(Component.literal("§c⚠ Критическое состояние!"));
        } else if (durability < maxDurability * 0.5) {
            tooltipComponents.add(Component.literal("§eТребует ремонта"));
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    private boolean hasFullSet(Player player) {
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (!(armorSlot.getItem() instanceof NecromancerArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Вызывается когда пассивная защита активируется
     * Можно переопределить логику урона брони
     */
    public void onPassiveActivated(Player player, ItemStack stack) {
        // Вариант 1: Полное уничтожение предмета
        stack.shrink(1);

        // Вариант 2: Серьёзный урон, но не уничтожение
//        int damage = stack.getMaxDamage() / 2;
//        EquipmentSlot slot = this.type.getSlot();
//        stack.hurtAndBreak(damage, player, p -> p.broadcastBreakEvent(slot));

        // Вариант 3: Полное уничтожение только если броня уже повреждена
//        int damage = stack.getMaxDamage() / 2;
//        EquipmentSlot slot = this.type.getSlot();
//
//        if (stack.getDamageValue() > stack.getMaxDamage() * 0.5) {
//            stack.shrink(1);
//            player.displayClientMessage(
//                Component.literal("§c✦ Броня некроманта разрушена! ✦"),
//                false
//            );
//        } else {
//            stack.hurtAndBreak(damage, player, p -> p.broadcastBreakEvent(slot));
//        }
    }

    /**
     * Метод для добавления бонусов при полном сете
     */
    public static void applySetBonus(Player player) {
        // Можно добавить дополнительные бонусы
        // Например, увеличенную регенерацию маны
        player.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA)
                .ifPresent(mana -> {
                    float currentBonus = mana.getBonusRegenRate();
                    mana.setBonusRegenRate(currentBonus + 0.5f); // +0.5 маны/сек
                });
    }

    /**
     * Снятие бонусов при снятии части сета
     */
    public static void removeSetBonus(Player player) {
        player.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA)
                .ifPresent(mana -> {
                    float currentBonus = mana.getBonusRegenRate();
                    mana.setBonusRegenRate(Math.max(0, currentBonus - 0.5f));
                });
    }
}