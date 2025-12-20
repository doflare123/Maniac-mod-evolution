package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.perk.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Подсветка (Пассивный) (Все)
 * При хаке компа подсвечивает рандомных выживших
 * Количество подсвеченных = количество маньяков с этим перком
 */
public class HighlightPerk extends Perk {

    private static final int GLOW_DURATION_TICKS = 60; // 5 секунд свечения

    // Храним маньяков, у которых активен этот перк
    private static final Set<UUID> activeManiacsWithPerk = new HashSet<>();

    public HighlightPerk() {
        super(new Builder("highlight")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC) // Все могут взять, но работает только у маньяков
                .phases(PerkPhase.ANY)
        );
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Проверяем, что игрок - маньяк
        PerkTeam team = PerkTeam.fromPlayer(player);
        if (team == PerkTeam.MANIAC) {
            activeManiacsWithPerk.add(player.getUUID());
            System.out.println("[Highlight] Registered maniac: " + player.getName().getString() +
                    " (Total: " + activeManiacsWithPerk.size() + ")");
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activeManiacsWithPerk.remove(player.getUUID());
        System.out.println("[Highlight] Unregistered player: " + player.getName().getString() +
                " (Total: " + activeManiacsWithPerk.size() + ")");
    }

    /**
     * Вызывается командой /maniacrev glowing_perks из датапака при хаке компьютера.
     * Подсвечивает N случайных выживших, где N = количество маньяков с перком.
     *
     * @param server Сервер для получения списка игроков
     * @return Количество подсвеченных игроков
     */
    public static int activateGlowing(net.minecraft.server.MinecraftServer server) {
        // Получаем количество маньяков с перком
        int perkCount = getActivePerkCount(server);

        System.out.println("[Highlight] Activating glowing for " + perkCount + " survivors");

        if (perkCount == 0) {
            System.out.println("[Highlight] No active perks found");
            return 0;
        }

        // Получаем всех выживших в режиме приключения
        List<ServerPlayer> eligibleSurvivors = getEligibleSurvivors(server);

        System.out.println("[Highlight] Found " + eligibleSurvivors.size() + " eligible survivors");

        if (eligibleSurvivors.isEmpty()) {
            System.out.println("[Highlight] No eligible survivors found");
            return 0;
        }

        // Выбираем N случайных выживших (или всех, если их меньше N)
        int survivorsToHighlight = Math.min(perkCount, eligibleSurvivors.size());

        // Перемешиваем список и берем первых N
        Collections.shuffle(eligibleSurvivors);
        List<ServerPlayer> selectedSurvivors = eligibleSurvivors.subList(0, survivorsToHighlight);

        // Применяем свечение
        for (ServerPlayer survivor : selectedSurvivors) {
            survivor.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    GLOW_DURATION_TICKS,
                    0,
                    false,
                    false,
                    true
            ));

            System.out.println("[Highlight] Applied glow to: " + survivor.getName().getString());
        }

        return survivorsToHighlight;
    }

    /**
     * Получает количество маньяков с активным перком.
     * Проверяет, что игроки все еще онлайн и в команде маньяков.
     */
    private static int getActivePerkCount(net.minecraft.server.MinecraftServer server) {
        // Очищаем отключившихся игроков
        Set<UUID> toRemove = new HashSet<>();

        for (UUID uuid : activeManiacsWithPerk) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                toRemove.add(uuid);
                continue;
            }

            // Проверяем, что игрок все еще маньяк
            PerkTeam team = PerkTeam.fromPlayer(player);
            if (team != PerkTeam.MANIAC) {
                toRemove.add(uuid);
            }
        }

        // Удаляем невалидных игроков
        activeManiacsWithPerk.removeAll(toRemove);

        return activeManiacsWithPerk.size();
    }

    /**
     * Получает список всех выживших в режиме приключения
     */
    private static List<ServerPlayer> getEligibleSurvivors(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> {
                    // Проверка команды
                    PerkTeam team = PerkTeam.fromPlayer(player);
                    if (team != PerkTeam.SURVIVOR) return false;

                    // Проверка режима игры
                    GameType gameMode = player.gameMode.getGameModeForPlayer();
                    if (gameMode != GameType.ADVENTURE) return false;

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Получает текущее количество активных перков (для отладки/статистики)
     */
    public static int getActiveCount() {
        return activeManiacsWithPerk.size();
    }

    /**
     * Очистка данных при удалении перка
     */
    public static void cleanup(UUID playerUUID) {
        activeManiacsWithPerk.remove(playerUUID);
    }
}