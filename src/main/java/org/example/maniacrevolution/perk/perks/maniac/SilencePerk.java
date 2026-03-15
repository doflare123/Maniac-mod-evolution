package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.perk.*;

/**
 * Тишина... (Активный) (Маньяк)
 * Накладывает эффект Silence на всех в радиусе RADIUS блоков.
 * Пока эффект активен — нельзя использовать активные перки.
 */
public class SilencePerk extends Perk {

    private static final double RADIUS       = 10.0;
    private static final int    DURATION_SEC = 10;
    private static final int    COOLDOWN_SEC = 100;
    private static final float  MANA_COST    = 10f;

    public SilencePerk() {
        super(new Builder("silence")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    @Override
    public Component getDescription() {
        return Component.literal("Накладывает ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("Тишину")
                        .withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" на всех в радиусе ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) RADIUS + " блоков")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" на ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(DURATION_SEC + " сек.")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" — запрещает использовать активные перки.")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" КД: " + COOLDOWN_SEC + " сек. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (player.getServer() == null) return;

        int silenced = 0;

        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other == player) continue;
            if (player.distanceTo(other) > RADIUS) continue;

            other.addEffect(new MobEffectInstance(
                    ModEffects.SILENCE.get(),
                    DURATION_SEC * 20,
                    0,
                    false, true, true
            ));
            silenced++;

            other.displayClientMessage(
                    Component.literal("🔇 Тишина! Перки заблокированы на " + DURATION_SEC + " сек.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 0.5f
        );

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i;
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        player.getX() + Math.cos(angle) * RADIUS,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * RADIUS,
                        3, 0, 0.3, 0, 0.02);
            }
        }

        player.displayClientMessage(
                Component.literal("🔇 Тишина активирована! Заглушено: " + silenced + ".")
                        .withStyle(ChatFormatting.DARK_RED), true);
    }
}
