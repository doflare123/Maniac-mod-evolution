package org.example.maniacrevolution.system;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.effect.ModEffects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система управления целями Агента 47
 * Автоматически переназначает цель при смерти текущей цели
 */
@Mod.EventBusSubscriber
public class Agent47TargetManager {

    // Хранит UUID агента -> UUID его текущей цели
    private static final Map<UUID, UUID> agentTargets = new ConcurrentHashMap<>();

    // Длительность эффекта (практически бесконечная)
    private static final int EFFECT_DURATION = 999999;
    private static final String SURVIVORS_TEAM = "survivors";

    /**
     * Устанавливает цель для агента
     */
    public static void setTarget(ServerPlayer agent, ServerPlayer target) {
        if (agent == null || target == null) return;

        // Убираем эффект со старой цели, если она была
        UUID oldTargetUUID = agentTargets.get(agent.getUUID());
        if (oldTargetUUID != null) {
            ServerPlayer oldTarget = agent.getServer().getPlayerList().getPlayer(oldTargetUUID);
            if (oldTarget != null) {
                oldTarget.removeEffect(ModEffects.TARGET_EFFECT.get());
            }
        }

        // Устанавливаем новую цель
        agentTargets.put(agent.getUUID(), target.getUUID());

        // Применяем эффект
        target.addEffect(new MobEffectInstance(
                ModEffects.TARGET_EFFECT.get(),
                EFFECT_DURATION,
                0,
                false,
                true,
                true
        ));

        // Сообщения
        agent.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cНовая цель: §e" + target.getName().getString()),
                false
        );

        target.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c§lВЫ СТАЛИ ЦЕЛЬЮ АГЕНТА!"),
                false
        );
    }

    /**
     * Получает текущую цель агента
     */
    public static ServerPlayer getCurrentTarget(ServerPlayer agent) {
        if (agent == null) return null;

        UUID targetUUID = agentTargets.get(agent.getUUID());
        if (targetUUID == null) return null;

        return agent.getServer().getPlayerList().getPlayer(targetUUID);
    }

    /**
     * Проверяет, является ли игрок целью какого-либо агента
     */
    public static boolean isTarget(ServerPlayer player) {
        return player.hasEffect(ModEffects.TARGET_EFFECT.get());
    }

    /**
     * Получает случайного выжившего для назначения целью
     */
    public static ServerPlayer getRandomSurvivor(ServerLevel level, ServerPlayer exclude) {
        List<ServerPlayer> survivors = new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (player == exclude) continue;
            if (player.isSpectator() || player.isCreative()) continue;

            Team team = player.getTeam();
            if (team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName())) {
                survivors.add(player);
            }
        }

        if (survivors.isEmpty()) return null;

        Random random = new Random();
        return survivors.get(random.nextInt(survivors.size()));
    }

    /**
     * Обработка смерти игрока
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
        if (deadPlayer.level().isClientSide()) return;

        // Проверяем, была ли у игрока цель
        if (!deadPlayer.hasEffect(ModEffects.TARGET_EFFECT.get())) return;

        // Игрок умер с эффектом "Цель" - нужно переназначить цель
        ServerLevel level = (ServerLevel) deadPlayer.level();

        // Находим агента, для которого этот игрок был целью
        UUID agentUUID = null;
        for (Map.Entry<UUID, UUID> entry : agentTargets.entrySet()) {
            if (entry.getValue().equals(deadPlayer.getUUID())) {
                agentUUID = entry.getKey();
                break;
            }
        }

        if (agentUUID == null) return;

        ServerPlayer agent = level.getServer().getPlayerList().getPlayer(agentUUID);
        if (agent == null) return;

        // Проверяем, убил ли агент цель сам
        boolean killedByAgent = event.getSource().getEntity() == agent;

        if (killedByAgent) {
            // Начисляем деньги агенту
            Agent47MoneyManager.addMoney(agent, Agent47ShopConfig.KILL_TARGET_REWARD);

            agent.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("§a§l+%d монет за убийство цели!", Agent47ShopConfig.KILL_TARGET_REWARD)
                    ),
                    false
            );
        }

        // Убираем эффект с мертвого игрока (на всякий случай)
        deadPlayer.removeEffect(ModEffects.TARGET_EFFECT.get());

        // Назначаем новую цель
        ServerPlayer newTarget = getRandomSurvivor(level, deadPlayer);
        if (newTarget != null) {
            setTarget(agent, newTarget);

            agent.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§eЦель ликвидирована. Новая цель назначена."),
                    false
            );
        } else {
            // Нет доступных целей
            agentTargets.remove(agent.getUUID());
            agent.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cНет доступных целей."),
                    false
            );
        }
    }

    /**
     * Очистка при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Если вышел агент - убираем его цель
        UUID targetUUID = agentTargets.remove(player.getUUID());
        if (targetUUID != null) {
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                target.removeEffect(ModEffects.TARGET_EFFECT.get());
            }
        }

        // Если вышла цель - переназначаем
        for (Map.Entry<UUID, UUID> entry : agentTargets.entrySet()) {
            if (entry.getValue().equals(player.getUUID())) {
                ServerPlayer agent = player.getServer().getPlayerList().getPlayer(entry.getKey());
                if (agent != null && agent.level() instanceof ServerLevel level) {
                    ServerPlayer newTarget = getRandomSurvivor(level, null);
                    if (newTarget != null) {
                        setTarget(agent, newTarget);
                    } else {
                        agentTargets.remove(entry.getKey());
                    }
                }
                break;
            }
        }
    }

    /**
     * Убирает цель у агента
     */
    public static void clearTarget(ServerPlayer agent) {
        if (agent == null) return;

        UUID targetUUID = agentTargets.remove(agent.getUUID());
        if (targetUUID != null) {
            ServerPlayer target = agent.getServer().getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                target.removeEffect(ModEffects.TARGET_EFFECT.get());
            }
        }
    }

    /**
     * Получает всех агентов и их цели
     */
    public static Map<UUID, UUID> getAllTargets() {
        return new HashMap<>(agentTargets);
    }
}