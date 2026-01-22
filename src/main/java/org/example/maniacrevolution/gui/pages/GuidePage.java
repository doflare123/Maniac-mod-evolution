package org.example.maniacrevolution.gui.pages;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;

public abstract class GuidePage {
    protected final GuideScreen parent;
    protected final Font font;
    protected int guiLeft, guiTop, guiWidth, guiHeight;

    public GuidePage(GuideScreen parent) {
        this.parent = parent;
        this.font = Minecraft.getInstance().font;
    }

    public enum PageType {
        MAIN,
        PERKS,
        TUTORIAL,
        MAPS
    }

    /**
     * Инициализация страницы (вызывается при переключении)
     */
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
    }

    /**
     * Рендеринг страницы
     */
    public abstract void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick);

    /**
     * Рендеринг тултипов
     */
    public void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        // По умолчанию ничего
    }

    /**
     * Обработка клика мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Обработка скролла
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /**
     * Обработка клавиш
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Утилита для переноса текста
     */
    protected List<String> wrapText(String text, int maxWidth) {
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
}