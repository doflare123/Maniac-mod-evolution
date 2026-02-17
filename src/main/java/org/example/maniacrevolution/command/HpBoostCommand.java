package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.settings.GameSettings;

public class HpBoostCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
            .then(Commands.literal("hp_boost")
                .requires(source -> source.hasPermission(2))
                .executes(HpBoostCommand::applyHpBoost)
            )
        );
    }

    private static int applyHpBoost(CommandContext<CommandSourceStack> context) {
        GameSettings settings = GameSettings.get(context.getSource().getServer());
        int hpBoost = settings.getHpBoost();
        
        if (hpBoost <= 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("§eДоп. ХП отключено (значение: " + hpBoost + ")"), true);
            return 0;
        }
        
        // Вычисляем уровень эффекта: hpBoost / 2 - 1
        int effectLevel = (hpBoost / 2) - 1;
        
        if (effectLevel < 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("§eУровень эффекта слишком низкий"), true);
            return 0;
        }
        
        int playersAffected = 0;
        
        // Применяем эффект ко всем игрокам в команде survivors
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            // Проверяем, находится ли игрок в команде survivors
            if (player.getTeam() != null && player.getTeam().getName().equals("survivors")) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.HEALTH_BOOST,
                    -1, // Бесконечная длительность
                    effectLevel,
                    false,
                    false
                ));
                playersAffected++;
            }
        }
        
        final int finalPlayersAffected = playersAffected;
        context.getSource().sendSuccess(() -> 
            Component.literal("§aПрименён Health Boost уровня " + effectLevel + 
                            " к " + finalPlayersAffected + " игрокам команды survivors"), true);
        return 1;
    }
}
