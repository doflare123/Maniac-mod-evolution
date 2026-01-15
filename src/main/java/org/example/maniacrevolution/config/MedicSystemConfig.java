package org.example.maniacrevolution.config;

public class MedicSystemConfig {

    /*
     * Настройки бинтов
     */
    public static final int BANDAGE_STACK_SIZE = 4;
    public static final int BANDAGE_SELF_USE_TIME = 90;      // 4.5 секунды
    public static final int BANDAGE_ALLY_USE_TIME = 60;      // 3 секунды
    public static final float BANDAGE_HEAL_AMOUNT = 2.0F;    // 1 сердце

    /*
     * Настройки планшета
     */
    public static final int TABLET_TRACKING_DURATION = 200;  // 10 секунд (в тиках)
    public static final long TABLET_COOLDOWN_MS = 30000;     // 30 секунд
    public static final double TABLET_REACH_DISTANCE = 3.0;  // 3 блока
    public static final float TABLET_MIN_HEALTH_PERCENT = 0.5F;  // 50%

    /*
     * Настройки пассивной способности
     */
    public static final double PASSIVE_DAMAGE_SHARE_RADIUS = 4.0;  // 4 блока
    public static final float PASSIVE_DAMAGE_SPLIT_RATIO = 0.5F;   // 50%
    public static final int MEDIC_CLASS_ID = 5;  // Значение в scoreboardSurvivorClass
    public static final String SURVIVORS_TEAM_NAME = "survivors";

    /*
     * Визуальные настройки
     */
    public static final int HUD_CIRCLE_RADIUS = 30;
    public static final int HUD_CIRCLE_OFFSET_X = 45;
    public static final int PARTICLE_MAX_COUNT = 40;
}
