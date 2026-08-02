package org.example.maniacrevolution.gui.pages;

import net.minecraft.client.gui.GuiGraphics;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.GuideTheme;

import java.util.ArrayList;
import java.util.List;

public class MainPage extends GuidePage {
    private final List<MenuCard> cards = new ArrayList<>();

    public MainPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        cards.clear();

        int gap = 10;
        int margin = 24;
        int cardWidth = (guiWidth - margin * 2 - gap) / 2;
        int cardHeight = Math.max(70, Math.min(112, (guiHeight - 104) / 2));
        int startY = guiTop + 62;
        int leftX = guiLeft + margin;
        int rightX = leftX + cardWidth + gap;
        int secondY = startY + cardHeight + gap;

        cards.add(new MenuCard("01", tr("guide.maniacrev.main.tutorial.title"), tr("guide.maniacrev.main.tutorial.desc"),
                leftX, startY, cardWidth, cardHeight, GuideTheme.GOLD, PageType.TUTORIAL));
        cards.add(new MenuCard("02", tr("guide.maniacrev.main.characters.title"), tr("guide.maniacrev.main.characters.desc"),
                rightX, startY, cardWidth, cardHeight, GuideTheme.PURPLE, PageType.CHARACTERS));
        cards.add(new MenuCard("03", tr("guide.maniacrev.main.perks.title"), tr("guide.maniacrev.main.perks.desc"),
                leftX, secondY, cardWidth, cardHeight, GuideTheme.GREEN, PageType.PERKS));
        cards.add(new MenuCard("04", tr("guide.maniacrev.main.maps.title"), tr("guide.maniacrev.main.maps.desc"),
                rightX, secondY, cardWidth, cardHeight, GuideTheme.RED, PageType.MAPS));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        GuideTheme.drawPageTitle(gui, font, tr("guide.maniacrev.main.title"),
                tr("guide.maniacrev.main.subtitle"),
                guiLeft + guiWidth / 2, guiTop + 10, GuideTheme.GOLD);

        for (MenuCard card : cards) {
            card.render(gui, mouseX, mouseY);
        }

        String hint = tr("guide.maniacrev.main.hint");
        gui.drawCenteredString(font, hint, guiLeft + guiWidth / 2,
                guiTop + guiHeight - 17, GuideTheme.TEXT_MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuCard card : cards) {
                if (card.isHovered(mouseX, mouseY)) {
                    parent.switchPage(card.targetPage);
                    return true;
                }
            }
        }
        return false;
    }

    private final class MenuCard {
        private final String number;
        private final String title;
        private final String subtitle;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int accent;
        private final PageType targetPage;
        private float hoverProgress;

        private MenuCard(String number, String title, String subtitle,
                         int x, int y, int width, int height, int accent, PageType targetPage) {
            this.number = number;
            this.title = title;
            this.subtitle = subtitle;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.accent = accent;
            this.targetPage = targetPage;
        }

        private boolean isHovered(double mouseX, double mouseY) {
            return GuideTheme.inside(mouseX, mouseY, x, y, width, height);
        }

        private void render(GuiGraphics gui, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY);
            hoverProgress += ((hovered ? 1.0f : 0.0f) - hoverProgress) * 0.22f;
            int hoverAccent = GuideTheme.lerpColor(accent, 0xFF000000, 0.22f);
            int animatedAccent = GuideTheme.lerpColor(accent, hoverAccent, hoverProgress);

            GuideTheme.drawCard(gui, x, y, width, height, accent, hovered);
            gui.fill(x + 12, y + 13, x + 42, y + 43, 0xFF222A33);
            gui.renderOutline(x + 12, y + 13, 30, 30, animatedAccent);
            gui.drawCenteredString(font, number, x + 27, y + 24, animatedAccent);

            int textX = x + 52;
            int textWidth = width - 64;
            gui.drawString(font, title, textX, y + 14, GuideTheme.TEXT, false);
            gui.fill(textX, y + 28, textX + Math.min(44, font.width(title)), y + 29, animatedAccent);

            List<String> lines = wrapText(subtitle, textWidth);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                gui.drawString(font, lines.get(i), textX, y + 37 + i * 11,
                        GuideTheme.TEXT_SECONDARY, false);
            }

            String action = hovered ? tr("guide.maniacrev.open") + "  →" : "→";
            gui.drawString(font, action, x + width - font.width(action) - 12,
                    y + height - 17, animatedAccent, false);
        }
    }
}
