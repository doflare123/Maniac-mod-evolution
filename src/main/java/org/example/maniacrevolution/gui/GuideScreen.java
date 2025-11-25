package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkRegistry;

import java.util.ArrayList;
import java.util.List;

public class GuideScreen extends Screen {
    private Chapter currentChapter = Chapter.COMMON;
    private int scrollOffset = 0;
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 220;

    // Для отслеживания наведения
    private Perk hoveredPerk = null;

    // ФИКС: Выбранный перк для детального просмотра
    private Perk selectedPerk = null;

    public GuideScreen() {
        super(Component.literal("Гайд по перкам"));
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // Кнопки глав
        int btnY = guiTop + 5;
        addRenderableWidget(Button.builder(Component.literal("Общие"), b -> switchChapter(Chapter.COMMON))
                .pos(guiLeft + 5, btnY).size(60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Выжившие"), b -> switchChapter(Chapter.SURVIVORS))
                .pos(guiLeft + 70, btnY).size(70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Маньяки"), b -> switchChapter(Chapter.MANIACS))
                .pos(guiLeft + 145, btnY).size(60, 18).build());

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 20, guiTop + 5).size(15, 15).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a1a);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF666666);

        if (selectedPerk != null) {
            // ФИКС: Режим детального просмотра
            renderPerkDetails(gui, mouseX, mouseY);
        } else {
            // Режим списка
            renderPerkList(gui, mouseX, mouseY);
        }

        super.render(gui, mouseX, mouseY, partialTick);

        // Тултип при наведении (только в режиме списка)
        if (selectedPerk == null && hoveredPerk != null) {
            renderHoverTooltip(gui, mouseX, mouseY);
        }
    }

    private void renderPerkList(GuiGraphics gui, int mouseX, int mouseY) {
        // Заголовок главы
        String chapterTitle = switch (currentChapter) {
            case COMMON -> "§6Общие перки";
            case SURVIVORS -> "§aПерки выживших";
            case MANIACS -> "§cПерки маньяков";
        };
        gui.drawCenteredString(font, chapterTitle, guiLeft + GUI_WIDTH / 2, guiTop + 28, 0xFFFFFF);
        gui.drawCenteredString(font, "§7(Нажмите на перк для подробностей)",
                guiLeft + GUI_WIDTH / 2, guiTop + 40, 0x888888);

        List<Perk> perks = getPerksForChapter();

        hoveredPerk = null;

        int y = guiTop + 55 - scrollOffset;
        int entryHeight = 35;

        // Область скролла
        gui.enableScissor(guiLeft + 5, guiTop + 50, guiLeft + GUI_WIDTH - 5, guiTop + GUI_HEIGHT - 10);

        for (Perk perk : perks) {
            if (y + entryHeight > guiTop + 45 && y < guiTop + GUI_HEIGHT - 10) {
                boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + GUI_WIDTH - 10
                        && mouseY >= y && mouseY < y + entryHeight - 2
                        && mouseY >= guiTop + 50 && mouseY < guiTop + GUI_HEIGHT - 10;

                if (hovered) {
                    hoveredPerk = perk;
                }

                renderPerkEntry(gui, perk, guiLeft + 10, y, hovered);
            }
            y += entryHeight;
        }

        gui.disableScissor();

        // Подсказка по скроллу
        if (perks.size() > 4) {
            gui.drawString(font, "§8↑↓ Колёсико мыши", guiLeft + GUI_WIDTH - 110,
                    guiTop + GUI_HEIGHT - 15, 0x666666, false);
        }
    }

    private void renderPerkEntry(GuiGraphics gui, Perk perk, int x, int y, boolean hovered) {
        int width = GUI_WIDTH - 20;
        int height = 33;

        // Фон
        int bgColor = hovered ? 0xAA444444 : 0x80333333;
        gui.fill(x, y, x + width, y + height, bgColor);

        if (hovered) {
            gui.renderOutline(x, y, width, height, 0xFFFFAA00);
        }

        // Иконка типа
        renderPerkIcon(gui, perk, x + 3, y + 3, 28);
//        int typeColor = switch (perk.getType()) {
//            case PASSIVE -> 0xFF5555FF;
//            case ACTIVE -> 0xFFFF5555;
//            case HYBRID -> 0xFFFF55FF;
//        };
//        gui.fill(x + 3, y + 3, x + 28, y + 28, typeColor);
//        String typeChar = switch (perk.getType()) {
//            case PASSIVE -> "П";
//            case ACTIVE -> "А";
//            case HYBRID -> "Г";
//        };
//        gui.drawCenteredString(font, typeChar, x + 15, y + 11, 0xFFFFFF);

        // Название
        gui.drawString(font, "§f" + perk.getName().getString(), x + 35, y + 5, 0xFFFFFF, false);

        // Тип и команда
        String info = perk.getType().getDisplayName().getString() + " | " + perk.getTeam().getDisplayName().getString();
        gui.drawString(font, "§7" + info, x + 35, y + 17, 0xAAAAAA, false);

        // КД
        if (perk.getCooldownTicks() > 0) {
            String cd = "КД: " + (perk.getCooldownTicks() / 20) + "с";
            gui.drawString(font, "§c" + cd, x + width - font.width(cd) - 5, y + 5, 0xFF5555, false);
        }
    }

    private void renderPerkIcon(GuiGraphics gui, Perk perk, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/perks/" + perk.getId() + ".png");

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, size, size, size, size);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            // Заглушка
            String initial = perk.getId().substring(0, 2).toUpperCase();
            gui.drawCenteredString(font, initial, x + size / 2, y + size / 2 - 4, 0xFFFFFF);
        }
    }

    // ФИКС: Новый метод для детального просмотра
    private void renderPerkDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка назад
        if (mouseX >= guiLeft + 5 && mouseX <= guiLeft + 70
                && mouseY >= guiTop + GUI_HEIGHT - 25 && mouseY <= guiTop + GUI_HEIGHT - 5) {
            gui.fill(guiLeft + 5, guiTop + GUI_HEIGHT - 25, guiLeft + 70, guiTop + GUI_HEIGHT - 5, 0xFF555555);
        } else {
            gui.fill(guiLeft + 5, guiTop + GUI_HEIGHT - 25, guiLeft + 70, guiTop + GUI_HEIGHT - 5, 0xFF333333);
        }
        gui.renderOutline(guiLeft + 5, guiTop + GUI_HEIGHT - 25, 65, 20, 0xFF888888);
        gui.drawCenteredString(font, "← Назад", guiLeft + 37, guiTop + GUI_HEIGHT - 20, 0xFFFFFF);

        // Заголовок
        gui.drawCenteredString(font, "§6§l" + selectedPerk.getName().getString(),
                guiLeft + GUI_WIDTH / 2, guiTop + 30, 0xFFFFFF);

        int x = guiLeft + 15;
        int y = guiTop + 50;
        int maxWidth = GUI_WIDTH - 30;

        // Тип
        gui.drawString(font, "§7Тип: ", x, y, 0xFFFFFF, false);
        gui.drawString(font, selectedPerk.getType().getDisplayName().getString(),
                x + font.width("Тип: "), y, 0xFFFFFF, false);
        y += 14;

        // Команда
        gui.drawString(font, "§7Команда: ", x, y, 0xFFFFFF, false);
        gui.drawString(font, selectedPerk.getTeam().getDisplayName().getString(),
                x + font.width("Команда: "), y, 0xFFFFFF, false);
        y += 14;

        // Фазы
        gui.drawString(font, "§7Фазы: " + getPhasesString(selectedPerk), x, y, 0xFFFFFF, false);
        y += 14;

        // Кулдаун
        if (selectedPerk.getCooldownTicks() > 0) {
            gui.drawString(font, "§cКулдаун: " + (selectedPerk.getCooldownTicks() / 20) + " секунд",
                    x, y, 0xFFFFFF, false);
            y += 14;
        }

        y += 8;

        // Разделитель
        gui.fill(x, y, x + maxWidth, y + 1, 0xFF555555);
        y += 10;

        // ФИКС: Полное описание с переносом строк
        gui.drawString(font, "§e§lОписание:", x, y, 0xFFFFFF, false);
        y += 14;

        String description = selectedPerk.getDescription().getString();
        List<String> lines = wrapText(description, maxWidth);

        for (String line : lines) {
            gui.drawString(font, "§f" + line, x, y, 0xFFFFFF, false);
            y += 11;
        }

        y += 10;

        // Подсказка по типу перка
        renderTypeHint(gui, x, y);
    }

    private void renderTypeHint(GuiGraphics gui, int x, int y) {
        String keyName;
        try {
            keyName = ModKeybinds.ACTIVATE_PERK.getTranslatedKeyMessage().getString();
        } catch (Exception e) {
            keyName = "R";
        }

        switch (selectedPerk.getType()) {
            case PASSIVE -> {
                gui.drawString(font, "§9ℹ Пассивный перк", x, y, 0x5555FF, false);
                gui.drawString(font, "§7Эффект работает автоматически", x, y + 11, 0xAAAAAA, false);
            }
            case ACTIVE -> {
                gui.drawString(font, "§cℹ Активный перк", x, y, 0xFF5555, false);
                gui.drawString(font, "§7Нажмите [" + keyName + "] для активации", x, y + 11, 0xAAAAAA, false);
            }
            case HYBRID -> {
                gui.drawString(font, "§dℹ Гибридный перк", x, y, 0xFF55FF, false);
                gui.drawString(font, "§7Пассивный эффект + активация [" + keyName + "]", x, y + 11, 0xAAAAAA, false);
            }
        }
    }

    // ФИКС: Метод для переноса текста
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String test = line.length() > 0 ? line + " " + word : word;
            if (font.width(test) > maxWidth) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            } else {
                line = new StringBuilder(test);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    private String getPhasesString(Perk perk) {
        if (perk.getActivePhases().contains(PerkPhase.ANY)) {
            return "§aЛюбая";
        }
        StringBuilder sb = new StringBuilder();
        for (PerkPhase phase : perk.getActivePhases()) {
            if (sb.length() > 0) sb.append("§7, ");
            sb.append(phase.getDisplayName().getString());
        }
        return sb.toString();
    }

    private void renderHoverTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("§e" + hoveredPerk.getName().getString()));
        tooltip.add(Component.literal("§7Нажмите для подробностей").withStyle(ChatFormatting.ITALIC));
        gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private List<Perk> getPerksForChapter() {
        return switch (currentChapter) {
            case COMMON -> PerkRegistry.getCommonPerks();
            case SURVIVORS -> PerkRegistry.getSurvivorPerks();
            case MANIACS -> PerkRegistry.getManiacPerks();
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Клик на кнопку "Назад"
            if (selectedPerk != null) {
                if (mouseX >= guiLeft + 5 && mouseX <= guiLeft + 70
                        && mouseY >= guiTop + GUI_HEIGHT - 25 && mouseY <= guiTop + GUI_HEIGHT - 5) {
                    selectedPerk = null;
                    return true;
                }
            }

            // Клик на перк в списке
            if (hoveredPerk != null && selectedPerk == null) {
                selectedPerk = hoveredPerk;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedPerk != null) return false;

        List<Perk> perks = getPerksForChapter();
        int maxScroll = Math.max(0, perks.size() * 35 - 140);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 25));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC закрывает детальный просмотр, а не весь экран
        if (selectedPerk != null && keyCode == 256) {
            selectedPerk = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void switchChapter(Chapter chapter) {
        currentChapter = chapter;
        scrollOffset = 0;
        selectedPerk = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Chapter { COMMON, SURVIVORS, MANIACS }
}
