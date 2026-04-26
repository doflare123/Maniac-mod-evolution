package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;

/**
 * Единый балансный конфиг механик Хранителя кошмаров.
 * Значения пока статические, чтобы в тестовой ветке можно было быстро менять
 * цифры в одном месте, не выискивая их по испытаниям, HUD, предметам и способностям.
 */
public final class NightmareConfig {
    private NightmareConfig() {}

    /** ID класса Хранителя кошмаров в модовой системе классов. */
    public static final int KEEPER_CLASS_ID = 17;

    /** Базовая дальняя зона, где строятся испытания, чтобы не задевать основную карту. */
    public static final BlockPos TRIAL_BASE_ORIGIN = new BlockPos(20000, 80, 20000);
    /** Горизонтальный шаг между зарезервированными зонами испытаний. */
    public static final int TRIAL_AREA_STEP = 220;

    /** Максимальная дистанция, на которой взгляд Хранителя влияет на выжившего. */
    public static final double GAZE_RANGE = 32.0D;
    /** Порог точности взгляда: чем выше, тем ближе цель должна быть к центру экрана. */
    public static final double GAZE_DOT_THRESHOLD = 0.965D;
    /** Сколько рассудка снимается каждый тик, пока Хранитель держит цель во взгляде. */
    public static final float SANITY_DRAIN_PER_TICK = 0.55F;
    /** Задержка после последнего взгляда перед началом восстановления рассудка. */
    public static final int SANITY_REGEN_DELAY_TICKS = 80;
    /** Сколько рассудка восстанавливается каждый тик после задержки. */
    public static final float SANITY_REGEN_PER_TICK = 0.18F;
    /** Значение рассудка, при котором запускается похищение в испытание. */
    public static final float SANITY_BREAKPOINT = 0.0F;
    /** Максимальный рассудок, отображаемый в HUD. */
    public static final float MAX_SANITY = 100.0F;
    /** Кулдаун после испытания перед повторным похищением того же выжившего. */
    public static final int ABDUCTION_COOLDOWN_TICKS = 20 * 20;

    /** Первые N попаданий всегда ведут в лабиринт, после этого включается рандом испытаний. */
    public static final int FORCED_MAZE_COUNT = 3;
    /** Длительность испытания лабиринтом. */
    public static final int MAZE_DURATION_TICKS = 60 * 20;
    /** Урон при провале лабиринта по таймеру. */
    public static final float MAZE_FAIL_DAMAGE = 6.0F;
    /** Длительность Darkness, который постоянно обновляется на игроке в лабиринте. */
    public static final int MAZE_DARKNESS_REFRESH_TICKS = 80;
    /** Дистанция до точки выхода, на которой лабиринт считается пройденным. */
    public static final double MAZE_EXIT_RADIUS = 2.4D;

    /** Размер стороны арены в блоках. */
    public static final int ARENA_SIZE = 31;
    /** Высота стен арены. */
    public static final int ARENA_WALL_HEIGHT = 5;
    /** Длительность испытания ареной. */
    public static final int ARENA_DURATION_TICKS = 45 * 20;
    /** Количество враждебных мобов, спавнящихся при старте арены. */
    public static final int ARENA_MOB_COUNT = 6;
    /** Количество случайно расставленных укрытий/столбов на арене. */
    public static final int ARENA_COVER_COUNT = 18;

    /** Длительность испытания гонкой со страхом. */
    public static final int FEAR_RACE_DURATION_TICKS = 30 * 20;
    /** Обратный отсчет перед стартом гонки, чтобы игрок успел прогрузиться. */
    public static final int FEAR_RACE_COUNTDOWN_TICKS = 3 * 20;
    /** Длина прямой дороги гонки в блоках. */
    public static final int FEAR_RACE_LENGTH = 96;
    /** Ширина дороги гонки в блоках. */
    public static final int FEAR_RACE_WIDTH = 7;
    /** Скорость преследователя страха за тик. */
    public static final double FEAR_CHASER_SPEED = 0.245D;
    /** Дистанция, на которой преследователь считается догнавшим выжившего. */
    public static final double FEAR_CHASER_CATCH_RADIUS = 1.35D;
    /** Урон при провале гонки по таймеру или при поимке. */
    public static final float FEAR_RACE_FAIL_DAMAGE = 6.0F;

    /** Прочность зажигалки кошмара. */
    public static final int LIGHTER_DURABILITY = 1200;
    /** Как часто зажигалка тратит прочность, пока находится в руке. */
    public static final int LIGHTER_DAMAGE_INTERVAL_TICKS = 10;
    /** Резервная длительность обновления света зажигалки, оставлена для настройки поведения. */
    public static final int LIGHTER_LIGHT_REFRESH_TICKS = 40;

    /** Сколько ударов Иглой пробуждения нужно для разрушения кокона. */
    public static final int COCOON_REQUIRED_HITS = 4;
    /** Урон выжившему, которого досрочно вытащили через кокон. */
    public static final float COCOON_RESCUE_DAMAGE = 0.5F;

    /** Кулдаун способности Концентрированный кошмар на голове Хранителя. */
    public static final int CONCENTRATED_NIGHTMARE_COOLDOWN_TICKS = 45 * 20;
    /** Процент от максимального рассудка, снимаемый Концентрированным кошмаром. */
    public static final float CONCENTRATED_NIGHTMARE_SANITY_PERCENT = 0.10F;
    /** Радиус поиска цели для Концентрированного кошмара. */
    public static final double CONCENTRATED_NIGHTMARE_RANGE = 40.0D;
    /** Длительность скримера на экране жертвы от Концентрированного кошмара. */
    public static final int CONCENTRATED_NIGHTMARE_SCREAMER_TICKS = 30;
}
