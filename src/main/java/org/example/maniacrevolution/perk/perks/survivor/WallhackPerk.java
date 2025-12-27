package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.perk.*;

import java.util.*;

/**
 * Пассивный перк с использованием встроенного Glowing эффекта Minecraft.
 * Подсвечивает маньяка обводкой на 2 секунды.
 */
public class WallhackPerk extends Perk {

    private static final double MAX_DISTANCE = 15.0;
    private static final int HIGHLIGHT_DURATION = 4 * 20; // 4 секунды в тиках
    private static final double VIEW_ANGLE_COS = Math.cos(Math.toRadians(45));

    public WallhackPerk() {
        super(new Builder("wallhack")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(45));
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        List<ServerPlayer> maniacs = findManiacsInView(player);
        return !maniacs.isEmpty();
    }

    @Override
    public void onTrigger(ServerPlayer player) {
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) return;
        List<ServerPlayer> maniacs = findManiacsInView(player);

        if (maniacs.isEmpty()) return;

        // Накладываем эффект Glowing на каждого маньяка
        for (ServerPlayer maniac : maniacs) {
            MobEffectInstance glowingEffect = new MobEffectInstance(
                    MobEffects.GLOWING,      // Эффект свечения
                    HIGHLIGHT_DURATION,       // Длительность (2 секунды = 40 тиков)
                    0,                        // Уровень эффекта (0 = уровень 1)
                    false,                    // ambient (частицы менее заметны)
                    false,                    // visible (показывать иконку эффекта)
                    true                      // showIcon (показывать над головой)
            );

            maniac.addEffect(glowingEffect);

            System.out.println("Applied Glowing to: " + maniac.getName().getString() + " for 2 seconds");
        }

        // Звук и сообщение для владельца перка
        player.playNotifySound(
                SoundEvents.BLAZE_BURN,
                SoundSource.PLAYERS,
                3.0f,
                4.0f
        );

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c⚠ Обнаружен маньяк!"),
                true
        );
    }

    @Override
    public void onTick(ServerPlayer player) {
        // Больше не нужно отслеживать вручную - эффект сам пропадёт через 2 секунды
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Не нужно ничего делать - эффект управляется самой игрой
    }

    private List<ServerPlayer> findManiacsInView(ServerPlayer viewer) {
        List<ServerPlayer> result = new ArrayList<>();

        Vec3 viewerPos = viewer.getEyePosition();
        Vec3 viewerLook = viewer.getLookAngle();

        AABB searchBox = new AABB(viewerPos, viewerPos).inflate(MAX_DISTANCE);
        List<Player> nearbyPlayers = viewer.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                p -> p != viewer && p instanceof ServerPlayer
        );

        for (Player other : nearbyPlayers) {
            if (!isManiac((ServerPlayer) other)) continue;

            double distance = viewerPos.distanceTo(other.getEyePosition());
            if (distance > MAX_DISTANCE) continue;

            Vec3 toTarget = other.getEyePosition().subtract(viewerPos).normalize();
            double dotProduct = viewerLook.dot(toTarget);

            if (dotProduct >= VIEW_ANGLE_COS) {
                result.add((ServerPlayer) other);
            }
        }

        return result;
    }

    private boolean isManiac(ServerPlayer player) {
        if (player.getTeam() != null) {
            String teamName = player.getTeam().getName();
            return teamName.equalsIgnoreCase("maniac") ||
                    teamName.equalsIgnoreCase("маньяк");
        }
        return false;
    }
}