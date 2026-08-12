package org.example.maniacrevolution.paint;

import net.minecraft.util.Mth;

public enum PaintColor {
    MAGENTA(0xFF22B8),
    CYAN(0x15E6FF),
    LIME(0x75FF18),
    ORANGE(0xFF8A12),
    PURPLE(0x9B42FF),
    RED(0xFF274B);

    private final int rgb;

    PaintColor(int rgb) {
        this.rgb = rgb;
    }

    public int getRgb() {
        return rgb;
    }

    public float red() {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0F;
    }

    public int getNetworkId() {
        return ordinal();
    }

    public static PaintColor fromNetworkId(int id) {
        PaintColor[] values = values();
        return values[Mth.clamp(id, 0, values.length - 1)];
    }
}
