package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class DodepovichClass extends CharacterClass {
    public DodepovichClass() {
        super("dodepovich", "Никита Додепович", CharacterType.SURVIVOR,
                "Азартный выживший, который решает проблемы монеткой и автоматом. Удача может спасти его, но долг казино всегда найдёт способ вернуться.",
                13, 4);

        addTag("Риск");
        addTag("Поддержка");
        addTag("Мобильность");
        addTag("Помеха");
        addTag("Живучесть");

        addFeature("Монетки", "Может подбрасывать особые монетки: 50% шанс на хороший эффект и 50% шанс на плохой.");
        addFeature("Казино-автомат", "Может вставлять монетки в автомат. Эффект срабатывает только при выпадении трёх одинаковых символов.");
        addFeature("Джекпот", "Редкий выигрыш даёт мощную регенерацию, скорость и силу на 60 секунд.");

        addItem("Монетки", "Расходуемые предметы с разными эффектами, включая Монеточку Судьбы.");
        addItem("Казино-автомат", "Блок для рискованного розыгрыша монетки.");
    }
}
