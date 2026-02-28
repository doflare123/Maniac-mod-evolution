package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.capability.PlagueCapability;
import org.example.maniacrevolution.capability.PlagueCapabilityProvider;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.PlagueLanternItem;

import java.util.List;

/**
 * Серверный обработчик событий для чумной лампы.
 *
 * Логика:
 *  1. Каждый серверный тик проверяем всех игроков на сервере.
 *  2. Если у игрока A в руке PlagueLanternItem — ищем игроков team "survivors"
 *     в режиме Adventure в радиусе 5 блоков (КРОМЕ самого A).
 *  3. Накладываем на них эффект чумы на 1 секунду (25 тиков с небольшим запасом).
 *  4. Параллельно: для КАЖДОГО игрока с активным эффектом чумы тикаем Capability.
 *  5. Если Capability вернула true (достигнут порог 5 сек) — наносим урон 3.0f.
 *
 * Зарегистрируйте этот класс в вашем главном классе мода:
 *   MinecraftForge.EVENT_BUS.register(new PlagueLanternEventHandler());
 */
@Mod.EventBusSubscriber
public class PlagueLanternEventHandler {

    private static final double LANTERN_RADIUS = 5.0;
    private static final int EFFECT_DURATION_TICKS = 25; // 1.25 сек, чтобы не было мерцания
    private static final String SURVIVORS_TEAM = "survivors";

    // ─── Привязка Capability к игроку ────────────────────────────────────────

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            PlagueCapabilityProvider provider = new PlagueCapabilityProvider();
            event.addCapability(PlagueCapabilityProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    // ─── Главный тик ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerPlayerTick(TickEvent.PlayerTickEvent event) {
        // Выполняем только в конце тика и только на сервере
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.level().isClientSide()) return;

        Player player = serverPlayer;

        // ── Шаг 1: распространение эффекта от лампы ──────────────────────────
        if (isHoldingPlagueLantern(player)) {
            applyPlagueToNearby(serverPlayer);
        }

        // ── Шаг 2: тикаем накопление для текущего игрока ─────────────────────
        tickPlagueAccumulation(serverPlayer);
    }

    // ─── Проверка предмета в руке ─────────────────────────────────────────────

    private static boolean isHoldingPlagueLantern(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() instanceof PlagueLanternItem
                || off.getItem() instanceof PlagueLanternItem;
    }

    // ─── Применение эффекта к ближайшим игрокам ──────────────────────────────

    private static void applyPlagueToNearby(ServerPlayer source) {
        List<ServerPlayer> targets = source.getServer().getPlayerList().getPlayers();

        for (ServerPlayer target : targets) {
            // Не на себя
            if (target == source) continue;

            // Только в режиме Adventure
            if (target.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE) continue;

            // Только из команды survivors
            var scoreboard = source.getServer().getScoreboard();
            var team = scoreboard.getPlayersTeam(target.getScoreboardName());
            if (team == null || !team.getName().equals(SURVIVORS_TEAM)) continue;

            // Только в радиусе 5 блоков (в одном измерении)
            if (!target.level().dimension().equals(source.level().dimension())) continue;
            if (target.distanceTo(source) > LANTERN_RADIUS) continue;

            // Накладываем эффект чумы на 25 тиков (обновляем каждый тик лампы)
            target.addEffect(new MobEffectInstance(
                    ModEffects.PLAGUE.get(),
                    EFFECT_DURATION_TICKS,
                    0,          // amplifier 0
                    false,      // не из маяка
                    false,       // показывать частицы
                    false        // показывать иконку
            ));
        }
    }

    // ─── Накопление и урон ────────────────────────────────────────────────────

    private static void tickPlagueAccumulation(ServerPlayer player) {
        // Проверяем наличие эффекта чумы
        boolean hasPlague = player.hasEffect(ModEffects.PLAGUE.get());

        PlagueCapability cap = PlagueCapabilityProvider.get(player);
        if (cap == null) return;

        if (hasPlague) {
            // Тикаем накопление — если вернул true, пора наносить урон
            boolean shouldDamage = cap.tickPlague();

            if (shouldDamage) {
                // Наносим урон чумой (игнорирует броню — используем magic)
                // Можно заменить на DamageSource.MAGIC для игнорирования брони
                player.hurt(
                        player.level().damageSources().magic(),
                        PlagueCapability.PLAGUE_DAMAGE
                );
            }
        }
        // Если эффекта нет — накопление не идёт, но уже накопленное НЕ сбрасываем
        // (суммарное накопление, как требовалось)

        // Синхронизируем с клиентом каждый тик (можно оптимизировать до 1 раза в 4 тика)
        cap.syncToClient(player);
    }
}