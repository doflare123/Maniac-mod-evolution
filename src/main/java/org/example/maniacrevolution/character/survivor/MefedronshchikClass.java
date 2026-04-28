package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class MefedronshchikClass extends CharacterClass {

    public MefedronshchikClass() {
        super("mefedronshchik", "Мефедронщик", CharacterType.SURVIVOR,
                "Если двойная доза не поможет, то я даже не знаю!",
                9, 3); // ID для scoreboard

        // Тэги
        addTag("Отвлечение");
        addTag("Мобильность");
        addTag("Побег");

        // Особенности
        addFeature("Адреналин", "[После взлома] Получает ещё один Шприц адреналина");

        // Предметы
        addItem("Шприц адреналина", "Даёт скорость IV на 4 сек., но в конце даёт свечение на 2 сек.");
        addItem("Дуделка)))", "Спавнит дым вокруг");
    }
}