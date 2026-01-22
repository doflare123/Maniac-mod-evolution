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
                false,
                false
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

        System.out.println("[Agent47] Target set: " + agent.getName().getString() + " -> " + target.getName().getString());
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

        // Получаем команду по имени
        Team survivorsTeam = level.getScoreboard().getPlayerTeam(SURVIVORS_TEAM);

        if (survivorsTeam == null) {
            System.err.println("[Agent47] Team '" + SURVIVORS_TEAM + "' not found!");
            return null;
        }

        for (ServerPlayer player : level.players()) {
            if (player == exclude) continue;
            if (player.isSpectator() || player.isCreative()) continue;

            // Проверяем, состоит ли игрок в нужной команде
            if (survivorsTeam.equals(player.getTeam())) {
                survivors.add(player);
            }
        }

        if (survivors.isEmpty()) {
            System.out.println("[Agent47] No survivors found in team: " + SURVIVORS_TEAM);
            return null;
        }

        Random random = new Random();
        ServerPlayer selected = survivors.get(random.nextInt(survivors.size()));
        System.out.println("[Agent47] Selected survivor: " + selected.getName().getString());
        return selected;
    }

    /**
     * Обработка смерти игрока
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
        if (deadPlayer.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) deadPlayer.level();

        // Находим агента, для которого этот игрок был целью
        UUID agentUUID = null;
        for (Map.Entry<UUID, UUID> entry : agentTargets.entrySet()) {
            if (entry.getValue().equals(deadPlayer.getUUID())) {
                agentUUID = entry.getKey();
                break;
            }
        }

        if (agentUUID == null) {
            System.out.println("[Agent47] Dead player was not a target: " + deadPlayer.getName().getString());
            return;
        }

        ServerPlayer agent = level.getServer().getPlayerList().getPlayer(agentUUID);
        if (agent == null) {
            System.out.println("[Agent47] Agent not found for UUID: " + agentUUID);
            agentTargets.remove(agentUUID);
            return;
        }

        // ИСПРАВЛЕНО: Более точная проверка убийцы
        boolean killedByAgent = false;

        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            killedByAgent = killer.getUUID().equals(agentUUID);
            System.out.println("[Agent47] Killer: " + killer.getName().getString() +
                    ", is agent: " + killedByAgent);
        } else {
            System.out.println("[Agent47] Death source: " + event.getSource().getMsgId());
        }

        System.out.println("[Agent47] Target died: " + deadPlayer.getName().getString() +
                ", killed by agent: " + killedByAgent);

        if (killedByAgent) {
            // Начисляем деньги агенту
            int currentMoney = Agent47MoneyManager.getMoney(agent);
            Agent47MoneyManager.addMoney(agent, Agent47ShopConfig.KILL_TARGET_REWARD);
            int newMoney = Agent47MoneyManager.getMoney(agent);

            System.out.println("[Agent47] Money updated: " + currentMoney + " -> " + newMoney);

            agent.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("§a§l+%d монет за убийство цели! Баланс: %d",
                                    Agent47ShopConfig.KILL_TARGET_REWARD, newMoney)
                    ),
                    false
            );

            // ВАЖНО: Принудительно синхронизируем деньги с клиентом
            Agent47MoneyManager.saveMoney(agent);
        }

        // Убираем эффект с мертвого игрока
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
            System.out.println("[Agent47] No targets available for agent: " + agent.getName().getString());
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
            System.out.println("[Agent47] Agent logged out: " + player.getName().getString());
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
                System.out.println("[Agent47] Target logged out: " + player.getName().getString());
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