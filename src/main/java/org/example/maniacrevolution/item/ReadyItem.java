package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ReadyStatusPacket;
import org.example.maniacrevolution.readiness.ReadinessManager;

/**
 * Предмет "Готово" - ПКМ для переключения готовности
 * Меняет текстуру в зависимости от состояния
 */
public class ReadyItem extends Item {

    public ReadyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            net.minecraft.server.level.ServerPlayer serverPlayer =
                    (net.minecraft.server.level.ServerPlayer) player;

            // Проверка класса (обязательна)
            if (!isClassSelected(level, serverPlayer)) {
                player.displayClientMessage(
                        Component.literal("§cСначала выбери класс!"), true);
                return InteractionResultHolder.fail(stack);
            }

            // Проверка перка (обязательна)
            if (!isPerkSelected(level, serverPlayer)) {
                player.displayClientMessage(
                        Component.literal("§cСначала выбери перк!"), true);
                return InteractionResultHolder.fail(stack);
            }

            // Если все проверки пройдены, выполняем основную логику
            boolean currentReady = ReadinessManager.isPlayerReady(player);
            boolean newReady = !currentReady;

            ReadinessManager.setPlayerReady(serverPlayer, newReady);

            // Подсчёт готовых игроков
            int totalPlayers = level.getServer().getPlayerList().getPlayerCount();
            int readyPlayers = 0;
            for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (ReadinessManager.isPlayerReady(p)) {
                    readyPlayers++;
                }
            }

            // Сообщение всем игрокам
            String statusColor = newReady ? "§a" : "§c";
            String statusText = newReady ? "готов" : "отменил готовность";
            Component message = Component.literal(statusColor + player.getName().getString() + " " + statusText +
                    " §7(" + readyPlayers + "/" + totalPlayers + ")");

            for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(message);
            }

            // Меняем предмет на другой в зависимости от состояния
            ItemStack newStack = new ItemStack(newReady ? ModItems.READY_ITEM_ACTIVE.get() : ModItems.READY_ITEM.get());
            player.setItemInHand(hand, newStack);
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * Проверка: выбран ли класс
     * Смотрим scoreboard:
     * - Если игрок в команде maniac: ищем ManiacClass
     * - Если игрок в команде survivors: ищем SurvivorClass
     */
    private boolean isClassSelected(Level level, net.minecraft.server.level.ServerPlayer player) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        String playerTeam = getPlayerTeam(player);

        String objectiveName = "maniac".equalsIgnoreCase(playerTeam) ? "ManiacClass" : "SurvivorClass";
        Objective classObjective = scoreboard.getObjective(objectiveName);

        if (classObjective == null) {
            return false; // Нет objective - класс точно не выбран
        }

        // Если значение = 0, то класс не выбран
        // Если значение != 0, то класс выбран
        Score classScore = scoreboard.getOrCreatePlayerScore(player.getName().getString(), classObjective);
        return classScore.getScore() != 0;
    }

    /**
     * Проверка: выбран ли перк
     * Смотрим в PlayerData - если выбран хотя бы один перк, то selectedPerks не пуст
     */
    private boolean isPerkSelected(Level level, net.minecraft.server.level.ServerPlayer player) {
        org.example.maniacrevolution.data.PlayerData playerData =
                org.example.maniacrevolution.data.PlayerDataManager.get(player);
        return !playerData.getSelectedPerks().isEmpty();
    }

    /**
     * Получить команду игрока (maniac или survivor)
     */
    private String getPlayerTeam(net.minecraft.server.level.ServerPlayer player) {
        if (player.getTeam() != null) {
            return player.getTeam().getName();
        }
        return "unknown";
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§cГотово");
    }
}