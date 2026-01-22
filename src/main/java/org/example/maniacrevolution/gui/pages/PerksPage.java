package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkRegistry;

import java.util.ArrayList;
import java.util.List;

public class PerksPage extends GuidePage {
    private Chapter currentChapter = Chapter.COMMON;
    private int scrollOffset = 0;
    private Perk hoveredPerk = null;
    private Perk selectedPerk = null;

    public PerksPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        // Сбрасываем состояние при переключении
        selectedPerk = null;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Кнопка "Назад на главную"
        renderBackButton(gui, mouseX, mouseY);

        if (selectedPerk != null) {
            renderPerkDetails(gui, mouseX, mouseY);
        } else {
            renderPerkList(gui, mouseX, mouseY);
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

    private void renderPerkList(GuiGraphics gui, int mouseX, int mouseY) {
        // Заголовок
        gui.drawCenteredString(font, "§6§lПерки и способности", guiLeft + guiWidth / 2, guiTop + 28, 0xFFFFFF);

        // Кнопки категорий
        int btnY = guiTop + 45;
        int btnWidth = 90;
        int btnHeight = 18;
        int spacing = 5;
        int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

        renderCategoryButton(gui, mouseX, mouseY, startX, btnY, btnWidth, btnHeight, "§fОбщие", Chapter.COMMON);
        renderCategoryButton(gui, mouseX, mouseY, startX + btnWidth + spacing, btnY, btnWidth, btnHeight, "§aВыжившие", Chapter.SURVIVORS);
        renderCategoryButton(gui, mouseX, mouseY, startX + (btnWidth + spacing) * 2, btnY, btnWidth, btnHeight, "§cМаньяки", Chapter.MANIACS);

        // Список перков
        List<Perk> perks = getPerksForChapter();
        hoveredPerk = null;

        int y = guiTop + 70 - scrollOffset;
        int entryHeight = 35;

        gui.enableScissor(guiLeft + 5, guiTop + 68, guiLeft + guiWidth - 5, guiTop + guiHeight - 10);

        for (Perk perk : perks) {
            if (y + entryHeight > guiTop + 65 && y < guiTop + guiHeight - 10) {
                boolean hovered = mouseX >= guiLeft + 10 && mouseX < guiLeft + guiWidth - 10
                        && mouseY >= y && mouseY < y + entryHeight - 2
                        && mouseY >= guiTop + 68 && mouseY < guiTop + guiHeight - 10;

                if (hovered) hoveredPerk = perk;

                renderPerkEntry(gui, perk, guiLeft + 10, y, hovered);
            }
            y += entryHeight;
        }

        gui.disableScissor();

        // Подсказка
        if (perks.size() > 5) {
            gui.drawString(font, "§8Прокрутка: колёсико мыши", guiLeft + guiWidth - 140,
                    guiTop + guiHeight - 12, 0x666666, false);
        }
    }

    private void renderCategoryButton(GuiGraphics gui, int mouseX, int mouseY, int x, int y, int width, int height, String text, Chapter chapter) {
        boolean selected = currentChapter == chapter;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int bgColor = selected ? 0xFF444444 : (hovered ? 0xFF3a3a3a : 0xFF2a2a2a);
        gui.fill(x, y, x + width, y + height, bgColor);
        gui.renderOutline(x, y, width, height, selected ? 0xFFFFAA00 : 0xFF666666);
        gui.drawCenteredString(font, text, x + width / 2, y + 5, 0xFFFFFF);
    }

    private void renderPerkEntry(GuiGraphics gui, Perk perk, int x, int y, boolean hovered) {
        int width = guiWidth - 20;
        int height = 33;

        gui.fill(x, y, x + width, y + height, hovered ? 0xAA444444 : 0x80333333);
        if (hovered) gui.renderOutline(x, y, width, height, 0xFFFFAA00);

        // Иконка
        renderPerkIcon(gui, perk, x + 3, y + 3, 28);

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
        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/perks/" + perk.getId() + ".png");

        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, size, size, size, size);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            String initial = perk.getId().substring(0, 2).toUpperCase();
            gui.drawCenteredString(font, initial, x + size / 2, y + size / 2 - 4, 0xFFFFFF);
        }
    }

    private void renderPerkDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "Назад к списку"
        int btnX = guiLeft + 5;
        int btnY = guiTop + guiHeight - 25;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 20;

        gui.fill(btnX, btnY, btnX + 70, btnY + 20, hovered ? 0xFF555555 : 0xFF333333);
        gui.renderOutline(btnX, btnY, 70, 20, 0xFF888888);
        gui.drawCenteredString(font, "← Назад", btnX + 35, btnY + 6, 0xFFFFFF);

        // Заголовок
        gui.drawCenteredString(font, "§6§l" + selectedPerk.getName().getString(),
                guiLeft + guiWidth / 2, guiTop + 30, 0xFFFFFF);

        int x = guiLeft + 15;
        int y = guiTop + 50;
        int maxWidth = guiWidth - 30;

        // Информация
        gui.drawString(font, "§7Тип: §f" + selectedPerk.getType().getDisplayName().getString(), x, y, 0xFFFFFF, false);
        y += 14;

        gui.drawString(font, "§7Команда: §f" + selectedPerk.getTeam().getDisplayName().getString(), x, y, 0xFFFFFF, false);
        y += 14;

        gui.drawString(font, "§7Фазы: §f" + getPhasesString(selectedPerk), x, y, 0xFFFFFF, false);
        y += 14;

        if (selectedPerk.getCooldownTicks() > 0) {
            gui.drawString(font, "§cКулдаун: " + (selectedPerk.getCooldownTicks() / 20) + " секунд", x, y, 0xFFFFFF, false);
            y += 14;
        }

        y += 8;
        gui.fill(x, y, x + maxWidth, y + 1, 0xFF555555);
        y += 10;

        gui.drawString(font, "§e§lОписание:", x, y, 0xFFFFFF, false);
        y += 14;

        String description = selectedPerk.getDescription().getString();
        List<String> lines = wrapText(description, maxWidth);

        for (String line : lines) {
            gui.drawString(font, "§f" + line, x, y, 0xFFFFFF, false);
            y += 11;
        }

        y += 10;
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

    @Override
    public void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (selectedPerk == null && hoveredPerk != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredPerk.getName().getString()));
            tooltip.add(Component.literal("§7Нажмите для подробностей").withStyle(ChatFormatting.ITALIC));
            gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
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
            if (selectedPerk != null) {
                if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 75 && mouseY >= guiTop + guiHeight - 25 && mouseY < guiTop + guiHeight - 5) {
                    selectedPerk = null;
                    return true;
                }
            }

            // Кнопки категорий
            if (selectedPerk == null) {
                int btnY = guiTop + 45;
                int btnWidth = 90;
                int spacing = 5;
                int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

                if (mouseY >= btnY && mouseY < btnY + 18) {
                    if (mouseX >= startX && mouseX < startX + btnWidth) {
                        currentChapter = Chapter.COMMON;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + btnWidth + spacing && mouseX < startX + (btnWidth + spacing) * 2) {
                        currentChapter = Chapter.SURVIVORS;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + (btnWidth + spacing) * 2 && mouseX < startX + (btnWidth + spacing) * 3) {
                        currentChapter = Chapter.MANIACS;
                        scrollOffset = 0;
                        return true;
                    }
                }
            }

            // Клик на перк
            if (hoveredPerk != null && selectedPerk == null) {
                selectedPerk = hoveredPerk;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedPerk != null) return false;

        List<Perk> perks = getPerksForChapter();
        int maxScroll = Math.max(0, perks.size() * 35 - 150);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 25));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedPerk != null && keyCode == 256) { // ESC
            selectedPerk = null;
            return true;
        }
        return false;
    }

    private List<Perk> getPerksForChapter() {
        return switch (currentChapter) {
            case COMMON -> PerkRegistry.getCommonPerks();
            case SURVIVORS -> PerkRegistry.getSurvivorPerks();
            case MANIACS -> PerkRegistry.getManiacPerks();
        };
    }

    private enum Chapter { COMMON, SURVIVORS, MANIACS }
}