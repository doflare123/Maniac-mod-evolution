package org.example.maniacrevolution.downed;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * UUID игрока который тащит → UUID лежачего которого он тащит.
     * Обновляется каждый тик в tickServerDrag.
     */
    private static final Map<UUID, UUID> DRAGGING = new HashMap<>();

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

        // ── Тащение: проверяем каждый тик кто держит ПКМ рядом ──────────
        // (EntityInteract срабатывает только на одиночный клик,
        //  поэтому логику тащения переносим в серверный тик)
        tickDrag(player, data);

        // ── Визуальные индикаторы для окружающих игроков ─────────────────
        // Каждые 10 тиков показываем частицы у ног и текст над головой
        if (data.getDownedTicksElapsed() % 10 == 0) {
            showDownedIndicators(player, data);
        }

        // ── Таймер и обратный отсчёт ─────────────────────────────────────
        data.incrementDownedTicks();
        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();

        if (remaining == 30 * 20 || remaining == 15 * 20 || remaining == 10 * 20
                || remaining == 5 * 20 || remaining == 3 * 20 || remaining == 2 * 20
                || remaining == 20) {
            player.displayClientMessage(
                    Component.literal("§c☠ Вы умрёте через §e" + (remaining / 20) + " сек§c!"),
                    true
            );
        }

        if (data.getDownedTicksElapsed() >= DownedData.DOWNED_TIMEOUT_TICKS) {
            killDowned(player, data);
        }
    }

    /**
     * Логика тащения — вызывается каждый тик для лежачего игрока.
     * Ищем игрока из другой команды который смотрит на лежачего и
     * нажимает ПКМ (определяем через UseItemInputPacket — недоступен напрямую,
     * поэтому используем proximity + DRAGGING Map которую заполняет EntityInteract).
     */
    private static void tickDrag(ServerPlayer downed, DownedData data) {
        UUID draggerUUID = DRAGGING.get(downed.getUUID());
        if (draggerUUID == null) return;

        ServerPlayer dragger = (ServerPlayer) downed.getServer().getPlayerList().getPlayer(draggerUUID);
        if (dragger == null || !dragger.isAlive()) {
            DRAGGING.remove(downed.getUUID());
            return;
        }

        // Если враг слишком далеко — прекращаем тащение
        if (dragger.distanceTo(downed) > 5.0) {
            DRAGGING.remove(downed.getUUID());
            return;
        }

        // Тащим лежачего к врагу (0.08 блока за тик = ~1.6 б/сек)
        double dx = dragger.getX() - downed.getX();
        double dz = dragger.getZ() - downed.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 1.2) {
            double step = 0.08;
            downed.teleportTo(
                    downed.getX() + (dx / dist) * step,
                    downed.getY(),
                    downed.getZ() + (dz / dist) * step
            );
        }

        // Синхронизируем позицию ArmorStand-маунта
        updateStandPosition(downed);
    }

    private static void tickWeakened(ServerPlayer player, DownedData data) {
        if (data.getDownedTicksElapsed() % 10 == 0) {
            applyWeakenedEffects(player);
        }
        data.incrementDownedTicks();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. ПКМ ПО ЛЕЖАЧЕМУ — одиночный клик
    //    Союзник: начинает / продолжает прогресс подъёма
    //    Враг: регистрирует в DRAGGING + замедление
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        DownedData targetData = DownedCapability.get(target);
        if (targetData == null) return;
        if (targetData.getState() != DownedState.DOWNED) return;

        event.setCanceled(true);

        if (areSameTeam(helper, target)) {
            handleAllyRevive(helper, target, targetData);
        } else {
            handleEnemyStartDrag(helper, target);
        }
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
            helper.displayClientMessage(
                    Component.literal("§aПоднимаете §e" + target.getName().getString()
                            + "§a... Держите ПКМ 5 сек"),
                    true
            );
        }

        targetData.incrementReviveProgress();

        // Прогресс каждую секунду
        if (targetData.getReviveProgressTicks() % 20 == 0) {
            int pct = (int) (targetData.getReviveProgress() * 100);
            helper.displayClientMessage(
                    Component.literal("§aПодъём: §e" + pct + "%"),
                    true
            );
        }

        // Прерываем если хелпер отошёл
        if (helper.distanceTo(target) > 3.0) {
            targetData.cancelRevive();
            helper.displayClientMessage(
                    Component.literal("§cПодъём прерван — вы отошли слишком далеко!"),
                    true
            );
            return;
        }

        if (targetData.getReviveProgressTicks() >= DownedData.REVIVE_HOLD_TICKS) {
            revivePlayer(target, targetData, helper);
        }
    }

    /**
     * Враг начинает тащить: регистрируем пару dragger→downed и даём замедление.
     * Само физическое перемещение происходит в tickDrag каждый тик.
     */
    private static void handleEnemyStartDrag(ServerPlayer enemy, ServerPlayer target) {
        // Регистрируем пару: этот враг тащит этого лежачего
        DRAGGING.put(target.getUUID(), enemy.getUUID());

        // Замедление = текущий уровень + 2
        // amplifier 0 = Slowness I, поэтому currentAmplifier начинаем с -1 если эффекта нет
        int currentAmplifier = -1;
        MobEffectInstance existing = enemy.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (existing != null) {
            currentAmplifier = existing.getAmplifier();
        }
        int newAmplifier = currentAmplifier + 2; // +2 уровня сверху текущего

        enemy.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                60, // 3 секунды — обновляется при следующем клике
                newAmplifier,
                false, false
        ));

        enemy.displayClientMessage(
                Component.literal("§6Вы тащите §e" + target.getName().getString()
                        + " §7(замедление ×" + (newAmplifier + 1) + ")"),
                true
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. СЕРВЕРНЫЙ ТИК — общая очистка DRAGGING
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Очищаем устаревшие записи из DRAGGING
        // (игрок вышел, умер, или уже не лежит)
        DRAGGING.entrySet().removeIf(entry -> {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return true;
            ServerPlayer downed = (ServerPlayer) server.getPlayerList().getPlayer(entry.getKey());
            if (downed == null) return true;
            DownedData data = DownedCapability.get(downed);
            return data == null || data.getState() != DownedState.DOWNED;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. РЕСПАУН — копируем Capability
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
        DRAGGING.remove(target.getUUID());

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

        DRAGGING.values().remove(player.getUUID());
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
        DRAGGING.remove(target.getUUID());

        // Сохраняем оригинальный макс HP и урезаем вдвое
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            data.setOriginalMaxHp(maxHp.getBaseValue()); // запоминаем для восстановления
            double newMax = Math.max(2.0, maxHp.getBaseValue() / 2.0);
            maxHp.setBaseValue(newMax);
            target.setHealth((float) Math.min(4.0, newMax));
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
        DRAGGING.remove(player.getUUID());

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

    private static void showDownedIndicators(ServerPlayer downed, DownedData data) {
        if (downed.getServer() == null) return;

        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();
        int remainingSec = remaining / 20;

        // Частицы сердца у ног лежачего — видны всем рядом
        // Спавним через ServerLevel чтобы были видны всем (true = видны даже издалека)
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) downed.level();
        double x = downed.getX();
        double y = downed.getY() + 0.1; // чуть выше пола
        double z = downed.getZ();

        // Красные частицы сердца — "нужна помощь"
        level.sendParticles(ParticleTypes.HEART, x, y + 0.5, z, 1, 0.3, 0.1, 0.3, 0);
        // Красные частицы дыма у ног — указывают куда нажимать
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 0.15, z, 2, 0.2, 0.05, 0.2, 0);

        // ActionBar для самого лежачего — таймер
        String bar = buildTimerBar(remaining);
        downed.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal("§c☠ " + bar + " §e" + remainingSec + "с")));

        // ActionBar для союзников рядом (в радиусе 8 блоков) — подсказка
        for (ServerPlayer nearby : downed.getServer().getPlayerList().getPlayers()) {
            if (nearby == downed) continue;
            if (nearby.distanceTo(downed) > 8.0) continue;

            DownedData nearbyData = DownedCapability.get(nearby);
            if (nearbyData != null && nearbyData.getState() == DownedState.DOWNED) continue;

            boolean ally = areSameTeam(nearby, downed);
            if (ally) {
                // Союзнику — подсказка про подъём
                nearby.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal("§a[ПКМ у ног] §eПоднять §f" + downed.getName().getString()
                                + " §7(" + remainingSec + "с)")));
            } else {
                // Врагу — подсказка про тащение
                nearby.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal("§6[ПКМ у ног] §eТащить §f" + downed.getName().getString())));
            }
        }
    }

    /** Строит полоску таймера из символов: ████░░░░ */
    private static String buildTimerBar(int remainingTicks) {
        int total = DownedData.DOWNED_TIMEOUT_TICKS;
        int filled = (int) Math.round(8.0 * remainingTicks / total);
        filled = Math.max(0, Math.min(8, filled));
        String bar = "§c" + "█".repeat(filled) + "§8" + "█".repeat(8 - filled);
        return bar;
    }

    // ── Эффекты ───────────────────────────────────────────────────────────

    private static void applyDownedEffects(ServerPlayer player) {
        // SLOWNESS 255 блокирует ходьбу
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
        // JUMP_BOOST 128 (signed -128) — невозможность прыгнуть
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
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

        if (data != null && data.hasHpPenalty()) {
            // Восстанавливаем точное значение которое было до урезания
            maxHp.setBaseValue(data.getOriginalMaxHp());
            data.setOriginalMaxHp(-1);
        }
        // Если originalMaxHp не был задан (подъём командой) — ничего не меняем
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
