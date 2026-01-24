package org.example.maniacrevolution.item.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.mana.ManaProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MedicalMaskItem extends ArmorItem implements IActivatableArmor, IItemWithAbility, ITimedAbility {

    private static final MedicalMaskMaterial MATERIAL = new MedicalMaskMaterial();

    // Параметры способности
    private static final float MANA_COST = 30.0f;
    private static final int DURATION_TICKS = 20 * 15; // 15 секунд
    private static final int COOLDOWN_TICKS = 20 * 60; // 60 секунд
    private static final String NBT_KEY_ACTIVE = "MedicMaskActive";

    public MedicalMaskItem() {
        super(MATERIAL, Type.HELMET, new Properties()
                .stacksTo(1)
                .durability(0)
        );
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return Maniacrev.MODID + ":textures/models/armor/medical_mask_layer_1.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6Способность: §e" + getAbilityName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7" + getAbilityDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§9Стоимость: §b" + (int)MANA_COST + " маны").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§9Длительность: §b" + (DURATION_TICKS / 20) + "с").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§9Кулдаун: §b" + (COOLDOWN_TICKS / 20) + "с").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§8Нажмите [V] для активации").withStyle(ChatFormatting.DARK_GRAY));
    }

    // === Реализация IActivatableArmor ===

    @Override
    public boolean activateAbility(ServerPlayer player) {
        if (!canActivate(player)) {
            return false;
        }

        boolean manaConsumed = player.getCapability(ManaProvider.MANA).map(mana -> {
            if (!mana.consumeMana(MANA_COST)) {
                player.displayClientMessage(
                        Component.literal("§cНедостаточно маны! (Нужно: " + (int)MANA_COST + ")"),
                        true
                );
                return false;
            }
            return true;
        }).orElse(false);

        if (!manaConsumed) {
            return false;
        }

        // Устанавливаем кулдаун
        ArmorAbilityCooldownManager.setCooldown(player, this, COOLDOWN_TICKS);

        // Активируем способность
        player.getPersistentData().putInt(NBT_KEY_ACTIVE,
                player.getServer().getTickCount() + DURATION_TICKS);

        // ИСПРАВЛЕНО: Синхронизируем с клиентом с длительностью
        ArmorAbilityCooldownManager.syncToClient(player, this, DURATION_TICKS / 20);

        player.displayClientMessage(
                Component.literal("§a✓ " + getAbilityName() + " активирована! (" + (DURATION_TICKS / 20) + "с)"),
                false
        );

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, player.getSoundSource(), 0.5f, 1.5f);

        return true;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        // Проверка надета ли маска
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() != this) {
            return false;
        }

        // Проверка кулдауна
        if (ArmorAbilityCooldownManager.isOnCooldown(player, this)) {
            int remaining = ArmorAbilityCooldownManager.getRemainingCooldown(player, this);
            player.displayClientMessage(
                    Component.literal("§cКулдаун: " + (remaining / 20) + "с"),
                    true
            );
            return false;
        }

        // Проверка активности
        if (isAbilityActive(player)) {
            player.displayClientMessage(
                    Component.literal("§cСпособность уже активна!"),
                    true
            );
            return false;
        }

        return true;
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public int getDuration() {
        return DURATION_TICKS;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN_TICKS;
    }

    @Override
    public String getAbilityName() {
        return "Разделение урона";
    }

    @Override
    public String getAbilityDescription() {
        return "Входящий урон делится с ближайшим союзником (50%) в радиусе 4 блоков";
    }

    /**
     * Проверка активности способности
     */
    public static boolean isAbilityActive(ServerPlayer player) {
        if (!player.getPersistentData().contains(NBT_KEY_ACTIVE)) {
            return false;
        }

        int expiryTick = player.getPersistentData().getInt(NBT_KEY_ACTIVE);
        int currentTick = player.getServer().getTickCount();

        if (currentTick >= expiryTick) {
            // ИСПРАВЛЕНО: При окончании способности обновляем кулдаун
            player.getPersistentData().remove(NBT_KEY_ACTIVE);

            // Синхронизируем с клиентом (длительность = 0, кулдаун актуальный)
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.getItem() instanceof MedicalMaskItem mask) {
                ArmorAbilityCooldownManager.syncToClient(player, mask, 0);
            }

            player.displayClientMessage(
                    Component.literal("§7Разделение урона закончилось"),
                    true
            );
            return false;
        }

        return true;
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/medical_mask.png");
    }

    @Override
    public int getCooldownSeconds(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // На клиенте используем клиентский кулдаун (если есть)
            return ClientAbilityData.getCooldownSeconds(this);
        }
        return ArmorAbilityCooldownManager.getRemainingCooldown(serverPlayer, this) / 20;
    }

    @Override
    public int getMaxCooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    @Override
    public int getDurationSeconds() {
        return DURATION_TICKS / 20;
    }

    @Override
    public int getRemainingDurationSeconds(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // На клиенте используем клиентские данные
            return ClientAbilityData.getRemainingDuration(this);
        }

        if (!serverPlayer.getPersistentData().contains(NBT_KEY_ACTIVE)) {
            return 0;
        }

        int expiryTick = serverPlayer.getPersistentData().getInt(NBT_KEY_ACTIVE);
        int currentTick = serverPlayer.getServer().getTickCount();
        int remainingTicks = Math.max(0, expiryTick - currentTick);

        return remainingTicks / 20;
    }

    @Override
    public boolean isAbilityActive(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return isAbilityActive(serverPlayer);
        }
        // На клиенте проверяем по длительности
        return getRemainingDurationSeconds(player) > 0;
    }

    // Материал маски
    private static class MedicalMaskMaterial implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "medical_mask";
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }
}