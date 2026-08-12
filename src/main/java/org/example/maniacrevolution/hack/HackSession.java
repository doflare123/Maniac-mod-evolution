package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.perk.perks.survivor.AltruistExePerk;
import org.example.maniacrevolution.perk.perks.survivor.DutchHelmPerk;
import org.example.maniacrevolution.perk.perks.survivor.IdealychPerk;
import org.example.maniacrevolution.perk.perks.survivor.EmergencyOverclockPerk;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ClientParticleEffectPacket;

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

    private boolean finished = false;

    private static final Random RANDOM = new Random();
    private static final String SURVIVORS_TEAM = "survivors";

    HackSession(ServerPlayer hacker, BlockPos pos, int computerId, float startPoints) {
        this.hacker = hacker;
        this.pos = pos;
        this.computerId = computerId;
        this.currentPoints = startPoints;
        this.nextQTEIntervalTicks = randomQTEInterval();
        this.currentQTEPlayers.add(hacker); // ← хакер всегда в списке
    }

    /** Публичный геттер участников сессии (хакер + саппортеры) */
    public List<ServerPlayer> getAllParticipants() {
        List<ServerPlayer> result = new ArrayList<>();
        result.add(hacker);
        result.addAll(getSupporters((ServerLevel) hacker.level()));
        return result;
    }

    public BlockPos getPos() { return pos; }

    /**
     * Тик сессии. Вызывается раз в секунду (20 тиков) из HackManager.tick().
     * Возвращает false если сессию нужно удалить.
     */
    boolean tick(MinecraftServer server) {
        if (finished) return false;

        ServerLevel level = (ServerLevel) hacker.level();

        if (!hacker.isAlive() || hacker.hasDisconnected()) {
            cancel();
            return false;
        }

        double hackerDist = hacker.position().distanceTo(
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        if (hackerDist > HackConfig.HACKER_RADIUS) {
            hacker.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cВзлом прерван: покинул зону!"), true);
            cancel();
            return false;
        }

        List<ServerPlayer> supporters = getSupporters(level);

        // Новые саппортеры — сразу даём им QTE
        for (ServerPlayer sp : supporters) {
            if (!currentQTEPlayers.contains(sp)) {
                HackManager.sendStartQTE(sp);
                currentQTEPlayers.add(sp);
            }
        }

        // Саппортеры вышедшие из зоны — останавливаем QTE
        List<ServerPlayer> leftZone = new ArrayList<>();
        for (ServerPlayer sp : currentQTEPlayers) {
            if (sp == hacker) continue;
            if (!supporters.contains(sp)) {
                HackManager.sendStopQTE(sp);
                leftZone.add(sp);
            }
        }
        currentQTEPlayers.removeAll(leftZone);

        float pointsThisTick = calcPoints(supporters);
        currentPoints = Math.min(currentPoints + pointsThisTick, HackConfig.HACK_POINTS_REQUIRED);

        updateBlockDisplay(level);

        ticksSinceLastQTE += 20;
        spawnRadiusParticles(level);

        // Периодический триггер QTE — переотправляем всем кто уже в списке
        if (ticksSinceLastQTE >= nextQTEIntervalTicks) {
            ticksSinceLastQTE = 0;
            nextQTEIntervalTicks = randomQTEInterval();
            triggerQTE(); // теперь без аргументов
        }

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

        // Все участники = хакер + саппортеры
        List<ServerPlayer> allParticipants = new ArrayList<>(supporters);
        allParticipants.add(0, hacker);

        // Находим ПЕРВОГО владельца DutchHelmPerk среди участников
        ServerPlayer dutchOwner = null;
        for (ServerPlayer sp : allParticipants) {
            if (DutchHelmPerk.hasThisPerk(sp)) {
                dutchOwner = sp;
                break;
            }
        }

        for (ServerPlayer sp : allParticipants) {
            if (sp != hacker && count >= HackConfig.MAX_BONUS_PLAYERS) break;

            // Аварийный разгон заменяет обычный вклад владельца ставкой на QTE.
            if (EmergencyOverclockPerk.isActive(sp)) {
                if (sp != hacker) count++;
                continue;
            }

            float points = isSpecialist(sp)
                    ? HackConfig.POINTS_PER_SPECIALIST_PER_SECOND
                    : HackConfig.POINTS_PER_PLAYER_PER_SECOND;

            // Голландский Штурвал — только у одного владельца, считает всех кроме себя
            if (sp == dutchOwner) {
                int others = allParticipants.size() - 1;
                points *= (1f + others * DutchHelmPerk.BONUS_PER_PLAYER);
            }

            // Альтруист.exe
            if (AltruistExePerk.hasActiveBonus(sp)) {
                points *= (1f + AltruistExePerk.HACK_BONUS);
            }

            // Идеалыч
            points *= IdealychPerk.getHackMultiplier(sp);

            if (DodepovichCasinoManager.hasActiveJackpot(sp)) {
                points *= 1.3f;
            }

            total += points;
            if (sp != hacker) count++;
        }


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

    private final List<ServerPlayer> currentQTEPlayers = new ArrayList<>();

    boolean hasQTEParticipant(ServerPlayer player) {
        for (ServerPlayer sp : currentQTEPlayers) {
            if (sp.getUUID().equals(player.getUUID())) return true;
        }
        return false;
    }

    private void triggerQTE() {
        for (ServerPlayer sp : currentQTEPlayers) {
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
        ClientParticleEffectPacket packet = ClientParticleEffectPacket.hackRadius(
                center, (float) HackConfig.SUPPORT_RADIUS, (float) HackConfig.HACKER_RADIUS);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= 64.0 * 64.0) {
                ModNetworking.sendToPlayer(packet, viewer);
            }
        }
    }

    // ── Завершение ────────────────────────────────────────────────────────────

    private void stopAllQTE() {
        HackManager.sendStopQTE(hacker);
        for (ServerPlayer sp : currentQTEPlayers) {
            if (sp != hacker) HackManager.sendStopQTE(sp);
        }
        currentQTEPlayers.clear();
    }

    private void onHackComplete(MinecraftServer server, ServerLevel level) {
        finished = true;
        IdealychPerk.resetStacks(hacker);
        currentPoints = HackConfig.HACK_POINTS_REQUIRED;
        updateBlockDisplay(level);

        stopAllQTE(); // ← вместо одиночного sendStopQTE(hacker)

        hacker.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a✔ Компьютер взломан!"), true);

        HackManager.get().onComputerHacked(server, computerId, pos);
    }

    private void cancel() {
        finished = true;
        IdealychPerk.resetStacks(hacker);
        stopAllQTE(); // ← вместо одиночного sendStopQTE(hacker)
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
