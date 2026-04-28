package org.example.maniacrevolution.dodepovich;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.example.maniacrevolution.ModItems;

public enum SlotMachineResult {
    NONE("Пусто", 0xFF777777, new ItemStack(Items.GRAY_DYE), false),
    JACKPOT("Джекпот", 0xFFFFD84D, new ItemStack(Items.GOLD_INGOT), true),
    COIN_GOOD("+ эффект монетки", 0xFF45D96F, ItemStack.EMPTY, true),
    COIN_BAD("- эффект монетки", 0xFFD94343, ItemStack.EMPTY, true),
    DIAMONDS("Алмазики", 0xFF72E9FF, new ItemStack(Items.DIAMOND), true),
    EMERALDS("Изумрудики", 0xFF53E27E, new ItemStack(Items.EMERALD), true),
    COAL("Уголь", 0xFF333333, new ItemStack(Items.COAL), true),
    SPIDER_EYE("Неудача", 0xFFB05AD8, new ItemStack(Items.SPIDER_EYE), true),
    ROTTEN_FLESH("Гниение", 0xFF8B5E36, new ItemStack(Items.ROTTEN_FLESH), true),
    INSURANCE("Страховка", 0xFF6AF28F, new ItemStack(Items.TOTEM_OF_UNDYING), true),
    CREDIT("Кредит", 0xFF33DD99, new ItemStack(Items.PAPER), true),
    DEATH("Смерть", 0xFF2B0B0B, ItemStack.EMPTY, true);

    private final String displayName;
    private final int color;
    private final ItemStack icon;
    private final boolean triple;

    SlotMachineResult(String displayName, int color, ItemStack icon, boolean triple) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.triple = triple;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public ItemStack getIcon(DodepovichCoin coin) {
        if (this == COIN_GOOD || this == COIN_BAD) {
            return switch (coin) {
                case ELUSIVENESS -> new ItemStack(ModItems.COIN_ELUSIVENESS.get());
                case INSIGHT -> new ItemStack(ModItems.COIN_INSIGHT.get());
                case SHACKLES -> new ItemStack(ModItems.COIN_SHACKLES.get());
                case HEALTH -> new ItemStack(ModItems.COIN_HEALTH.get());
                case EAGLE -> new ItemStack(ModItems.COIN_EAGLE.get());
                case DEBT -> new ItemStack(ModItems.COIN_DEBT.get());
                case REROLL -> new ItemStack(ModItems.COIN_REROLL.get());
                case FATE -> new ItemStack(ModItems.COIN_FATE.get());
            };
        }
        return icon.copy();
    }

    public boolean isTriple() {
        return triple;
    }
}
