package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.maze.MazeGenerator;
import org.example.maniacrevolution.maze.MazeManager;

import java.util.UUID;

/**
 * Тестовый предмет-генератор лабиринта.
 *
 * ПКМ → находит точку в 400 блоках по Z → спавнит лабиринт
 *       в свободном месте (без пересечений с другими) → пишет координаты в чат.
 * Лабиринт уничтожается через 60 секунд.
 *
 * Иконка: ванильный COMPASS (ресурспак не нужен).
 * Получить: /give @s maniacrevolution:maze_spawner
 */
public class MazeSpawnerItem extends Item {

    private static final int SPAWN_DISTANCE = 400; // блоков по Z

    public MazeSpawnerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        ServerLevel serverLevel  = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // ── Желаемый origin: 400 по Z от игрока, поверхность ─────────────────
        int baseX = (int) player.getX();
        int baseZ = (int) player.getZ() + SPAWN_DISTANCE;
        int baseY = findSafeY(serverLevel, baseX, baseZ);
        BlockPos preferredOrigin = new BlockPos(baseX, baseY, baseZ);

        // ── Спавним (MazeManager сам найдёт свободное место) ─────────────────
        long seed = System.currentTimeMillis();
        UUID mazeId = MazeManager.getInstance().spawnMaze(serverLevel, preferredOrigin, seed);

        // ── Получаем реально использованный origin ────────────────────────────
        BlockPos usedOrigin = MazeManager.getInstance().getOrigin(mazeId);
        if (usedOrigin == null) usedOrigin = preferredOrigin; // fallback (не должно случаться)

        // ── Геттеры точек через новый seed-экземпляр генератора ──────────────
        MazeGenerator gen   = new MazeGenerator(seed);
        BlockPos entry = gen.getEntryPoint(usedOrigin);
        BlockPos exit  = gen.getExitPoint(usedOrigin);

        int mazeW = MazeGenerator.worldWidth();
        int mazeD = MazeGenerator.worldDepth();
        boolean wasShifted = !usedOrigin.equals(preferredOrigin);

        // ── Сообщение в чат ──────────────────────────────────────────────────
        msg(serverPlayer, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ChatFormatting.DARK_PURPLE);
        msg(serverPlayer, "🧩 Лабиринт сгенерирован!", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);

        if (wasShifted) {
            msg(serverPlayer,
                    "⚠ Смещён: рядом уже есть лабиринт!",
                    ChatFormatting.YELLOW);
        }

        sendPair(serverPlayer, "Origin:  ", formatPos(usedOrigin));
        sendPair(serverPlayer, "Вход:    ", formatPos(entry));
        sendPair(serverPlayer, "Выход:   ", formatPos(exit));
        sendPair(serverPlayer, "Размер:  ", mazeW + " × " + mazeD + " блоков");
        sendPair(serverPlayer, "⏱ Снос через: ", "60 секунд");

        // Готовая команда телепорта
        serverPlayer.sendSystemMessage(
                Component.literal("  /tp " + entry.getX() + " " + entry.getY() + " " + entry.getZ())
                        .withStyle(ChatFormatting.AQUA));

        msg(serverPlayer, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ChatFormatting.DARK_PURPLE);

        // Cooldown 2 секунды
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(stack);
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /**
     * Сканирует по Y сверху вниз, ищет первый твёрдый блок с воздухом над ним.
     * Возвращает Y+1 (пол лабиринта будет лежать на найденной поверхности).
     */
    private int findSafeY(ServerLevel level, int x, int z) {
        for (int y = 200; y > 0; y--) {
            BlockPos check = new BlockPos(x, y, z);
            if (level.getBlockState(check).isSolid()
                    && level.getBlockState(check.above()).isAir()) {
                return y + 1;
            }
        }
        return 65; // запасной вариант
    }

    private String formatPos(BlockPos p) {
        return "X:" + p.getX() + "  Y:" + p.getY() + "  Z:" + p.getZ();
    }

    private void msg(ServerPlayer p, String text, ChatFormatting... fmt) {
        Component c = Component.literal(text);
        for (ChatFormatting f : fmt) c = c.copy().withStyle(f);
        p.sendSystemMessage(c);
    }

    private void sendPair(ServerPlayer p, String label, String value) {
        p.sendSystemMessage(
                Component.literal("  " + label).withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(value).withStyle(ChatFormatting.WHITE)));
    }
}