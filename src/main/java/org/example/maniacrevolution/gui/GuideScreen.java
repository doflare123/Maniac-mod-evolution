package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.pages.*;

public class GuideScreen extends Screen {
    private GuidePage currentPage;
    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 260;

    // Страницы
    private final MainPage mainPage;
    private final PerksPage perksPage;
    private final TutorialPage tutorialPage;
    private final MapsPage mapsPage;
    private final CharactersPage charactersPage;

    public GuideScreen() {
        this(null);
    }

    public GuideScreen(GuidePage.PageType initialPage) {
        super(Component.literal("Гайд по режиму"));

        // Инициализируем страницы
        this.mainPage = new MainPage(this);
        this.perksPage = new PerksPage(this);
        this.tutorialPage = new TutorialPage(this);
        this.mapsPage = new MapsPage(this);
        this.charactersPage = new CharactersPage(this);

        // Устанавливаем начальную страницу
        if (initialPage != null) {
            this.currentPage = getPageByType(initialPage);
        } else {
            this.currentPage = mainPage;
        }
    }

    @Override
    protected void init() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        clearWidgets();

        // Кнопка закрытия
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .pos(guiLeft + GUI_WIDTH - 22, guiTop + 3).size(18, 18).build());

        // Инициализируем текущую страницу
        currentPage.init(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        // Основной фон
        gui.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a1a);
        gui.renderOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF666666);

        // Рендерим текущую страницу
        currentPage.render(gui, mouseX, mouseY, partialTick);

        super.render(gui, mouseX, mouseY, partialTick);

        // Тултипы от страницы
        currentPage.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentPage.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentPage.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentPage.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Переключает страницу
     */
    public void switchPage(GuidePage.PageType pageType) {
        this.currentPage = getPageByType(pageType);
        init(); // Пересоздаем виджеты
    }

    private GuidePage getPageByType(GuidePage.PageType type) {
        return switch (type) {
            case MAIN -> mainPage;
            case PERKS -> perksPage;
            case TUTORIAL -> tutorialPage;
            case MAPS -> mapsPage;
            case CHARACTERS -> charactersPage;
        };
    }

    public int getGuiLeft() { return guiLeft; }
    public int getGuiTop() { return guiTop; }
    public int getGuiWidth() { return GUI_WIDTH; }
    public int getGuiHeight() { return GUI_HEIGHT; }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}