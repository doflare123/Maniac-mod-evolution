package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class IAmSpeedPerk extends Perk {
    private static final int SPEED_DURATION_TICKS = 200; // 10 секунд
    private static final int SPEED_AMPLIFIER = 0; // Уровень 1 (0 = Speed I)

    public IAmSpeedPerk() {
        super(new Builder("i_am_speed")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.ANY)
        );
    }

    /**
     * Применяет эффект скорости маньяку после убийства
     */
    public void applySpeedOnKill(ServerPlayer killer) {
        MobEffectInstance speedEffect = new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                SPEED_DURATION_TICKS,
                SPEED_AMPLIFIER,
                false, // ambient
                true,  // visible
                true   // showIcon
        );
        killer.addEffect(speedEffect);
    }

    /**
     * Обработчик события убийства игрока.
     * Должен быть зарегистрирован в EventBusSubscriber.
     */
    @Mod.EventBusSubscriber(modid = "maniacrev")
    public static class EventHandler {

        @SubscribeEvent
        public static void onPlayerKill(LivingDeathEvent event) {
            // Проверяем, что убитый - игрок
            if (!(event.getEntity() instanceof ServerPlayer victim)) {
                return;
            }

            // Проверяем, что убийца - игрок
            if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
                return;
            }

            // Не засчитываем самоубийство
            if (killer.equals(victim)) {
                return;
            }

            // Проверяем команду убийцы
            PerkTeam killerTeam = PerkTeam.fromPlayer(killer);
            if (killerTeam != PerkTeam.MANIAC) {
                return;
            }

            // Получаем данные игрока
            var playerData = PlayerDataManager.get(killer);
            if (playerData == null) return;

            // Проверяем активные перки
            for (var perkInstance : playerData.getSelectedPerks()) {
                Perk perk = perkInstance.getPerk();

                // Если у маньяка есть этот перк - активируем эффект
                if (perk instanceof IAmSpeedPerk speedPerk) {
                    speedPerk.applySpeedOnKill(killer);
                    break; // Один раз за убийство достаточно
                }
            }
        }
    }
}