package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WallhackHighlightPacket;
import org.example.maniacrevolution.perk.*;

import java.util.*;

/**
 * Активный перк: подсвечивает маньяка через стены.
 * Работает только для владельца перка.
 */
public class WallhackPerk extends Perk {

    private static final double MAX_DISTANCE = 15.0;
    private static final int HIGHLIGHT_DURATION = 5 * 20; // 5 секунд в тиках
    private static final double VIEW_ANGLE_COS = Math.cos(Math.toRadians(45)); // 45° угол обзора

    // Отслеживание активных подсветок: UUID владельца -> данные
    private static final Map<UUID, HighlightData> ACTIVE_HIGHLIGHTS = new HashMap<>();

    public WallhackPerk() {
        super(new Builder("wallhack")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(70));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Ищем маньяков в поле зрения
        List<ServerPlayer> maniacs = findManiacsInView(player);

        if (maniacs.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cНикого не обнаружено"),
                    true
            );
            return;
        }

        // Собираем UUID маньяков
        Set<UUID> maniacUUIDs = new HashSet<>();
        for (ServerPlayer maniac : maniacs) {
            maniacUUIDs.add(maniac.getUUID());
        }

        // Сохраняем данные активной подсветки
        ACTIVE_HIGHLIGHTS.put(player.getUUID(), new HighlightData(
                maniacUUIDs,
                System.currentTimeMillis() + (HIGHLIGHT_DURATION * 50) // тики -> мс
        ));

        // Отправляем пакет только этому игроку
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new WallhackHighlightPacket(maniacUUIDs, HIGHLIGHT_DURATION)
        );

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aОбнаружено маньяков: " + maniacs.size()),
                true
        );
    }

    @Override
    public void onTick(ServerPlayer player) {
        HighlightData data = ACTIVE_HIGHLIGHTS.get(player.getUUID());

        if (data != null && System.currentTimeMillis() >= data.endTime) {
            // Подсветка закончилась
            ACTIVE_HIGHLIGHTS.remove(player.getUUID());

            // Отправляем пакет на отключение подсветки
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new WallhackHighlightPacket(Collections.emptySet(), 0)
            );
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // При снятии перка убираем подсветку
        if (ACTIVE_HIGHLIGHTS.containsKey(player.getUUID())) {
            ACTIVE_HIGHLIGHTS.remove(player.getUUID());

            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new WallhackHighlightPacket(Collections.emptySet(), 0)
            );
        }
    }

    /**
     * Находит маньяков в поле зрения игрока.
     */
    private List<ServerPlayer> findManiacsInView(ServerPlayer viewer) {
        List<ServerPlayer> result = new ArrayList<>();

        Vec3 viewerPos = viewer.getEyePosition();
        Vec3 viewerLook = viewer.getLookAngle();

        // Поиск в радиусе
        AABB searchBox = new AABB(viewerPos, viewerPos).inflate(MAX_DISTANCE);
        List<Player> nearbyPlayers = viewer.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                p -> p != viewer && p instanceof ServerPlayer
        );

        for (Player other : nearbyPlayers) {
            // Проверяем, что это маньяк
            if (!isManiac((ServerPlayer) other)) continue;

            // Проверяем расстояние
            double distance = viewerPos.distanceTo(other.getEyePosition());
            if (distance > MAX_DISTANCE) continue;

            // Проверяем, что маньяк в поле зрения
            Vec3 toTarget = other.getEyePosition().subtract(viewerPos).normalize();
            double dotProduct = viewerLook.dot(toTarget);

            if (dotProduct >= VIEW_ANGLE_COS) {
                result.add((ServerPlayer) other);
            }
        }

        return result;
    }

    /**
     * Проверяет, является ли игрок маньяком.
     */
    private boolean isManiac(ServerPlayer player) {
        // Проверяем команду игрока
        if (player.getTeam() != null) {
            String teamName = player.getTeam().getName();
            return teamName.equalsIgnoreCase("maniac") ||
                    teamName.equalsIgnoreCase("маньяк");
        }
        return false;
    }

    /**
     * Проверяет, активна ли подсветка для игрока.
     */
    public static boolean hasActiveHighlight(UUID playerId) {
        return ACTIVE_HIGHLIGHTS.containsKey(playerId);
    }

    /**
     * Принудительно отключает подсветку.
     */
    public static void forceDisable(ServerPlayer player) {
        ACTIVE_HIGHLIGHTS.remove(player.getUUID());

        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new WallhackHighlightPacket(Collections.emptySet(), 0)
        );
    }

    private record HighlightData(Set<UUID> maniacUUIDs, long endTime) {}
}