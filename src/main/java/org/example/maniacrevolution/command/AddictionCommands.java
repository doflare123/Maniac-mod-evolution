package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;

import java.util.Collection;

/**
 * Команды зависимости (требуют уровень 2 — работают в командных блоках).
 *
 * /maniacrev addiction reset   <цели>  — сбросить шкалу
 * /maniacrev addiction syringes <цели> — сбросить счётчик шприцов
 * /maniacrev addiction clear   <цели>  — сбросить всё
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AddictionCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(
                Commands.literal("maniacrev").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("addiction")

                                .then(Commands.literal("reset")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> doResetAddiction(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets")))))

                                .then(Commands.literal("syringes")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> doResetSyringes(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets")))))

                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> doResetAll(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets")))))
                        )
        );
    }

    // ── Сброс шкалы ──────────────────────────────────────────────────────────

    private static int doResetAddiction(CommandSourceStack src, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer p : targets) {
            AddictionCapability cap = AddictionCapabilityProvider.get(p);
            if (cap == null) continue;
            cap.resetAddiction();
            // Снимаем дебафы
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.removeEffect(MobEffects.DARKNESS);
            cap.syncToClient(p);
            count++;
        }
        int n = count;
        src.sendSuccess(() -> Component.literal("§aШкала зависимости сброшена у §f" + n + " §aигроков"), true);
        return count;
    }

    // ── Сброс счётчика шприцов ───────────────────────────────────────────────

    private static int doResetSyringes(CommandSourceStack src, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer p : targets) {
            AddictionCapability cap = AddictionCapabilityProvider.get(p);
            if (cap == null) continue;
            cap.resetSyringes();
            cap.syncToClient(p);
            count++;
        }
        int n = count;
        src.sendSuccess(() -> Component.literal("§aСчётчик шприцов сброшен у §f" + n + " §aигроков"), true);
        return count;
    }

    // ── Полный сброс ─────────────────────────────────────────────────────────

    private static int doResetAll(CommandSourceStack src, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer p : targets) {
            AddictionCapability cap = AddictionCapabilityProvider.get(p);
            if (cap == null) continue;
            cap.resetAll();
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.removeEffect(MobEffects.DARKNESS);
            cap.syncToClient(p);
            count++;
        }
        int n = count;
        src.sendSuccess(() -> Component.literal("§aДанные зависимости полностью сброшены у §f" + n + " §aигроков"), true);
        return count;
    }
}