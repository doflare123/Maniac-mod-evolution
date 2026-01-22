package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class FreddyBearClass extends CharacterClass {

    public FreddyBearClass() {
        super("freddy_bear", "Мишка Фредди", CharacterType.MANIAC,
                "Оу оу оу оу оу оу оу",
                12, 5); // ID для scoreboard

        // Тэги
        addTag("Неожиданность");
        addTag("Контроль");
        addTag("Ближний бой");

        // Особенности
        addFeature("Песня Фредди", "[Активная КД ?] Станит всех врагов в радиусе 7 б.");
        addFeature("Генератор", "Спавнит на карте генератор, заряд которого если упадёт - то все выжившие получат эффект Тьмы. Изначально генератор включен");

        // Предметы
        addItem("Микрофон", "4 урона + стан (А)");
        addItem("Кожанная броня", "6 брони");
    }
}