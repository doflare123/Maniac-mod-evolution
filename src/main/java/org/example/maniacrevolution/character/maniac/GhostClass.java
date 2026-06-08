package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public class GhostClass extends CharacterClass {

    public GhostClass() {
        super("ghost", "Призрак", CharacterType.MANIAC,
                "Чужое тело для него лишь временная оболочка.",
                8, 5);

        addTag("Контроль");
        addTag("Скрытность");
        addTag("Неожиданность");
        addTag("Мобильность");

        addFeature("Призрачная рука", "[Активная КД 60] Shift + ПКМ по выжившему: вселение на 30 сек.");
        addFeature("Подчинение", "Во время вселения и Призрак, и жертва получают Speed I.");
        addFeature("Фантомный срыв", "[Активная] Игрушечный нож даёт полную невидимость без ограничения по времени.");
        addFeature("Срыв маски", "Повторный ПКМ завершает инвиз: 2 сек не может атаковать и остаётся скрытым.");

        addItem("Игрушечный нож", "2 урона, ПКМ - полная невидимость.");
        addItem("Призрачная рука", "Shift + ПКМ по выжившему - вселение. Повторный Shift + ПКМ - выйти раньше.");
        addItem("Белая кожаная броня", "Видима обычно, но исчезает при полном инвизе и одержимости.");
    }
}
