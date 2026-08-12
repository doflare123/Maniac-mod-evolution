package org.example.maniacrevolution.colorroulette;

import org.example.maniacrevolution.ModItems;
import net.minecraft.world.item.Item;

/** Three equally represented results of the color roulette. */
public enum ColorCard {
    RED(0xF23846),
    BLUE(0x26A9FF),
    GREEN(0x49E34D);

    private final int color;

    ColorCard(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public Item item() {
        return switch (this) {
            case RED -> ModItems.RED_COLOR_CARD.get();
            case BLUE -> ModItems.BLUE_COLOR_CARD.get();
            case GREEN -> ModItems.GREEN_COLOR_CARD.get();
        };
    }

    public static ColorCard byOrdinal(int ordinal) {
        ColorCard[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RED;
    }
}
