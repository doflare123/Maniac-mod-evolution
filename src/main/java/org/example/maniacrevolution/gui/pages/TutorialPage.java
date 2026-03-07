package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;

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
        renderNavButton(gui, mouseX, mouseY, "← Главная", guiLeft + 5, guiTop + 5, 80, 18);

        // Заголовок
        gui.drawCenteredString(font, "§6§l✦ ПОЛНЫЙ ГАЙД ✦",
                guiLeft + guiWidth / 2, guiTop + 15, 0xFFFFFF);
        gui.drawCenteredString(font, "§7Выберите тему:",
                guiLeft + guiWidth / 2, guiTop + 32, 0xAAAAAA);

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
            gui.fill(guiLeft + guiWidth - 92, guiTop + guiHeight - 18,
                    guiLeft + guiWidth - 5,  guiTop + guiHeight - 5, 0xAA000000);
            gui.drawString(font, "§8↑↓ Прокрутка",
                    guiLeft + guiWidth - 88, guiTop + guiHeight - 15, 0xAAAAAA, false);
        }
    }

    // ── Содержимое темы ───────────────────────────────────────────────────────

    private void renderTopic(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка "← Назад"
        renderNavButton(gui, mouseX, mouseY, "← Назад", guiLeft + 5, guiTop + 5, 80, 18);

        // Заголовок темы
        gui.drawCenteredString(font, currentTopic.title,
                guiLeft + guiWidth / 2, guiTop + 15, 0xFFFFFF);

        // Область прокрутки
        int clipTop    = guiTop + 30;
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
            gui.fill(guiLeft + guiWidth - 92, guiTop + guiHeight - 18,
                    guiLeft + guiWidth - 5,  guiTop + guiHeight - 5, 0xAA000000);
            gui.drawString(font, "§8↑↓ Прокрутка",
                    guiLeft + guiWidth - 88, guiTop + guiHeight - 15, 0xAAAAAA, false);
        }
    }

    // ── Вспомогательный рендер кнопки ────────────────────────────────────────

    private void renderNavButton(GuiGraphics gui, int mouseX, int mouseY,
                                 String label, int x, int y, int w, int h) {
        boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        gui.fill(x, y, x + w, y + h, hov ? 0xFF444444 : 0xFF333333);
        gui.renderOutline(x, y, w, h, 0xFF666666);
        gui.drawCenteredString(font, label, x + w / 2, y + 5, 0xFFFFFF);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MOUSE / SCROLL
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int navX = guiLeft + 5, navY = guiTop + 5, navW = 80, navH = 18;

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
        INTRO       ("§e§l📖 Введение, правила и фазы игры", "§6§lВведение, правила и фазы игры"),
        HUD         ("§e§l🖥 Кастомный HUD и мана",            "§6§lКастомный HUD и мана"),
        BEFORE_START("§e§l🎮 Перед стартом и места спавна",    "§6§lПеред стартом и места спавна"),
        COMPUTERS   ("§e§l💻 Взлом компьютеров",               "§6§lВзлом компьютеров"),
        DOWNED      ("§e§l💀 При смерти / Нокдаун",            "§6§lПри смерти — Нокдаун"),
        MAP_FEATURES("§e§l🗺 Особенности карт",                "§6§lОсобенности карт"),
        PERKS       ("§e§l⚡ Система перков",                  "§6§lСистема перков"),
        END         ("§e§l✨ Заключение",                      "§6§lЗаключение");

        final String buttonLabel;  // Текст кнопки в оглавлении
        final String title;        // Заголовок страницы темы

        Topic(String buttonLabel, String title) {
            this.buttonLabel = buttonLabel;
            this.title       = title;
        }

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
            s.add(new TextSection(
                    "Добро пожаловать в режим Maniac! Это асимметричный PvP-режим, вдохновлённый игрой DBD, " +
                            "где команда выживших противостоит маньякам. Выживание требует командной работы, стратегии " +
                            "и умения использовать перки."
            ));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l⚔ Основные правила"));
            s.add(new TextSection("§7Команды:§r Игроки делятся на §bВыживших§r и §cМаньяков§r."));
            s.add(new TextSection("§7Цель выживших:§r Взломать все компьютеры и убить маньяка до окончания времени."));
            s.add(new TextSection("§7Цель маньяков:§r Устранить всех выживших или дожить до конца таймера."));
            s.add(new SpacerSection(12));
            s.add(new HeaderSection("§e§l⏱ Фазы игры"));
            s.add(new TextSection("Игра делится на три фазы — они меняют условия и доступные перки."));
            s.add(new SpacerSection(6));
            s.add(new TextSection("§6§lФаза 1 — Охота.§r Маньяки ищут выживших. Некоторые перки недоступны."));
            s.add(new SpacerSection(4));
            s.add(new TextSection("§6§lФаза 2 — Мидгейм.§r Прошла половина времени. Открываются дополнительные перки. Игра становится интенсивнее."));
            s.add(new SpacerSection(4));
            s.add(new TextSection(
                    "§6§lФаза 3 — Переворот.§r Наступает после взлома нужного числа компьютеров. " +
                            "Выжившие получают карточки для сейфов с оружием."
            ));
            return s;
        }

        // ── HUD ──────────────────────────────────────────────────────────────

        private static List<Section> buildHud() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(
                    "В игре используется полностью кастомный HUD. Здоровье отображается в виде полоски, " +
                            "добавлена система маны для способностей (у некоторых классов) и слоты перков."
            ));
            s.add(new SpacerSection(5));
            s.add(new ImageSection("guide/before_start_game/custom_hud.png", 450, 150));
            s.add(new SpacerSection(5));
            s.add(new TextSection(
                    "В HUD отображаются выбранные перки и способности. Вы можете видеть манакост, " +
                            "назначенные кнопки, тип перка и кулдаун после применения."
            ));
            s.add(new SpacerSection(5));
            s.add(new ImageSection("guide/before_start_game/full_custom_hud.png", 450, 150));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l💧 Система маны"));
            s.add(new TextSection(
                    "Мана — конечный ресурс. Её менеджмент является ключевой механикой у классов " +
                            "с активными скиллами. Следите за полоской маны в HUD."
            ));
            return s;
        }

        // ── ДО СТАРТА ────────────────────────────────────────────────────────

        private static List<Section> buildBeforeStart() {
            var s = new ArrayList<Section>();
            s.add(new HeaderSection("§e§l1. Выбор карты"));
            s.add(new ImageSection("guide/before_start_game/pick_map.png", 450, 400));
            s.add(new TextSection(
                    "Выбираете карту, подтверждаете. При равном распределении голосов запускается " +
                            "анимация рандомизации, и в чате появляется выбранная карта."
            ));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l2. Предметы при старте"));
            s.add(new ImageSection("guide/before_start_game/items_for_game.png", 200, 150));
            s.add(new TextSection("При распределении по командам вам выдаются:"));
            s.add(new TextSection("§7● §61 слот§r — выбор перков"));
            s.add(new TextSection("§7● §62 слот§r — выбор класса (текстура зависит от команды)"));
            s.add(new TextSection("§7● §63 слот§r — кнопка готовности"));
            s.add(new TextSection("Кнопку готовности можно нажать только после выбора перков, класса и карты."));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l3. Выбор перков"));
            s.add(new ImageSection("guide/before_start_game/perks.png", 450, 300));
            s.add(new TextSection(
                    "Наведитесь на перк чтобы узнать что он делает, на какой стадии работает, " +
                            "его тип и перезарядку. Кликните левой кнопкой для выбора — выбранный перк обведётся зелёным."
            ));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l4. Выбор персонажа"));
            s.add(new ImageSection("guide/before_start_game/pick_hero.png", 450, 450));
            s.add(new TextSection(
                    "Листайте персонажей стрелочками. Слева — фильтры классов, справа — описание. " +
                            "После выбора нажмите «Выбрать»."
            ));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l5. Готовность"));
            s.add(new TextSection("После выбора класса и перков активируйте предмет готовности. Когда все готовы:"));
            s.add(new ImageSection("guide/before_start_game/start_game.png", 450, 50));
            s.add(new SpacerSection(14));
            s.add(new HeaderSection("§e§l6. Места спавна"));
            s.add(new TextSection("§e§lОсобняк"));
            s.add(new TextSection("§bВыжившие:"));
            s.add(new ImageSection("guide/start_game/start_survivors_mansion.png", 500, 300));
            s.add(new TextSection("§cМаньяки:"));
            s.add(new ImageSection("guide/start_game/start_maniac_mansion.png", 500, 300));
            s.add(new SpacerSection(10));
            s.add(new TextSection("§e§lПиццерия Фрэдэ"));
            s.add(new TextSection("§bВыжившие:"));
            s.add(new ImageSection("guide/start_game/start_survivors_freddy.png", 500, 300));
            s.add(new TextSection("§cМаньяки:"));
            s.add(new ImageSection("guide/start_game/start_maniac_freddy.png", 500, 300));
            return s;
        }

        // ── КОМПЬЮТЕРЫ ───────────────────────────────────────────────────────

        private static List<Section> buildComputers() {
            var s = new ArrayList<Section>();
            s.add(new ImageSection("guide/maps/computers_watch.png", 550, 300));
            s.add(new SpacerSection(6));
            s.add(new HeaderSection("§e§l🖱 Как начать взлом"));
            s.add(new TextSection(
                    "Подойдите к компьютеру в радиусе 1 блока и нажмите §lправую кнопку мыши§r. " +
                            "Начнётся взлом. Активирующий игрок не может отходить дальше 1 блока — иначе взлом прервётся."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l👥 Помощь при взломе"));
            s.add(new ImageSection("guide/maps/active_computer.png", 450, 250));
            s.add(new TextSection(
                    "Союзники могут ускорить взлом, находясь в радиусе §b3 блоков§r от компьютера. " +
                            "Эта область обозначена §fбелыми частицами§r при активном взломе."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l⚡ QTE — мини-игра взлома"));
            s.add(new TextSection(
                    "Раз в 3–5 секунд появляется QTE: сжимающаяся рамка. " +
                            "Нужно нажать нужную клавишу когда рамка совпадёт с целевой областью."
            ));
            s.add(new ImageSection("guide/mechanics/qte.png", 200, 200));
            s.add(new SpacerSection(4));
            s.add(new TextSection("§7● §aЗелёная зона§7 — обычный успех, небольшой бонус к прогрессу взлома."));
            s.add(new TextSection("§7● §dФиолетовая зона§7 — §l§dкритический успех§r§7, значительно ускоряет взлом."));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l📊 Индикатор прогресса"));
            s.add(new ImageSection("guide/in_game/count_computers.png", 200, 80));
            s.add(new TextSection(
                    "В правом нижнем углу экрана отображается счётчик взломанных компьютеров. " +
                            "Он показывает сколько уже взломано из нужного количества для победы."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l🔦 Подсветка компьютеров"));
            s.add(new TextSection(
                    "В начале игры компьютеры подсвечиваются для всех. " +
                            "§cМаньяки§r видят их всю игру, §bвыжившие§r — только первые 40 секунд."
            ));
            s.add(new TextSection(
                    "Если на карте есть «Алхимик», ему также всю игру подсвечиваются зельеварки."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l🏆 Конец игры — Переворот"));
            s.add(new TextSection(
                    "После взлома нужного числа компьютеров выжившие получают ключ-карты для сейфов с оружием " +
                            "(количество ограничено)."
            ));
            s.add(new ImageSection("guide/maps/safe_with_weapon.png", 450, 300));
            return s;
        }

        // ── НОКДАУН ──────────────────────────────────────────────────────────

        private static List<Section> buildDowned() {
            var s = new ArrayList<Section>();
            s.add(new ImageSection("guide/mechanics/downed.png", 400, 250));
            s.add(new SpacerSection(8));
            s.add(new TextSection(
                    "У каждого §bвыжившего§r есть второй шанс. После смертельного удара он не умирает сразу, " +
                            "а падает в нокдаун — ложится на землю. Это срабатывает §l только один раз§r за игру."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l⏱ Поднятие"));
            s.add(new TextSection("§7● §fОбычный игрок§7 поднимает за §a6 секунд§7."));
            s.add(new TextSection("§7● §bМедик§7 поднимает за §a3 секунды§7 — вдвое быстрее."));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l⚠ Особые условия"));
            s.add(new TextSection(
                    "Если ты §cпоследний не лежащий выживший§r — нокдауна не будет: " +
                            "следующий смертельный удар убьёт тебя сразу. " +
                            "Исключение — перк на автоподнятие."
            ));
            s.add(new SpacerSection(8));
            s.add(new TextSection(
                    "§7Лёжа на земле, выживший ждёт §e60 секунд§7. " +
                            "Если за это время его никто не поднял — он умирает."
            ));
            return s;
        }

        // ── ОСОБЕННОСТИ КАРТ ─────────────────────────────────────────────────

        private static List<Section> buildMapFeatures() {
            var s = new ArrayList<Section>();
            s.add(new HeaderSection("§e§lОсобняк — потайные ходы"));
            s.add(new TextSection("Потайные двери:"));
            s.add(new ImageSection("guide/mechanics/close_door.png", 450, 200));
            s.add(new ImageSection("guide/mechanics/open_door.png",  450, 200));
            s.add(new SpacerSection(5));
            s.add(new TextSection("Потайные люки:"));
            s.add(new ImageSection("guide/mechanics/close_hatch.png", 450, 200));
            s.add(new ImageSection("guide/mechanics/open_hatch.png",  450, 200));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§lПиццерия Фрэдэ — канализации и вентиляции"));
            s.add(new ImageSection("guide/mechanics/ventilation.png", 450, 200));
            return s;
        }

        // ── ПЕРКИ ────────────────────────────────────────────────────────────

        private static List<Section> buildPerks() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(
                    "Перки — уникальные способности, дающие преимущества. Каждый игрок выбирает перки перед игрой."
            ));
            s.add(new SpacerSection(8));
            s.add(new HeaderSection("§e§l🎯 Типы по механике"));
            s.add(new TextSection("§9● Пассивные§r — работают автоматически, не требуют активации."));
            s.add(new TextSection("§c● Активные§r — нужно нажать клавишу для применения."));
            s.add(new TextSection("§d● Гибридные§r — пассивный эффект + возможность активации."));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l👥 Типы по команде"));
            s.add(new TextSection("§7● §bПерки выживших§r — доступны только команде выживших."));
            s.add(new TextSection("§7● §cПерки маньяков§r — доступны только маньякам."));
            s.add(new TextSection("§7● §fОбщие перки§r — могут взять обе стороны."));
            s.add(new SpacerSection(10));
            s.add(new HeaderSection("§e§l⏱ Фаза активации перка"));
            s.add(new TextSection(
                    "§c§lВажно!§r Обращайте внимание на фазу в которой работает перк:"
            ));
            s.add(new TextSection("§7● §6Фаза 1 (Охота)§r — перк сработает/начнёт работу с самого начала игры."));
            s.add(new TextSection("§7● §6Фаза 2 (Мидгейм)§r — перк активируется только после наступления мидгейма."));
            s.add(new TextSection("§7● §6Фаза 3 (Переворот)§r — перк становится доступен лишь в финальной фазе."));
            s.add(new TextSection("§7● §aЛюбая фаза§r — перк работает всегда."));
            s.add(new SpacerSection(4));
            s.add(new TextSection("§8Подробнее о фазах — в теме «Введение, правила и фазы игры»."));
            s.add(new SpacerSection(10));
            s.add(new LinkSection("§e§n➤ Полный список перков и способностей", PageType.PERKS));
            return s;
        }

        // ── ЗАКЛЮЧЕНИЕ ───────────────────────────────────────────────────────

        private static List<Section> buildEnd() {
            var s = new ArrayList<Section>();
            s.add(new TextSection(
                    "Это основные механики режима! Дальше — свободное плавание: изучайте карту, " +
                            "придумывайте фишечки и находите интересные комбинации перков. Удачи и приятной игры!"
            ));
            s.add(new SpacerSection(10));
            s.add(new TextSection(
                    "§7§lP.S.§r Если вам понравится карта, заходите в наш ТГК: §9§nhttps://t.me/necrodwarfs§r " +
                            "или найдите нас по §9@necrodwarfs§r в Telegram."
            ));
            s.add(new SpacerSection(10));
            s.add(new TextSection(
                    "§6§lГайд не одноразовый!§r Откройте его повторно через бинд клавиши в настройках."
            ));
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
            gui.fill(rx, ry, rx + w, ry + h, hov ? 0xFF3a3a3a : 0xFF2a2a2a);
            gui.renderOutline(rx, ry, w, h, hov ? 0xFFFFAA00 : 0xFF555555);
            gui.drawString(font, topic.buttonLabel, rx + 10, ry + (h - 8) / 2, 0xFFFFFF, false);
            if (hov) {
                String arrow = "→";
                gui.drawString(font, "§7" + arrow, rx + w - font.width(arrow) - 8, ry + (h - 8) / 2, 0xAAAAAA, false);
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
            gui.fill(x, y + 12, x + w, y + 13, 0xFF444444);
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
                gui.fill(imgX, y, imgX + w, y + h, 0xFF333333);
                gui.renderOutline(imgX, y, w, h, 0xFF666666);
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
            gui.drawString(font, text, x, y, hov ? 0xFFFFDD44 : 0xFFFFAA00, false);
            if (hov) {
                gui.drawString(font, "§8 (клик)", x + font.width(text) + 2, y, 0x888888, false);
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
}