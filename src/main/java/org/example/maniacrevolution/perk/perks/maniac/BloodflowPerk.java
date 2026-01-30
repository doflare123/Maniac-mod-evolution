package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.perk.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Кровоток (Пассивный с кд) (Все)
 * Следующий удар заставляет жертву оставлять за собой кровавый след
 */
@Mod.EventBusSubscriber
public class BloodflowPerk extends Perk {

    private static final int EFFECT_DURATION_TICKS = 200; // 10 секунд эффекта
    private static final int COOLDOWN_SECONDS = 25;

    // Храним игроков, у которых перк готов к использованию
    private static final Map<UUID, BloodflowPerk> activePerkInstances = new HashMap<>();

    public BloodflowPerk() {
        super(new Builder("bloodflow")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Перк готов к использованию - регистрируем игрока
        activePerkInstances.put(player.getUUID(), this);
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Перк на кулдауне - убираем из активных
        activePerkInstances.remove(player.getUUID());
    }

//    @Override
//    public boolean shouldTrigger(ServerPlayer player) {
//        // Триггер обрабатывается в событии атаки
//        // Здесь всегда false, т.к. кулдаун запускается через событие
//        return false;
//    }

    /**
     * Проверяет, активен ли перк у игрока
     */
    public static boolean isActive(UUID playerUUID) {
        return activePerkInstances.containsKey(playerUUID);
    }

    /**
     * Получает экземпляр перка игрока для запуска триггера
     */
    public static BloodflowPerk getInstance(UUID playerUUID) {
        return activePerkInstances.get(playerUUID);
    }

    /**
     * Обработчик события атаки
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        // Проверяем, что атакующий - игрок
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        // Проверяем, что жертва - игрок
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Проверяем, что у атакующего активен перк Кровоток
        if (!isActive(attacker.getUUID())) return;

        // Получаем экземпляр перка
        BloodflowPerk perkInstance = getInstance(attacker.getUUID());
        if (perkInstance == null) return;

        // Применяем эффект "Открытая рана" к жертве
        victim.addEffect(new MobEffectInstance(
                ModEffects.OPEN_WOUND.get(),
                EFFECT_DURATION_TICKS,
                0, // amplifier
                false, // ambient
                false, // visible
                false // showIcon
        ));

        // ВАЖНО: Теперь нужно запустить кулдаун через PerkInstance
        // Это делается через вызов shouldTrigger -> onTrigger в PerkInstance
        // Но поскольку событие вне системы тиков, нужно пометить для триггера

        // Помечаем игрока для запуска триггера в следующем тике
        markForTrigger(attacker.getUUID());
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
        // Здесь можно добавить визуальные/звуковые эффекты

        // Опционально: отправить сообщение игроку
        // player.displayClientMessage(
        //     Component.literal("Кровоток активирован!").withStyle(ChatFormatting.RED),
        //     true
        // );
    }
}