package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.example.maniacrevolution.settings.GameSettings;

import java.util.UUID;

public class HpBoostCommand {
    private static final UUID SETTINGS_HEALTH_ID = UUID.fromString("35bddcdb-54a1-49d1-b91c-aaaf560324b6");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .then(Commands.literal("hp_boost")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            applyHpBoost(context.getSource().getServer());
                            context.getSource().sendSuccess(
                                    () -> Component.literal("§aДополнительное здоровье применено"), true);
                            return 1;
                        })));
    }

    public static void applyHpBoost(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyHpBoost(player);
        }
    }

    public static void applyHpBoost(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(SETTINGS_HEALTH_ID);
        int bonus = GameSettings.get(player.server).getHpBoost();
        if (bonus > 0 && player.getTeam() != null
                && player.getTeam().getName().equalsIgnoreCase("survivors")) {
            // One menu HP means one health point, independent of class effects.
            health.addTransientModifier(new AttributeModifier(SETTINGS_HEALTH_ID,
                    "Game settings health", bonus, AttributeModifier.Operation.ADDITION));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
