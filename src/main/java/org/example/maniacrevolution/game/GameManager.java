package org.example.maniacrevolution.game;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.GameStatePacket;
import org.example.maniacrevolution.perk.PerkPhase;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class GameManager {
    private static MinecraftServer server;

    // Таймер
    private static int maxGameTime = 10 * 60 * 20; // 10 минут в тиках
    private static int currentTime = 0;
    private static boolean timerRunning = false;
    private static boolean maniacGlowing = false;

    // Название scoreboard objective для фазы
    private static final String PHASE_OBJECTIVE = "phaseGame";

    // Константы
    private static final int GLOWING_THRESHOLD = 2400; // 2 минуты в тиках
    private static final int GLOWING_DURATION = 2500; // Чуть больше 2 минут, чтобы эффект не пропал
    private static final String MANIAC_TEAM_NAME = "maniac";

    public static void init(MinecraftServer server) {
        GameManager.server = server;
        ensureObjectiveExists();
    }

    private static void ensureObjectiveExists() {
        if (server == null) return;
        Scoreboard scoreboard = server.getScoreboard();
        if (scoreboard.getObjective(PHASE_OBJECTIVE) == null) {
            scoreboard.addObjective(PHASE_OBJECTIVE, ObjectiveCriteria.DUMMY,
                    Component.literal("Game Phase"), ObjectiveCriteria.RenderType.INTEGER);
        }
    }

    // === Фазы игры ===

    public static int getPhaseValue() {
        if (server == null) return 0;
        Scoreboard scoreboard = server.getScoreboard();
        Objective obj = scoreboard.getObjective(PHASE_OBJECTIVE);
        if (obj == null) return 0;

        Score score = scoreboard.getOrCreatePlayerScore("game", obj);
        return score.getScore();
    }

    public static void setPhase(int phase) {
        if (server == null) return;

        int oldPhase = getPhaseValue();
        if (oldPhase == phase) return;

        // Проверка правил перехода фаз
        if (phase == 2 && oldPhase != 1) {
            Maniacrev.LOGGER.warn("Cannot set phase to 2 (midgame) from phase {}", oldPhase);
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        ensureObjectiveExists();
        Objective obj = scoreboard.getObjective(PHASE_OBJECTIVE);
        Score score = scoreboard.getOrCreatePlayerScore("game", obj);
        score.setScore(phase);

        // Оповещаем всех игроков
        PerkPhase newPhase = PerkPhase.fromScoreboardValue(phase);
        if (newPhase != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.playNotifySound(Maniacrev.PHASE_CHANGE_SOUND.get(), SoundSource.MASTER, 0.5f, 1.0f);

                PlayerData data = PlayerDataManager.get(player);
                data.onPhaseChange(player, newPhase);
            }
        }

        syncGameState();
        Maniacrev.LOGGER.info("Game phase changed from {} to {}", oldPhase, phase);
    }

    public static PerkPhase getCurrentPhase() {
        return PerkPhase.fromScoreboardValue(getPhaseValue());
    }

    // === Таймер ===

    public static void startTimer() {
        currentTime = maxGameTime;
        timerRunning = true;
        maniacGlowing = false; // ИСПРАВЛЕНО: Сбрасываем флаг при старте
        syncGameState();
        Maniacrev.LOGGER.info("Timer started: {} seconds", maxGameTime / 20);
    }

    public static void stopTimer() {
        timerRunning = false;
        syncGameState();
    }

    public static void setMaxTime(int seconds) {
        maxGameTime = seconds * 20;
        Maniacrev.LOGGER.info("Max game time set to {} seconds", seconds);
    }

    public static void addTime(int seconds) {
        currentTime += seconds * 20;
        if (currentTime < 0) currentTime = 0;
        syncGameState();
    }

    public static void setTime(int seconds) {
        currentTime = seconds * 20;
        syncGameState();
    }

    public static int getCurrentTimeSeconds() {
        return currentTime / 20;
    }

    public static int getMaxTimeSeconds() {
        return maxGameTime / 20;
    }

    public static boolean isTimerRunning() {
        return timerRunning;
    }

    // === Команды игры ===

    public static void startGame(CommandSourceStack source) {
        if (server == null) return;

        setPhase(1);
        startTimer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData data = PlayerDataManager.get(player);
            data.onGameStart(player);
        }

        source.sendSuccess(() -> Component.literal("§aИгра началась!"), true);
        Maniacrev.LOGGER.info("Game started by {}", source.getTextName());
    }

    public static void stopGame(CommandSourceStack source) {
        setPhase(0);
        stopTimer();
        currentTime = 0;
        maniacGlowing = false; // ИСПРАВЛЕНО: Сбрасываем флаг

        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerData data = PlayerDataManager.get(player);
                data.clearPerks(player);
            }
        }

        source.sendSuccess(() -> Component.literal("§cИгра остановлена!"), true);
    }

    // === Тик ===

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!timerRunning || server == null) return;

        currentTime--;

        // Проверка на половину времени -> переход в мидгейм
        if (currentTime == maxGameTime / 2 && getPhaseValue() == 1) {
            setPhase(2);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6Начинается мидгейм!"), false
            );
        }

        // ИСПРАВЛЕНО: Подсветка маньяков в фазе 3 при остатке времени <= 2 минут
        if (currentTime <= GLOWING_THRESHOLD && getPhaseValue() == 3 && !maniacGlowing) {
            applyManiacGlowing();
            maniacGlowing = true;

            Maniacrev.LOGGER.info("Applied glowing to maniacs (time remaining: {} seconds)", currentTime / 20);
        }

        // Проверка на конец времени
        if (currentTime <= 0) {
            currentTime = 0;
            timerRunning = false;

            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(),
                    "function maniac:game/time_end"
            );

            Maniacrev.LOGGER.info("Timer ended, called maniac:game/time_end");
        }

        // Синхронизируем каждую секунду
        if (currentTime % 20 == 0) {
            syncGameState();
        }
    }

    // === Подсветка маньяков ===

    /**
     * Применяет эффект подсветки ко всем игрокам команды "maniac"
     */
    private static void applyManiacGlowing() {
        if (server == null) return;

        int glowingCount = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Проверяем команду игрока
            if (isInManiacTeam(player)) {
                // Применяем эффект подсветки на оставшееся время + запас
                player.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        GLOWING_DURATION, // ~2 минуты + запас
                        0,
                        false,
                        false,
                        false
                ));

                glowingCount++;
            }
        }

        Maniacrev.LOGGER.info("Applied glowing to {} maniac(s)", glowingCount);
    }

    /**
     * Проверяет, находится ли игрок в команде "maniac"
     */
    private static boolean isInManiacTeam(ServerPlayer player) {
        PlayerTeam team = (PlayerTeam) player.getTeam();

        if (team == null) {
            return false;
        }

        // ИСПРАВЛЕНО: Правильная проверка имени команды
        return MANIAC_TEAM_NAME.equalsIgnoreCase(team.getName());
    }

    // === Синхронизация ===

    public static void syncGameState() {
        if (server == null) return;

        GameStatePacket packet = new GameStatePacket(
                getPhaseValue(),
                currentTime,
                maxGameTime,
                timerRunning
        );

        ModNetworking.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}