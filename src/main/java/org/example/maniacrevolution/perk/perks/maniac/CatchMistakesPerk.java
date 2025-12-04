package org.example.maniacrevolution.perk.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.maniacrevolution.perk.*;

import java.util.*;

/**
 * Ловля на ошибках (Пассивный с кд) (Все)
 * Подсвечивает выжившего если тот промахивается при хаке
 */
public class CatchMistakesPerk extends Perk {

    private static final int GLOW_DURATION_TICKS = 100; // 5 секунд свечения
    private static final int COOLDOWN_SECONDS = 40;

    // Храним игроков, у которых перк активен (НЕ на кулдауне)
    private static final Map<UUID, CatchMistakesPerk> readyPlayers = new HashMap<>();

    // Храним всех игроков с этим перком (включая тех, кто на кулдауне)
    private static final Set<UUID> allPerkHolders = new HashSet<>();

    public CatchMistakesPerk() {
        super(new Builder("catch_mistakes")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Перк готов к использованию
        readyPlayers.put(player.getUUID(), this);
        allPerkHolders.add(player.getUUID());
        System.out.println("[CatchMistakes] Applied to player " + player.getName().getString() +
                " (UUID: " + player.getUUID() + ")");
        System.out.println("[CatchMistakes] Ready players: " + readyPlayers.size() +
                ", All holders: " + allPerkHolders.size());
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Перк на кулдауне - убираем из готовых
        readyPlayers.remove(player.getUUID());
        System.out.println("[CatchMistakes] Removed from player " + player.getName().getString() +
                " (on cooldown)");
        System.out.println("[CatchMistakes] Ready players: " + readyPlayers.size());
        // Но оставляем в общем списке
    }

    /**
     * Вызывается при промахе в QTE.
     * Находит ПЕРВОГО игрока с готовым перком и активирует его.
     *
     * @param failedPlayer Игрок, который промахнулся
     * @return true если перк сработал, false если никто не готов
     */
    public static boolean onQTEFailed(ServerPlayer failedPlayer) {
        System.out.println("[CatchMistakes] onQTEFailed called for player: " + failedPlayer.getName().getString());

        // Проверяем, что промахнувшийся игрок - выживший
        PerkTeam failedTeam = PerkTeam.fromPlayer(failedPlayer);
        System.out.println("[CatchMistakes] Failed player team: " + (failedTeam != null ? failedTeam.name() : "NULL"));

        if (failedTeam != PerkTeam.SURVIVOR) {
            System.out.println("[CatchMistakes] Failed player is not a SURVIVOR, aborting");
            return false;
        }

        System.out.println("[CatchMistakes] Ready players count: " + readyPlayers.size());
        System.out.println("[CatchMistakes] All perk holders count: " + allPerkHolders.size());

        // Ищем ПЕРВОГО игрока с готовым перком
        // Используем sorted для стабильной сортировки (по UUID)
        Optional<Map.Entry<UUID, CatchMistakesPerk>> readyPerk = readyPlayers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .findFirst();

        if (readyPerk.isEmpty()) {
            System.out.println("[CatchMistakes] No ready players found!");
            return false; // Никто не готов
        }

        UUID perkOwnerUUID = readyPerk.get().getKey();
        System.out.println("[CatchMistakes] Found ready perk owner: " + perkOwnerUUID);

        ServerPlayer perkOwner = failedPlayer.server.getPlayerList().getPlayer(perkOwnerUUID);

        if (perkOwner == null) {
            System.out.println("[CatchMistakes] Perk owner is offline, cleaning up");
            // Игрок отключился, очищаем
            readyPlayers.remove(perkOwnerUUID);
            allPerkHolders.remove(perkOwnerUUID);
            return false;
        }

        System.out.println("[CatchMistakes] Applying glow to failed player and marking perk for cooldown");

        // Применяем эффект свечения к промахнувшемуся игроку
        failedPlayer.addEffect(new MobEffectInstance(
                MobEffects.GLOWING,
                GLOW_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        // ВАЖНО: Помечаем для запуска кулдауна
        markForTrigger(perkOwnerUUID);

        System.out.println("[CatchMistakes] Perk activated successfully!");
        return true;
    }

    // Храним игроков, которых нужно триггернуть
    private static final Map<UUID, Boolean> pendingTriggers = new HashMap<>();

    private static void markForTrigger(UUID playerUUID) {
        pendingTriggers.put(playerUUID, true);
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        // Проверяем, есть ли отложенный триггер
        boolean shouldTrigger = pendingTriggers.getOrDefault(player.getUUID(), false);
        if (shouldTrigger) {
            pendingTriggers.remove(player.getUUID());
        }
        return shouldTrigger;
    }

    @Override
    public void onTrigger(ServerPlayer player) {
        // Этот метод вызывается автоматически из PerkInstance
        // после shouldTrigger возвращает true
        // Кулдаун запускается автоматически

        // Опционально: можно добавить звуковой эффект или сообщение
        // player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /**
     * Проверяет, есть ли хотя бы один игрок с этим перком (включая на кулдауне)
     */
    public static boolean hasAnyHolder() {
        return !allPerkHolders.isEmpty();
    }

    /**
     * Проверяет, есть ли хотя бы один игрок с готовым перком
     */
    public static boolean hasReadyHolder() {
        return !readyPlayers.isEmpty();
    }

    /**
     * Очистка данных при удалении перка
     */
    public static void cleanup(UUID playerUUID) {
        readyPlayers.remove(playerUUID);
        allPerkHolders.remove(playerUUID);
        pendingTriggers.remove(playerUUID);
    }
}