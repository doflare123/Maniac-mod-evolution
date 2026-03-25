package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.perk.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Путеводная Света (Активный) (Маньяк)
 * Рисует линию из частиц до ближайшего выжившего — только для владельца перка.
 * Частицы держатся TRAIL_DURATION_MS миллисекунд.
 */
public class GuidingLightPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int    COOLDOWN_SEC      = 35;
    private static final float  MANA_COST         = 2f;
    private static final double STEP              = 0.75; // расстояние между частицами
    private static final long   TRAIL_DURATION_MS = 2000; // 2 секунды

    // uuid -> время окончания трейла
    private static final Map<UUID, Long>         activeTrails  = new ConcurrentHashMap<>();
    // uuid -> цель трейла
    private static final Map<UUID, ServerPlayer> trailTargets  = new ConcurrentHashMap<>();

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
                .append(Component.literal(" только для тебя, на ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(TRAIL_DURATION_MS / 1000 + " сек.")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" КД: " + COOLDOWN_SEC + " сек. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public void onActivate(ServerPlayer player) {
        if (player.getServer() == null) return;

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

        // Запускаем трейл
        activeTrails.put(player.getUUID(), System.currentTimeMillis() + TRAIL_DURATION_MS);
        trailTargets.put(player.getUUID(), nearest);

        // Сразу рисуем первый раз
        drawTrail(player, nearest);

        double dist = Math.sqrt(nearestDist);
        player.displayClientMessage(
                Component.literal("✨ Ближайший выживший в " + (int) dist + " блоках!")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }

    // ── Тик: обновляем частицы пока трейл активен ─────────────────────────

    @Override
    public void onTick(ServerPlayer player) {
        Long endTime = activeTrails.get(player.getUUID());
        if (endTime == null) return;

        // Время вышло
        if (System.currentTimeMillis() >= endTime) {
            activeTrails.remove(player.getUUID());
            trailTargets.remove(player.getUUID());
            return;
        }

        ServerPlayer target = trailTargets.get(player.getUUID());
        if (target == null || !target.isAlive()) {
            activeTrails.remove(player.getUUID());
            trailTargets.remove(player.getUUID());
            return;
        }

        // Перерисовываем линию каждый тик только для владельца
        drawTrail(player, target);
    }

    // ── Снятие перка ──────────────────────────────────────────────────────

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activeTrails.remove(player.getUUID());
        trailTargets.remove(player.getUUID());
    }

    // ── Отрисовка линии только для владельца ──────────────────────────────

    private static void drawTrail(ServerPlayer player, ServerPlayer target) {
        Vec3 start = player.position().add(0, 1.0, 0);
        Vec3 end   = target.position().add(0, 1.0, 0);
        Vec3 dir   = end.subtract(start);
        double length = dir.length();
        if (length < 0.1) return;

        Vec3 stepVec = dir.normalize().scale(STEP);
        int count = (int)(length / STEP);

        for (int i = 0; i <= count; i++) {
            Vec3 pos = start.add(stepVec.scale(i));
            boolean isRod = i % 3 == 0;

            // Отправляем пакет напрямую только владельцу
            var packet = new ClientboundLevelParticlesPacket(
                    isRod ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                    true,
                    pos.x, pos.y, pos.z,
                    0f, 0f, 0f,
                    0f, 1
            );
            player.connection.send(packet);
        }

        // Частицы у цели
        var heartPacket = new ClientboundLevelParticlesPacket(
                ParticleTypes.HEART,
                true,
                target.getX(), target.getY() + 2.0, target.getZ(),
                0.3f, 0.3f, 0.3f,
                0.05f, 3
        );
        player.connection.send(heartPacket);
    }
}