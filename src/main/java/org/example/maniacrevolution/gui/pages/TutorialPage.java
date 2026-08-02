package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.GuideTheme;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.hack.HackConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Полный гайд по режиму.
 *
 * Структура:
 *   - При открытии показывается оглавление (список тем-кнопок)
 *   - Нажатие на тему открывает её содержимое (скроллируемый список секций)
 *   - Кнопка "← Назад" возвращает в оглавление (или на главную из оглавления)
 */
public class TutorialPage extends GuidePage {

    // ── Состояние ─────────────────────────────────────────────────────────────
    private Topic currentTopic = null;   // null = оглавление
    private int scrollOffset    = 0;  // скролл содержимого темы
    private int tocScrollOffset = 0;  // скролл оглавления
    private LinkSection hoveredLink = null;

    // ── Оглавление ────────────────────────────────────────────────────────────
    private final List<TopicButton> topicButtons = new ArrayList<>();

    public TutorialPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset    = 0;
        tocScrollOffset = 0;
        currentTopic    = null;
        buildTopicButtons();
    }

    private static final int TOC_BTN_H   = 32;
    private static final int TOC_BTN_GAP = 8;
    private static final int TOC_CLIP_TOP_OFFSET = 48; // отступ от guiTop до начала списка

    private void buildTopicButtons() {
        topicButtons.clear();
        int bW = guiWidth - 40;
        // Y хранится как индекс — реальный Y вычисляется при рендере с учётом скролла
        for (int i = 0; i < Topic.values().length; i++) {
            int logicalY = i * (TOC_BTN_H + TOC_BTN_GAP);
            topicButtons.add(new TopicButton(guiLeft + 20, logicalY, bW, TOC_BTN_H, Topic.values()[i]));
        }
    }

    private int tocTotalHeight() {
        return topicButtons.size() * (TOC_BTN_H + TOC_BTN_GAP);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (currentTopic == null) {
            renderTableOfContents(gui, mouseX, mouseY);
        } else {
            renderTopic(gui, mouseX, mouseY);
        }
    }

    // ── Оглавление ────────────────────────────────────────────────────────────

    private void renderTableOfContents(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "← Главная"
        renderNavButton(gui, mouseX, mouseY, tr("guide.maniacrev.back_main"), guiLeft + 5, guiTop + 10, 80, 18);

        // Заголовок
        GuideTheme.drawPageTitle(gui, font, tr("guide.maniacrev.tutorial.title"),
                tr("guide.maniacrev.tutorial.subtitle"),
                guiLeft + guiWidth / 2, guiTop + 10, GuideTheme.GOLD);

        // Кнопки тем со скроллом
        int clipTop2    = guiTop + TOC_CLIP_TOP_OFFSET;
        int clipBottom2 = guiTop + guiHeight - 8;
        gui.enableScissor(guiLeft + 5, clipTop2, guiLeft + guiWidth - 5, clipBottom2);
        for (TopicButton btn : topicButtons) {
            int screenY = guiTop + TOC_CLIP_TOP_OFFSET + btn.y - tocScrollOffset;
            int screenYEnd = screenY + btn.h;
            if (screenYEnd > clipTop2 && screenY < clipBottom2) {
                btn.renderAt(gui, mouseX, mouseY, btn.x, screenY);
            }
        }
        gui.disableScissor();

        // Подсказка прокрутки если список не влезает
        int tocVisible = guiHeight - TOC_CLIP_TOP_OFFSET - 8;
        if (tocTotalHeight() > tocVisible) {
            GuideTheme.drawScrollHint(gui, font, guiLeft + guiWidth - 6,
                    guiTop + guiHeight - 5);
        }
    }

    // ── Содержимое темы ───────────────────────────────────────────────────────

    private void renderTopic(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "← Назад"
        renderNavButton(gui, mouseX, mouseY, tr("guide.maniacrev.back"), guiLeft + 5, guiTop + 10, 80, 18);

        // Заголовок темы
        GuideTheme.drawPageTitle(gui, font, currentTopic.title(), null,
                guiLeft + guiWidth / 2, guiTop + 11, GuideTheme.GOLD);

        // Область прокрутки
        int clipTop    = guiTop + 35;
        int clipBottom = guiTop + guiHeight - 15;
        gui.enableScissor(guiLeft + 5, clipTop, guiLeft + guiWidth - 5, clipBottom);

        int y        = clipTop + 5 - scrollOffset;
        int maxWidth = guiWidth - 30;

        List<Section> sections = currentTopic.buildSections();
        hoveredLink = null;
        for (Section s : sections) {
            int h = s.getHeight(maxWidth);
            if (y + h > clipTop && y < clipBottom) {
                s.render(gui, guiLeft + 15, y, maxWidth, mouseX, mouseY);
                if (s instanceof LinkSection ls) {
                    net.minecraft.client.gui.Font f = net.minecraft.client.Minecraft.getInstance().font;
                    if (mouseX >= guiLeft + 15 && mouseX < guiLeft + 15 + f.width(ls.text) + 4
                            && mouseY >= y && mouseY < y + 11
                            && mouseY >= clipTop && mouseY < clipBottom) {
                        hoveredLink = ls;
                    }
                }
            }
            y += h;
        }

        gui.disableScissor();

        // Подсказка прокрутки
        int totalH = sections.stream().mapToInt(s -> s.getHeight(guiWidth - 30)).sum();
        if (totalH > guiHeight - 50) {
            GuideTheme.drawScrollHint(gui, font, guiLeft + guiWidth - 6,
                    guiTop + guiHeight - 5);
        }
    }

    // ── Вспомогательный рендер кнопки ────────────────────────────────────────

    private void renderNavButton(GuiGraphics gui, int mouseX, int mouseY,
                                 String label, int x, int y, int w, int h) {
        boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        GuideTheme.drawButton(gui, font, x, y, w, h, label,
                GuideTheme.GOLD, hov, false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MOUSE / SCROLL
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int navX = guiLeft + 5, navY = guiTop + 10, navW = 80, navH = 18;

        if (mx >= navX && mx < navX + navW && my >= navY && my < navY + navH) {
            if (currentTopic == null) {
                parent.switchPage(PageType.MAIN);
            } else {
                currentTopic = null;
                scrollOffset = 0;
            }
            return true;
        }

        if (currentTopic == null) {
            int clipTop2    = guiTop + TOC_CLIP_TOP_OFFSET;
            int clipBottom2 = guiTop + guiHeight - 8;
            if (my >= clipTop2 && my < clipBottom2) {
                for (TopicButton btn : topicButtons) {
                    int screenY = guiTop + TOC_CLIP_TOP_OFFSET + btn.y - tocScrollOffset;
                    if (mx >= btn.x && mx < btn.x + btn.w
                            && my >= screenY && my < screenY + btn.h) {
                        currentTopic = btn.topic;
                        scrollOffset = 0;
                        return true;
                    }
                }
            }
        }

        // Клик по ссылке внутри темы
        if (hoveredLink != null) {
            parent.switchPage(hoveredLink.targetPage);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentTopic == null) {
            // Скролл оглавления
            int tocVisible = guiHeight - TOC_CLIP_TOP_OFFSET - 8;
            int maxScroll  = Math.max(0, tocTotalHeight() - tocVisible);
            tocScrollOffset = (int) Math.max(0, Math.min(maxScroll, tocScrollOffset - delta * 25));
            return true;
        }
        // Скролл содержимого темы
        int maxWidth  = guiWidth - 30;
        int totalH    = currentTopic.buildSections()
                .stream().mapToInt(s -> s.getHeight(maxWidth)).sum();
        int maxScroll = Math.max(0, totalH - (guiHeight - 50));
        scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && currentTopic != null) { // ESC
            currentTopic = null;
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ТЕМЫ (TOPICS)
    // ═════════════════════════════════════════════════════════════════════════

    private enum Topic {
        INTRO("intro"), HUD("hud"), BEFORE_START("before_start"), COMPUTERS("computers"),
        DOWNED("downed"), MAP_FEATURES("map_features"), PERKS("perks"), END("end");

        final String id;

        Topic(String id) { this.id = id; }

        String buttonLabel() { return t("guide.maniacrev.tutorial.topic." + id + ".button"); }
        String title() { return t("guide.maniacrev.tutorial.topic." + id + ".title"); }

        /** Строит список секций для данной темы. */
        List<Section> buildSections() {
            return switch (this) {
                case INTRO        -> buildIntro();
                case HUD          -> buildHud();
                case BEFORE_START -> buildBeforeStart();
                case COMPUTERS    -> buildComputers();
                case DOWNED       -> buildDowned();
                case MAP_FEATURES -> buildMapFeatures();
                case PERKS        -> buildPerks();
                case END          -> buildEnd();
            };
        }

        // ── ВВЕДЕНИЕ ─────────────────────────────────────────────────────────

        private static List<Section> buildIntro() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.welcome")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.intro.rules")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.teams")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.survivor_goal")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.maniac_goal")));
            s.add(new SpacerSection(12));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.intro.phases")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.phases_desc")));
            s.add(new SpacerSection(6));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.phase_1")));
            s.add(new SpacerSection(4));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.phase_2")));
            s.add(new SpacerSection(4));
            s.add(new TextSection(t("guide.maniacrev.tutorial.intro.phase_3")));
            return s;
        }

        // ── HUD ──────────────────────────────────────────────────────────────

        private static List<Section> buildHud() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(t("guide.maniacrev.tutorial.hud.overview")));
            s.add(new SpacerSection(5));
            s.add(new ImageSection("guide/before_start_game/custom_hud.png", 450, 150));
            s.add(new SpacerSection(5));
            s.add(new TextSection(t("guide.maniacrev.tutorial.hud.elements")));
            s.add(new SpacerSection(5));
            s.add(new ImageSection("guide/before_start_game/full_custom_hud.png", 450, 150));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.hud.mana_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.hud.mana_desc")));
            return s;
        }

        // ── ДО СТАРТА ────────────────────────────────────────────────────────

        private static List<Section> buildBeforeStart() {
            var s = new ArrayList<Section>();
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.map_title")));
            s.add(new ImageSection("guide/before_start_game/pick_map.png", 450, 400));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.map_desc")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.items_title")));
            s.add(new ImageSection("guide/before_start_game/items_for_game.png", 200, 150));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.items_desc")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.slot_perks", 1)));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.slot_class", 2)));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.slot_ready", 3)));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.ready_requirement")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.perks_title")));
            s.add(new ImageSection("guide/before_start_game/perks.png", 450, 300));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.perks_desc")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.character_title")));
            s.add(new ImageSection("guide/before_start_game/pick_hero.png", 450, 450));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.character_desc")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.ready_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.ready_desc")));
            s.add(new ImageSection("guide/before_start_game/start_game.png", 450, 50));
            s.add(new SpacerSection(14));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.before.spawns_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.mansion")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.survivors")));
            s.add(new ImageSection("guide/start_game/start_survivors_mansion.png", 500, 300));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.maniacs")));
            s.add(new ImageSection("guide/start_game/start_maniac_mansion.png", 500, 300));
            s.add(new SpacerSection(10));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.pizzeria")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.survivors")));
            s.add(new ImageSection("guide/start_game/start_survivors_freddy.png", 500, 300));
            s.add(new TextSection(t("guide.maniacrev.tutorial.before.maniacs")));
            s.add(new ImageSection("guide/start_game/start_maniac_freddy.png", 500, 300));
            return s;
        }

        // ── КОМПЬЮТЕРЫ ───────────────────────────────────────────────────────

        private static List<Section> buildComputers() {
            var s = new ArrayList<Section>();
            s.add(new ImageSection("guide/maps/computers_watch.png", 550, 300));
            s.add(new SpacerSection(6));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.start_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.start_desc", HackConfig.HACKER_RADIUS)));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.help_title")));
            s.add(new ImageSection("guide/maps/active_computer.png", 450, 250));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.help_desc", HackConfig.SUPPORT_RADIUS)));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.qte_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.qte_desc",
                    HackConfig.QTE_INTERVAL_MIN_SECONDS, HackConfig.QTE_INTERVAL_MAX_SECONDS)));
            s.add(new ImageSection("guide/mechanics/qte.png", 200, 200));
            s.add(new SpacerSection(4));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.qte_success")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.qte_critical")));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.progress_title")));
            s.add(new ImageSection("guide/in_game/count_computers.png", 200, 80));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.progress_desc",
                    HackConfig.COMPUTERS_NEEDED_FOR_WIN)));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.glow_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.glow_desc")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.alchemist_glow")));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.computers.end_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.computers.end_desc",
                    HackConfig.COMPUTERS_NEEDED_FOR_WIN)));
            s.add(new ImageSection("guide/maps/safe_with_weapon.png", 450, 300));
            return s;
        }

        // ── НОКДАУН ──────────────────────────────────────────────────────────

        private static List<Section> buildDowned() {
            var s = new ArrayList<Section>();
            s.add(new ImageSection("guide/mechanics/downed.png", 400, 250));
            s.add(new SpacerSection(8));
            s.add(new TextSection(t("guide.maniacrev.tutorial.downed.overview")));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.downed.revive_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.downed.normal_revive",
                    DownedData.NORMAL_REVIVE_TICKS / 20)));
            s.add(new TextSection(t("guide.maniacrev.tutorial.downed.medic_revive",
                    DownedData.MEDIC_REVIVE_TICKS / 20)));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.downed.conditions_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.downed.last_survivor")));
            s.add(new SpacerSection(8));
            s.add(new TextSection(t("guide.maniacrev.tutorial.downed.timeout",
                    DownedData.DOWNED_TIMEOUT_TICKS / 20)));
            return s;
        }

        // ── ОСОБЕННОСТИ КАРТ ─────────────────────────────────────────────────

        private static List<Section> buildMapFeatures() {
            var s = new ArrayList<Section>();
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.maps.mansion_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.maps.secret_doors")));
            s.add(new ImageSection("guide/mechanics/close_door.png", 450, 200));
            s.add(new ImageSection("guide/mechanics/open_door.png",  450, 200));
            s.add(new SpacerSection(5));
            s.add(new TextSection(t("guide.maniacrev.tutorial.maps.secret_hatches")));
            s.add(new ImageSection("guide/mechanics/close_hatch.png", 450, 200));
            s.add(new ImageSection("guide/mechanics/open_hatch.png",  450, 200));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.maps.pizzeria_title")));
            s.add(new ImageSection("guide/mechanics/ventilation.png", 450, 200));
            return s;
        }

        // ── ПЕРКИ ────────────────────────────────────────────────────────────

        private static List<Section> buildPerks() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.overview")));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.perks.mechanics_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.passive")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.active")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.hybrid")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.perks.teams_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.survivor")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.maniac")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.common")));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection(t("guide.maniacrev.tutorial.perks.phase_title")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_warning")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_1")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_2")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_3")));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_any")));
            s.add(new SpacerSection(4));
            s.add(new TextSection(t("guide.maniacrev.tutorial.perks.phase_more")));
            s.add(new SpacerSection(10));
            s.add(new LinkSection(t("guide.maniacrev.tutorial.perks.link"), PageType.PERKS));
            return s;
        }

        // ── ЗАКЛЮЧЕНИЕ ───────────────────────────────────────────────────────

        private static List<Section> buildEnd() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(t("guide.maniacrev.tutorial.end.summary")));
            s.add(new SpacerSection(10));
            s.add(new TextSection(t("guide.maniacrev.tutorial.end.community")));
            s.add(new SpacerSection(10));
            s.add(new TextSection(t("guide.maniacrev.tutorial.end.reopen")));
            return s;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  КНОПКА ТЕМЫ В ОГЛАВЛЕНИИ
    // ═════════════════════════════════════════════════════════════════════════

    private class TopicButton {
        final int x, y, w, h;
        final Topic topic;

        TopicButton(int x, int y, int w, int h, Topic topic) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.topic = topic;
        }

        boolean isHovered(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void render(GuiGraphics gui, int mouseX, int mouseY) {
            renderAt(gui, mouseX, mouseY, x, y);
        }

        void renderAt(GuiGraphics gui, int mouseX, int mouseY, int rx, int ry) {
            boolean hov = mouseX >= rx && mouseX < rx + w && mouseY >= ry && mouseY < ry + h;
            GuideTheme.drawCard(gui, rx, ry, w, h, GuideTheme.GOLD, hov);
            gui.drawString(font, topic.buttonLabel(), rx + 12, ry + (h - 8) / 2,
                    GuideTheme.TEXT, false);
            if (hov) {
                String arrow = "→";
                gui.drawString(font, arrow, rx + w - font.width(arrow) - 10,
                        ry + (h - 8) / 2, GuideTheme.GOLD, false);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  СЕКЦИИ КОНТЕНТА (вложенные классы)
    // ═════════════════════════════════════════════════════════════════════════

    private abstract static class Section {
        abstract int  getHeight(int maxWidth);
        abstract void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY);
    }

    private static class HeaderSection extends Section {
        final String text;
        HeaderSection(String text) { this.text = text; }

        @Override int getHeight(int w) { return 16; }

        @Override
        void render(GuiGraphics gui, int x, int y, int w, int mx, int my) {
            // Линия-разделитель
            gui.fill(x, y + 12, x + w, y + 13, GuideTheme.BORDER_SOFT);
            gui.fill(x, y + 12, x + Math.min(52, w), y + 13, GuideTheme.GOLD);
            gui.drawString(net.minecraft.client.Minecraft.getInstance().font, text, x, y, 0xFFFFFF, false);
        }
    }

    private static class TextSection extends Section {
        final String text;
        TextSection(String text) { this.text = text; }

        @Override
        int getHeight(int maxWidth) {
            return wrapStatic(text, maxWidth).size() * 11 + 2;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int w, int mx, int my) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            List<String> lines = wrapStatic(text, w);
            for (int i = 0; i < lines.size(); i++) {
                gui.drawString(font, lines.get(i), x, y + i * 11, 0xFFFFFF, false);
            }
        }
    }

    private static class ImageSection extends Section {
        final String path;
        final int displayW, displayH;

        ImageSection(String path, int w, int h) {
            this.path = path; this.displayW = w; this.displayH = h;
        }

        @Override int getHeight(int maxWidth) { return displayH + 6; }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mx, int my) {
            ResourceLocation tex = new ResourceLocation("maniacrev", "textures/gui/" + path);
            int w = Math.min(displayW, maxWidth);
            int h = (displayW > maxWidth)
                    ? (int)(displayH * ((float) maxWidth / displayW))
                    : displayH;
            int imgX = x + (maxWidth - w) / 2;

            try {
                RenderSystem.setShaderTexture(0, tex);
                RenderSystem.enableBlend();
                gui.blit(tex, imgX, y, 0, 0, w, h, w, h);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                gui.fill(imgX, y, imgX + w, y + h, GuideTheme.SURFACE);
                gui.renderOutline(imgX, y, w, h, GuideTheme.BORDER);
                gui.drawString(net.minecraft.client.Minecraft.getInstance().font,
                        "§8" + path, imgX + 4, y + h / 2 - 4, 0x888888, false);
            }
        }
    }

    private static class SpacerSection extends Section {
        final int h;
        SpacerSection(int h) { this.h = h; }
        @Override int  getHeight(int w) { return h; }
        @Override void render(GuiGraphics gui, int x, int y, int w, int mx, int my) {}
    }

    private static class LinkSection extends Section {
        final String text;
        final PageType targetPage;

        LinkSection(String text, PageType targetPage) {
            this.text = text;
            this.targetPage = targetPage;
        }

        @Override int getHeight(int w) { return 14; }

        @Override
        void render(GuiGraphics gui, int x, int y, int w, int mx, int my) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            boolean hov = mx >= x && mx < x + font.width(text) + 4 && my >= y && my < y + 11;
            gui.drawString(font, text, x, y, hov ? GuideTheme.TEXT : GuideTheme.GOLD, false);
            if (hov) {
                gui.drawString(font, t("guide.maniacrev.click"), x + font.width(text) + 2, y, 0x888888, false);
            }
        }
    }

    // Статический враппер текста (используется из static context Topic)
    private static List<String> wrapStatic(String text, int maxWidth) {
        net.minecraft.client.gui.Font font =
                net.minecraft.client.Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String test = line.length() > 0 ? line + " " + word : word;
            if (font.width(test) > maxWidth) {
                if (line.length() > 0) { lines.add(line.toString()); line = new StringBuilder(word); }
                else lines.add(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static String t(String key, Object... args) {
        return tr(key, args);
    }
}
