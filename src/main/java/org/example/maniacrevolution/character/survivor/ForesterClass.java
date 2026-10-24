package org.example.maniacrevolution.character.survivor;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.forester.ForesterRules;

public final class ForesterClass extends CharacterClass {
    public ForesterClass() {
        super("forester", "Лесничий", CharacterType.SURVIVOR,
                "Знает каждую тропу и встречает незваных гостей во всеоружии.", ForesterRules.CLASS_ID, 3);
        addTag("Ловушки"); addTag("Разведка"); addTag("Контроль");
        addFeature("Осторожно, капкан", "Капканы и перцовый дым действуют на всех, включая владельца. Фонарик ослепляет только маньяков.");
        addItem("Капкан", "Задерживает на 1,5 с, затем даёт 5 с на два попадания пробелом. Удар другого игрока освобождает и вызывает кровотечение. ПКМ запускает установку или зарядку на 2,5 с; Ctrl + ПКМ — сбор на 1,5 с. Удерживать кнопку не нужно. Движение или поворот прерывают действие.");
        addItem("Перцовая шашка", "Удерживайте ПКМ для прицеливания, отпустите для броска. Дым 15 с, радиус 5 блоков. Чихание выдаёт позицию; эффект длится ещё 5 с после выхода.");
        addItem("Фонарик", "Удерживайте ПКМ до 3 с: свет и ослепление маньяков до 10 блоков. Белый экран сохраняется 2 с после потери луча. Число включений ограничено.");
    }
}
