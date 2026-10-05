package org.example.maniacrevolution.character.maniac;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

/** Warden vision, sniff and barehanded combat. */
public final class WardenClass extends CharacterClass {
    public static final int SCOREBOARD_ID = 9;

    public WardenClass() {
        super("warden", "Варден", CharacterType.MANIAC,
                "Ориентируется по звуку: шум проявляет окружение и текущие модели игроков.",
                SCOREBOARD_ID, 5);
        addTag("Эхолокация");
        addFeature("Звуковое зрение", "Во время матча обычное зрение заменяется затухающими точками. Текущие модели скрываются за стенами.");
        addFeature("Движение", "Стоящие и движущиеся на Shift игроки белые; движущиеся без Shift выжившие синие. Раскрыть их может и чужой шум.");
        addFeature("Нюх", "После подготовки 0,5 с показывает затухающие следы движения выживших на 5 с. Следы живут 10 с; Shift ослабляет их. КД 10 с.");
        addFeature("Усиленный удар", "ЛКМ: 9 урона, дальность 3 блока, КД 5 с.");
        addFeature("Звуковая Волна", "С пустыми руками удерживайте ПКМ и отпустите: заряд 0,2–1 с, урон 3–12, дальность 8–40 блоков. Останавливается о стены, издаёт импульсы в полёте. КД 30 с.");
        addFeature("Панцирь Вардена", "Во время матча постоянный эффект даёт +15 брони, как полный железный комплект, без предметов в слотах брони.");
        addFeature("Рывок", "Обычная скорость −10%, быстрый бег +60%. Стамины хватает на 5 с бега; восстановление 15 с после задержки 1 с. После истощения остановите бег перед новым рывком.");
        addFeature("Тяжёлый прыжок", "После прыжка эффект запрещает следующий прыжок на 3 с.");
    }

    @Override
    public ResourceLocation getFrescoTexture() {
        return Maniacrev.loc("textures/gui/frescos/warden_portrait.png");
    }

    @Override
    public ResourceLocation getSelectionFrescoTexture() {
        return Maniacrev.loc("textures/gui/frescos/warden_card.png");
    }
}
