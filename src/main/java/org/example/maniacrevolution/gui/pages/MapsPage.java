package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;

public class MapsPage extends GuidePage {
    private int scrollOffset = 0;
    private int detailScrollOffset = 0; // Новый скролл для детального просмотра
    private List<MapInfo> maps = new ArrayList<>();
    private MapInfo selectedMap = null;

    public MapsPage(GuideScreen parent) {
        super(parent);
        initMaps();
    }

    private void initMaps() {
        maps.clear();

        maps.add(new MapInfo(
                "Особняк",
                "mansion",
                "§7Классическая карта.",
                new String[]{
                        "§e● Особенности:",
                        "  - Много этажей",
                        "  - Коридорнность",
                        "  - Потайные проходы",
                        "",
                        "§e● Советы:",
                        "  §aВыжившим:§r Пытайтесь сокращать путь через потайные проходы (не забывая их закрывать)",
                        "  §cМаньякам:§r Пытайтесь ловить выживших в узких проходах",
                        "",
                        "§e● Размер: §fСредний",
                        "§e● Сложность: §6★★★☆☆"
                }
        ));

        maps.add(new MapInfo(
                "Пиццерия Фрэдэ",
                "pizzeria",
                "§7 Карта основанная на 1 и 2 частях франшизы.",
                new String[]{
                        "§e● Особенности:",
                        "  - Открытая карта",
                        "  - Вентиляции",
                        "  - Канализации",
                        "  - Есть места взаимодействия с окружением",
                        "",
                        "§e● Советы:",
                        "  §aВыжившим:§r Используйте вентиляции для быстрого перемещения к генераторам",
                        "  §cМаньякам:§r Пытайтесь ловить выживших возле генераторов",
                        "",
                        "§e● Размер: §fБольшой",
                        "§e● Сложность: §6★★★★☆"
                }
        ));
//
//        maps.add(new MapInfo(
//                "Промзона",
//                "industrial",
//                "§7Заброшенный завод с опасными механизмами.",
//                new String[]{
//                        "§e● Особенности:",
//                        "  - Конвейерные ленты",
//                        "  - Токсичные зоны",
//                        "  - Лабиринт труб и платформ",
//                        "  - Опасные ловушки",
//                        "",
//                        "§e● Советы:",
//                        "  §aВыжившим:§r Избегайте токсичных зон",
//                        "  §cМаньякам:§r Загоняйте жертв в ловушки",
//                        "",
//                        "§e● Размер: §fСредний",
//                        "§e● Сложность: §6★★★★★"
//                }
//        ));
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
        int btnY = guiTop + 5;
        int btnW = 80;
        int btnH = 18;

        boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

        gui.fill(btnX, btnY, btnX + btnW, btnY + btnH, hovered ? 0xFF444444 : 0xFF333333);
        gui.renderOutline(btnX, btnY, btnW, btnH, 0xFF666666);
        gui.drawCenteredString(font, "← Главная", btnX + btnW / 2, btnY + 5, 0xFFFFFF);
    }

    private void renderMapList(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawCenteredString(font, "§6§lКарты и их особенности",
                guiLeft + guiWidth / 2, guiTop + 28, 0xFFFFFF);

        gui.drawCenteredString(font, "§7Нажмите на карту для подробностей",
                guiLeft + guiWidth / 2, guiTop + 42, 0xAAAAAA);

        int y = guiTop + 60 - scrollOffset;
        int entryHeight = 90;

        gui.enableScissor(guiLeft + 5, guiTop + 55, guiLeft + guiWidth - 5, guiTop + guiHeight - 10);

        for (MapInfo map : maps) {
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
            gui.drawString(font, "§8Прокрутка: колёсико мыши", guiLeft + guiWidth - 140,
                    guiTop + guiHeight - 12, 0x666666, false);
        }
    }

    private void renderMapEntry(GuiGraphics gui, MapInfo map, int x, int y, boolean hovered, int mouseX, int mouseY) {
        int width = guiWidth - 20;
        int height = 85;

        gui.fill(x, y, x + width, y + height, hovered ? 0xAA444444 : 0x80333333);
        if (hovered) {
            gui.renderOutline(x, y, width, height, 0xFFFFAA00);
        }

        // Превью карты
        renderMapPreview(gui, map, x + 5, y + 5, 75, 75);

        // Название
        gui.drawString(font, "§6§l" + map.name, x + 85, y + 8, 0xFFFFFF, false);

        // Описание
        List<String> descLines = wrapText(map.description, width - 95);
        for (int i = 0; i < Math.min(3, descLines.size()); i++) {
            gui.drawString(font, descLines.get(i), x + 85, y + 22 + i * 11, 0xAAAAAA, false);
        }

        if (hovered) {
            gui.drawString(font, "§e§oКлик для подробностей →", x + 85, y + height - 15, 0xFFAA00, false);
        }
    }

    private void renderMapPreview(GuiGraphics gui, MapInfo map, int x, int y, int width, int height) {
        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/gui/maps/" + map.id + ".png");

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, width, height, width, height);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Placeholder
            gui.fill(x, y, x + width, y + height, 0xFF555555);
            gui.renderOutline(x, y, width, height, 0xFF888888);
            gui.drawCenteredString(font, "§8🗺", x + width / 2, y + height / 2 - 5, 0xFFFFFF);
        }
    }

    private void renderMapDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "Назад к списку"
        int btnX = guiLeft + 5;
        int btnY = guiTop + guiHeight - 25;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 20;

        gui.fill(btnX, btnY, btnX + 70, btnY + 20, hovered ? 0xFF555555 : 0xFF333333);
        gui.renderOutline(btnX, btnY, 70, 20, 0xFF888888);
        gui.drawCenteredString(font, "← Назад", btnX + 35, btnY + 6, 0xFFFFFF);

        // Заголовок
        gui.drawCenteredString(font, "§6§l" + selectedMap.name,
                guiLeft + guiWidth / 2, guiTop + 30, 0xFFFFFF);

        // ИСПРАВЛЕНО: Область с прокруткой для контента
        gui.enableScissor(guiLeft + 5, guiTop + 45, guiLeft + guiWidth - 5, guiTop + guiHeight - 30);

        int y = guiTop + 50 - detailScrollOffset;
        int maxWidth = guiWidth - 30;

        // Превью карты (большое)
        int previewSize = 150;
        renderMapPreview(gui, selectedMap, guiLeft + (guiWidth - previewSize) / 2, y, previewSize, 100);
        y += 110;

        // Описание
        gui.drawString(font, selectedMap.description, guiLeft + 15, y, 0xFFFFFF, false);
        y += 15;

        // Детали
        for (String detail : selectedMap.details) {
            gui.drawString(font, detail, guiLeft + 15, y, 0xFFFFFF, false);
            y += 11;
        }

        y += 10; // Отступ после текста

        gui.disableScissor();

        // ИСПРАВЛЕНО: Индикатор прокрутки
        int totalHeight = 110 + 15 + (selectedMap.details.length * 11) + 10;
        int visibleHeight = guiHeight - 75; // Высота видимой области

        if (totalHeight > visibleHeight) {
            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 80,
                    guiTop + guiHeight - 30, 0x666666, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Кнопка "Назад на главную"
            if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 85 && mouseY >= guiTop + 5 && mouseY < guiTop + 23) {
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

                for (MapInfo map : maps) {
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
            int totalHeight = 110 + 15 + (selectedMap.details.length * 11) + 10;
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

    private static class MapInfo {
        String name;
        String id;
        String description;
        String[] details;

        MapInfo(String name, String id, String description, String[] details) {
            this.name = name;
            this.id = id;
            this.description = description;
            this.details = details;
        }
    }
}