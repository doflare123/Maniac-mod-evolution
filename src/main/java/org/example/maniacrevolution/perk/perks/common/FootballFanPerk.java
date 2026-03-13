package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.perk.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Футбольный фанат (Активный) (Все) — одноразовый перк.
 * Замораживает игрока на 60 сек. (survivors) или 30 сек. (maniac),
 * после чего выдаёт Clarity.
 */
@Mod.EventBusSubscriber
public class FootballFanPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int DURATION_SURVIVOR = 60;
    private static final int DURATION_MANIAC   = 30;
    private static final int COOLDOWN_SEC      = 9999; // одноразовый

    // Храним команду игрока на момент активации
    private static final Map<UUID, PerkTeam> waitingPlayers = new HashMap<>();

    public FootballFanPerk() {
        super(new Builder("football_fan")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Стой неподвижно ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(DURATION_SURVIVOR + " сек.")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (выжившие) или ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(DURATION_MANIAC + " сек.")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (маньяк) — и получи ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("Балтику")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("! Одноразовый.")
                        .withStyle(ChatFormatting.GRAY));
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public void onActivate(ServerPlayer player) {
        PerkTeam team = PerkTeam.fromPlayer(player);
        int durationSec = (team == PerkTeam.MANIAC) ? DURATION_MANIAC : DURATION_SURVIVOR;
        int durationTicks = durationSec * 20;

        // Накладываем эффект заморозки
        player.addEffect(new MobEffectInstance(
                ModEffects.FROZEN.get(),
                durationTicks,
                0,
                false, true, true
        ));

        // Запоминаем команду на момент активации
        waitingPlayers.put(player.getUUID(), team);

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL,
                SoundSource.PLAYERS,
                1.0f, 0.8f
        );

        player.displayClientMessage(
                Component.literal("🍺 Ждём Балтику... " + durationSec + " сек.")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }

    // ── Выдача награды когда эффект истёк ─────────────────────────────────

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.FROZEN.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        if (!waitingPlayers.containsKey(uuid)) return;

        PerkTeam teamAtActivation = waitingPlayers.remove(uuid);

        // Проверяем что игрок всё ещё в той же команде
        PerkTeam currentTeam = PerkTeam.fromPlayer(player);
        if (currentTeam != teamAtActivation) {
            player.displayClientMessage(
                    Component.literal("Команда сменилась — Балтика не выдана.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Проверяем режим игры
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            player.displayClientMessage(
                    Component.literal("Неверный режим игры — Балтика не выдана.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Выдаём Clarity
        player.addItem(new ItemStack(ModItems.CLARITY.get(), 1));

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL_DRAGONBREATH,
                SoundSource.PLAYERS,
                1.0f, 1.2f
        );

        player.displayClientMessage(
                Component.literal("🍺 Балтика получена! Приятного!")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
    }

    // ── Сброс если перк сняли досрочно ────────────────────────────────────

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        waitingPlayers.remove(player.getUUID());
        player.removeEffect(ModEffects.FROZEN.get());
    }
}