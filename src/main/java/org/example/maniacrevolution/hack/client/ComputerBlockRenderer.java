package org.example.maniacrevolution.hack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.hack.ModHackRegistry;

/**
 * Рендерер блока компьютера.
 * Регистрация (ClientModEvents):
 *   BlockEntityRenderers.register(ModHackRegistry.COMPUTER_BLOCK_ENTITY.get(),
 *       ComputerBlockRenderer::new);
 */
public class ComputerBlockRenderer implements BlockEntityRenderer<ComputerBlockEntity> {

    public ComputerBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ComputerBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;

        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

        // ── 3D модель ─────────────────────────────────────────────────────────
        // ItemRenderer в FIXED-контексте рендерит предмет центрированным по X/Z,
        // с центром по Y тоже в нуле item-пространства.
        // Модель: 16×19.5×17 BB-единиц. С FIXED scale=0.785: ~0.785×0.96×0.834 блока.
        // Центр модели по Y: 19.5/2 = 9.75 BB = 9.75/16 = 0.609 блока от низа.
        // Ставим центр модели на высоту 0.609/0 = модель встанет снизу блока.
        poseStack.pushPose();

        // Центрируем по X/Z на середине блока
        poseStack.translate(0.5, 0.0, 0.5);

        // Поворот по facing
        float yRot = switch (facing) {
            case SOUTH -> 180f;
            case WEST  -> 90f;
            case EAST  -> 270f;
            default    -> 0f; // NORTH
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

        // FIXED display translation y=-0.75 (item units) смещает вниз на 0.75*0.5=0.375 блока
        // Чтобы модель стояла на земле: поднимаем центр модели (0.609 блока) вверх,
        // компенсируем FIXED translation (-0.75 item units = -0.75/2 блока = -0.375)
        // итого Y = 0.609 + 0.375 ≈ 0.5 (примерно в середине по Y — подбираемо)
        // Опытным путём: ItemRenderer FIXED ставит предмет с центром в (0,0,0) poseStack.
        // Нам нужно чтобы низ модели был на Y=0. Высота модели с scale=0.785: 0.959 блока.
        // Значит центр на Y=0.959/2=0.48. Плюс FIXED translation y=-0.75 в item coords.
        // Item coords: 1 unit = 1/16 блока * 16 = 1 блок... нет, item space = 1 unit = 1 блок.
        // FIXED translation [0,-0.75,0] это -0.75 блока сдвиг вниз.
        // Компенсируем: +0.75. Плюс поднимаем центр модели до нужной высоты.
        // Финально: translate Y = 0.48 + 0.75 = нет... проще экспериментально.
        // Для блока высотой ~0.96 блока (с учётом scale): низ на Y=0 → центр на Y=0.48
        // FIXED автоматически опускает на 0.375 (=-0.75/2), значит нам нужно +0.48+0.375=0.855
        // Но это теория. Реальное значение подобрано ниже:
        poseStack.translate(0.0, 0.48, 0.0);

        Minecraft mc = Minecraft.getInstance();
        ItemStack itemStack = new ItemStack(ModHackRegistry.COMPUTER_ITEM.get());

        mc.getItemRenderer().renderStatic(
                itemStack,
                ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, be.getLevel(), be.hashCode());

        poseStack.popPose();

        // ── Текст на мониторе ─────────────────────────────────────────────────
        renderMonitorText(be, poseStack, buffers, facing);
    }

    private void renderMonitorText(ComputerBlockEntity be,
                                   PoseStack poseStack, MultiBufferSource buffers,
                                   Direction facing) {
        float progress = be.getHackProgress();
        boolean blocked = be.isBlocked();

        if (progress <= 0f && !be.isHacked() && !blocked) return;

        String line1;
        int textColor;

        if (blocked) {
            // Мигающий Error
            long time = System.currentTimeMillis();
            boolean blink = (time / 500) % 2 == 0;
            line1 = blink ? "Error" : "";
            textColor = 0xFF0000;
        } else if (be.isHacked()) {
            line1 = "Complete";
            textColor = 0x00FF00;
        } else {
            line1 = (int)(progress * 100) + "%";
            textColor = progressColor(progress);
        }

        poseStack.pushPose();

        // Центр монитора в пространстве блока:
        // Элемент 5 (экран монитора): from=[2,5.68,1.14] to=[14,17.68,9.14] (BB-единицы)
        // Центр X=(2+14)/2/16 = 0.5, Y=(5.68+17.68)/2/16 = 0.729, Z=1.14/16 = 0.071
        // Текст рисуем на передней грани (Z минимальный = 0.071), перед ней немного
        poseStack.translate(0.5, 0.729, 0.5);

        // Поворот по facing — текст смотрит туда же куда монитор
        float yRot = switch (facing) {
            case SOUTH -> 180f;
            case WEST  -> 90f;
            case EAST  -> 270f;
            default    -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

        // Выдвигаем к передней грани монитора
        // Для NORTH facing: монитор смотрит на -Z, передняя грань Z=0.071
        // От центра (0.5) до передней грани: 0.5 - 0.071 = 0.429 → сдвиг -Z
        poseStack.translate(0.0, 0.0, -0.38);

        // Масштаб текста. Отрицательный Y чтобы текст не был перевёрнут.
        // 0.012 = примерно 12pt в мировых единицах
        poseStack.scale(0.006f, -0.011f, 0.011f);

        // РАЗВОРОТ 180° чтобы текст смотрел на игрока (а не в стену)
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        var font = Minecraft.getInstance().font;

        // Процент / Complete
        int w1 = font.width(line1);
        font.drawInBatch(line1, -w1 / 2f, -8f, textColor | 0xFF000000,
                false, poseStack.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0, LightTexture.FULL_BRIGHT);

        // Полоска прогресса
        if (!be.isHacked() && !blocked && progress > 0f) {
            int totalBars = 10;
            int filled = Math.round(progress * totalBars);
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < totalBars; i++) {
                bar.append(i < filled ? "§a█" : "§7░");
            }
            bar.append("§f]");
            String barStr = bar.toString();
            int w2 = font.width(barStr);
            font.drawInBatch(barStr, -w2 / 2f, 4f, 0xFFFFFF,
                    false, poseStack.last().pose(), buffers,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                    0, LightTexture.FULL_BRIGHT);
        }

        poseStack.popPose();
    }

    private static int progressColor(float t) {
        if (t < 0.5f) {
            int r = (int)(255 * t * 2);
            return (r << 16) | (200 << 8);
        } else {
            int r = (int)(255 * (1f - (t - 0.5f) * 2));
            return (r << 16) | (255 << 8);
        }
    }
}