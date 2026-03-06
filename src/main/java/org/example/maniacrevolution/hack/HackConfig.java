package org.example.maniacrevolution.hack;

/**
 * Конфигурация механики взлома компьютеров.
 * Все параметры — в одном месте.
 */
public class HackConfig {

    // ── Очки взлома ───────────────────────────────────────────────────────────
    /** Сколько очков нужно набрать чтобы взломать ОДИН компьютер */
    public static float HACK_POINTS_REQUIRED = 10.0f;

    /** Базовый прирост очков в секунду на одного игрока (кроме спеца) */
    public static float POINTS_PER_PLAYER_PER_SECOND = 0.1f;

    /** Прирост очков для игрока с SurvivorClass == SPECIALIST_CLASS */
    public static float POINTS_PER_SPECIALIST_PER_SECOND = 0.15f;

    /** Scoreboard-объектив для определения класса */
    public static String SURVIVOR_CLASS_OBJECTIVE = "SurvivorClass";

    /** Значение SurvivorClass для "специалиста" (ускоряет взлом) */
    public static int SPECIALIST_CLASS = 10;

    /** Максимальное количество игроков-бонусников на одном компьютере */
    public static int MAX_BONUS_PLAYERS = 4;

    // ── Радиусы ───────────────────────────────────────────────────────────────
    /** Радиус (в блоках) в котором хакер должен находиться у компьютера */
    public static double HACKER_RADIUS = 1.7;

    /** Радиус (в блоках) в котором стоят помощники */
    public static double SUPPORT_RADIUS = 3.0;

    // ── QTE ───────────────────────────────────────────────────────────────────
    /** Минимальная задержка между QTE (секунды) */
    public static int QTE_INTERVAL_MIN_SECONDS = 3;

    /** Максимальная задержка между QTE (секунды) */
    public static int QTE_INTERVAL_MAX_SECONDS = 5;

    /** Бонус очков за успешный QTE */
    public static float QTE_SUCCESS_BONUS = 0.5f;

    // ── Счётчики победы ───────────────────────────────────────────────────────
    /**
     * Сколько компьютеров нужно взломать чтобы выполнилось условие победы
     * и запустились команды фазы 3.
     */
    public static int COMPUTERS_NEEDED_FOR_WIN = 3;

    // ── Команды при достижении цели ───────────────────────────────────────────
    /**
     * Команды выполняются последовательно когда взломано COMPUTERS_NEEDED_FOR_WIN компьютеров.
     * Используйте стандартный синтаксис Minecraft без слэша.
     */
    public static final String[] WIN_COMMANDS = {
            "give @a[team=survivors] securitycraft:keycard_lv5{signature: 00000, linked: 1b, ownerUUID: \"535bded9-d49b-488c-821c-c7a7ed83b410\",ownerName:\"VitaminLLO\"} 1",
            "effect clear @a[team=maniac] minecraft:resistance",
            "effect clear @a[team=survivors] minecraft:weakness",
            "execute as @e[type=marker,tag=weaponMarker] at @s run summon armor_stand ~-0.5 ~ ~-0.5 {Tags:[\"removeMe\"],NoGravity:1b,Invisible:1b,Glowing:1b,glow_color_override:9408399}",
            "maniacrev phase 3"
    };

    /** Функция датапака, вызывается после каждого взломанного компьютера */
    public static final String DATAPACK_FUNCTION_ON_HACK = "maniac:classes/gen_effect";

    // ── Отображение партиклов ──────────────────────────────────────────────────
    /** Период обновления партиклов радиуса (тики) */
    public static int PARTICLE_UPDATE_INTERVAL = 10;
}