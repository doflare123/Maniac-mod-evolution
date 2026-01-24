package org.example.maniacrevolution.gui.pages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.gui.GuideScreen;

import java.util.ArrayList;
import java.util.List;

public class TutorialPage extends GuidePage {
    private int scrollOffset = 0;
    private List<Section> sections = new ArrayList<>();
    private Section hoveredLink = null;

    public TutorialPage(GuideScreen parent) {
        super(parent);
        initContent();
    }

    private void initContent() {
        sections.clear();

        // Заголовок
        sections.add(new TitleSection("§6§l✦ ПОЛНЫЙ ГАЙД ПО РЕЖИМУ ✦"));
        sections.add(new SpacerSection(10));

        // Введение
        sections.add(new HeaderSection("§e§l📖 Введение"));
        sections.add(new TextSection(
                "Добро пожаловать в режим Maniac! Это асимметричный PvP режим, вдохновленный игрой DBD, " +
                        "где команда выживших противостоит безжалостным маньякам. " +
                        "Выживание требует командной работы, стратегии и умения использовать перки."
        ));
        sections.add(new SpacerSection(15));

        // Правила игры
        sections.add(new HeaderSection("§e§l⚔ Основные правила"));
        sections.add(new TextSection(
                "§7Команды:§r Игроки делятся на две команды - §bВыжившие§r и §cМаньяки§r."
        ));
        sections.add(new TextSection(
                "§7Цель выживших:§r Хакнуть все компьютеры и убить маньяка до окончания времени игры"
        ));
        sections.add(new TextSection(
                "§7Цель маньяков:§r Устранить всех выживших или дожить до окончания времени."
        ));
        sections.add(new SpacerSection(10));

        // Фазы игры
        sections.add(new HeaderSection("§e§l⏱ Фазы игры"));
        sections.add(new TextSection(
                "§6Фаза 1 - Охота:§r Маньяки ищут выживших. Некоторые перки недоступны."
        ));
        sections.add(new TextSection(
                "§6Фаза 2 - Мидгейм:§r Прошла половина времени от таймера. Открываются дополнительные перки. Игра становится интенсивнее."
        ));
        sections.add(new TextSection(
                "§6Фаза 3 - Переворот:§r Если выжившие хакнули все компы, то они получают карточку для открытия сейфов"
        ));
        sections.add(new SpacerSection(15));

        // Перки
        sections.add(new HeaderSection("§e§l⚡ Система перков"));
        sections.add(new TextSection(
                "Перки - это уникальные способности, которые дают преимущества в бою. " +
                        "Существует три типа перков:"
        ));
        sections.add(new TextSection("§9● Пассивные§r - работают автоматически"));
        sections.add(new TextSection("§c● Активные§r - требуют активации клавишей"));
        sections.add(new TextSection("§d● Гибридные§r - имеют пассивный эффект и активацию"));
        sections.add(new SpacerSection(5));
        sections.add(new LinkSection("§n➤ Полный список перков", PageType.PERKS));
        sections.add(new SpacerSection(15));

        // Карты
        sections.add(new HeaderSection("§e§l🗺 Карты"));
        sections.add(new TextSection(
                "Каждая карта имеет уникальные особенности, которые влияют на геймплей. " +
                        "Изучите карту перед игрой, чтобы использовать её преимущества."
        ));
        sections.add(new SpacerSection(5));
        sections.add(new LinkSection("§n➤ Подробнее о картах", PageType.MAPS));
        sections.add(new SpacerSection(15));

        // === НОВОЕ: Основы режима ===
        sections.add(new HeaderSection("§e§l🎮 Основы режима"));
        sections.add(new SpacerSection(5));

        // 1. Кастомный худ
        sections.add(new SubHeaderSection("§61. Кастомный HUD"));
        sections.add(new ImageSection("guide/before_start_game/custom_hud.png", 450, 150));
        sections.add(new TextSection(
                "Его особенность заключается в том, что переработана система отображения здоровья в полоску, " +
                        "также добавлена система маны для некоторых способностей (способности привязаны к героям, " +
                        "поэтому нужно смотреть описание героев) и перков."
        ));
        sections.add(new TextSection(
                "В худе будут отображаться перки и способности после их получения и выбора. " +
                        "Вы можете увидеть манакост перков, способностей, также увидеть на какие кнопки вы назначили " +
                        "применение и смену перков, а также какого типа данные перки. " +
                        "После применения у перка показывается кулдаун."
        ));
        sections.add(new SpacerSection(5));
        sections.add(new ImageSection("guide/before_start_game/full_custom_hud.png", 450, 150));
        sections.add(new SpacerSection(5));
        sections.add(new LinkSection("§n➤ Подробнее о персонажах", PageType.CHARACTERS));
        sections.add(new SpacerSection(10));

        // 2. Мана
        sections.add(new SubHeaderSection("§62. Система маны"));
        sections.add(new TextSection(
                "Уникальная система для этого пака. Мана - конечный ресурс, поэтому её менеджмент " +
                        "является одной из главных механик у некоторых классов, у которых есть активные скиллы."
        ));
        sections.add(new SpacerSection(10));

        // 3. Пик карты
        sections.add(new SubHeaderSection("§63. Выбор карты"));
        sections.add(new ImageSection("guide/before_start_game/pick_map.png", 450, 400));
        sections.add(new TextSection(
                "Выбираете карту, подтверждаете. Если голоса распределились одинаково, то запускается " +
                        "анимация и рандомизация, после чего в чате пишется карта, которая будет у вас в игре."
        ));
        sections.add(new SpacerSection(10));

        // 4. Начало игры
        sections.add(new SubHeaderSection("§64. Как начинается игра"));
        sections.add(new ImageSection("guide/before_start_game/items_for_game.png", 200, 150));
        sections.add(new TextSection(
                "Когда вас распределяет по лобби (выживших и маньяков), вам выдаются предметы:"
        ));
        sections.add(new TextSection("§7● §61 слот§r - выбор перков"));
        sections.add(new TextSection("§7● §62 слот§r - выбор класса (текстура зависит от команды)"));
        sections.add(new TextSection("§7● §63 слот§r - кнопка готовности"));
        sections.add(new TextSection(
                "Кнопку готовности можно прожать только если вы выбрали перки, класс и карта уже выбрана."
        ));
        sections.add(new SpacerSection(10));

        // 5. Пик перков
        sections.add(new SubHeaderSection("§65. Выбор перков"));
        sections.add(new ImageSection("guide/before_start_game/perks.png", 450, 300));
        sections.add(new TextSection(
                "Чтобы узнать что делает данный перк, на какой стадии он работает и " +
                        "активный/пассивный/гибридный ли он, а также его перезарядку - наведитесь на него мышкой. " +
                        "Чтобы выбрать перк, кликните левой кнопкой мыши - выбранный перк обведется зеленым."
        ));
        sections.add(new SpacerSection(10));

        // 6. Пик класса
        sections.add(new SubHeaderSection("§66. Выбор персонажа"));
        sections.add(new ImageSection("guide/before_start_game/pick_hero.png", 450, 450));
        sections.add(new TextSection(
                "Чтобы листать персонажей, используйте стрелочки на клавиатуре или на интерфейсе. " +
                        "Слева есть фильтры для классов, а справа описание класса. " +
                        "После выбора нажмите кнопку «Выбрать»."
        ));
        sections.add(new SpacerSection(10));

        // 7. Готовность
        sections.add(new SubHeaderSection("§67. Готовность"));
        sections.add(new TextSection(
                "После того как класс и перк выбраны, вы можете активировать предмет готовности. " +
                        "При готовности каждого из игроков в чате появится следующее сообщение:"
        ));
        sections.add(new ImageSection("guide/before_start_game/start_game.png", 450, 50));
        sections.add(new SpacerSection(10));

        // 8. Спавн игроков
        sections.add(new SubHeaderSection("§68. Места спавна на картах"));

        // Особняк
        sections.add(new TextSection("§e§lОсобняк:"));
        sections.add(new TextSection("§bВыжившие:"));
        sections.add(new ImageSection("guide/start_game/start_survivors_mansion.png", 500, 300));
        sections.add(new TextSection("§cМаньяки:"));
        sections.add(new ImageSection("guide/start_game/start_maniac_mansion.png", 500, 300));
        sections.add(new SpacerSection(5));

        // Пиццерия
        sections.add(new TextSection("§e§lПиццерия Фрэдэ:"));
        sections.add(new TextSection("§bВыжившие:"));
        sections.add(new ImageSection("guide/start_game/start_survivors_freddy.png", 500, 300));
        sections.add(new TextSection("§cМаньяки:"));
        sections.add(new ImageSection("guide/start_game/start_maniac_freddy.png", 500, 300));
        sections.add(new SpacerSection(10));

        // 9. Основная механика
        sections.add(new SubHeaderSection("§69. Основная механика карт"));
        sections.add(new TextSection("§e§lХак компьютера:"));
        sections.add(new ImageSection("guide/maps/computers_watch.png", 600, 300));
        sections.add(new TextSection(
                "Обычно они помечены блоком компьютера и нажимной плитой на изумрудном блоке. " +
                        "Чтобы начать взламывать компьютер, встаньте на плиту. " +
                        "Если планируете взламывать не в одиночку (это ускоряет взлом), " +
                        "ваши союзники должны находиться в окружности, которая очерчивается при начале взлома."
        ));
        sections.add(new SpacerSection(5));
        sections.add(new TextSection(
                "§c§lВажная механика!§r Периодически раз в 3-5 секунд будет выскакивать QTE-игра. " +
                        "Если вы успеваете нажать, зарядка ускоряется на 5%."
        ));
        sections.add(new ImageSection("guide/mechanics/qte.png", 150, 150));
        sections.add(new SpacerSection(5));
        sections.add(new TextSection(
                "§e§lПодсветка в начале игры:§r Для всех, в том числе и маньяков, " +
                        "включается подсветка нахождения компьютеров. Маньяки их видят всю игру, " +
                        "а выжившие только первые 40 секунд."
        ));
        sections.add(new TextSection(
                "Если на карте присутствует игрок с классом «Алхимик», то ему также на протяжении всей игры " +
                        "подсвечиваются места нахождения зельеварок."
        ));
        sections.add(new SpacerSection(5));
        sections.add(new TextSection("§e§lКонец игры?"));
        sections.add(new TextSection(
                "Конец игры (стадия переворот) наступает только тогда, когда игроки хакают нужное количество " +
                        "компьютеров, после чего им выдаются ключ-карты от сейфов с оружием (оружие ограничено по количеству)."
        ));
        sections.add(new ImageSection("guide/maps/safe_with_weapon.png", 450, 300));
        sections.add(new SpacerSection(10));

        // 10. Особенности карт
        sections.add(new SubHeaderSection("§610. Особенности карт"));
        sections.add(new TextSection("§e§lОсобняк - потайные ходы:"));
        sections.add(new TextSection("Потайные двери:"));
        sections.add(new ImageSection("guide/mechanics/close_door.png", 450, 200));
        sections.add(new ImageSection("guide/mechanics/open_door.png", 450, 200));
        sections.add(new SpacerSection(5));
        sections.add(new TextSection("Потайные люки:"));
        sections.add(new ImageSection("guide/mechanics/close_hatch.png", 450, 200));
        sections.add(new ImageSection("guide/mechanics/open_hatch.png", 450, 200));
        sections.add(new SpacerSection(5));
        sections.add(new TextSection("§e§lПиццерия Фрэдэ - канализации и вентиляции:"));
        sections.add(new ImageSection("guide/mechanics/ventilation.png", 450, 200));
        sections.add(new SpacerSection(15));

        // Заключение
        sections.add(new HeaderSection("§e§l✨ Заключение"));
        sections.add(new TextSection(
                "Это основные вещи, что вам нужно знать про игру! Дальше вы отправляетесь в свободное плаванье, " +
                        "в котором вы сможете изучить карту лучше, а также придумать фишечки для игры. " +
                        "Поэтому желаем вам удачи и приятной игры!"
        ));
        sections.add(new SpacerSection(10));
        sections.add(new TextSection(
                "§7§lP.S.§r Если вам понравится карта, то можете присоединиться к нашему ТГК: §9§nhttps://t.me/necrodwarfs§r " +
                        "или найдите его по §9@necrodwarfs§r в Telegram. Также у нас много других проектов, о которых вы можете " +
                        "узнать в нашем ТГК (ну, или просто зайдите и поделитесь впечатлениями/механиками, которые вы нашли, " +
                        "или на крайний случай багами)."
        ));
        sections.add(new SpacerSection(10));
        sections.add(new TextSection(
                "§6§lЭтот гайд не одноразовый!§r Вы сможете его открыть повторно, если настроите бинд клавиши в настройках, " +
                        "поэтому не теряйте!"
        ));
        sections.add(new SpacerSection(20));
    }

    @Override
    public void init(int guiLeft, int guiTop, int guiWidth, int guiHeight) {
        super.init(guiLeft, guiTop, guiWidth, guiHeight);
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackButton(gui, mouseX, mouseY);

        hoveredLink = null;

        gui.enableScissor(guiLeft + 5, guiTop + 30, guiLeft + guiWidth - 5, guiTop + guiHeight - 15);

        int y = guiTop + 35 - scrollOffset;
        int maxWidth = guiWidth - 30;

        for (Section section : sections) {
            int sectionHeight = section.getHeight(maxWidth);

            if (y + sectionHeight > guiTop + 30 && y < guiTop + guiHeight - 15) {
                section.render(gui, guiLeft + 15, y, maxWidth, mouseX, mouseY);

                if (section instanceof LinkSection link) {
                    if (link.isHovered(mouseX, mouseY, guiLeft + 15, y, maxWidth)) {
                        hoveredLink = link;
                    }
                }
            }

            y += sectionHeight;
        }

        gui.disableScissor();

        int totalHeight = sections.stream().mapToInt(s -> s.getHeight(maxWidth)).sum();
        if (totalHeight > guiHeight - 50) {
            gui.fill(guiLeft + guiWidth - 90, guiTop + guiHeight - 18,
                    guiLeft + guiWidth - 5, guiTop + guiHeight - 5, 0xAA000000);
            gui.drawString(font, "§8↑↓ Прокрутка", guiLeft + guiWidth - 85,
                    guiTop + guiHeight - 15, 0xAAAAAA, false);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 85 && mouseY >= guiTop + 5 && mouseY < guiTop + 23) {
                parent.switchPage(PageType.MAIN);
                return true;
            }

            if (hoveredLink instanceof LinkSection link) {
                parent.switchPage(link.targetPage);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxWidth = guiWidth - 30;
        int totalHeight = sections.stream().mapToInt(s -> s.getHeight(maxWidth)).sum();
        int maxScroll = Math.max(0, totalHeight - (guiHeight - 50));

        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 30));
        return true;
    }

    // === Секции контента ===

    private abstract class Section {
        abstract int getHeight(int maxWidth);
        abstract void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY);
    }

    private class TitleSection extends Section {
        String text;

        TitleSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            return 15;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            gui.drawCenteredString(font, text, x + maxWidth / 2, y, 0xFFFFFF);
        }
    }

    private class HeaderSection extends Section {
        String text;

        HeaderSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            return 14;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            gui.drawString(font, text, x, y, 0xFFFFFF, false);
        }
    }

    // НОВОЕ: Подзаголовок
    private class SubHeaderSection extends Section {
        String text;

        SubHeaderSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            return 12;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            gui.drawString(font, text, x, y, 0xFFFFFF, false);
        }
    }

    private class TextSection extends Section {
        String text;

        TextSection(String text) {
            this.text = text;
        }

        @Override
        int getHeight(int maxWidth) {
            List<String> lines = wrapText(text, maxWidth);
            return lines.size() * 11 + 2;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            List<String> lines = wrapText(text, maxWidth);
            for (int i = 0; i < lines.size(); i++) {
                gui.drawString(font, lines.get(i), x, y + i * 11, 0xFFFFFF, false);
            }
        }
    }

    // НОВОЕ: Секция с картинкой
    // УПРОЩЕННАЯ версия: автоматический размер из ресурса
    private class ImageSection extends Section {
        String imagePath;
        int displayWidth, displayHeight;

        // Конструктор с оригинальным размером (автомасштабирование)
        ImageSection(String imagePath) {
            this.imagePath = imagePath;
            this.displayWidth = -1; // Флаг для автоматического определения
            this.displayHeight = -1;
        }

        // Конструктор с указанным размером
        ImageSection(String imagePath, int width, int height) {
            this.imagePath = imagePath;
            this.displayWidth = width;
            this.displayHeight = height;
        }

        @Override
        int getHeight(int maxWidth) {
            // Если размер не указан, используем примерный
            return displayHeight > 0 ? displayHeight + 10 : 200;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            ResourceLocation texture = new ResourceLocation("maniacrev", "textures/gui/" + imagePath);

            try {
                int width = displayWidth;
                int height = displayHeight;

                // НОВОЕ: Если размер не задан, берем из файла
                if (width < 0 || height < 0) {
                    // Используем реальный размер PNG
                    // Для этого нужно загрузить картинку
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    net.minecraft.client.renderer.texture.AbstractTexture tex = mc.getTextureManager().getTexture(texture);

                    // Простой подход: используем стандартные размеры скриншотов
                    width = 640;  // Типичная ширина скриншота
                    height = 360; // Типичная высота (16:9)

                    // Масштабируем если не влезает
                    if (width > maxWidth) {
                        float scale = (float) maxWidth / width;
                        height = (int) (height * scale);
                        width = maxWidth;
                    }
                }

                int imgX = x + (maxWidth - width) / 2; // Центрируем

                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();
                gui.blit(texture, imgX, y, 0, 0, width, height, width, height);
                RenderSystem.disableBlend();

            } catch (Exception e) {
                // Заглушка
                int fallbackWidth = displayWidth > 0 ? displayWidth : 300;
                int fallbackHeight = displayHeight > 0 ? displayHeight : 150;
                int imgX = x + (maxWidth - fallbackWidth) / 2;

                gui.fill(imgX, y, imgX + fallbackWidth, y + fallbackHeight, 0xFF333333);
                gui.renderOutline(imgX, y, fallbackWidth, fallbackHeight, 0xFF666666);
                gui.drawCenteredString(font, "§cИзображение не найдено",
                        imgX + fallbackWidth / 2, y + fallbackHeight / 2 - 4, 0xFFFFFF);
                gui.drawString(font, "§8" + imagePath, imgX + 2, y + fallbackHeight - 10, 0x888888, false);
            }
        }
    }

    private class LinkSection extends Section {
        String text;
        PageType targetPage;

        LinkSection(String text, PageType targetPage) {
            this.text = text;
            this.targetPage = targetPage;
        }

        @Override
        int getHeight(int maxWidth) {
            return 14;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY, x, y, maxWidth);
            String displayText = hovered ? "§e" + text : "§e" + text;

            gui.drawString(font, displayText, x, y, 0xFFFFFF, false);

            if (hovered) {
                gui.drawString(font, "§7(клик для перехода)", x + font.width(text.replace("§n", "")) + 5, y, 0xAAAAAA, false);
            }
        }

        boolean isHovered(int mouseX, int mouseY, int x, int y, int maxWidth) {
            int textWidth = font.width(text.replace("§n", ""));
            return mouseX >= x && mouseX < x + textWidth && mouseY >= y && mouseY < y + 11;
        }
    }

    private class SpacerSection extends Section {
        int height;

        SpacerSection(int height) {
            this.height = height;
        }

        @Override
        int getHeight(int maxWidth) {
            return height;
        }

        @Override
        void render(GuiGraphics gui, int x, int y, int maxWidth, int mouseX, int mouseY) {
            // Пустое пространство
        }
    }
}