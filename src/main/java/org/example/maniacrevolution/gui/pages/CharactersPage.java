package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.character.TagRegistry;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CharactersPage extends GuidePage {
    private CharacterType currentFilter = null; // null = все
    private String selectedTag = null;
    private int scrollOffset = 0;
    private CharacterClass hoveredCharacter = null;
    private CharacterClass selectedCharacter = null;
    private int detailScrollOffset = 0;

    // ── Доп. секции для конкретных персонажей ────────────────────────────────
    // Заполняются один раз при первом обращении
    private static final Map<String, List<ExtraSection>> EXTRA_SECTIONS = new HashMap<>();

    static {
        buildExtraSections();
    }

    // ── Размеры фрески ────────────────────────────────────────────────────────
    private static final int FRESCO_WIDTH = 200;
    private static final int FRESCO_HEIGHT = 500;
    private static final float FRESCO_SCALE = 0.3f; // Масштаб для списка
    private static final int SCALED_FRESCO_WIDTH = (int) (FRESCO_WIDTH * FRESCO_SCALE);
    private static final int SCALED_FRESCO_HEIGHT = (int) (FRESCO_HEIGHT * FRESCO_SCALE);

    public CharactersPage(GuideScreen parent) {
        super(parent);
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset = 0;
        detailScrollOffset = 0;
        selectedCharacter = null;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackButton(gui, mouseX, mouseY);

        if (selectedCharacter != null) {
            renderCharacterDetails(gui, mouseX, mouseY);
        } else {
            renderCharacterList(gui, mouseX, mouseY);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ДОПОЛНИТЕЛЬНЫЕ СЕКЦИИ ДЛЯ ПЕРСОНАЖЕЙ
    // ═════════════════════════════════════════════════════════════════════════

    private static void buildExtraSections() {

        // ── agent ─────────────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l📱 Планшет агента"));
            s.add(new ExtraText(
                    "Агент использует планшет для просмотра заказов и взаимодействия с магазином."));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/agent47_tablet.png", 400, 220,
                    "Планшет — основной инструмент агента"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/agent_hunt.png", 400, 220,
                    "Вкладка охоты: показывает текущую цель"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/agent_shop.png", 400, 220,
                    "Открытое меню магазина"));
            s.add(new ExtraSpacer(6));
            s.add(new ExtraHeader("§e§l👁 Вид со стороны цели"));
            s.add(new ExtraText(
                    "Когда вы получаете метку цели, на вашем экране появляется следующий элемент:"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/agent_effect.png", 400, 220,
                    "Так выглядит экран у цели при получении метки"));
            EXTRA_SECTIONS.put("agent", s);
        }

        // ── freddy_bear ───────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l⚡ Генератор Фредди"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/generator_fnaf.png", 300, 150,
                    "Индикатор заряда генератора — правый угол экрана"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/generator.png", 400, 220,
                    "Как выглядит генератор Фредди в мире"));
            EXTRA_SECTIONS.put("freddy_bear", s);
        }

        // ── plague_doctor ─────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l☣ Индикатор чумы"));
            s.add(new ExtraText(
                    "Как понять, что на вас была наложена чума и какой прогресс болезни? " +
                            "Посмотрите на своё здоровье: полоска будет заполняться §aзелёным цветом§r — " +
                            "это индикатор того, сколько ещё нужно до получения урона."));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/plague.png", 400, 180,
                    "Зелёный прогресс на полоске HP = накопленная чума"));
            EXTRA_SECTIONS.put("plague_doctor", s);
        }

        // ── alchemist ─────────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l🧪 Подсветка зельеварок"));
            s.add(new ExtraText(
                    "Не знаете где находятся зельеварки? Не проблема — " +
                            "алхимику они подсвечиваются на протяжении всей игры."));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/alch.png", 400, 220,
                    "Подсветка зельеварок на карте"));
            EXTRA_SECTIONS.put("alchemist", s);
        }

        // ── doctor ────────────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l🩺 Планшет доктора"));
            s.add(new ExtraText(
                    "Пульс при открытии планшета показывает состояние выживших: " +
                            "§cчем быстрее ритм сердца — тем меньше у игрока HP§r. " +
                            "Если HP ниже 50%, вы можете отследить игрока и прийти к нему на помощь."));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/medic_tablet.png", 400, 220,
                    "Планшет доктора с пульсом игроков"));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/medic_interface.png", 400, 220,
                    "Полный интерфейс доктора"));
            EXTRA_SECTIONS.put("doctor", s);
        }

        // ── mefedronshchik ──────────────────────────────────────────────────────
        {
            List<ExtraSection> s = new ArrayList<>();
            s.add(new ExtraHeader("§e§l⚠ Система зависимости"));
            s.add(new ExtraText(
                    "§cОсторожно, вы зависимый! Следите за своим состоянием."));
            s.add(new ExtraImageCaption(
                    "guide/mechanics/characters/addiction.png", 400, 180,
                    "Шкала зависимости"));
            s.add(new ExtraSpacer(6));
            s.add(new ExtraHeader("§e§lСтадии ломки"));
            s.add(new ExtraText("§70 стадия§r — точка отсчёта, никакого эффекта."));
            s.add(new ExtraText("§71 стадия§r — небольшая слабость."));
            s.add(new ExtraText("§72 стадия§r — замедление."));
            s.add(new ExtraText("§73 стадия§r — потемнение в глазах."));
            s.add(new ExtraSpacer(8));
            s.add(new ExtraText(
                    "Ломку можно снимать и останавливать с помощью §bбонга§r и §bшприцов адреналина§r."));
            s.add(new ExtraText(
                    "§c§l⚠ Внимательно прочитайте действие шприца!"));
            s.add(new ExtraSpacer(8));
            // ── Описание шприца ──
            s.add(new ExtraHeader("§b§l⚡ Шприц адреналина"));
            s.add(new ExtraText("§b● §fСнижает шкалу зависимости §aна 20%§f."));
            s.add(new ExtraText("§b● §fДаёт скорость §7(эффекты складываются)§f."));
            s.add(new ExtraSpacer(4));
            s.add(new ExtraText("§e⚠ Деградация эффекта:"));
            s.add(new ExtraText("  §71-й шприц:§f Скорость 3 §7на §f8 сек§f."));
            s.add(new ExtraText("  §7Каждый следующий:§c -1 сек §7длительности."));
            s.add(new ExtraText("  §7Каждые 3 шприца:§c -1 уровень §7скорости."));
            s.add(new ExtraSpacer(4));
            s.add(new ExtraDangerText("§4☠ Опасность:"));
            s.add(new ExtraDangerText("  §c4 шприца подряд §7(< 20 сек) = §4§lСМЕРТЬ"));
            s.add(new ExtraDangerText("  §cСтадия 3 + 3 общих §7= §c10%/сек шанс смерти"));
            s.add(new ExtraSpacer(4));
            s.add(new ExtraText("§7Каждый шприц ускоряет ломку."));
            EXTRA_SECTIONS.put("mefedronshchik", s);
        }
    }

    // ── Рендер доп. секций ────────────────────────────────────────────────────

    /**
     * Рендерит дополнительные секции для персонажа и возвращает новый Y.
     */
    private int renderExtraSections(GuiGraphics gui, String characterId, int startY, int maxWidth) {
        List<ExtraSection> sections = EXTRA_SECTIONS.get(characterId);
        if (sections == null) return startY;

        int y = startY;
        for (ExtraSection sec : sections) {
            y = sec.render(gui, font, guiLeft + 15, y, maxWidth);
        }
        return y;
    }

    /**
     * Подсчёт высоты доп. секций для расчёта maxScroll.
     */
    private int extraSectionsHeight(String characterId, int maxWidth) {
        List<ExtraSection> sections = EXTRA_SECTIONS.get(characterId);
        if (sections == null) return 0;
        int h = 0;
        for (ExtraSection sec : sections) h += sec.height(font, maxWidth);
        return h;
    }

    // ── Вспомогательные классы секций ────────────────────────────────────────

    private abstract static class ExtraSection {
        abstract int height(net.minecraft.client.gui.Font font, int maxWidth);
        /** Рендерит секцию и возвращает следующий Y. */
        abstract int render(GuiGraphics gui, net.minecraft.client.gui.Font font, int x, int y, int maxWidth);
    }

    private static class ExtraHeader extends ExtraSection {
        final String text;
        ExtraHeader(String text) { this.text = text; }

        @Override int height(net.minecraft.client.gui.Font f, int w) { return 18; }

        @Override
        int render(GuiGraphics gui, net.minecraft.client.gui.Font f, int x, int y, int w) {
            gui.fill(x, y + 13, x + w, y + 14, 0xFF444444);
            gui.drawString(f, text, x, y, 0xFFFFFF, false);
            return y + 18;
        }
    }

    private static class ExtraText extends ExtraSection {
        final String text;
        ExtraText(String text) { this.text = text; }

        private List<String> lines(net.minecraft.client.gui.Font f, int w) {
            List<String> result = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String test = line.length() > 0 ? line + " " + word : word;
                if (f.width(test) > w) {
                    if (line.length() > 0) { result.add(line.toString()); line = new StringBuilder(word); }
                    else result.add(word);
                } else line = new StringBuilder(test);
            }
            if (line.length() > 0) result.add(line.toString());
            return result;
        }

        @Override int height(net.minecraft.client.gui.Font f, int w) { return lines(f, w).size() * 11 + 2; }

        @Override
        int render(GuiGraphics gui, net.minecraft.client.gui.Font f, int x, int y, int w) {
            List<String> ls = lines(f, w);
            for (int i = 0; i < ls.size(); i++) gui.drawString(f, ls.get(i), x, y + i * 11, 0xFFFFFF, false);
            return y + ls.size() * 11 + 2;
        }
    }

    /** Красный текст для предупреждений об опасности. */
    private static class ExtraDangerText extends ExtraSection {
        final String text;
        ExtraDangerText(String text) { this.text = text; }

        private List<String> lines(net.minecraft.client.gui.Font f, int w) {
            List<String> result = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String test = line.length() > 0 ? line + " " + word : word;
                if (f.width(test) > w) {
                    if (line.length() > 0) { result.add(line.toString()); line = new StringBuilder(word); }
                    else result.add(word);
                } else line = new StringBuilder(test);
            }
            if (line.length() > 0) result.add(line.toString());
            return result;
        }

        @Override int height(net.minecraft.client.gui.Font f, int w) { return lines(f, w).size() * 11 + 2; }

        @Override
        int render(GuiGraphics gui, net.minecraft.client.gui.Font f, int x, int y, int w) {
            // Фон-предупреждение
            int totalH = lines(f, w).size() * 11 + 2;
            gui.fill(x - 2, y - 1, x + w + 2, y + totalH, 0x55FF0000);
            List<String> ls = lines(f, w);
            for (int i = 0; i < ls.size(); i++) gui.drawString(f, ls.get(i), x, y + i * 11, 0xFF5555FF, false);
            return y + totalH;
        }
    }

    private static class ExtraImageCaption extends ExtraSection {
        final String path;
        final int imgW, imgH;
        final String caption;

        ExtraImageCaption(String path, int imgW, int imgH, String caption) {
            this.path = path; this.imgW = imgW; this.imgH = imgH; this.caption = caption;
        }

        @Override int height(net.minecraft.client.gui.Font f, int w) { return imgH + 6 + 13; }

        @Override
        int render(GuiGraphics gui, net.minecraft.client.gui.Font f, int x, int y, int maxWidth) {
            int w = Math.min(imgW, maxWidth);
            int h = imgW > maxWidth ? (int)(imgH * ((float) maxWidth / imgW)) : imgH;
            int imgX = x + (maxWidth - w) / 2;

            ResourceLocation tex = new ResourceLocation("maniacrev", "textures/gui/" + path);
            try {
                RenderSystem.setShaderTexture(0, tex);
                RenderSystem.enableBlend();
                gui.blit(tex, imgX, y, 0, 0, w, h, w, h);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                gui.fill(imgX, y, imgX + w, y + h, 0xFF2a2a2a);
                gui.renderOutline(imgX, y, w, h, 0xFF555555);
                gui.drawString(f, "§8" + path, imgX + 4, y + h / 2 - 4, 0x888888, false);
            }
            // Подпись
            gui.drawCenteredString(f, "§8§o" + caption, x + maxWidth / 2, y + h + 2, 0xAAAAAA);
            return y + h + 6 + 13;
        }
    }

    private static class ExtraSpacer extends ExtraSection {
        final int h;
        ExtraSpacer(int h) { this.h = h; }
        @Override int height(net.minecraft.client.gui.Font f, int w) { return h; }
        @Override int render(GuiGraphics gui, net.minecraft.client.gui.Font f, int x, int y, int w) { return y + h; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  END EXTRA SECTIONS
    // ═════════════════════════════════════════════════════════════════════════

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

    private void renderCharacterList(GuiGraphics gui, int mouseX, int mouseY) {
        // Заголовок
        gui.drawCenteredString(font, "§6§lПерсонажи режима",
                guiLeft + guiWidth / 2, guiTop + 28, 0xFFFFFF);

        // Фильтры по типу
        renderTypeFilters(gui, mouseX, mouseY);

        // Фильтр по тегам (если выбран тип)
        int tagFilterY = guiTop + 70;
        int listStartY = tagFilterY + 5;

        if (currentFilter != null) {
            renderTagFilter(gui, mouseX, mouseY, tagFilterY);
            listStartY = tagFilterY + 35;
        }

        // Список персонажей
        List<CharacterClass> characters = getFilteredCharacters();
        hoveredCharacter = null;

        int y = listStartY - scrollOffset;
        int entryHeight = SCALED_FRESCO_HEIGHT + 10;

        // ИСПРАВЛЕНО: Уменьшена видимая область снизу для индикатора прокрутки
        int bottomMargin = guiTop + guiHeight - 5; // Вместо -10

        gui.enableScissor(guiLeft + 10, listStartY, guiLeft + guiWidth, bottomMargin);

        for (CharacterClass character : characters) {
            if (y + entryHeight > listStartY && y < bottomMargin) {
                boolean hovered = isMouseOverCharacter(mouseX, mouseY, guiLeft + 10, y, guiWidth - 20, entryHeight, listStartY);

                if (hovered) {
                    hoveredCharacter = character;
                }

                renderCharacterEntry(gui, character, guiLeft + 10, y, hovered);
            }
            y += entryHeight;
        }

        gui.disableScissor();

        // ИСПРАВЛЕНО: Индикатор прокрутки размещён в зарезервированной области
//        if (characters.size() > 2) {
//            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 85,
//                    guiTop + guiHeight - 30, 0xAAAAAA, false);
//        }
    }

    private void renderTypeFilters(GuiGraphics gui, int mouseX, int mouseY) {
        int btnY = guiTop + 45;
        int btnWidth = 85;
        int btnHeight = 18;
        int spacing = 5;
        int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

        renderFilterButton(gui, mouseX, mouseY, startX, btnY, btnWidth, btnHeight, "§fВсе", null);
        renderFilterButton(gui, mouseX, mouseY, startX + btnWidth + spacing, btnY, btnWidth, btnHeight, "§aВыжившие", CharacterType.SURVIVOR);
        renderFilterButton(gui, mouseX, mouseY, startX + (btnWidth + spacing) * 2, btnY, btnWidth, btnHeight, "§cМаньяки", CharacterType.MANIAC);
    }

    private void renderFilterButton(GuiGraphics gui, int mouseX, int mouseY, int x, int y, int width, int height, String text, CharacterType type) {
        boolean selected = (currentFilter == type);
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int bgColor = selected ? 0xFF444444 : (hovered ? 0xFF3a3a3a : 0xFF2a2a2a);
        gui.fill(x, y, x + width, y + height, bgColor);
        gui.renderOutline(x, y, width, height, selected ? 0xFFFFAA00 : 0xFF666666);
        gui.drawCenteredString(font, text, x + width / 2, y + 5, 0xFFFFFF);
    }

    private void renderTagFilter(GuiGraphics gui, int mouseX, int mouseY, int y) {
        List<String> availableTags = getAvailableTags();

        if (availableTags.isEmpty()) return;

        int tagX = guiLeft + 10;
        int tagY = y;
        int maxTagY = y + 36; // ИСПРАВЛЕНО: Максимум 2 строки тегов

        gui.drawString(font, "§7Фильтр:", tagX, tagY + 4, 0xAAAAAA, false);
        tagX += font.width("Фильтр: ") + 5;

        int displayedTags = 0;
        int maxTags = 12; // ИСПРАВЛЕНО: Максимум 12 тегов

        for (String tag : availableTags) {
            if (displayedTags >= maxTags) break; // ИСПРАВЛЕНО: Ограничение

            boolean selected = tag.equals(selectedTag);
            int tagWidth = font.width(tag) + 8;

            boolean hovered = mouseX >= tagX && mouseX < tagX + tagWidth && mouseY >= tagY && mouseY < tagY + 16;

            int bgColor = selected ? 0xFF555555 : (hovered ? 0xFF3a3a3a : 0xFF2a2a2a);
            gui.fill(tagX, tagY, tagX + tagWidth, tagY + 16, bgColor);
            gui.renderOutline(tagX, tagY, tagWidth, 16, selected ? 0xFFFFAA00 : 0xFF666666);
            gui.drawCenteredString(font, "§f" + tag, tagX + tagWidth / 2, tagY + 4, 0xFFFFFF);

            tagX += tagWidth + 3;
            displayedTags++;

            // Переносим на новую строку если не влезает
            if (tagX > guiLeft + guiWidth - 50) {
                tagX = guiLeft + 10;
                tagY += 18;

                // ИСПРАВЛЕНО: Проверяем, не вышли ли за пределы
                if (tagY >= maxTagY) break;
            }
        }
    }

    private void renderCharacterEntry(GuiGraphics gui, CharacterClass character, int x, int y, boolean hovered) {
        int width = guiWidth - 20;
        int height = SCALED_FRESCO_HEIGHT + 5;

        // Фон
        gui.fill(x, y, x + width, y + height, hovered ? 0xAA444444 : 0x80333333);
        if (hovered) {
            gui.renderOutline(x, y, width, height, 0xFFFFAA00);
        }

        // Фреска
        renderFresco(gui, character, x + 5, y + 3, FRESCO_SCALE);

        // Информация справа от фрески
        int infoX = x + SCALED_FRESCO_WIDTH + 15;
        int infoY = y + 10;

        // Имя
        String typeColor = character.getType() == CharacterType.SURVIVOR ? "§a" : "§c";
        gui.drawString(font, typeColor + "§l" + character.getName(), infoX, infoY, 0xFFFFFF, false);
        infoY += 12;

        // Тип
        String combatType = getCombatType(character);
        if (combatType != null) {
            gui.drawString(font, "§7" + combatType, infoX, infoY, 0xAAAAAA, false);
            infoY += 11;
        }

        // Сложность
        gui.drawString(font, "§7Сложность: " + character.getDifficultyStars(), infoX, infoY, 0xFFFFFF, false);
        infoY += 11;

        // Теги (первые 3)
        List<String> tags = character.getTags();
        if (!tags.isEmpty()) {
            String tagText = tags.stream().limit(3).collect(Collectors.joining("§7, §e"));
            gui.drawString(font, "§e" + tagText, infoX, infoY, 0xFFFFFF, false);
        }

        // Подсказка при наведении
        if (hovered) {
            gui.drawString(font, "§e§oКлик для подробностей →", infoX, y + height - 15, 0xFFAA00, false);
        }
    }

    private void renderFresco(GuiGraphics gui, CharacterClass character, int x, int y, float scale) {
        ResourceLocation texture = character.getFrescoTexture();

        try {
            gui.pose().pushPose();
            gui.pose().translate(x, y, 0);
            gui.pose().scale(scale, scale, 1.0f);

            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, 0, 0, 0, 0, FRESCO_WIDTH, FRESCO_HEIGHT, FRESCO_WIDTH, FRESCO_HEIGHT);
            RenderSystem.disableBlend();

            gui.pose().popPose();
        } catch (Exception e) {
            // Placeholder
            int scaledW = (int) (FRESCO_WIDTH * scale);
            int scaledH = (int) (FRESCO_HEIGHT * scale);
            gui.fill(x, y, x + scaledW, y + scaledH, 0xFF555555);
            gui.renderOutline(x, y, scaledW, scaledH, 0xFF888888);
            gui.drawCenteredString(font, "§8?", x + scaledW / 2, y + scaledH / 2 - 5, 0xFFFFFF);
        }
    }

    private void renderCharacterDetails(GuiGraphics gui, int mouseX, int mouseY) {
        // Кнопка назад
        int btnX = guiLeft + 5;
        int btnY = guiTop + guiHeight - 25;
        boolean hovered = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 20;

        gui.fill(btnX, btnY, btnX + 70, btnY + 20, hovered ? 0xFF555555 : 0xFF333333);
        gui.renderOutline(btnX, btnY, 70, 20, 0xFF888888);
        gui.drawCenteredString(font, "← Назад", btnX + 35, btnY + 6, 0xFFFFFF);

        // ИСПРАВЛЕНО: Рассчитываем точную высоту контента
        int contentStartY = guiTop + 35;
        int maxWidth = guiWidth - 30;

        // Временный Y для расчёта высоты
        int calculatedY = 0;
//
//        // Фреска
//        calculatedY += (int)(FRESCO_HEIGHT * 0.5f) + 10;

        // Имя
        calculatedY += 15;

        // Сложность
        calculatedY += 15;

        // Описание
        calculatedY += wrapText(selectedCharacter.getDescription(), maxWidth).size() * 11 + 10;

        // Теги
        if (!selectedCharacter.getTags().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (String tag : selectedCharacter.getTags()) {
                String desc = TagRegistry.getTagDescription(tag);
                String fullText = "● " + tag + ": " + desc;
                // ИСПРАВЛЕНО: Учитываем перенос длинных тегов
                calculatedY += wrapText(fullText, maxWidth - 5).size() * 11;
            }
            calculatedY += 10;
        }

        // Особенности
        if (!selectedCharacter.getFeatures().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (CharacterClass.Feature feature : selectedCharacter.getFeatures()) {
                calculatedY += 11; // Название
                calculatedY += wrapText(feature.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            calculatedY += 10;
        }

        // Предметы
        if (!selectedCharacter.getItems().isEmpty()) {
            calculatedY += 12; // Заголовок
            for (CharacterClass.Item item : selectedCharacter.getItems()) {
                calculatedY += 11; // Название
                calculatedY += wrapText(item.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            calculatedY += 10;
        }

        // Дополнительные секции для конкретного персонажа
        calculatedY += extraSectionsHeight(selectedCharacter.getId(), maxWidth);

        // Рассчитываем максимальный скролл
        int visibleHeight = guiHeight - 65;
        int totalContentHeight = calculatedY;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);

        // ИСПРАВЛЕНО: Ограничиваем скролл
        detailScrollOffset = Math.min(detailScrollOffset, maxScroll);

        // Область скролла
        gui.enableScissor(guiLeft + 5, guiTop + 30, guiLeft + guiWidth - 5, guiTop + guiHeight - 30);

        int y = contentStartY - detailScrollOffset;

        // Фреска (большая)
//        int frescoX = guiLeft + (guiWidth - (int)(FRESCO_WIDTH * 0.5f)) / 2;
//        renderFresco(gui, selectedCharacter, frescoX, y, 0.5f);
//        y += (int)(FRESCO_HEIGHT * 0.5f) + 10;

        // Имя
        String typeColor = selectedCharacter.getType() == CharacterType.SURVIVOR ? "§a" : "§c";
        gui.drawCenteredString(font, typeColor + "§l" + selectedCharacter.getName(),
                guiLeft + guiWidth / 2, y, 0xFFFFFF);
        y += 15;

        // Сложность
        gui.drawCenteredString(font, "§7Сложность: " + selectedCharacter.getDifficultyStars(),
                guiLeft + guiWidth / 2, y, 0xFFFFFF);
        y += 15;

        // Описание
        List<String> descLines = wrapText(selectedCharacter.getDescription(), maxWidth);
        for (String line : descLines) {
            gui.drawString(font, "§7" + line, guiLeft + 15, y, 0xFFFFFF, false);
            y += 11;
        }
        y += 10;

        // Теги
        if (!selectedCharacter.getTags().isEmpty()) {
            gui.drawString(font, "§e§lТеги:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (String tag : selectedCharacter.getTags()) {
                String desc = TagRegistry.getTagDescription(tag);
                String fullText = "§e● " + tag + "§7: " + desc;

                // ИСПРАВЛЕНО: Переносим длинные теги
                List<String> tagLines = wrapText(fullText, maxWidth - 5);
                for (int i = 0; i < tagLines.size(); i++) {
                    gui.drawString(font, "§7" + tagLines.get(i), guiLeft + 20, y, 0xFFFFFF, false);
                    y += 11;
                }
            }
            y += 10;
        }

        // Особенности
        if (!selectedCharacter.getFeatures().isEmpty()) {
            gui.drawString(font, "§e§lОсобенности:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (CharacterClass.Feature feature : selectedCharacter.getFeatures()) {
                gui.drawString(font, "§6● " + feature.getName(), guiLeft + 20, y, 0xFFFFFF, false);
                y += 11;

                List<String> featureLines = wrapText(feature.getDescription(), maxWidth - 30);
                for (String line : featureLines) {
                    gui.drawString(font, "§7  " + line, guiLeft + 25, y, 0xFFFFFF, false);
                    y += 11;
                }
                y += 5;
            }
            y += 10;
        }

        // Предметы
        if (!selectedCharacter.getItems().isEmpty()) {
            gui.drawString(font, "§e§lПредметы:", guiLeft + 15, y, 0xFFFFFF, false);
            y += 12;

            for (CharacterClass.Item item : selectedCharacter.getItems()) {
                gui.drawString(font, "§b● " + item.getName(), guiLeft + 20, y, 0xFFFFFF, false);
                y += 11;

                List<String> itemLines = wrapText(item.getDescription(), maxWidth - 30);
                for (String line : itemLines) {
                    gui.drawString(font, "§7  " + line, guiLeft + 25, y, 0xFFFFFF, false);
                    y += 11;
                }
                y += 5;
            }
        }

        // ── Дополнительные секции для конкретного персонажа ──────────────────
        if (EXTRA_SECTIONS.containsKey(selectedCharacter.getId())) {
            y += 8;
            gui.fill(guiLeft + 15, y, guiLeft + guiWidth - 15, y + 1, 0xFF555555);
            y += 6;
            y = renderExtraSections(gui, selectedCharacter.getId(), y, maxWidth);
        }

        gui.disableScissor();

        // ИСПРАВЛЕНО: Показываем индикатор только если есть что прокручивать
        if (totalContentHeight > visibleHeight) {
            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 85,
                    guiTop + guiHeight - 32, 0xAAAAAA, false);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (selectedCharacter == null && hoveredCharacter != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredCharacter.getName()));

            String combatType = getCombatType(hoveredCharacter);
            if (combatType != null) {
                tooltip.add(Component.literal("§7" + combatType));
            }

            tooltip.add(Component.literal("§7Клик для подробностей"));
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
            if (selectedCharacter != null) {
                if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 75 &&
                        mouseY >= guiTop + guiHeight - 25 && mouseY < guiTop + guiHeight - 5) {
                    selectedCharacter = null;
                    detailScrollOffset = 0;
                    return true;
                }
            }

            // Фильтры типа
            if (selectedCharacter == null) {
                int btnY = guiTop + 45;
                int btnWidth = 85;
                int spacing = 5;
                int startX = guiLeft + (guiWidth - (btnWidth * 3 + spacing * 2)) / 2;

                if (mouseY >= btnY && mouseY < btnY + 18) {
                    if (mouseX >= startX && mouseX < startX + btnWidth) {
                        currentFilter = null;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + btnWidth + spacing && mouseX < startX + (btnWidth + spacing) * 2) {
                        currentFilter = CharacterType.SURVIVOR;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    } else if (mouseX >= startX + (btnWidth + spacing) * 2 && mouseX < startX + (btnWidth + spacing) * 3) {
                        currentFilter = CharacterType.MANIAC;
                        selectedTag = null;
                        scrollOffset = 0;
                        return true;
                    }
                }

                // Фильтр по тегам
                if (currentFilter != null) {
                    int tagY = guiTop + 70;
                    int tagX = guiLeft + 10 + font.width("Фильтр: ") + 5;

                    for (String tag : getAvailableTags()) {
                        int tagWidth = font.width(tag) + 8;

                        if (mouseX >= tagX && mouseX < tagX + tagWidth && mouseY >= tagY && mouseY < tagY + 16) {
                            selectedTag = selectedTag != null && selectedTag.equals(tag) ? null : tag;
                            scrollOffset = 0;
                            return true;
                        }

                        tagX += tagWidth + 3;
                        if (tagX > guiLeft + guiWidth - 50) {
                            tagX = guiLeft + 10;
                            tagY += 18;
                        }
                    }
                }
            }

            // Клик на персонажа
            if (hoveredCharacter != null && selectedCharacter == null) {
                selectedCharacter = hoveredCharacter;
                detailScrollOffset = 0;
                return true;
            }
        }
        return false;
    }

    /** Считает полную высоту контента детальной страницы персонажа. */
    private int calculateDetailHeight(CharacterClass character) {
        int maxWidth = guiWidth - 30;
        int h = 0;
        h += 15; // имя
        h += 15; // сложность
        h += wrapText(character.getDescription(), maxWidth).size() * 11 + 10;
        if (!character.getTags().isEmpty()) {
            h += 12;
            for (String tag : character.getTags()) {
                String desc = TagRegistry.getTagDescription(tag);
                h += wrapText("● " + tag + ": " + desc, maxWidth - 5).size() * 11;
            }
            h += 10;
        }
        if (!character.getFeatures().isEmpty()) {
            h += 12;
            for (CharacterClass.Feature f : character.getFeatures()) {
                h += 11;
                h += wrapText(f.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            h += 10;
        }
        if (!character.getItems().isEmpty()) {
            h += 12;
            for (CharacterClass.Item item : character.getItems()) {
                h += 11;
                h += wrapText(item.getDescription(), maxWidth - 30).size() * 11 + 5;
            }
            h += 10;
        }
        // Доп. секции
        int extraH = extraSectionsHeight(character.getId(), maxWidth);
        if (extraH > 0) h += 8 + 6 + extraH; // разделитель + отступ + контент
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedCharacter != null) {
            int maxScroll = Math.max(0, calculateDetailHeight(selectedCharacter) - (guiHeight - 65));
            detailScrollOffset = (int) Math.max(0, Math.min(maxScroll, detailScrollOffset - delta * 30));
        } else {
            List<CharacterClass> characters = getFilteredCharacters();
            int maxScroll = Math.max(0, characters.size() * (SCALED_FRESCO_HEIGHT + 10) - 150);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedCharacter != null && keyCode == 256) {
            selectedCharacter = null;
            detailScrollOffset = 0;
            return true;
        }
        return false;
    }

    private List<CharacterClass> getFilteredCharacters() {
        List<CharacterClass> characters = currentFilter != null
                ? CharacterRegistry.getClassesByType(currentFilter)
                : new ArrayList<>(CharacterRegistry.getAllClasses());

        if (selectedTag != null) {
            characters = characters.stream()
                    .filter(c -> c.getTags().contains(selectedTag))
                    .collect(Collectors.toList());
        }

        return characters;
    }

    private List<String> getAvailableTags() {
        return getFilteredCharacters().stream()
                .flatMap(c -> c.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String getCombatType(CharacterClass character) {
        if (character.getTags().contains("Ближний бой")) return "Ближний бой";
        if (character.getTags().contains("Дальний бой")) return "Дальний бой";
        return null;
    }

    private boolean isMouseOverCharacter(int mouseX, int mouseY, int x, int y, int width, int height, int minY) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height
                && mouseY >= minY && mouseY < guiTop + guiHeight - 10;
    }
}