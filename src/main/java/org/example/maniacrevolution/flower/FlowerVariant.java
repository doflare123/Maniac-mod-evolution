package org.example.maniacrevolution.flower;

import net.minecraft.resources.ResourceLocation;

/**
 * Ванильные цветы в порядке визуального приоритета: сначала самые насыщенные,
 * затем светлые и тёмные варианты.
 */
public enum FlowerVariant {
    POPPY("poppy", false, 10.0F, 0xFF3148, true),
    BLUE_ORCHID("blue_orchid", false, 10.0F, 0x35CFFF, true),
    ALLIUM("allium", false, 10.0F, 0xD85CFF, true),
    RED_TULIP("red_tulip", false, 10.0F, 0xFF3044, true),
    ORANGE_TULIP("orange_tulip", false, 10.0F, 0xFF7A22, true),
    PINK_TULIP("pink_tulip", false, 10.0F, 0xFF65C7, true),
    CORNFLOWER("cornflower", false, 10.0F, 0x496DFF, true),
    TORCHFLOWER("torchflower", false, 11.0F, 0xFF9C28, true),
    SUNFLOWER("sunflower", true, 16.0F, 0xFFD52E, true),
    LILAC("lilac", true, 16.0F, 0xCA70FF, true),
    PEONY("peony", true, 16.0F, 0xFF65AF, true),
    ROSE_BUSH("rose_bush", true, 16.0F, 0xFF294C, true),
    PITCHER_PLANT("pitcher_plant", true, 16.0F, 0x6D83FF, true),
    SPORE_BLOSSOM("spore_blossom", false, 16.0F, 0xDE62F1, true),
    PINK_PETALS("pink_petals", false, 16.0F, 0xFF8BD7, true),
    DANDELION("dandelion", false, 10.0F, 0xFFE13A, false),
    AZURE_BLUET("azure_bluet", false, 10.0F, 0xD7ECFF, false),
    OXEYE_DAISY("oxeye_daisy", false, 10.0F, 0xFFF4C2, false),
    WHITE_TULIP("white_tulip", false, 10.0F, 0xFFF0F7, false),
    LILY_OF_THE_VALLEY("lily_of_the_valley", false, 10.0F, 0xE9FFF2, false),
    WITHER_ROSE("wither_rose", false, 10.0F, 0x714A79, false);

    private static final FlowerVariant[] VALUES = values();

    private final ResourceLocation blockId;
    private final boolean upperHalf;
    private final float cropPixels;
    private final int saturatedColor;
    private final boolean colorfulPriority;

    FlowerVariant(String blockPath, boolean upperHalf, float cropPixels,
                  int saturatedColor, boolean colorfulPriority) {
        this.blockId = new ResourceLocation("minecraft", blockPath);
        this.upperHalf = upperHalf;
        this.cropPixels = cropPixels;
        this.saturatedColor = saturatedColor;
        this.colorfulPriority = colorfulPriority;
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }

    public boolean usesUpperHalf() {
        return upperHalf;
    }

    public float getCropPixels() {
        return cropPixels;
    }

    public int getSaturatedColor() {
        return saturatedColor;
    }

    public boolean hasColorfulPriority() {
        return colorfulPriority;
    }

    public int getNetworkId() {
        return ordinal();
    }

    public static FlowerVariant fromNetworkId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return POPPY;
        }
        return VALUES[id];
    }
}
