package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.perk.*;

/**
 * Путеводная Света (Активный) (Маньяк)
 * Рисует линию из частиц до ближайшего выжившего в Adventure режиме.
 */
public class GuidingLightPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int   COOLDOWN_SEC  = 35;
    private static final float MANA_COST     = 2f;
    private static final double STEP         = 0.5; // расстояние между частицами

    public GuidingLightPerk() {
        super(new Builder("guiding_light")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Рисует линию из частиц до ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("ближайшего выжившего")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" КД: " + COOLDOWN_SEC + " сек. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public void onActivate(ServerPlayer player) {
        if (player.getServer() == null) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Ищем ближайшего выжившего в Adventure
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other == player) continue;

            PerkTeam team = PerkTeam.fromPlayer(other);
            if (team != PerkTeam.SURVIVOR) continue;

            if (other.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) continue;

            double dist = player.distanceToSqr(other);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }

        if (nearest == null) {
            player.displayClientMessage(
                    Component.literal("Нет выживших поблизости!")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
            return;
        }

        // Рисуем линию частиц от маньяка до выжившего
        Vec3 start = player.position().add(0, 1.0, 0); // на уровне груди
        Vec3 end   = nearest.position().add(0, 1.0, 0);
        Vec3 dir   = end.subtract(start);
        double length = dir.length();
        Vec3 step = dir.normalize().scale(STEP);

        int count = (int)(length / STEP);

        for (int i = 0; i <= count; i++) {
            Vec3 pos = start.add(step.scale(i));

            // Чередуем цвета для красивой линии
            if (i % 3 == 0) {
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0
                );
            } else {
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0
                );
            }
        }

        // Частицы у цели
        serverLevel.sendParticles(
                ParticleTypes.HEART,
                nearest.getX(), nearest.getY() + 2.0, nearest.getZ(),
                5, 0.3, 0.3, 0.3, 0.05
        );

        double dist = Math.sqrt(nearestDist);
        player.displayClientMessage(
                Component.literal("✨ Ближайший выживший в " + (int) dist + " блоках!")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }
}
