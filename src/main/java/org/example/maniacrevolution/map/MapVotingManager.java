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
    private int tickCounter = 0;
    private String cachedWinnerMapId = null;
    private int resultDelayTicks = 0;
    private static final int RESULT_DELAY = 160; // ~8 секунд (длина анимации)

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
        this.tickCounter = 0;
        this.cachedWinnerMapId = null;
        this.resultDelayTicks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack votingTicket = new ItemStack(ModItems.VOTING_TICKET.get());
            player.getInventory().add(votingTicket);
            ModNetworking.send(new MapVotingPacket(true, duration, new HashMap<>(), null), player);
        }

        Maniacrev.LOGGER.info("Map voting started for {} seconds", duration);
    }

    public void tick() {
        if (!votingActive) {
            // Обрабатываем отложенную отправку результатов
            if (cachedWinnerMapId != null && resultDelayTicks > 0) {
                resultDelayTicks--;
                if (resultDelayTicks == 0) {
                    sendDeferredResults();
                }
            }
            return;
        }

        tickCounter++;

        // Уменьшаем таймер раз в 20 тиков (1 секунда)
        if (tickCounter >= 20) {
            tickCounter = 0;
            timeRemaining--;

            if (!timerLocked && votes.size() == server.getPlayerList().getPlayerCount()) {
                if (timeRemaining > 5) {
                    timeRemaining = 5;
                    timerLocked = true;
                    Maniacrev.LOGGER.info("All players voted, timer set to 5 seconds");
                }
            }

            if (timeRemaining <= 0) {
                endVoting();
                return;
            }
        }

        // Отправляем обновление голосов всем игрокам каждый тик
        Map<String, Integer> voteCount = getVoteCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerVote = votes.get(player.getUUID());
            ModNetworking.send(new MapVotingPacket(false, timeRemaining, voteCount, playerVote), player);
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

    public String getPlayerVote(UUID playerId) {
        return votes.get(playerId);
    }

    private void endVoting() {
        votingActive = false;

        Map<String, Integer> voteCount = getVoteCount();
        cachedWinnerMapId = determineWinner(voteCount);

        removeVotingTickets();

        // Отправляем результат всем игрокам (они будут показывать анимацию)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.send(new MapVotingResultPacket(cachedWinnerMapId, voteCount), player);
        }

        // Запускаем отложенную отправку результатов (после анимации)
        resultDelayTicks = RESULT_DELAY;

        Maniacrev.LOGGER.info("Voting ended. Winner: {} (results will be sent in ~8 seconds)",
                cachedWinnerMapId);
    }

    private void sendDeferredResults() {
        if (server == null || cachedWinnerMapId == null) {
            Maniacrev.LOGGER.warn("Cannot send deferred results: server or winner is null");
            return;
        }

        MapData winnerMap = MapRegistry.getMapById(cachedWinnerMapId);
        if (winnerMap == null) {
            Maniacrev.LOGGER.warn("Winner map not found: {}", cachedWinnerMapId);
            return;
        }

        // Устанавливаем scoreboard
        setMapScoreboard(winnerMap.getNumericId());

        // Отправляем сообщение в чат
        net.minecraft.network.chat.Component chatMessage = net.minecraft.network.chat.Component.literal(
                "§6§l[Голосование] §r§aПобедившая карта: §e" + winnerMap.getName() + " §7(ID: " + winnerMap.getNumericId() + ")"
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(chatMessage);
        }

        Maniacrev.LOGGER.info("Deferred results sent. Winner: {}", winnerMap.getName());

        cachedWinnerMapId = null;
    }

    private void removeVotingTickets() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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

        Objective mapObjective = scoreboard.getObjective("map");
        if (mapObjective == null) {
            mapObjective = scoreboard.addObjective(
                    "map",
                    ObjectiveCriteria.DUMMY,
                    net.minecraft.network.chat.Component.literal("Map"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
        }

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