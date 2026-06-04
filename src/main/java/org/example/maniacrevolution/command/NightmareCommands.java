package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.nightmare.NightmareManager;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NightmareCommands {
    private NightmareCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("maniacrev").requires(src -> src.hasPermission(2))
                .then(Commands.literal("nightmare")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> clearTargets(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("clear_all")
                                .executes(ctx -> clearAll(ctx.getSource())))));
    }

    private static int clearTargets(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            NightmareManager.getInstance().clear(player);
        }
        int count = targets.size();
        source.sendSuccess(() -> Component.literal("Nightmare data cleared for " + count + " player(s)"), true);
        return count;
    }

    private static int clearAll(CommandSourceStack source) {
        int count = NightmareManager.getInstance().clearAll(source.getServer());
        source.sendSuccess(() -> Component.literal("Nightmare data cleared for all online players: " + count), true);
        return count;
    }
}
