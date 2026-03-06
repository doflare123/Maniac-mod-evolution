package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Одна активная сессия взлома.
 * Создаётся когда игрок кликает по компьютеру.
 * Уничтожается когда взлом завершён, прерван, или хакер вышел из радиуса.
 */
public class HackSession {

    final ServerPlayer hacker;
    final BlockPos pos;
    final int computerId;
    float currentPoints;

    /** Тик-счётчик для QTE (в тиках, не в секундах) */
    private int ticksSinceLastQTE = 0;
    private int nextQTEIntervalTicks;

    /** Игроки которым уже отправили StartQTE в этой сессии (чтобы не дублировать) */
    private final Set<UUID> activeQTEPlayers = new HashSet<>();

    private boolean finished = false;

    private static final Random RANDOM = new Random();
    private static final String SURVIVORS_TEAM = "survivors";

    HackSession(ServerPlayer hacker, BlockPos pos, int computerId, float startPoints) {
        this.hacker = hacker;
        this.pos = pos;
        this.computerId = computerId;
        this.currentPoints = startPoints;
        this.nextQTEIntervalTicks = randomQTEInterval();
    }

    /**
     * Тик сессии. Вызывается раз в секунду (20 тиков) из HackManager.tick().
     * Возвращает false если сессию нужно удалить.
     */
    boolean tick(MinecraftServer server) {
        if (finished) return false;

        ServerLevel level = (ServerLevel) hacker.level();

        // 1. Проверяем что хакер ещё онлайн и в радиусе
        if (!hacker.isAlive() || hacker.hasDisconnected()) {
            cancel();
            return false;
        }

        double hackerDist = hacker.position().distanceTo(
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        if (hackerDist > HackConfig.HACKER_RADIUS) {
            // Хакер вышел из радиуса — прерываем
            hacker.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cВзлом прерван: покинул зону!"), true);
            cancel();
            return false;
        }

        // 2. Считаем очки от поддержки
        List<ServerPlayer> supporters = getSupporters(level);
        float pointsThisTick = calcPoints(supporters);
        currentPoints = Math.min(currentPoints + pointsThisTick, HackConfig.HACK_POINTS_REQUIRED);

        // 3. Обновляем блок-сущность для отображения прогресса
        updateBlockDisplay(level);

        // 4. Партиклы радиуса поддержки
        ticksSinceLastQTE += 20; // каждый вызов = 1 секунда = 20 тиков
        spawnRadiusParticles(level);

        // 5. QTE для хакера и поддержки
        if (ticksSinceLastQTE >= nextQTEIntervalTicks) {
            ticksSinceLastQTE = 0;
            nextQTEIntervalTicks = randomQTEInterval();
            triggerQTE(supporters);
        }

        // 6. Готово?
        if (currentPoints >= HackConfig.HACK_POINTS_REQUIRED) {
            onHackComplete(server, level);
            return false;
        }

        return true;
    }

    // ── Подсчёт очков ─────────────────────────────────────────────────────────

    private float calcPoints(List<ServerPlayer> supporters) {
        float total = 0;
        int count = 0;
        for (ServerPlayer sp : supporters) {
            if (count >= HackConfig.MAX_BONUS_PLAYERS) break;
            total += isSpecialist(sp)
                    ? HackConfig.POINTS_PER_SPECIALIST_PER_SECOND
                    : HackConfig.POINTS_PER_PLAYER_PER_SECOND;
            count++;
        }
        // Хакер всегда даёт базовый вклад (он уже в радиусе)
        total += HackConfig.POINTS_PER_PLAYER_PER_SECOND;
        return total;
    }

    /** Игроки в радиусе поддержки (team survivors, adventure, кроме самого хакера) */
    private List<ServerPlayer> getSupporters(ServerLevel level) {
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        List<ServerPlayer> result = new ArrayList<>();
        double r2 = HackConfig.SUPPORT_RADIUS * HackConfig.SUPPORT_RADIUS;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p == hacker) continue;
            if (!isSurvivorAdventure(p)) continue;
            if (p.position().distanceToSqr(center) <= r2) result.add(p);
        }
        return result;
    }

    private static boolean isSurvivorAdventure(ServerPlayer p) {
        if (p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) return false;
        var team = p.getTeam();
        return team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName());
    }

    private static boolean isSpecialist(ServerPlayer p) {
        var sb = p.getServer().getScoreboard();
        var obj = sb.getObjective(HackConfig.SURVIVOR_CLASS_OBJECTIVE);
        if (obj == null) return false;
        return sb.getOrCreatePlayerScore(p.getScoreboardName(), obj)
                .getScore() == HackConfig.SPECIALIST_CLASS;
    }

    // ── QTE ───────────────────────────────────────────────────────────────────

    private void triggerQTE(List<ServerPlayer> supporters) {
        // Хакер
        HackManager.sendStartQTE(hacker);

        // Помощники (тоже участвуют в QTE)
        for (ServerPlayer sp : supporters) {
            HackManager.sendStartQTE(sp);
        }
    }

    private static int randomQTEInterval() {
        int min = HackConfig.QTE_INTERVAL_MIN_SECONDS * 20;
        int max = HackConfig.QTE_INTERVAL_MAX_SECONDS * 20;
        return min + RANDOM.nextInt(max - min + 1);
    }

    // ── Партиклы ──────────────────────────────────────────────────────────────

    private void spawnRadiusParticles(ServerLevel level) {
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        double r = HackConfig.SUPPORT_RADIUS;
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double px = center.x + Math.cos(angle) * r;
            double pz = center.z + Math.sin(angle) * r;
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    px, center.y, pz,
                    1, 0, 0.05, 0, 0.01);
        }
        // Внутренний круг хакера
        double rh = HackConfig.HACKER_RADIUS;
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI / 12) * i;
            double px = center.x + Math.cos(angle) * rh;
            double pz = center.z + Math.sin(angle) * rh;
            level.sendParticles(
                    ParticleTypes.CRIT,
                    px, center.y, pz,
                    1, 0, 0.05, 0, 0.01);
        }
    }

    // ── Завершение ────────────────────────────────────────────────────────────

    private void onHackComplete(MinecraftServer server, ServerLevel level) {
        finished = true;
        currentPoints = HackConfig.HACK_POINTS_REQUIRED;
        updateBlockDisplay(level);

        // Останавливаем QTE
        HackManager.sendStopQTE(hacker);

        hacker.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a✔ Компьютер взломан!"), true);

        HackManager.get().onComputerHacked(server, computerId, pos);
    }

    private void cancel() {
        finished = true;
        HackManager.sendStopQTE(hacker);
        hacker.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cВзлом прерван."), true);
    }

    /** Обновляем BlockEntity для рендеринга прогресса */
    private void updateBlockDisplay(ServerLevel level) {
        if (level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
            be.setHackProgress(currentPoints / HackConfig.HACK_POINTS_REQUIRED);
            be.setHacked(currentPoints >= HackConfig.HACK_POINTS_REQUIRED);
            be.setChanged();
        }
    }
}