package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.helsing.HelsingConfig;

public final class HelsingClass extends CharacterClass {
    public HelsingClass() {
        super("helsing", "Хельсинг", CharacterType.SURVIVOR,
                "Ни одна блядина не уйдет от меня!", HelsingConfig.CLASS_ID, 4);
        addTag("Ослабление"); addTag("Разведка"); addTag("Поддержка");
        addFeature("Заговорённая кровь", "V: защита себя или союзника под прицелом до 5 блоков. Первый урон маньяка переносит на него слабость и остановку перезарядок способностей и перков.");
        addFeature("Изучение добычи", "Удерживайте маньяка рядом с прицелом 3 секунды: видны его ресурс и готовность способностей и перков. После потери контакта сведения сохраняются 2 секунды.");
        addItem("Джекал", "Арбалет с железными, руническими, серебряными и заговорёнными болтами. Боезапас выдаётся на карте.");
        addItem("Шляпа Хельсинга", "Красная широкополая шляпа с оранжевыми очками. Позволяет наложить заговорённую кровь.");
    }
}
