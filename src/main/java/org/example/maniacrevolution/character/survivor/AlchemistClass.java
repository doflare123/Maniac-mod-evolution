package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class AlchemistClass extends CharacterClass {

    public AlchemistClass() {
        super("alchemist", "Алхимик", CharacterType.SURVIVOR,
                "Я соединю весь мир!",
                6, 4); // ID для scoreboard

        // Тэги
        addTag("Поддержка");
        addTag("Отвлечение");
        addTag("Побег");
        addTag("Помеха");
        addTag("Живучесть");

        // Особенности
        addFeature("Зельевар",
                "[Старт] На каждой карте появляются 3 зельеварки и всю игру подсвечиваются Алхимику фиолетовым");
        addFeature("Сбор ингридиентов", "[После взлома] Получает случайный ингридиент");

        // Предметы
        addItem("Набор алхимика", "5 взрывных зелья воды для варки, 3 случайных ингредиента для варки и огненный порошок");
        addItem("Зельеварка",
                "На каждой карте появляются 3 зельеварки. Алхимику они всю игру подсвечиваются фиолетовым.");
    }
}
