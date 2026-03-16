package org.example.maniacrevolution.hack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Команды для управления взломом компьютеров.
 *
 * /maniacrev computer reset all          — сбросить всё
 * /maniacrev computer reset id <id>      — сбросить конкретный id
 * /maniacrev computer setid <x> <y> <z> <id>  — задать id блока
 * /maniacrev computer status             — показать статус
 * /maniacrev computer setgoal <n>        — изменить кол-во нужных взломов
 * /maniacrev computer setpoints <pts>    — изменить нужное кол-во очков
 *
 * Вызов из ModCommands.register(dispatcher):
 *   HackCommands.register(dispatcher);
 */
public class HackCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("maniacrev").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("computer")

                                // reset all
                                .then(Commands.literal("reset")
                                        .then(Commands.literal("all")
                                                .executes(ctx -> resetAll(ctx.getSource())))

                                        // reset id <id>
                                        .then(Commands.literal("id")
                                                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> resetById(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "id")))))

                                        .then(Commands.literal("blocks")
                                                .executes(ctx -> resetBlocks(ctx.getSource()))))

                                .then(Commands.literal("unblock")
                                        .executes(ctx -> unblockAll(ctx.getSource())))

                                // setid <x> <y> <z> <id>
                                .then(Commands.literal("setid")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setId(ctx.getSource(),
                                                                BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                IntegerArgumentType.getInteger(ctx, "id"))))))

                                // status
                                .then(Commands.literal("status")
                                        .executes(ctx -> showStatus(ctx.getSource())))

                                // setgoal <n>
                                .then(Commands.literal("setgoal")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> setGoal(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "count")))))

                                // setpoints <pts>
                                .then(Commands.literal("setpoints")
                                        .then(Commands.argument("points", FloatArgumentType.floatArg(0.1f))
                                                .executes(ctx -> setPoints(ctx.getSource(),
                                                        FloatArgumentType.getFloat(ctx, "points")))))
                        )
        );
    }

    private static int resetBlocks(CommandSourceStack src) {
        resetAllBlockEntities(src);
        src.sendSuccess(() -> Component.literal("§aВизуальное состояние всех компьютеров сброшено."), true);
        return 1;
    }

    private static int unblockAll(CommandSourceStack src) {
        // Снимаем все блокировки
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositions()) {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
                    be.setBlocked(false);
                }
            }
        }
        src.sendSuccess(() -> Component.literal("§aВсе блокировки компьютеров сняты."), true);
        return 1;
    }

    private static int resetAll(CommandSourceStack src) {
        HackManager.get().resetAll(src.getServer());
        resetAllBlockEntities(src);
        // Снимаем флаги blocked
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositions()) {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
                    be.setBlocked(false);
                }
            }
        }
        src.sendSuccess(() -> Component.literal("§aВсе компьютеры сброшены."), true);
        return 1;
    }

    private static int resetById(CommandSourceStack src, int id) {
        HackManager.get().resetById(src.getServer(), id);
        // Сбрасываем BlockEntity с этим id
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositions()) {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer
                        && computer.getComputerId() == id) {
                    computer.resetProgress();
                }
            }
        }
        src.sendSuccess(() -> Component.literal(
                "§aКомпьютеры #" + id + " сброшены."), true);
        return 1;
    }

    private static int setId(CommandSourceStack src, BlockPos pos, int id) {
        ServerLevel level = src.getLevel();
        if (level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
            be.setComputerId(id);
            src.sendSuccess(() -> Component.literal(
                    "§aID компьютера на " + pos.toShortString() + " установлен: §e" + id), true);
            return 1;
        }
        src.sendFailure(Component.literal("§cНет компьютера на этой позиции."));
        return 0;
    }

    private static int showStatus(CommandSourceStack src) {
        HackManager m = HackManager.get();
        src.sendSuccess(() -> Component.literal(
                "§6=== Статус взлома ===\n" +
                        "§fВзломано: §e" + m.getTotalHacked() + "§f/§e" + HackConfig.COMPUTERS_NEEDED_FOR_WIN + "\n" +
                        "§fОчков для взлома: §e" + HackConfig.HACK_POINTS_REQUIRED + "\n" +
                        "§fЦель (кол-во компьютеров): §e" + HackConfig.COMPUTERS_NEEDED_FOR_WIN
        ), false);
        return 1;
    }

    private static int setGoal(CommandSourceStack src, int count) {
        HackConfig.COMPUTERS_NEEDED_FOR_WIN = count;
        // Сохраняем и синхронизируем клиентам немедленно
        HackManager.get().saveAndSync(src.getServer());
        src.sendSuccess(() -> Component.literal(
                "§aЦель изменена: нужно взломать §e" + count + " §aкомпьютеров."), true);
        return 1;
    }

    private static int setPoints(CommandSourceStack src, float points) {
        HackConfig.HACK_POINTS_REQUIRED = points;
        HackManager.get().saveAndSync(src.getServer());
        src.sendSuccess(() -> Component.literal(
                "§aОчков для взлома изменено: §e" + points), true);
        return 1;
    }

    private static void resetAllBlockEntities(CommandSourceStack src) {
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositions()) {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                    computer.resetProgress();
                }
            }
        }
    }
}