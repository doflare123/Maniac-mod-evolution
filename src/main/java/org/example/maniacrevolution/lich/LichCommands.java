package org.example.maniacrevolution.lich;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.ModItems;

public final class LichCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("lich")
                    .then(Commands.literal("spawn_corpses").executes(c -> {
                        var result = LichManager.spawnCorpsesAtAllPoints(c.getSource().getServer());
                        c.getSource().sendSuccess(() -> Component.literal("Создано трупов на точках всех карт: " + result.spawned()
                                + ". Пропущено занятых или недоступных точек: " + result.skipped()), true);
                        return result.spawned();
                    }))
                    .then(Commands.literal("clear_undead").executes(c -> {
                        int count = LichManager.clearUnits(); c.getSource().sendSuccess(() -> Component.literal("Удалено нежити: " + count), true); return count;
                    }))
                    .then(Commands.literal("clear_corpses").executes(c -> {
                        int count = LichManager.clearCorpses(); LichManager.pauseGuaranteedCorpses(true);
                        c.getSource().sendSuccess(() -> Component.literal("Удалено трупов: " + count + ". Базовое пополнение приостановлено до resume_corpses или следующего матча."), true); return count;
                    }))
                    .then(Commands.literal("resume_corpses").executes(c -> {LichManager.pauseGuaranteedCorpses(false);c.getSource().sendSuccess(()->Component.literal("Базовое пополнение трупов включено."),true);return 1;}))
                    .then(Commands.literal("corpses").executes(c -> { LichManager.editor(c.getSource().getPlayerOrException(), true); return 1; }))
                    .then(Commands.literal("corpse_editor").executes(c -> {
                        var p = c.getSource().getPlayerOrException(); p.getInventory().add(new ItemStack(ModItems.LICH_CORPSE_EDITOR.get()));
                        LichManager.editor(p, true); return 1;
                    }))));
    }
}
