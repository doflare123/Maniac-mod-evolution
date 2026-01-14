package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class AgentClass extends CharacterClass {
    public AgentClass() {
        super("agent", "Агент 47", CharacterType.MANIAC,
                "Пять врагов - пять наград",
                4); // ID для scoreboard

        // Тэги
        addTag("Дальний бой");
        addTag("Прогресс");

        // Особенности
        addFeature("Наёмный убийцы", "Всегда есть цель, за убийство которой дадут деньги");
        addFeature("Чёрный рынок", "Может за свои деньги покупать различные предметы");

        // Предметы
        addItem("Пистолет", "6.75 урона, 5 патрон");
        addItem("Нож", "2 урона");
        addItem("Кожанная броня", "6 брони");
    }
}
