package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class DoctorClass extends CharacterClass {
    public DoctorClass() {
        super("doctor", "Доктор", CharacterType.SURVIVOR,
                "Врача вызывали?",
                5); // ID для scoreboard

        // Тэги
        addTag("Поддержка");
        addTag("Живучесть");

        // Особенности
        addFeature("Большой брат", "Может видеть сколько у кого остлось здоровья");
        addFeature("Благодарность", "[Активка КД ?] Следующая 1 атака по доктору в следующие 30 секунд перенесётся на ближайшего союзника в радиусе 7 б.");

        // Предметы
        addItem("Бинты", "5 штук");
        addItem("Аптечка", "Использует бинты для исцеления других выживших");
    }
}
