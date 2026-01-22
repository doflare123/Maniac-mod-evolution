package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class PlagueDoctorClass extends CharacterClass {

    public PlagueDoctorClass() {
        super("plague_doctor", "Чумной доктор", CharacterType.MANIAC,
                "Мои снадобья дарят либо жизнь, либо вечный сон",
                6, 1); // ID для scoreboard

        // Тэги
        addTag("Периодический урон");
        addTag("AoE");
        addTag("Призыватель");
        addTag("Ближний бой");

        // Особенности
        addFeature("Чумная Аура", "Заражает всех врагов вокруг чумой");
        addFeature("Распространение чумы", "[После взлома] Появляется в случайном месте Чумной Зомби");

        // Предметы
        addItem("Коса", "4 урона + аура чумы");
        addItem("Кожанная броня", "6 брони");
    }
}