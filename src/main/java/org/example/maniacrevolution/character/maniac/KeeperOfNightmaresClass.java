package org.example.maniacrevolution.character.maniac;

import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.nightmare.NightmareConfig;

public class KeeperOfNightmaresClass extends CharacterClass {
    public KeeperOfNightmaresClass() {
        super("keeper_of_nightmares", "Хранитель кошмаров", CharacterType.MANIAC,
                "Медленный маньяк, ломающий рассудок взглядом и отправляющий выживших в кошмарные испытания.",
                NightmareConfig.KEEPER_CLASS_ID, 5);

        addTag("Контроль");
        addTag("Испытания");
        addTag("Психологическое давление");

        addFeature("Взгляд бездны", "Долгий взгляд снижает рассудок выжившего. При срыве рассудка начинается испытание.");
        addFeature("Кошмарные испытания", "Первые три похищения ведут в лабиринт, затем открываются другие испытания.");
        addFeature("Концентрированный кошмар", "Шлем Хранителя вызывает скример у цели и снимает часть рассудка.");

        addItem("Голова Хранителя", "Шлем со способностью Концентрированный кошмар.");
    }
}
