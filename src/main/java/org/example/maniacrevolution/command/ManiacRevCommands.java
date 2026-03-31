package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.FurySwipesCapabilityProvider;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncPlayerClassPacket;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ManiacRevCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("maniacrev")
                        .requires(src -> src.hasPermission(2)) // OP уровень 2 — работает из командных блоков

                        // maniacrev reset class <targets>
                        .then(Commands.literal("reset")
                                .then(Commands.literal("class")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> resetClass(ctx,
                                                        EntityArgument.getPlayers(ctx, "targets")))
                                        )
                                )

                                // maniacrev reset furystacks <targets>
                                .then(Commands.literal("furystacks")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> resetFuryStacks(ctx,
                                                        EntityArgument.getPlayers(ctx, "targets")))
                                        )
                                )

                                // maniacrev reset all <targets>
                                .then(Commands.literal("all")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> resetAll(ctx,
                                                        EntityArgument.getPlayers(ctx, "targets")))
                                        )
                                )
                        )
        );
    }

    // ── Сброс класса (скорборд + клиентские данные) ───────────────────────────

    private static int resetClass(CommandContext<CommandSourceStack> ctx,
                                  Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            clearScoreboards(player);
            sendResetClassPacket(player);
            count++;
        }

        if (count == 1) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aСброшен класс игрока §f" + players.iterator().next().getName().getString()
            ), true);
        } else {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aСброшен класс у §f" + finalCount + " §aигроков"
            ), true);
        }

        return count;
    }

    // ── Сброс стаков Fury Swipes ──────────────────────────────────────────────

    private static int resetFuryStacks(CommandContext<CommandSourceStack> ctx,
                                       Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            var cap = FurySwipesCapabilityProvider.get(player);
            if (cap != null) {
                cap.clearStacks();
                cap.syncToClient(player);

                // Уведомляем остальных игроков об обнулении стаков
                player.level().players().forEach(p -> {
                    if (p instanceof ServerPlayer other && !other.getUUID().equals(player.getUUID())) {
                        ModNetworking.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> other),
                                new org.example.maniacrevolution.network.packets
                                        .SyncFurySwipesTargetPacket(player.getUUID(), java.util.List.of())
                        );
                    }
                });
            }
            count++;
        }

        if (count == 1) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aСброшены стаки Fury Swipes у §f" + players.iterator().next().getName().getString()
            ), true);
        } else {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aСброшены стаки Fury Swipes у §f" + finalCount + " §aигроков"
            ), true);
        }

        return count;
    }

    // ── Сброс всего ───────────────────────────────────────────────────────────

    private static int resetAll(CommandContext<CommandSourceStack> ctx,
                                Collection<ServerPlayer> players) {
        // Вызываем оба сброса без дублирования сообщений
        for (ServerPlayer player : players) {
            clearScoreboards(player);
            sendResetClassPacket(player);

            var cap = FurySwipesCapabilityProvider.get(player);
            if (cap != null) {
                cap.clearStacks();
                cap.syncToClient(player);

                player.level().players().forEach(p -> {
                    if (p instanceof ServerPlayer other && !other.getUUID().equals(player.getUUID())) {
                        ModNetworking.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> other),
                                new org.example.maniacrevolution.network.packets
                                        .SyncFurySwipesTargetPacket(player.getUUID(), java.util.List.of())
                        );
                    }
                });
            }
        }

        int count = players.size();
        if (count == 1) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aПолный сброс данных игрока §f" + players.iterator().next().getName().getString()
            ), true);
        } else {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aПолный сброс данных §f" + finalCount + " §aигроков"
            ), true);
        }

        return count;
    }

    // ── Вспомогательные ───────────────────────────────────────────────────────

    /** Обнуляет SurvivorClass и ManiacClass в скорборде */
    private static void clearScoreboards(ServerPlayer player) {
        Scoreboard sb = player.getServer().getScoreboard();

        for (CharacterType type : CharacterType.values()) {
            Objective obj = sb.getObjective(type.getScoreboardName());
            if (obj == null) continue;
            if (sb.hasPlayerScore(player.getScoreboardName(), obj)) {
                sb.resetPlayerScore(player.getScoreboardName(), obj);
            }
        }
    }

    /** Отправляет клиенту пакет сброса класса (-1 для обоих типов) */
    private static void sendResetClassPacket(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerClassPacket(CharacterType.SURVIVOR, -1)
        );
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerClassPacket(CharacterType.MANIAC, -1)
        );
    }
}