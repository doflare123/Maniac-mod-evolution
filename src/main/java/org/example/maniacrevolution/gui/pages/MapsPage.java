package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.GuideTheme;
import org.example.maniacrevolution.guide.MapGuideRegistry;

import java.util.ArrayList;
import java.util.List;

public class MapsPage extends GuidePage {
    private int scrollOffset = 0;
    private int detailScrollOffset = 0; // Новый скролл для детального просмотра
    private List<MapGuideRegistry.Entry> maps = new ArrayList<>();
    private MapGuideRegistry.Entry selectedMap = null;

    public MapsPage(GuideScreen parent) {
        super(parent);
        initMaps();
    }

    private void initMaps() {
        maps.clear();
        maps.addAll(MapGuideRegistry.getAll());
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset = 0;
        detailScrollOffset = 0;
        selectedMap = null;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackButton(gui, mouseX, mouseY);

        if (selectedMap != null) {
            renderMapDetails(gui, mouseX, mouseY);
        } else {
            renderMapList(gui, mouseX, mouseY);
        }
    }

    private void renderBackButton(GuiGraphics gui, int mouseX, int mouseY) {
        int btnX = guiLeft + 5;
        int btnY = guiTop + 10;
        int btnW = 80;
        int btnH = 18;

        GuideTheme.drawBackButton(gui, font, btnX, btnY, btnW,
                "← Главная", GuideTheme.RED, mouseX, mouseY);
    }

    private void renderMapList(GuiGraphics gui, int mouseX, int mouseY) {
        GuideTheme.drawPageTitle(gui, font, "КАРТЫ И АРЕНЫ",
                "Размер, сложность и уникальные особенности",
                guiLeft + guiWidth / 2, guiTop + 11, GuideTheme.RED);

        int y = guiTop + 60 - scrollOffset;
        int entryHeight = 90;

        gui.enableScissor(guiLeft + 5, guiTop + 55, guiLeft + guiWidth - 5, guiTop + guiHeight - 10);

        for (MapGuideRegistry.Entry map : maps) {
            if (y + entryHeight > guiTop + 50 && y < guiTop + guiHeight - 10) {
                boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + guiWidth - 10
                        && mouseY >= y && mouseY < y + entryHeight - 5
                        && mouseY >= guiTop + 55 && mouseY < guiTop + guiHeight - 10;

                renderMapEntry(gui, map, guiLeft + 10, y, hovered, mouseX, mouseY);
            }
            y += entryHeight;
        }

        gui.disableScissor();

        if (maps.size() > 2) {
            GuideTheme.drawScrollHint(gui, font, guiLeft + guiWidth - 6,
                    guiTop + guiHeight - 5);
        }
    }

    private void renderMapEntry(GuiGraphics gui, MapGuideRegistry.Entry map, int x, int y, boolean hovered, int mouseX, int mouseY) {
        int width = guiWidth - 20;
        int height = 85;

        GuideTheme.drawCard(gui, x, y, width, height, GuideTheme.RED, hovered);

        // Превью карты
        renderMapPreview(gui, map, x + 5, y + 5, 75, 75);

        // Название
        gui.drawString(font, map.name(), x + 85, y + 8, GuideTheme.TEXT, false);

        // Описание
        List<String> descLines = wrapText(map.description(), width - 95);
        for (int i = 0; i < Math.min(3, descLines.size()); i++) {
            gui.drawString(font, descLines.get(i), x + 85, y + 22 + i * 11,
                    GuideTheme.TEXT_SECONDARY, false);
        }

        gui.drawString(font, "Размер: " + map.size(), x + 85, y + 56,
                GuideTheme.TEXT_MUTED, false);
        String difficulty = map.difficultyStars();
        gui.drawString(font, difficulty, x + width - font.width(difficulty) - 8, y + 56,
                GuideTheme.GOLD, false);

        if (hovered) {
            gui.drawString(font, "Подробнее  →", x + width - font.width("Подробнее  →") - 8,
                    y + height - 15, GuideTheme.RED, false);
        }
    }

    private void renderMapPreview(GuiGraphics gui, MapGuideRegistry.Entry map, int x, int y, int width, int height) {
        ResourceLocation texture = map.previewTexture();

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, width, height, width, height);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Placeholder
            gui.fill(x, y, x + width, y + height, GuideTheme.SURFACE);
            gui.renderOutline(x, y, width, height, GuideTheme.BORDER);
            gui.drawCenteredString(font, "?", x + width / 2, y + height / 2 - 5, GuideTheme.TEXT_MUTED);
        }
    }

    private void renderMapDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "Назад к списку"
        int btnX = guiLeft + 5;
        int btnY = guiTop + guiHeight - 25;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 20;

        GuideTheme.drawButton(gui, font, btnX, btnY, 70, 20, "← Назад",
                GuideTheme.RED, hovered, false);

        // Заголовок
        GuideTheme.drawPageTitle(gui, font, selectedMap.name(),
                "Подробности выбранной арены",
                guiLeft + guiWidth / 2, guiTop + 10, GuideTheme.RED);

        // ИСПРАВЛЕНО: Область с прокруткой для контента
        gui.enableScissor(guiLeft + 5, guiTop + 45, guiLeft + guiWidth - 5, guiTop + guiHeight - 30);

        int y = guiTop + 50 - detailScrollOffset;
        int maxWidth = guiWidth - 30;

        // Превью карты (большое)
        int previewSize = 150;
        renderMapPreview(gui, selectedMap, guiLeft + (guiWidth - previewSize) / 2, y, previewSize, 100);
        y += 110;

        // Описание
        gui.drawString(font, selectedMap.description(), guiLeft + 15, y, GuideTheme.TEXT, false);
        y += 15;

        // Детали
        for (String detail : selectedMap.details()) {
            gui.drawString(font, detail, guiLeft + 15, y, GuideTheme.TEXT, false);
            y += 11;
        }

        y += 4;
        gui.drawString(font, "§e● Размер: §f" + selectedMap.size(), guiLeft + 15, y, 0xFFFFFF, false);
        y += 11;
        gui.drawString(font, "§e● Сложность: §6" + selectedMap.difficultyStars(), guiLeft + 15, y, 0xFFFFFF, false);
        y += 11;

        y += 10; // Отступ после текста

        gui.disableScissor();

        // ИСПРАВЛЕНО: Индикатор прокрутки
        int totalHeight = 110 + 15 + (selectedMap.details().size() * 11) + 36;
        int visibleHeight = guiHeight - 75; // Высота видимой области

        if (totalHeight > visibleHeight) {
            GuideTheme.drawScrollHint(gui, font, guiLeft + guiWidth - 6,
                    guiTop + guiHeight - 5);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Кнопка "Назад на главную"
            if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 85 && mouseY >= guiTop + 10 && mouseY < guiTop + 28) {
                parent.switchPage(PageType.MAIN);
                return true;
            }

            // Кнопка "Назад к списку"
            if (selectedMap != null) {
                if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 75 &&
                        mouseY >= guiTop + guiHeight - 25 && mouseY < guiTop + guiHeight - 5) {
                    selectedMap = null;
                    detailScrollOffset = 0; // Сбрасываем скролл
                    return true;
                }
            }

            // Клик на карту в списке
            if (selectedMap == null) {
                int y = guiTop + 60 - scrollOffset;
                int entryHeight = 90;

                for (MapGuideRegistry.Entry map : maps) {
                    if (mouseY >= y && mouseY < y + entryHeight - 5 &&
                            mouseX >= guiLeft + 10 && mouseX < guiLeft + guiWidth - 10 &&
                            mouseY >= guiTop + 55 && mouseY < guiTop + guiHeight - 10) {
                        selectedMap = map;
                        detailScrollOffset = 0; // Сбрасываем скролл при открытии
                        return true;
                    }
                    y += entryHeight;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedMap != null) {
            // ИСПРАВЛЕНО: Прокрутка в детальном просмотре
            int totalHeight = 110 + 15 + (selectedMap.details().size() * 11) + 36;
            int visibleHeight = guiHeight - 75;
            int maxScroll = Math.max(0, totalHeight - visibleHeight);

            detailScrollOffset = (int) Math.max(0, Math.min(maxScroll, detailScrollOffset - delta * 30));
            return true;
        } else {
            // Прокрутка списка карт
            int maxScroll = Math.max(0, maps.size() * 90 - 180);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
            return true;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedMap != null && keyCode == 256) { // ESC
            selectedMap = null;
            detailScrollOffset = 0;
            return true;
        }
        return false;
    }

}
