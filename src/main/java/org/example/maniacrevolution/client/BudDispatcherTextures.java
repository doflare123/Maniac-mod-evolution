package org.example.maniacrevolution.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.perk.perks.maniac.BudDispatcherPerk;

/** Общий доступ к ячейкам цветочного атласа для GUI и мирового рендера. */
public final class BudDispatcherTextures {
    public static final ResourceLocation FLOWER_ATLAS = new ResourceLocation(
            Maniacrev.MODID, "textures/bud_dispatcher/flowers.png");

    public static final int ATLAS_WIDTH = 1024;
    public static final int ATLAS_HEIGHT = 1536;
    private static final int CELL_EDGE_INSET = 1;

    private static final int[] ACCENT_COLORS = {
            0xFFE3312B,
            0xFFFFD52A,
            0xFF20D7EE,
            0xFFC450EA,
            0xFFFF8A16,
            0xFFFF74C8,
            0xFF3974FF,
            0xFFFFF4C7,
            0xFFECE8FF
    };

    private BudDispatcherTextures() {
    }

    public static void blit(GuiGraphics graphics, int flowerIndex, int stage,
                            int x, int y, int width, int height) {
        int flower = clampFlower(flowerIndex);
        int wiltStage = clampStage(stage);
        int minU = minUPixels(wiltStage);
        int minV = minVPixels(flower);
        int sourceWidth = maxUPixels(wiltStage) - minU;
        int sourceHeight = maxVPixels(flower) - minV;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(FLOWER_ATLAS, x, y, width, height,
                minU, minV, sourceWidth, sourceHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
        RenderSystem.disableBlend();
    }

    public static float minU(int stage) {
        return minUPixels(clampStage(stage)) / (float) ATLAS_WIDTH;
    }

    public static float maxU(int stage) {
        return maxUPixels(clampStage(stage)) / (float) ATLAS_WIDTH;
    }

    public static float minV(int flowerIndex) {
        return minVPixels(clampFlower(flowerIndex)) / (float) ATLAS_HEIGHT;
    }

    public static float maxV(int flowerIndex) {
        return maxVPixels(clampFlower(flowerIndex)) / (float) ATLAS_HEIGHT;
    }

    public static int accentColor(int flowerIndex) {
        return ACCENT_COLORS[clampFlower(flowerIndex)];
    }

    private static int minUPixels(int stage) {
        return Math.round(stage * ATLAS_WIDTH
                / (float) BudDispatcherPerk.WILT_STAGE_COUNT) + CELL_EDGE_INSET;
    }

    private static int maxUPixels(int stage) {
        return Math.round((stage + 1) * ATLAS_WIDTH
                / (float) BudDispatcherPerk.WILT_STAGE_COUNT) - CELL_EDGE_INSET;
    }

    private static int minVPixels(int flowerIndex) {
        return Math.round(flowerIndex * ATLAS_HEIGHT
                / (float) BudDispatcherPerk.FLOWER_VARIANT_COUNT) + CELL_EDGE_INSET;
    }

    private static int maxVPixels(int flowerIndex) {
        return Math.round((flowerIndex + 1) * ATLAS_HEIGHT
                / (float) BudDispatcherPerk.FLOWER_VARIANT_COUNT) - CELL_EDGE_INSET;
    }

    private static int clampFlower(int flowerIndex) {
        return Math.max(0, Math.min(BudDispatcherPerk.FLOWER_VARIANT_COUNT - 1,
                flowerIndex));
    }

    private static int clampStage(int stage) {
        return Math.max(0, Math.min(BudDispatcherPerk.WILT_STAGE_COUNT - 1, stage));
    }
}
