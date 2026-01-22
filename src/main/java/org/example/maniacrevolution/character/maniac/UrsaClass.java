package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class UrsaClass extends CharacterClass {

    public UrsaClass() {
        super("ursa", "Урса", CharacterType.MANIAC,
                "Не буди во мне зверя. Первобытная ярость медведя не знает пределов - каждый удар делает Урсу сильнее, а её мощные лапы способны настигнуть жертву молниеносным прыжком.",
                7, 2); // ID для scoreboard

        // Тэги
        addTag("Мобильность");
        addTag("Преследователь");
        addTag("Ближний бой");

        // Особенности
        addFeature("Острые когти", "При ударе игрока накладывает стакающийся эффект, который добавляет урон к атаке по этому игроку");
        addFeature("Прыжок", "[Активная КД 10] Может прыгать на несколько блоков вперёд");

        // Предметы
        addItem("Кожаная броня", "6 единиц брони");
        addItem("Лапа медведя", "0.5 урона");
    }
}