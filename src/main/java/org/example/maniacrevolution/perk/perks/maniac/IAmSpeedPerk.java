package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.perk.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Я скорость (Пассивный) (Охота/Мидгейм)
 * При убийстве игрока даёт скорость 1 на 30 секунд
 */
@Mod.EventBusSubscriber
public class IAmSpeedPerk extends Perk {

    private static final int SPEED_DURATION_TICKS = 600; // 30 секунд
    private static final int SPEED_AMPLIFIER = 0; // Уровень 1 (0 = Speed I)

    // Храним игроков, у которых активен перк
    private static final Map<UUID, Boolean> activePlayers = new HashMap<>();

    public IAmSpeedPerk() {
        super(new Builder("i_am_speed")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
        );
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        // Регистрируем игрока как активного
        activePlayers.put(player.getUUID(), true);
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Убираем игрока из активных
        activePlayers.remove(player.getUUID());
    }

    /**
     * Проверяет, активен ли перк у игрока
     */
    public static boolean isActive(UUID playerUUID) {
        return activePlayers.getOrDefault(playerUUID, false);
    }

    /**
     * Обработчик события смерти
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Проверяем, что жертва - игрок
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Проверяем, что убийца - игрок
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        // Проверяем, что убийца и жертва - разные игроки
        if (killer.getUUID().equals(victim.getUUID())) return;

        // Проверяем, что убийца - маньяк
        PerkTeam killerTeam = PerkTeam.fromPlayer(killer);
        if (killerTeam != PerkTeam.MANIAC) return;

        // Проверяем, что у убийцы активен перк "Я скорость"
        if (!isActive(killer.getUUID())) return;

        // Применяем эффект скорости
        killer.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                SPEED_DURATION_TICKS,
                SPEED_AMPLIFIER,
                false, // ambient
                true,  // visible
                true   // showIcon
        ));

        // Опционально: отправить сообщение игроку
        // killer.displayClientMessage(
        //     Component.literal("Я - скорость!").withStyle(ChatFormatting.YELLOW),
        //     true // actionBar
        // );
    }

    /**
     * Очистка данных при удалении перка
     */
    public static void cleanup(UUID playerUUID) {
        activePlayers.remove(playerUUID);
    }
}