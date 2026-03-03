package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.FurySwipesCapability;
import org.example.maniacrevolution.capability.FurySwipesCapabilityProvider;
import org.example.maniacrevolution.entity.RageBeeEntity;

import java.util.Collection;
import java.util.List;

/**
 * Команды:
 *   /maniacrev fury clear <targets>   — очистить стаки Fury Swipes
 *   /maniacrev bees kill <targets>    — убить пчёл указанных игроков
 *   /maniacrev bees kill *            — убить всех RageBee на сервере
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeastCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(
                Commands.literal("maniacrev").requires(src -> src.hasPermission(2))

                        // /maniacrev fury clear <targets>
                        .then(Commands.literal("fury")
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> clearFury(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"))))
                                )
                        )

                        // /maniacrev bees kill <targets> | all
                        .then(Commands.literal("bees")
                                .then(Commands.literal("kill")
                                        // По владельцу: убивает пчёл принадлежащих этим игрокам
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> killBeesOfOwners(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"))))
                                        // Все пчёлы на сервере
                                        .then(Commands.literal("all")
                                                .executes(ctx -> killAllBees(ctx.getSource())))
                                )
                        )
        );
    }

    // ── Очистка Fury Swipes ───────────────────────────────────────────────────

    private static int clearFury(CommandSourceStack src, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer p : targets) {
            FurySwipesCapability cap = FurySwipesCapabilityProvider.get(p);
            if (cap == null) continue;
            cap.clearStacks();
            cap.syncToClient(p);
            count++;
        }
        int n = count;
        src.sendSuccess(() -> Component.literal(
                "§aСтаки Fury Swipes очищены у §f" + n + " §aигроков"), true);
        return count;
    }

    // ── Убийство пчёл по владельцу ────────────────────────────────────────────

    private static int killBeesOfOwners(CommandSourceStack src,
                                        Collection<ServerPlayer> owners) {
        int killed = 0;
        for (ServerPlayer owner : owners) {
            if (!(owner.level() instanceof ServerLevel sl)) continue;
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof RageBeeEntity bee
                        && bee.getOwnerUUID() != null
                        && bee.getOwnerUUID().equals(owner.getUUID())) {
                    bee.kill();
                    killed++;
                }
            }
        }
        int n = killed;
        src.sendSuccess(() -> Component.literal(
                "§aУбито §f" + n + " §aпчёл"), true);
        return killed;
    }

    // ── Убийство всех RageBee ─────────────────────────────────────────────────

    private static int killAllBees(CommandSourceStack src) {
        if (src.getServer() == null) return 0;
        int killed = 0;
        for (ServerLevel level : src.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof RageBeeEntity) {
                    e.kill();
                    killed++;
                }
            }
        }
        int n = killed;
        src.sendSuccess(() -> Component.literal(
                "§aУбиты все разъярённые пчёлы: §f" + n), true);
        return killed;
    }
}