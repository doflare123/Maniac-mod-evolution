package org.example.maniacrevolution.downed;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DownedEventHandler {

    // UUID ArmorStand-маунта для каждого лежачего игрока
    private static final java.util.Map<java.util.UUID, java.util.UUID> DOWNED_MOUNTS = new java.util.HashMap<>();

    // ══════════════════════════════════════════════════════════════════════
    // 0. БЛОКИРОВКА УРОНА ДЛЯ ЛЕЖАЧЕГО
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        if (data.getState() == DownedState.DOWNED) {
            // Разрешаем только наш магический урон из killDowned()
            // (там state уже ALIVE к моменту удара, так что этот блок не сработает)
            // Всё остальное — блокируем
            event.setCanceled(true);
        }
    }

    // Дополнительная страховка на случай если урон каким-то образом прошёл
    @SubscribeEvent
    public static void onDownedPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        // Если state == DOWNED — смерть незаконная, блокируем и восстанавливаем HP
        if (data.getState() == DownedState.DOWNED) {
            event.setCanceled(true);
            player.setHealth(1.0f);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. ПЕРЕХВАТ СМЕРТИ
    // ══════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        // Уже использовал шанс или уже лежит — смерть фатальна
        if (data.hasUsedSecondChance()) return;
        if (data.getState() == DownedState.DOWNED) return;

        // Первая смерть — переводим в DOWNED
        event.setCanceled(true);

        data.setState(DownedState.DOWNED);
        data.setDownedTicksElapsed(0);
        data.cancelRevive();
        data.setUsedSecondChance(true);

        // Восстанавливаем HP до 1 чтобы игрок не умер сразу
        player.setHealth(1.0f);
        applyDownedEffects(player);

        broadcastMessage(player,
                "§c☠ " + player.getName().getString() + " §cупал! Помогите ему в течение §e60 сек§c!");

        Maniacrev.LOGGER.info("[Downed] {} -> DOWNED", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. ТИК — таймер лежания и эффект ослабленного
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
        // Обновляем эффекты каждые 10 тиков
        if (data.getDownedTicksElapsed() % 10 == 0) {
            applyDownedEffects(player);
        }

        // Полная заморозка: обнуляем velocity каждый тик
        player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, 0), 0);

        // Поза "спящего" (горизонтальное лежание)
        // Используем sleeping direction чтобы задать позу
        if (!player.isSleeping()) {
            player.setPose(net.minecraft.world.entity.Pose.SLEEPING);
        }

        data.incrementDownedTicks();

        // Предупреждения обратного отсчёта
        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();
        if (remaining == 30 * 20 || remaining == 15 * 20 || remaining == 10 * 20
                || remaining == 5 * 20 || remaining == 3 * 20 || remaining == 2 * 20
                || remaining == 20) {
            player.displayClientMessage(
                    Component.literal("§c☠ Вы умрёте через §e" + (remaining / 20) + " сек§c!"),
                    true
            );
        }

        // Таймаут — принудительная смерть
        if (data.getDownedTicksElapsed() >= DownedData.DOWNED_TIMEOUT_TICKS) {
            killDowned(player, data);
        }
    }

    private static void tickWeakened(ServerPlayer player, DownedData data) {
        // Поддерживаем слабость у поднятого
        if (data.getDownedTicksElapsed() % 10 == 0) {
            applyWeakenedEffects(player);
        }
        data.incrementDownedTicks();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. ПКМ ПО ЛЕЖАЧЕМУ
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
            handleEnemyDrag(helper, target);
        }
    }

    private static void handleAllyRevive(ServerPlayer helper, ServerPlayer target, DownedData targetData) {
        // Если другой союзник уже поднимает — сбрасываем его прогресс
        if (targetData.getReviverUUID() != null
                && !targetData.getReviverUUID().equals(helper.getUUID())) {
            targetData.cancelRevive();
        }

        // Начинаем или продолжаем подъём
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

        // Показываем прогресс каждую секунду
        if (targetData.getReviveProgressTicks() % 20 == 0) {
            int pct = (int) (targetData.getReviveProgress() * 100);
            helper.displayClientMessage(
                    Component.literal("§aПодъём: §e" + pct + "%"),
                    true
            );
        }

        // Проверяем расстояние — если отошёл, сбрасываем
        if (helper.distanceTo(target) > 3.0) {
            targetData.cancelRevive();
            helper.displayClientMessage(
                    Component.literal("§cПодъём прерван — вы отошли слишком далеко!"),
                    true
            );
            return;
        }

        // Завершение подъёма
        if (targetData.getReviveProgressTicks() >= DownedData.REVIVE_HOLD_TICKS) {
            revivePlayer(target, targetData, helper);
        }
    }

    private static void handleEnemyDrag(ServerPlayer enemy, ServerPlayer target) {
        // Определяем новый уровень замедления: текущий + 2
        int currentAmplifier = -1;
        MobEffectInstance existing = enemy.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (existing != null) {
            currentAmplifier = existing.getAmplifier();
        }
        // +2 уровня (amplifier 0-based: 0 = Slowness I, 1 = Slowness II, ...)
        int newAmplifier = currentAmplifier + 2;

        enemy.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                40, // 2 сек, обновляется при каждом нажатии
                newAmplifier,
                false, false
        ));

        // Тащим лежачего к врагу (шаг 0.15 блока)
        double dx = enemy.getX() - target.getX();
        double dz = enemy.getZ() - target.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 1.5) {
            double step = 0.15;
            target.teleportTo(
                    target.getX() + (dx / dist) * step,
                    target.getY(),
                    target.getZ() + (dz / dist) * step
            );
        }

        enemy.displayClientMessage(
                Component.literal("§6Вы тащите §e" + target.getName().getString()),
                true
        );
    }

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
    }

    // ══════════════════════════════════════════════════════════════════════
    // ПУБЛИЧНЫЕ МЕТОДЫ (вызываются из DownedCommand)
    // ══════════════════════════════════════════════════════════════════════

    /** Поднять игрока командой (без хелпера) */
    public static void instantRevive(ServerPlayer target) {
        DownedData data = DownedCapability.get(target);
        if (data == null || data.getState() != DownedState.DOWNED) return;
        revivePlayer(target, data, null);
    }

    /** Полный сброс состояния командой reset */
    public static void resetPlayer(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        if (data.getState() == DownedState.DOWNED) {
            removeDownedEffects(player);
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

        // Урезаем макс HP вдвое
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double newMax = Math.max(2.0, maxHp.getBaseValue() / 2.0);
            maxHp.setBaseValue(newMax);
            // Даём 2 сердца HP после подъёма
            target.setHealth((float) Math.min(4.0, newMax));
        }

        applyWeakenedEffects(target);

        if (helper != null) {
            helper.displayClientMessage(
                    Component.literal("§aВы подняли §e" + target.getName().getString() + "§a!"),
                    false
            );
        }
        target.displayClientMessage(
                Component.literal("§eВас подняли! §cСледующий нокаут §4фатален§c. HP урезаны вдвое!"),
                false
        );

        broadcastMessage(target, "§a✚ " + target.getName().getString() +
                " §aбыл поднят" +
                (helper != null ? " игроком §e" + helper.getName().getString() : "") + "§a!");

        Maniacrev.LOGGER.info("[Downed] {} -> WEAKENED, макс HP: {}",
                target.getName().getString(),
                target.getAttribute(Attributes.MAX_HEALTH).getValue());
    }

    public static void killDowned(ServerPlayer player, DownedData data) {
        // Переводим в ALIVE чтобы LivingDeathEvent не отменил смерть снова
        data.setState(DownedState.ALIVE);
        data.setDownedTicksElapsed(0);
        data.cancelRevive();
        removeDownedEffects(player);

        player.hurt(player.level().damageSources().magic(), Float.MAX_VALUE);

        broadcastMessage(player, "§4✝ " + player.getName().getString() + " §4не был поднят и погиб.");
        Maniacrev.LOGGER.info("[Downed] {} умер (таймаут)", player.getName().getString());
    }

    // ── Эффекты ───────────────────────────────────────────────────────────

    private static void applyDownedEffects(ServerPlayer player) {
        // SLOWNESS 255 — блокирует ходьбу
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
        // JUMP BOOST с отрицательным amplifier — блокирует прыжки (amplifier 128 = -128 signed)
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 255, false, false));
    }

    private static void removeDownedEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        // Возвращаем нормальную позу стоя
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
    }

    private static void applyWeakenedEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
    }

    private static void removeWeakenedEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
    }

    private static void restoreMaxHp(ServerPlayer player) {
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(maxHp.getBaseValue() * 2.0);
        }
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
