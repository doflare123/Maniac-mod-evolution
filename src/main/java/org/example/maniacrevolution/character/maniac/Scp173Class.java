package org.example.maniacrevolution.character.maniac;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

/** Rigid statue form, server gaze/blink rules and bare-hand combat. */
public final class Scp173Class extends CharacterClass {
    public static final int SCOREBOARD_ID = 13;

    public Scp173Class() {
        super("scp_173", "SCP-173", CharacterType.MANIAC,
                "Статуя: взгляд живого, не лежащего выжившего останавливает движение и запрещает атаку.",
                SCOREBOARD_ID, 5);
        addTag("Контроль");
        addFeature("Удержание взглядом", "Видимая часть тела в пределах 20 блоков и области 120° × 80° удерживает статую. Стекло пропускает взгляд.");
        addFeature("Под взглядом", "Камеру можно поворачивать, пока остальным видна неподвижная статуя. Перки, способности, гравитация и внешние толчки продолжают действовать.");
        addFeature("Моргание", "Выжившие моргают на 0,5 с; ручное моргание — C. Давление нарастает за 15 с и восстанавливается за 15 с без взгляда.");
        addFeature("Напряжение", "10 с удержания взглядом заряжают способность. Заряд сохраняется. На полном заряде ПКМ заставляет всех выживших в радиусе 20 блоков моргнуть на 0,5 с и тратит заряд.");
        addFeature("Погасить свет", "V: 3 с темноты на всю карту. КД 180 с, 10 маны. Наблюдатели ускоряют КД и накапливают скидку следующего применения.");
        addFeature("Удар рукой", "10 урона, обычная дальность удара, КД 5 с. Прицел отдельно показывает удержание и близкую цель; импакт появляется после подтверждённого попадания.");
        addFeature("Каменная оболочка", "Снимаемый эффект даёт 7 брони, как полный кожаный комплект. Обычные удары и стрелы не отбрасывают; импульсы перков сохраняются.");
        addFeature("Разгон", "После 8 с удержания разгон даёт до +50% к обычной скорости сверх базовых +50% статуи. За 2 с движения разгон спадает до 0%. Вне поля зрения в покое разгон сохраняется.");
        addFeature("Движение", "Обычный прыжок — раз в 1 с: ограничение показано эффектом восстановления прыжка. При плавании статуя наклоняется целиком.");
    }

    @Override
    public ResourceLocation getFrescoTexture() {
        return Maniacrev.loc("textures/gui/frescos/scp_173_portrait_lore.png");
    }

    @Override
    public ResourceLocation getSelectionFrescoTexture() {
        return Maniacrev.loc("textures/gui/frescos/scp_173_card_lore.png");
    }
}
