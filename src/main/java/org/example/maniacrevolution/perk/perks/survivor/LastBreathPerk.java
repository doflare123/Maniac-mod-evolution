package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Пассивный перк: при смерти накладывает эффект Glowing на убийцу на 7 секунд.
 */
@Mod.EventBusSubscriber(modid = "maniacrev", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LastBreathPerk extends Perk {

    private static final int GLOW_DURATION = 7 * 20; // 7 секунд в тиках

    public LastBreathPerk() {
        super(new Builder("last_breath")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY));
    }

    /**
     * Обработчик смерти игрока.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Проверяем что умер игрок
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Проверяем что у жертвы есть этот перк
        if (!hasLastBreathPerk(victim)) return;

        // Получаем убийцу
        ServerPlayer killer = getKiller(event.getSource());
        if (killer == null) return;

        // Проверяем что убийца - маньяк
        if (!isManiac(killer)) return;

        // СРАБАТЫВАНИЕ ПЕРКА - накладываем Glowing на убийцу
        MobEffectInstance glowingEffect = new MobEffectInstance(
                MobEffects.GLOWING,
                GLOW_DURATION,
                0,
                false,
                false,
                true
        );

        killer.addEffect(glowingEffect);

        // Получаем всех союзников жертвы
        List<ServerPlayer> teammates = getTeammates(victim);

        // Оповещаем команду
        for (ServerPlayer teammate : teammates) {
            teammate.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c💀 " + victim.getName().getString() + " был убит! Маньяк подсвечен!"
                    ),
                    false
            );

            // Звук
            teammate.playNotifySound(
                    SoundEvents.ENDER_EYE_DEATH,
                    SoundSource.PLAYERS,
                    1.0f,
                    0.8f
            );
        }

        // Сообщение самой жертве
        victim.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§a✓ Ваш убийца подсвечен для всех!"
                ),
                false
        );

        System.out.println("LastBreath activated! Victim: " + victim.getName().getString() +
                ", Killer: " + killer.getName().getString() + " (Glowing for 7 sec)");
    }

    /**
     * Проверяет, есть ли у игрока перк Last Breath.
     */
    private static boolean hasLastBreathPerk(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        return data.getSelectedPerks().stream()
                .anyMatch(inst -> inst.getPerk() instanceof LastBreathPerk);
    }

    /**
     * Получает убийцу из источника урона.
     */
    private static ServerPlayer getKiller(DamageSource source) {
        // Прямая атака игроком
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }

        // Атака через снаряд (стрела, снежок и т.д.)
        if (source.getDirectEntity() != null &&
                source.getEntity() instanceof ServerPlayer player) {
            return player;
        }

        return null;
    }

    /**
     * Проверяет, является ли игрок маньяком.
     */
    private static boolean isManiac(ServerPlayer player) {
        if (player.getTeam() != null) {
            String teamName = player.getTeam().getName();
            return teamName.equalsIgnoreCase("maniac") ||
                    teamName.equalsIgnoreCase("маньяк");
        }
        return false;
    }

    /**
     * Получает всех союзников игрока (включая самого игрока).
     */
    private static List<ServerPlayer> getTeammates(ServerPlayer player) {
        List<ServerPlayer> teammates = new ArrayList<>();

        if (player.getTeam() == null) {
            teammates.add(player);
            return teammates;
        }

        // Получаем всех игроков из той же команды
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.getTeam() != null &&
                    other.getTeam().getName().equals(player.getTeam().getName())) {
                teammates.add(other);
            }
        }

        return teammates;
    }
}