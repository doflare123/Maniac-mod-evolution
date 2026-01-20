package org.example.maniacrevolution.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.MapVotingPacket;
import org.example.maniacrevolution.network.packets.MapVotingResultPacket;

import java.util.*;

public class MapVotingManager {
    private static MapVotingManager instance;
    private static final Random RANDOM = new Random();

    private MinecraftServer server;
    private boolean votingActive = false;
    private int timeRemaining = 0;
    private final Map<UUID, String> votes = new HashMap<>();
    private boolean timerLocked = false;
    private boolean chatMessageSent = false;
    private String cachedWinnerMapId = null; // Кешируем ID победителя

    public static MapVotingManager getInstance() {
        if (instance == null) {
            instance = new MapVotingManager();
        }
        return instance;
    }

    public void startVoting(MinecraftServer server, int duration) {
        this.server = server;
        this.votingActive = true;
        this.timeRemaining = duration;
        this.votes.clear();
        this.timerLocked = false;
        this.chatMessageSent = false;
        this.cachedWinnerMapId = null; // Сбрасываем кеш

        // Выдаем всем игрокам предмет для голосования и открываем меню
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Выдаем предмет
            ItemStack votingTicket = new ItemStack(ModItems.VOTING_TICKET.get());
            player.getInventory().add(votingTicket);

            // Отправляем пакет об открытии меню
            ModNetworking.send(new MapVotingPacket(true, duration, new HashMap<>()), player);
        }

        Maniacrev.LOGGER.info("Map voting started for {} seconds", duration);
    }

    public void tick() {
        if (!votingActive) return;

        timeRemaining--;

        // Проверяем, проголосовали ли все
        if (!timerLocked && votes.size() == server.getPlayerList().getPlayerCount()) {
            if (timeRemaining > 5) {
                timeRemaining = 5;
                timerLocked = true;
                Maniacrev.LOGGER.info("All players voted, timer set to 5 seconds");
            }
        }

        // Отправляем обновление всем игрокам (БЕЗ флага открытия)
        Map<String, Integer> voteCount = getVoteCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.send(new MapVotingPacket(false, timeRemaining, voteCount), player);
        }

        // Голосование закончилось
        if (timeRemaining <= 0) {
            endVoting();
        }
    }

    public void vote(UUID playerId, String mapId) {
        if (!votingActive) return;
        votes.put(playerId, mapId);
        Maniacrev.LOGGER.info("Player {} voted for map {}", playerId, mapId);
    }

    public void removeVote(UUID playerId) {
        if (!votingActive) return;
        votes.remove(playerId);
    }

    private void endVoting() {
        votingActive = false;

        Map<String, Integer> voteCount = getVoteCount();
        String winnerMapId = determineWinner(voteCount);
        cachedWinnerMapId = winnerMapId; // Сохраняем победителя
        MapData winnerMap = MapRegistry.getMapById(winnerMapId);

        // Устанавливаем scoreboard
        if (winnerMap != null) {
            setMapScoreboard(winnerMap.getNumericId());
        }

        // Удаляем предметы для голосования у всех игроков
        removeVotingTickets();

        // Отправляем результат всем игрокам
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.send(new MapVotingResultPacket(winnerMapId, voteCount), player);
        }

        Maniacrev.LOGGER.info("Voting ended. Winner: {} (ID: {})", winnerMapId, winnerMap != null ? winnerMap.getNumericId() : -1);
    }

    // Вызывается клиентом после завершения анимации
    public void sendChatMessage() {
        if (chatMessageSent || server == null) return;
        chatMessageSent = true;

        // Используем кешированного победителя вместо пересчета
        String winnerMapId = cachedWinnerMapId;
        if (winnerMapId == null) {
            Maniacrev.LOGGER.warn("Winner map ID is null when sending chat message!");
            return;
        }

        MapData winnerMap = MapRegistry.getMapById(winnerMapId);

        // Отправляем сообщение в чат
        if (winnerMap != null) {
            net.minecraft.network.chat.Component chatMessage = net.minecraft.network.chat.Component.literal(
                    "§6§l[Голосование] §r§aПобедившая карта: §e" + winnerMap.getName() + " §7(ID: " + winnerMap.getNumericId() + ")"
            );

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(chatMessage);
            }
        }
    }

    private void removeVotingTickets() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Удаляем все предметы для голосования из инвентаря
            player.getInventory().clearOrCountMatchingItems(
                    stack -> stack.getItem() == ModItems.VOTING_TICKET.get(),
                    Integer.MAX_VALUE,
                    player.inventoryMenu.getCraftSlots()
            );
        }
    }

    private void setMapScoreboard(int mapNumericId) {
        if (server == null) return;

        Scoreboard scoreboard = server.getScoreboard();

        // Получаем или создаем objective "map"
        Objective mapObjective = scoreboard.getObjective("map");
        if (mapObjective == null) {
            mapObjective = scoreboard.addObjective(
                    "map",
                    ObjectiveCriteria.DUMMY,
                    net.minecraft.network.chat.Component.literal("Map"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
        }

        // Устанавливаем значение для псевдоигрока "Game"
        scoreboard.getOrCreatePlayerScore("Game", mapObjective).setScore(mapNumericId);

        Maniacrev.LOGGER.info("Set scoreboard 'map' for 'Game' to {}", mapNumericId);
    }

    public Map<String, Integer> getVoteCount() {
        Map<String, Integer> count = new HashMap<>();
        for (String mapId : votes.values()) {
            count.put(mapId, count.getOrDefault(mapId, 0) + 1);
        }
        return count;
    }

    private String determineWinner(Map<String, Integer> voteCount) {
        if (voteCount.isEmpty()) {
            // Никто не проголосовал - выбираем рандомную карту
            List<MapData> maps = MapRegistry.getAllMaps();
            if (maps.isEmpty()) return null;
            return maps.get(RANDOM.nextInt(maps.size())).getId();
        }

        int maxVotes = Collections.max(voteCount.values());
        List<String> winners = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() == maxVotes) {
                winners.add(entry.getKey());
            }
        }

        // Если несколько победителей - выбираем рандомно
        if (winners.isEmpty()) return null;
        return winners.get(RANDOM.nextInt(winners.size()));
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public List<String> getTiedMaps() {
        if (!votingActive) return new ArrayList<>();

        Map<String, Integer> voteCount = getVoteCount();
        if (voteCount.isEmpty()) return new ArrayList<>();

        int maxVotes = Collections.max(voteCount.values());
        List<String> tied = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() == maxVotes) {
                tied.add(entry.getKey());
            }
        }

        return tied.size() > 1 ? tied : new ArrayList<>();
    }
}