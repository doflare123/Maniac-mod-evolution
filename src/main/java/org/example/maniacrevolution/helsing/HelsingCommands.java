package org.example.maniacrevolution.helsing;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class HelsingCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maniacrev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("helsing").then(Commands.literal("equip")
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> {
                            int count = 0;
                            for (var p : EntityArgument.getPlayers(ctx, "targets")) {
                                if (HelsingManager.isHelsing(p)) { HelsingLoadout.equip(p); count++; }
                            }
                            return count;
                        })))));
    }
    private HelsingCommands() {}
}
