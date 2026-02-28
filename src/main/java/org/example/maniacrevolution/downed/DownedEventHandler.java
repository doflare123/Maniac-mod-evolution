package org.example.maniacrevolution.downed;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.network.ModNetworking;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DownedEventHandler {

    /**
     * Глобальный флаг — если false, система лежания полностью отключена.
     * Команда /maniacrev downed system enable/disable
     */
    public static boolean SYSTEM_ENABLED = true;

    /**
     * UUID лежачего → UUID его невидимого ArmorStand-маунта.
     * ArmorStand фиксирует позицию и даёт нормальный хитбокс на уровне тела.
     */
    private static final Map<UUID, UUID> DOWNED_STANDS = new HashMap<>();



    // ══════════════════════════════════════════════════════════════════════
    // 0. БЛОКИРОВКА УРОНА ДЛЯ ЛЕЖАЧЕГО
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DownedData data = DownedCapability.get(player);
        if (data == null) return;
        // В состоянии DOWNED весь входящий урон блокируем.
        // killDowned() переводит state в ALIVE перед ударом — поэтому он не блокируется.
        if (data.getState() == DownedState.DOWNED) {
            event.setCanceled(true);
        }
    }

    // Страховка на случай если урон каким-то образом прошёл
    @SubscribeEvent
    public static void onDownedPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DownedData data = DownedCapability.get(player);
        if (data == null) return;
        if (data.getState() == DownedState.DOWNED) {
            event.setCanceled(true);
            player.setHealth(1.0f);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. ПЕРЕХВАТ СМЕРТИ (первый нокаут)
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SYSTEM_ENABLED) return;

        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        // Уже использовал шанс — смерть фатальна
        if (data.hasUsedSecondChance()) return;
        // Уже лежит — не вмешиваемся (страховка)
        if (data.getState() == DownedState.DOWNED) return;

        event.setCanceled(true);

        data.setState(DownedState.DOWNED);
        data.setDownedTicksElapsed(0);
        data.cancelRevive();
        data.setUsedSecondChance(true);

        player.setHealth(1.0f);
        applyDownedEffects(player);
        spawnDownedStand(player);

        broadcastMessage(player,
                "§c☠ " + player.getName().getString() + " §cупал! Помогите ему в течение §e60 сек§c!");
        Maniacrev.LOGGER.info("[Downed] {} -> DOWNED", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. ТИК — таймер, заморозка, тащение, прогресс подъёма
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        if (data.getState() == DownedState.DOWNED) {
            tickDowned(player, data);
        } else if (data.getState() == DownedState.WEAKENED) {
            tickWeakened(player, data);
        }
    }

    private static void tickDowned(ServerPlayer player, DownedData data) {
        // ── Обновляем эффекты каждые 10 тиков ────────────────────────────
        if (data.getDownedTicksElapsed() % 10 == 0) {
            applyDownedEffects(player);
        }

        // ── Полная заморозка: обнуляем velocity каждый тик ───────────────
        // Y оставляем только если падает вниз (гравитация) — чтобы не левитировал
        double vy = player.getDeltaMovement().y;
        player.setDeltaMovement(0, Math.min(vy, 0), 0);

        // ── Поза SLEEPING — горизонтальное лежание ──────────────────────
        // Просто setPose каждый тик — без setSleepingPos и setForcedPose,
        // иначе камера примагничивается и голову не повернуть.
        player.setPose(Pose.SLEEPING);

        // ── Визуальные индикаторы для окружающих игроков ─────────────────
        // Каждые 10 тиков показываем частицы у ног и текст над головой
        if (data.getDownedTicksElapsed() % 10 == 0) {
            sendHudPackets(player, data);
        }

        // ── Сброс подъёма если хелпер перестал кликать (>5 тиков без клика) ────
        if (data.getReviverUUID() != null) {
            long serverTick = player.getServer() != null ? player.getServer().getTickCount() : 0;
            long lastClick = data.getLastReviveInteractTick();
            if (lastClick >= 0 && serverTick - lastClick > 5) {
                // Хелпер отпустил ПКМ — сбрасываем прогресс
                UUID reviverUUID = data.getReviverUUID();
                data.cancelRevive();
                // Уведомляем хелпера
                ServerPlayer reviver = (ServerPlayer) player.getServer().getPlayerList().getPlayer(reviverUUID);
                if (reviver != null) {
                    reviver.displayClientMessage(
                            Component.literal("§cПодъём прерван!"), true);
                }
            }
        }

        // ── Таймер — стоит на паузе пока кто-то поднимает ──────────────
        boolean beingRevived = data.getReviverUUID() != null;
        if (!beingRevived) {
            data.incrementDownedTicks();
        }
        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();

        if (data.getDownedTicksElapsed() >= DownedData.DOWNED_TIMEOUT_TICKS) {
            killDowned(player, data);
        }
    }



    private static void tickWeakened(ServerPlayer player, DownedData data) {
        if (data.getDownedTicksElapsed() % 10 == 0) {
            applyWeakenedEffects(player);
        }
        data.incrementDownedTicks();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. ПКМ ПО ЛЕЖАЧЕМУ — только союзник может поднять
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        DownedData targetData = DownedCapability.get(target);
        if (targetData == null) return;
        if (targetData.getState() != DownedState.DOWNED) return;

        // Только союзники могут взаимодействовать с лежачим
        if (!areSameTeam(helper, target)) return;

        event.setCanceled(true);
        handleAllyRevive(helper, target, targetData);
    }

    /**
     * Возвращает нужное количество тиков для подъёма.
     * Обычный союзник: 6 сек (120 тиков).
     * Медик (команда survivors + SurvivorClass=5): 3 сек (60 тиков).
     */
    private static int getReviveTicks(ServerPlayer helper) {
        Team team = helper.getTeam();
        if (team != null && team.getName().equalsIgnoreCase("survivors")) {
            net.minecraft.world.scores.Objective obj = helper.getScoreboard()
                    .getObjective("SurvivorClass");
            if (obj != null) {
                net.minecraft.world.scores.Score score = helper.getScoreboard()
                        .getOrCreatePlayerScore(helper.getScoreboardName(), obj);
                if (score.getScore() == 5) {
                    return 3 * 20; // медик — 3 сек
                }
            }
        }
        return 6 * 20; // обычный — 6 сек
    }

    private static void handleAllyRevive(ServerPlayer helper, ServerPlayer target, DownedData targetData) {
        // Если другой союзник уже поднимает — сбрасываем его прогресс
        if (targetData.getReviverUUID() != null
                && !targetData.getReviverUUID().equals(helper.getUUID())) {
            targetData.cancelRevive();
        }

        if (targetData.getReviverUUID() == null) {
            targetData.setReviverUUID(helper.getUUID());
            targetData.setReviveProgressTicks(0);
            targetData.setRequiredReviveTicks(getReviveTicks(helper));
        }

        targetData.incrementReviveProgress();
        // Обновляем тик последнего клика — пока хелпер кликает, таймер живёт
        targetData.setLastReviveInteractTick(
                target.getServer() != null ? target.getServer().getTickCount() : 0);

        // Прерываем если хелпер отошёл
        if (helper.distanceTo(target) > 3.0) {
            targetData.cancelRevive();
            return;
        }

        if (targetData.getReviveProgressTicks() >= targetData.getRequiredReviveTicks()) {
            revivePlayer(target, targetData, helper);
        }
    }



    // ══════════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════
    // 4. РЕСПАУН — копируем Capability
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.reviveCaps();
        try {
            DownedData oldData = DownedCapability.get(original);
            DownedData newData = DownedCapability.get(newPlayer);
            if (oldData != null && newData != null) {
                newData.deserializeNBT(oldData.serializeNBT());
            }
        } finally {
            original.invalidateCaps();
        }

        // Убираем ArmorStand если он остался
        removeStandForPlayer(original.getUUID());
    }

    // ══════════════════════════════════════════════════════════════════════
    // ПУБЛИЧНЫЕ МЕТОДЫ (для DownedCommand)
    // ══════════════════════════════════════════════════════════════════════

    public static void instantRevive(ServerPlayer target) {
        DownedData data = DownedCapability.get(target);
        if (data == null || data.getState() != DownedState.DOWNED) return;

        // Командный подъём — полное восстановление без штрафа к HP
        data.cancelRevive();
        data.setState(DownedState.WEAKENED);
        data.setDownedTicksElapsed(0);

        removeDownedEffects(target);
        removeStandForPlayer(target.getUUID());
        clearHudForNearby(target);

        // Восстанавливаем полное здоровье (без урезания)
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            target.setHealth((float) maxHp.getValue());
        }

        target.displayClientMessage(
                Component.literal("§aВы были подняты командой! §eПолное здоровье восстановлено."), false);
        broadcastMessage(target, "§a✚ " + target.getName().getString() + " §aбыл поднят командой!");
        Maniacrev.LOGGER.info("[Downed] {} поднят командой (полное HP)", target.getName().getString());
    }

    public static void resetPlayer(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        if (data.getState() == DownedState.DOWNED) {
            removeDownedEffects(player);
            removeStandForPlayer(player.getUUID());
        } else if (data.getState() == DownedState.WEAKENED) {
            restoreMaxHp(player);
            removeWeakenedEffects(player);
        }

        data.fullReset();

        player.displayClientMessage(
                Component.literal("§aВаш статус «лежания» был сброшен. Вы снова можете упасть."),
                false
        );
        Maniacrev.LOGGER.info("[Downed] {} сброшен -> ALIVE", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ══════════════════════════════════════════════════════════════════════

    private static void revivePlayer(ServerPlayer target, DownedData data, ServerPlayer helper) {
        data.cancelRevive();
        data.setState(DownedState.WEAKENED);
        data.setDownedTicksElapsed(0);

        removeDownedEffects(target);
        removeStandForPlayer(target.getUUID());
        clearHudForNearby(target);

        // Сохраняем оригинальный макс HP и урезаем вдвое
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            data.setOriginalMaxHp(maxHp.getBaseValue()); // запоминаем для восстановления
            double newMax = Math.max(2.0, maxHp.getBaseValue() / 2.0);
            maxHp.setBaseValue(newMax);
            // Ставим полное HP от нового урезанного максимума
            target.setHealth((float) newMax);
        }

        applyWeakenedEffects(target);

        if (helper != null) {
            helper.displayClientMessage(
                    Component.literal("§aВы подняли §e" + target.getName().getString() + "§a!"), false);
        }
        target.displayClientMessage(
                Component.literal("§eВас подняли! §cСледующий нокаут §4фатален§c. HP урезаны вдвое!"), false);

        broadcastMessage(target, "§a✚ " + target.getName().getString() +
                " §aбыл поднят" +
                (helper != null ? " игроком §e" + helper.getName().getString() : "") + "§a!");

        Maniacrev.LOGGER.info("[Downed] {} -> WEAKENED, макс HP: {}",
                target.getName().getString(),
                target.getAttribute(Attributes.MAX_HEALTH).getValue());
    }

    public static void killDowned(ServerPlayer player, DownedData data) {
        data.setState(DownedState.ALIVE);
        data.setDownedTicksElapsed(0);
        data.cancelRevive();
        removeDownedEffects(player);
        removeStandForPlayer(player.getUUID());
        clearHudForNearby(player);

        player.hurt(player.level().damageSources().magic(), Float.MAX_VALUE);

        broadcastMessage(player, "§4✝ " + player.getName().getString() + " §4не был поднят и погиб.");
        Maniacrev.LOGGER.info("[Downed] {} умер (таймаут)", player.getName().getString());
    }

    // ── ArmorStand — невидимый маркер для нормального хитбокса ───────────

    /**
     * Спавним невидимый ArmorStand без гравитации под лежачим игроком.
     * Он не мешает взаимодействию, но делает хитбокс на уровне тела (0.5 блока).
     * Игроки взаимодействуют с ним через EntityInteract → мы перехватываем
     * и переадресуем на игрока.
     *
     * На самом деле хитбокс для взаимодействия с лежачим игроком при CROUCHING
     * составляет ~1.5 блока — этого достаточно. ArmorStand здесь не нужен,
     * убираем его из spawn и оставляем только CROUCHING позу.
     */
    private static void spawnDownedStand(ServerPlayer player) {
        // Удаляем старый стенд если был
        removeStandForPlayer(player.getUUID());
        // Для 1.20.1 CROUCHING даёт хитбокс 1.5 блока — достаточно для ПКМ
        // ArmorStand оставляем как запасной вариант если потребуется
    }

    private static void updateStandPosition(ServerPlayer player) {
        // Резерв для будущего использования
    }

    private static void removeStandForPlayer(UUID playerUUID) {
        DOWNED_STANDS.remove(playerUUID);
    }

    // ── Визуальные индикаторы ────────────────────────────────────────────────

    private static void sendHudPackets(ServerPlayer downed, DownedData data) {
        if (downed.getServer() == null) return;

        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();
        boolean beingRevived = data.getReviverUUID() != null;
        float reviveProgress = beingRevived ? data.getReviveProgress() : 0f;
        String downedName = downed.getName().getString();

        // Частицы у ног — видны всем рядом
        net.minecraft.server.level.ServerLevel level =
                (net.minecraft.server.level.ServerLevel) downed.level();
        double x = downed.getX(), y = downed.getY(), z = downed.getZ();
        level.sendParticles(ParticleTypes.HEART, x, y + 0.5, z, 1, 0.3, 0.1, 0.3, 0);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 0.15, z, 2, 0.2, 0.05, 0.2, 0);

        // Пакет самому лежачему
        ModNetworking.sendToPlayer(
                new DownedHudPacket(DownedHudPacket.ROLE_SELF, downedName,
                        remaining, reviveProgress, beingRevived),
                downed);

        // Пакеты игрокам рядом (радиус 8 блоков)
        for (ServerPlayer nearby : downed.getServer().getPlayerList().getPlayers()) {
            if (nearby == downed) continue;
            if (nearby.distanceTo(downed) > 8.0) {
                // Если был в зоне — отправляем clear
                ModNetworking.sendToPlayer(DownedHudPacket.clear(), nearby);
                continue;
            }
            DownedData nearbyData = DownedCapability.get(nearby);
            if (nearbyData != null && nearbyData.getState() == DownedState.DOWNED) continue;

            int role = areSameTeam(nearby, downed)
                    ? DownedHudPacket.ROLE_ALLY
                    : DownedHudPacket.ROLE_ENEMY;

            // Для союзника показываем его прогресс подъёма если он поднимает
            float allyProgress = (role == DownedHudPacket.ROLE_ALLY && beingRevived
                    && nearby.getUUID().equals(data.getReviverUUID()))
                    ? reviveProgress : 0f;

            ModNetworking.sendToPlayer(
                    new DownedHudPacket(role, downedName, remaining, allyProgress, beingRevived),
                    nearby);
        }
    }

    /** Отправляет clear-пакет всем рядом когда лежачий поднялся/умер */
    public static void clearHudForNearby(ServerPlayer downed) {
        if (downed.getServer() == null) return;
        ModNetworking.sendToPlayer(DownedHudPacket.clear(), downed);
        for (ServerPlayer nearby : downed.getServer().getPlayerList().getPlayers()) {
            if (nearby == downed) continue;
            ModNetworking.sendToPlayer(DownedHudPacket.clear(), nearby);
        }
    }

    // ── Эффекты ───────────────────────────────────────────────────────────

    private static void applyDownedEffects(ServerPlayer player) {
        // SLOWNESS 255 блокирует ходьбу
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
        // JUMP_BOOST 128 (signed -128) — невозможность прыгнуть
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 255, false, false));
    }

    private static void removeDownedEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        // Сбрасываем позу лежания
        player.setPose(Pose.STANDING);
    }

    private static void applyWeakenedEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
    }

    private static void removeWeakenedEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
    }

    private static void restoreMaxHp(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp == null) return;
        // Всегда восстанавливаем базовые 20 HP (ванильный дефолт)
        maxHp.setBaseValue(20.0);
        if (data != null) data.setOriginalMaxHp(-1);
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private static boolean areSameTeam(Player a, Player b) {
        Team teamA = a.getTeam();
        Team teamB = b.getTeam();
        if (teamA == null || teamB == null) return false;
        return teamA.getName().equals(teamB.getName());
    }

    private static void broadcastMessage(ServerPlayer player, String text) {
        if (player.getServer() == null) return;
        player.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(text), false);
    }
}
