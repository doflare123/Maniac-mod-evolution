package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenGuiPacket;

import java.util.Collection;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(src -> src.hasPermission(2))

                // /maniacrev start
                .then(Commands.literal("start")
                        .executes(ctx -> {
                            GameManager.startGame(ctx.getSource());
                            return 1;
                        }))

                // /maniacrev stop
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            GameManager.stopGame(ctx.getSource());
                            return 1;
                        }))

                // /maniacrev timer ...
                .then(Commands.literal("timer")
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    GameManager.startTimer();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aТаймер запущен"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    GameManager.stopTimer();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§cТаймер остановлен"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.setTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eТаймер установлен на " + sec + " сек"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("add")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.addTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eДобавлено " + sec + " сек к таймеру"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("maxtime")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.setMaxTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eМакс. время установлено: " + sec + " сек"), true);
                                            return 1;
                                        }))))

                // /maniacrev phase <0-3>
                .then(Commands.literal("phase")
                        .then(Commands.argument("phase", IntegerArgumentType.integer(0, 3))
                                .executes(ctx -> {
                                    int phase = IntegerArgumentType.getInteger(ctx, "phase");
                                    GameManager.setPhase(phase);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§eФаза установлена: " + phase), true);
                                    return 1;
                                })))

                // /maniacrev addexp <targets> <amount>
                .then(Commands.literal("addexp")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addExp(ctx)))))

                // /maniacrev addmoney <targets> <amount>
                .then(Commands.literal("addmoney")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> addMoney(ctx)))))

                // /maniacrev perks clear [targets]
                .then(Commands.literal("perks")
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearPerks(ctx, null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> clearPerks(ctx, EntityArgument.getPlayers(ctx, "targets")))))
                        // ФИКСnull: теперь принимает несколько игроков через EntityArgument.players()
                        .then(Commands.literal("open")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> openPerkGui(ctx)))))

                // /maniacrev guide
                .then(Commands.literal("guide")
                        .executes(ctx -> openGuide(ctx)))
        );
    }

    private static int addExp(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);
            data.addExperience(amount);
            PlayerDataManager.syncToClient(player);
            player.displayClientMessage(Component.literal("§a+" + amount + " опыта!"), true);
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aДобавлено " + amount + " опыта " + targets.size() + " игрокам"), true);
        return targets.size();
    }

    private static int addMoney(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);
            data.addCoins(amount);
            PlayerDataManager.syncToClient(player);
            if (amount > 0) {
                player.displayClientMessage(Component.literal("§6+" + amount + " монет!"), true);
            }
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aДобавлено " + amount + " монет " + targets.size() + " игрокам"), true);
        return targets.size();
    }

    private static int clearPerks(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (targets == null) {
            targets = ctx.getSource().getServer().getPlayerList().getPlayers();
        }

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);
            data.clearPerks(player);
            PlayerDataManager.syncToClient(player);
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§cПерки сняты у " + count + " игроков"), true);
        return count;
    }

    // ФИКС: Теперь открывает UI для нескольких игроков (@a, @a[team=...] и т.д.)
    private static int openPerkGui(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

        for (ServerPlayer target : targets) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new OpenGuiPacket(OpenGuiPacket.GuiType.PERK_SELECTION)
            );
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§aОткрыто меню выбора перков для " + count + " игроков"), true);
        return count;
    }

    private static int openGuide(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenGuiPacket(OpenGuiPacket.GuiType.GUIDE)
            );
        }
        return 1;
    }
}
