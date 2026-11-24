package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class ScientistClass extends CharacterClass {
    public static final int SCOREBOARD_ID = 10;
    public static final float HACK_SPEED_MULTIPLIER = 1.2f;

    public ScientistClass() {
        super("scientist", "Учёный", CharacterType.SURVIVOR,
                "I am perfectly suited for this environment",
                SCOREBOARD_ID, 4);

        // Тэги
        addTag("Хаккер");
        addTag("Побег");

        // Особенности
        addFeature("Быстрый взлом", "Ваши знания позволяют быстрее взламывать компьютеры на 20%");
        addFeature("Бонус генератора", "[После взлома] К каждому незяраженному добавляется по 500 очков");

        // Предметы
        addItem("Квантовый телепорт", "Помогает меняться местами с другими игроками и получает невидимость на 7 сек.");
    }
}
