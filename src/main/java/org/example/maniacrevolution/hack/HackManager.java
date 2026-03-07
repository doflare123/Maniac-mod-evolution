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


        // Синхронизируем HUD клиентам
        sendSyncPacket(server);

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
        sendResetPacket(server);
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
    public void applyQTEBonus(net.minecraft.server.level.ServerPlayer player, boolean critical) {
        for (HackSession session : activeSessions.values()) {
            if (session.hacker.getUUID().equals(player.getUUID())) {
                float bonus = critical
                        ? HackConfig.QTE_CRIT_BONUS
                        : HackConfig.QTE_SUCCESS_BONUS;
                session.currentPoints = Math.min(
                        session.currentPoints + bonus,
                        HackConfig.HACK_POINTS_REQUIRED);
                System.out.println("[HackManager] QTE " + (critical ? "CRIT" : "normal") +
                        " bonus +" + bonus + " для " + player.getName().getString() +
                        " -> " + session.currentPoints);
                return;
            }
        }
    }

    /** Совместимость */
    public void applyQTEBonus(net.minecraft.server.level.ServerPlayer player) {
        applyQTEBonus(player, false);
    }

    // ── Персистентность ───────────────────────────────────────────────────────

    /**
     * Сохраняет прогресс взломов в файл.
     * Вызывать из ServerLifecycleHooks.SERVER_STOPPING или периодически.
     */
    public void save(net.minecraft.server.MinecraftServer server) {
        try {
            java.io.File file = getDataFile(server);
            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag progress = new net.minecraft.nbt.CompoundTag();
            for (var e : hackProgress.entrySet()) {
                progress.putFloat("p_" + e.getKey(), e.getValue());
            }
            net.minecraft.nbt.CompoundTag hacked = new net.minecraft.nbt.CompoundTag();
            for (var e : hackedComputers.entrySet()) {
                hacked.putBoolean("h_" + e.getKey(), e.getValue());
            }
            root.put("progress", progress);
            root.put("hacked", hacked);
            root.putInt("totalHacked", totalHacked);
            // Сохраняем настройки конфига чтобы они пережили рестарт
            root.putInt("goal", HackConfig.COMPUTERS_NEEDED_FOR_WIN);
            root.putFloat("pointsRequired", HackConfig.HACK_POINTS_REQUIRED);
            net.minecraft.nbt.NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            System.err.println("[HackManager] Save error: " + e.getMessage());
        }
    }

    /**
     * Загружает прогресс взломов из файла.
     * Вызывать из ServerLifecycleHooks.SERVER_STARTED после HackManager.reset().
     */
    public void load(net.minecraft.server.MinecraftServer server) {
        try {
            java.io.File file = getDataFile(server);
            if (!file.exists()) return;
            net.minecraft.nbt.CompoundTag root = net.minecraft.nbt.NbtIo.readCompressed(file);
            net.minecraft.nbt.CompoundTag progress = root.getCompound("progress");
            for (String key : progress.getAllKeys()) {
                int id = Integer.parseInt(key.substring(2));
                hackProgress.put(id, progress.getFloat(key));
            }
            net.minecraft.nbt.CompoundTag hacked = root.getCompound("hacked");
            for (String key : hacked.getAllKeys()) {
                int id = Integer.parseInt(key.substring(2));
                hackedComputers.put(id, hacked.getBoolean(key));
            }
            totalHacked = root.getInt("totalHacked");
            // Восстанавливаем настройки конфига
            if (root.contains("goal")) {
                HackConfig.COMPUTERS_NEEDED_FOR_WIN = root.getInt("goal");
            }
            if (root.contains("pointsRequired")) {
                HackConfig.HACK_POINTS_REQUIRED = root.getFloat("pointsRequired");
            }

            System.out.println("[HackManager] Loaded: totalHacked=" + totalHacked
                    + " computers=" + hackProgress.size()
                    + " goal=" + HackConfig.COMPUTERS_NEEDED_FOR_WIN);

            // Синхронизируем HUD клиентам (goal мог измениться)
            sendSyncPacket(server);
        } catch (Exception e) {
            System.err.println("[HackManager] Load error: " + e.getMessage());
        }
    }

    private static java.io.File getDataFile(net.minecraft.server.MinecraftServer server) {
        java.io.File worldDir = server.getWorldPath(
                net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        java.io.File modDir = new java.io.File(worldDir, "maniacrev");
        if (!modDir.exists()) modDir.mkdirs();
        return new java.io.File(modDir, "hackdata.dat");
    }

    /** Рассылает актуальный прогресс взломов всем клиентам для HUD. */
    private void sendSyncPacket(net.minecraft.server.MinecraftServer server) {
        var packet = new org.example.maniacrevolution.network.packets.SyncHackDataPacket(
                totalHacked, HackConfig.COMPUTERS_NEEDED_FOR_WIN);
        for (net.minecraft.server.level.ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            org.example.maniacrevolution.network.ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    packet);
        }
    }

    /** Вызывается при сбросе — обнуляем HUD у всех клиентов. */
    private void sendResetPacket(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        var packet = new org.example.maniacrevolution.network.packets.SyncHackDataPacket(
                0, HackConfig.COMPUTERS_NEEDED_FOR_WIN);
        for (net.minecraft.server.level.ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            org.example.maniacrevolution.network.ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    packet);
        }
    }

    /** Сохраняет данные и немедленно синхронизирует HUD всем клиентам. */
    public void saveAndSync(net.minecraft.server.MinecraftServer server) {
        save(server);
        sendSyncPacket(server);
    }
}