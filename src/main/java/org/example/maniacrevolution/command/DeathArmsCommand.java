package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.item.DeathScytheItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.DeathTeleportWarningPacket;

/** Developer command for previewing the local Death embrace without teleporting. */
public final class DeathArmsCommand {
    private DeathArmsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deatharms")
                .requires(source -> source.hasPermission(2))
                .executes(context -> play(context.getSource(), DeathScytheItem.TELEPORT_WINDUP_TICKS))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                        .executes(context -> play(context.getSource(),
                                IntegerArgumentType.getInteger(context, "ticks")))));
    }

    private static int play(CommandSourceStack source, int durationTicks) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ModNetworking.sendToPlayer(new DeathTeleportWarningPacket(durationTicks), player);
            source.sendSuccess(() -> Component.literal(
                    "§aЭффект рук запущен на §e" + durationTicks + "§a тиков."), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("§cКоманду нужно выполнять от имени игрока."));
            return 0;
        }
    }
}
