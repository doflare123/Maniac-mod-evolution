package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверный менеджер взломов.
 *
 * Хранит состояние каждого активного взлома.
 * Вызывается из тик-ивента (каждые 20 тиков = 1 раз в секунду).
 *
 * Ключ — пара (computerId, blockPos) чтобы компьютеры с одним id
 * на разных картах имели ОБЩИЙ прогресс (по id), но каждый блок
 * отдельно отслеживает кто его взламывает.
 */
public class HackManager {

    private static HackManager INSTANCE;

    /** Прогресс взлома по computerId: id -> очки */
    private final Map<Integer, Float> hackProgress = new ConcurrentHashMap<>();

    /** Сколько компьютеров с данным id уже взломано */
    private final Map<Integer, Boolean> hackedComputers = new ConcurrentHashMap<>();

    /** Суммарное количество взломанных компьютеров (все id) */
    private int totalHacked = 0;

    /**
     * Активные сессии взлома: pos -> сессия.
     * Один блок — одна сессия.
     */
    private final Map<BlockPos, HackSession> activeSessions = new ConcurrentHashMap<>();

    private HackManager() {}

    public static HackManager get() {
        if (INSTANCE == null) INSTANCE = new HackManager();
        return INSTANCE;
    }

    /** Вызывается при старте сервера — сбрасываем состояние */
    public static void reset() {
        INSTANCE = new HackManager();
    }

    // ── Публичное API ─────────────────────────────────────────────────────────

    /**
     * Игрок кликнул по компьютеру в adventure-режиме.
     * Если сессии нет — создаём. Если уже взломан — ничего.
     */
    public void onPlayerActivate(ServerPlayer player, BlockPos pos, int computerId) {
        // Уже взломан?
        if (Boolean.TRUE.equals(hackedComputers.get(computerId))) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§aКомпьютер уже взломан!"), true);
            return;
        }

        // Уже идёт сессия на этом блоке?
        if (activeSessions.containsKey(pos)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§eВзлом уже активен!"), true);
            return;
        }

        float currentProgress = hackProgress.getOrDefault(computerId, 0f);
        HackSession session = new HackSession(player, pos, computerId, currentProgress);
        activeSessions.put(pos, session);

        // Запускаем QTE для хакера
        sendStartQTE(player);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6Взлом начат..."), true);
    }

    /**
     * Тик менеджера. Вызывать раз в секунду (каждые 20 тиков).
     */
    public void tick(MinecraftServer server) {
        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, HackSession> entry : activeSessions.entrySet()) {
            BlockPos pos = entry.getKey();
            HackSession session = entry.getValue();

            boolean alive = session.tick(server);
            if (!alive) {
                toRemove.add(pos);
                // Сохраняем прогресс
                hackProgress.put(session.computerId, session.currentPoints);
            }
        }

        toRemove.forEach(activeSessions::remove);
    }

    /**
     * Вызывается из HackSession когда компьютер взломан.
     */
    void onComputerHacked(MinecraftServer server, int computerId, BlockPos pos) {
        hackedComputers.put(computerId, true);
        hackProgress.put(computerId, HackConfig.HACK_POINTS_REQUIRED);
        totalHacked++;

        // Функция датапака после каждого взлома
        executeCommand(server, "function " + HackConfig.DATAPACK_FUNCTION_ON_HACK);

        // Уведомление всем
        server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Взлом] §fКомпьютер §e#" + computerId + " §fвзломан! (" +
                                totalHacked + "/" + HackConfig.COMPUTERS_NEEDED_FOR_WIN + ")"),
                false);

        // Достигли цели?
        if (totalHacked >= HackConfig.COMPUTERS_NEEDED_FOR_WIN) {
            for (String cmd : HackConfig.WIN_COMMANDS) {
                executeCommand(server, cmd);
            }
        }
    }

    // ── Команды сброса ────────────────────────────────────────────────────────

    /** Сбросить ВСЕ компьютеры и прогресс */
    public void resetAll(MinecraftServer server) {
        activeSessions.values().forEach(s -> sendStopQTE(s.hacker));
        activeSessions.clear();
        hackProgress.clear();
        hackedComputers.clear();
        totalHacked = 0;
    }

    /** Сбросить конкретный computerId */
    public void resetById(MinecraftServer server, int computerId) {
        hackProgress.remove(computerId);
        hackedComputers.remove(computerId);

        // Останавливаем активные сессии с этим id
        List<BlockPos> toStop = new ArrayList<>();
        for (Map.Entry<BlockPos, HackSession> e : activeSessions.entrySet()) {
            if (e.getValue().computerId == computerId) {
                sendStopQTE(e.getValue().hacker);
                toStop.add(e.getKey());
            }
        }
        toStop.forEach(activeSessions::remove);

        // Пересчитываем totalHacked
        totalHacked = (int) hackedComputers.values().stream().filter(v -> v).count();
    }

    // ── Геттеры ───────────────────────────────────────────────────────────────

    public float getProgress(int computerId) {
        return hackProgress.getOrDefault(computerId, 0f);
    }

    public boolean isHacked(int computerId) {
        return Boolean.TRUE.equals(hackedComputers.get(computerId));
    }

    public int getTotalHacked() { return totalHacked; }

    public boolean hasActiveSession(BlockPos pos) {
        return activeSessions.containsKey(pos);
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    static void executeCommand(MinecraftServer server, String cmd) {
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(), cmd);
        } catch (Exception e) {
            System.err.println("[HackManager] Command error: " + cmd + " -> " + e.getMessage());
        }
    }

    static void sendStartQTE(ServerPlayer player) {
        boolean hasPerk = hasQuickReflexesPerk(player);
        org.example.maniacrevolution.network.ModNetworking.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.example.maniacrevolution.network.packets.StartQTEPacket(0, hasPerk));
    }

    /**
     * Проверяет наличие перка "quick_reflexes" у игрока.
     * Использует PlayerDataManager + PlayerData.getSelectedPerks().
     */
    private static boolean hasQuickReflexesPerk(ServerPlayer player) {
        try {
            org.example.maniacrevolution.data.PlayerData data =
                    org.example.maniacrevolution.data.PlayerDataManager.get(player);
            if (data == null) return false;
            for (org.example.maniacrevolution.perk.PerkInstance inst : data.getSelectedPerks()) {
                if ("quick_reflexes".equals(inst.getPerk().getId())) return true;
            }
        } catch (Exception e) {
            System.err.println("[HackManager] hasQuickReflexesPerk error: " + e.getMessage());
        }
        return false;
    }

    static void sendStopQTE(ServerPlayer player) {
        if (player == null) return;
        org.example.maniacrevolution.network.ModNetworking.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.example.maniacrevolution.network.packets.StopQTEPacket());
    }

    /**
     * Добавляет QTE_SUCCESS_BONUS к сессии где этот игрок является хакером.
     * Вызывается из QTEKeyPressPacket при успешном QTE.
     */
    public void applyQTEBonus(net.minecraft.server.level.ServerPlayer player) {
        for (HackSession session : activeSessions.values()) {
            if (session.hacker.getUUID().equals(player.getUUID())) {
                session.currentPoints = Math.min(
                        session.currentPoints + HackConfig.QTE_SUCCESS_BONUS,
                        HackConfig.HACK_POINTS_REQUIRED);
                System.out.println("[HackManager] QTE bonus +" + HackConfig.QTE_SUCCESS_BONUS +
                        " для " + player.getName().getString() +
                        " -> " + session.currentPoints);
                return;
            }
        }
    }
}