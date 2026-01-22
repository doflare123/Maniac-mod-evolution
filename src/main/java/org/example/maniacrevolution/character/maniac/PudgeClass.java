package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class PudgeClass extends CharacterClass {
    public PudgeClass() {
        super("pudge", "Мясник", CharacterType.MANIAC,
                "Свежее мясо!",
                5, 2); // ID для scoreboard

        // Тэги
        addTag("Ближний бой");
        addTag("Контроль");

        // Особенности
        addFeature("Бросок хука", "[Активка КД ?] Бросает свой хук вперёд, и притягивает попавшее на него существо");
        addFeature("Свежее мясо", "Убийство дают ему +2 к максимальному здоровью");

        // Предметы
        addItem("Хук", "4 урона + Активка");
        addItem("Кожанная броня", "6 брони");
    }
}
