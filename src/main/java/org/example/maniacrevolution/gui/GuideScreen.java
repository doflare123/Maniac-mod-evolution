package org.example.maniacrevolution.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.example.maniacrevolution.guide.GuideProgressClient;
import org.example.maniacrevolution.gui.pages.CharactersPage;
import org.example.maniacrevolution.gui.pages.GuidePage;
import org.example.maniacrevolution.gui.pages.MainPage;
import org.example.maniacrevolution.gui.pages.MapsPage;
import org.example.maniacrevolution.gui.pages.PerksPage;
import org.example.maniacrevolution.gui.pages.TutorialPage;

public class GuideScreen extends Screen {
    private static final int MAX_WIDTH = 600;
    private static final int MAX_HEIGHT = 350;

    private GuidePage currentPage;
    private GuidePage.PageType currentPageType;
    private int guiLeft;
    private int guiTop;
    private int guiWidth;
    private int guiHeight;

    private final MainPage mainPage;
    private final PerksPage perksPage;
    private final TutorialPage tutorialPage;
    private final MapsPage mapsPage;
    private final CharactersPage charactersPage;

    private long openedAt;
    private long pageChangedAt;

    public GuideScreen() {
        this(null);
    }

    public GuideScreen(GuidePage.PageType initialPage) {
        super(Component.literal("Гайд по режиму"));
        GuideProgressClient.markCurrentGuideSeen();

        mainPage = new MainPage(this);
        perksPage = new PerksPage(this);
        tutorialPage = new TutorialPage(this);
        mapsPage = new MapsPage(this);
        charactersPage = new CharactersPage(this);

        currentPageType = initialPage != null ? initialPage : GuidePage.PageType.MAIN;
        currentPage = getPageByType(currentPageType);
    }

    @Override
    protected void init() {
        guiWidth = Math.min(MAX_WIDTH, Math.max(320, width - 20));
        guiHeight = Math.min(MAX_HEIGHT, Math.max(240, height - 16));
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;

        currentPage.init(guiLeft, guiTop, guiWidth, guiHeight);
        if (openedAt == 0L) {
            openedAt = System.currentTimeMillis();
            pageChangedAt = openedAt;
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float intro = easeOutCubic(Mth.clamp((now - openedAt) / 260.0f, 0.0f, 1.0f));
        int animatedTop = guiTop + Math.round((1.0f - intro) * 10.0f);
        int offsetY = animatedTop - guiTop;

        renderBackdrop(gui, now);
        gui.pose().pushPose();
        gui.pose().translate(0.0f, offsetY, 0.0f);
        renderShell(gui);

        float pageIntro = easeOutCubic(Mth.clamp((now - pageChangedAt) / 180.0f, 0.0f, 1.0f));
        gui.pose().pushPose();
        gui.pose().translate(Math.round((1.0f - pageIntro) * 7.0f), 0.0f, 0.0f);
        currentPage.render(gui, mouseX, mouseY - offsetY, partialTick);
        gui.pose().popPose();

        renderCloseButton(gui, mouseX, mouseY - offsetY);
        gui.pose().popPose();

        super.render(gui, mouseX, mouseY, partialTick);
        currentPage.renderTooltip(gui, mouseX, mouseY - offsetY);
    }

    private void renderBackdrop(GuiGraphics gui, long now) {
        renderBackground(gui);
        gui.fillGradient(0, 0, width, height, 0xD915202A, 0xE0263948);
        int accent = GuideTheme.accent(currentPageType);
        for (int i = 0; i < 16; i++) {
            int x = Math.floorMod(i * 79 + (int) (now / (25L + i % 3 * 8L)), Math.max(1, width + 30)) - 15;
            int y = Math.floorMod(i * 43 + Math.round((float) Math.sin(now / 950.0 + i) * 11.0f),
                    Math.max(1, height));
            gui.fill(x, y, x + (i % 5 == 0 ? 2 : 1), y + 1, (24 + i % 4 * 7) << 24 | accent & 0xFFFFFF);
        }
        for (int y = 2; y < height; y += 4) {
            gui.fill(0, y, width, y + 1, 0x08000000);
        }
    }

    private void renderShell(GuiGraphics gui) {
        int right = guiLeft + guiWidth;
        int bottom = guiTop + guiHeight;
        int accent = GuideTheme.accent(currentPageType);

        gui.fill(guiLeft - 7, guiTop + 7, right + 7, bottom + 9, 0x78000000);
        gui.fill(guiLeft - 3, guiTop - 3, right + 3, bottom + 3, 0xD9111820);
        gui.fill(guiLeft, guiTop, right, bottom, GuideTheme.PANEL);
        gui.renderOutline(guiLeft, guiTop, guiWidth, guiHeight, GuideTheme.BORDER);
        gui.fill(guiLeft + 1, guiTop + 1, right - 1, guiTop + 3, accent);
        gui.fill(guiLeft + 1, guiTop + 3, right - 1, guiTop + 35, GuideTheme.HEADER);
        gui.fill(guiLeft + 1, guiTop + 35, right - 1, guiTop + 36, GuideTheme.BORDER_SOFT);

        drawCorner(gui, guiLeft - 2, guiTop - 2, 1, 1, accent);
        drawCorner(gui, right + 2, guiTop - 2, -1, 1, accent);
        drawCorner(gui, guiLeft - 2, bottom + 2, 1, -1, accent);
        drawCorner(gui, right + 2, bottom + 2, -1, -1, accent);
    }

    private void renderCloseButton(GuiGraphics gui, int mouseX, int mouseY) {
        int x = guiLeft + guiWidth - 27;
        int y = guiTop + 10;
        boolean hovered = GuideTheme.inside(mouseX, mouseY, x, y, 18, 18);
        GuideTheme.drawButton(gui, font, x, y, 18, 18, "×", GuideTheme.RED, hovered, false);
    }

    private void drawCorner(GuiGraphics gui, int x, int y, int directionX, int directionY, int color) {
        gui.fill(Math.min(x, x + directionX * 7), y,
                Math.max(x, x + directionX * 7) + 1, y + 1, color);
        gui.fill(x, Math.min(y, y + directionY * 7),
                x + 1, Math.max(y, y + directionY * 7) + 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int closeX = guiLeft + guiWidth - 27;
        int closeY = guiTop + 10;
        if (button == 0 && GuideTheme.inside(mouseX, mouseY, closeX, closeY, 18, 18)) {
            playClick(0.9f);
            onClose();
            return true;
        }
        if (currentPage.mouseClicked(mouseX, mouseY, button)) {
            playClick(1.05f);
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

    public void switchPage(GuidePage.PageType pageType) {
        currentPageType = pageType;
        currentPage = getPageByType(pageType);
        currentPage.init(guiLeft, guiTop, guiWidth, guiHeight);
        pageChangedAt = System.currentTimeMillis();
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

    private void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0f - value;
        return 1.0f - inverse * inverse * inverse;
    }

    public int getGuiLeft() { return guiLeft; }
    public int getGuiTop() { return guiTop; }
    public int getGuiWidth() { return guiWidth; }
    public int getGuiHeight() { return guiHeight; }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
