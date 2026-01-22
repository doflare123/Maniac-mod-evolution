package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class ShamanClass extends CharacterClass {

    public ShamanClass() {
        super("shaman", "Шаман", CharacterType.SURVIVOR, "Абуталабашунеба", 1, 3); // ID для scoreboard

        // Тэги
        addTag("Видение");
        addTag("Поддержка");
        addTag("Живучесть");

        // Особенности
        addFeature("Духи предков", "Видит вокруг маньяка летающих маленьких духов");
        addFeature("Помощь предков", "[После взлома] Получает Тотем");

        // Предметы
        addItem("Тотем", "[Можно поставить] Подсвечивает маньяка вокруг себя");
        addItem("Душа предков", "Восстанавливает 8 хп");
    }
}