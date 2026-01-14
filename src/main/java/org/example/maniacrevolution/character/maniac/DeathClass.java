package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class DeathClass extends CharacterClass {

    public DeathClass() {
        super("death", "Смерть", CharacterType.MANIAC,
                "Memento mori",
                10); // ID для scoreboard

        // Тэги
        addTag("Прогресс");
        addTag("Мобильность");
        addTag("Неожиданность");
        addTag("Ближний бой");

        // Особенности
        addFeature("Рабочие будни", "Видит в радиусе 7 блоков выживших, которые имеют меньше 10 хп");
        addFeature("Судный час", "[Активная КД 90] Может телепортироваться к случайному выжившему");
        addFeature("Смертельная гонка", "Каждое убийство усиливает урон");

        // Предметы
        addItem("Коса смерти", "4 урона");
        addItem("Броня смерти", "7 брони + телепортация");
    }
}