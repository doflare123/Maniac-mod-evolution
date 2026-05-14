package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class GhostClass extends CharacterClass {

    public GhostClass() {
        super("ghost", "Призрак", CharacterType.MANIAC,
                "Чужое тело для него лишь временная оболочка.",
                13, 5);

        addTag("Контроль");
        addTag("Скрытность");
        addTag("Неожиданность");

        addFeature("Вселение", "[Активная КД 25] Крадучись ПКМ по выжившему захватывает его тело на 8 секунд.");
        addFeature("Подчинение", "Во время вселения жертва теряет контроль, а Призрак двигает её телом.");
        addFeature("Эфирная оболочка", "Во время контроля становится невидимым и смотрит глазами жертвы.");

        addItem("Эктоплазма", "Тестовая версия способности работает без отдельного предмета.");
    }
}
