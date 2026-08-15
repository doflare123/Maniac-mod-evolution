package org.example.maniacrevolution.pregame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.PreGameReadyStatePacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер готовности игроков ДО старта игры (прелобби).
 * Полностью отдельный от ReadinessManager (который для лобби после старта).
 */
public class PreGameReadyManager {

    private static final Map<UUID, Boolean> readyPlayers = new ConcurrentHashMap<>();
    private static PreGameCountdownTask countdownTask = null;
    private static MinecraftServer server = null;
    private static boolean voteActive = false;
    private static String voteInitiatorName = "";

    public static void setServer(MinecraftServer s) {
        server = s;
    }

    // ─────────────────────────────────────────────
    //  Установка/снятие готовности
    // ─────────────────────────────────────────────

    public static void setPlayerReady(ServerPlayer player, boolean ready) {
        if (!voteActive) {
            voteActive = true;
            voteInitiatorName = player.getName().getString();
        }
        readyPlayers.put(player.getUUID(), ready);
        updateItem(player, ready);
        syncStateToAll(player.getServer());
        checkAllReady(player.getServer());
    }

    public static boolean isPlayerReady(UUID uuid) {
        return readyPlayers.getOrDefault(uuid, false);
    }

    public static boolean isPlayerReady(ServerPlayer player) {
        return isPlayerReady(player.getUUID());
    }

    // ─────────────────────────────────────────────
    //  Сброс
    // ─────────────────────────────────────────────

    /**
     * Сбросить готовность всем игрокам и заменить предмет на неактивный.
     * Вызывается командой /maniacrev reset_pregame_ready и после старта игры.
     */
    public static void resetAll(MinecraftServer srv) {
        readyPlayers.clear();
        cancelCountdown();
        voteActive = false;
        voteInitiatorName = "";

        if (srv == null) return;
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            replaceReadyItem(player, false);
        }
        syncStateToAll(srv);
        Maniacrev.LOGGER.info("[PreGame] All players readiness reset");
    }

    // ─────────────────────────────────────────────
    //  Выдача предмета
    // ─────────────────────────────────────────────

    /**
     * Выдать предмет "Готово" всем игрокам.
     * Вызывается командой /maniacrev give_pregame_ready.
     */
    public static void giveToAll(MinecraftServer srv) {
        if (srv == null) return;
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            // Сначала убираем старые экземпляры предмета
            removeReadyItems(player);
            // Выдаём неактивный
            net.minecraft.world.item.ItemStack stack =
                    new net.minecraft.world.item.ItemStack(ModItems.PRE_GAME_READY_ITEM.get());
            player.addItem(stack);
        }
        Maniacrev.LOGGER.info("[PreGame] Ready items given to all players");
    }

    // ─────────────────────────────────────────────
    //  Внутренняя логика
    // ─────────────────────────────────────────────

    private static void checkAllReady(MinecraftServer srv) {
        List<ServerPlayer> players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            cancelCountdown();
            return;
        }

        boolean allReady = players.stream().allMatch(p -> isPlayerReady(p.getUUID()));

        if (allReady) {
            voteActive = false;
            syncStateToAll(srv);
            startCountdown(srv);
        } else {
            cancelCountdown();
        }
    }

    private static void startCountdown(MinecraftServer srv) {
        if (countdownTask != null && countdownTask.isRunning()) return;

        countdownTask = new PreGameCountdownTask(srv);
        countdownTask.start();
        Maniacrev.LOGGER.info("[PreGame] Countdown started - all players ready!");
    }

    private static void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    /** Заменяет предмет в руке (или в инвентаре) на нужный вариант */
    private static void updateItem(ServerPlayer player, boolean ready) {
        replaceReadyItem(player, ready);
    }

    /**
     * Находит PreGameReadyItem или PreGameReadyItemActive в инвентаре игрока
     * и заменяет на нужный вариант.
     */
    private static void replaceReadyItem(ServerPlayer player, boolean active) {
        net.minecraft.world.item.Item from = active
                ? ModItems.PRE_GAME_READY_ITEM.get()
                : ModItems.PRE_GAME_READY_ITEM_ACTIVE.get();
        net.minecraft.world.item.Item to = active
                ? ModItems.PRE_GAME_READY_ITEM_ACTIVE.get()
                : ModItems.PRE_GAME_READY_ITEM.get();

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = inv.getItem(i);
            if (s.getItem() == from) {
                inv.setItem(i, new net.minecraft.world.item.ItemStack(to));
                return;
            }
        }
    }

    /** Удаляет оба варианта предмета из инвентаря (перед выдачей нового) */
    private static void removeReadyItems(ServerPlayer player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = inv.getItem(i);
            if (s.getItem() == ModItems.PRE_GAME_READY_ITEM.get()
                    || s.getItem() == ModItems.PRE_GAME_READY_ITEM_ACTIVE.get()) {
                inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    public static void syncStateToAll(MinecraftServer srv) {
        if (srv == null) return;
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            sendState(player, srv);
        }
    }

    private static void sendState(ServerPlayer player, MinecraftServer srv) {
        int total = srv.getPlayerList().getPlayerCount();
        int ready = (int) srv.getPlayerList().getPlayers().stream()
                .filter(p -> isPlayerReady(p.getUUID()))
                .count();
        ModNetworking.sendToPlayer(new PreGameReadyStatePacket(
                voteActive,
                voteInitiatorName,
                ready,
                total,
                isPlayerReady(player)
        ), player);
    }

    public static void broadcast(String message) {
        if (server == null) return;
        server.execute(() -> {
            Component msg = Component.literal(message);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(msg);
            }
        });
    }
}
