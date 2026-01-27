package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class NecromancerClass extends CharacterClass {
    public NecromancerClass() {
        super("necromancer", "Некромант", CharacterType.SURVIVOR,
                "Душа ушла к богам, так что тело им не нужно",
                8, 2); // ID для scoreboard

        // Тэги
        addTag("Поддержка");
        addTag("Прогресс");
        addTag("Отвлечение");
        addTag("Живучесть");

        // Особенности
        addFeature("Защита от смерти", "Может 1 раз обмануть смерть (Пережить 1 смертельный удар)");

        // Предметы
        addItem("Прах", "[Можно поставить] Накладывает слепоту тому кто наступил на неё");
        addItem("Посох Мёртвых", "Возрождает 1 из мёртвых союзников в виде медленного зомби");
    }
}
