package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.effect.ThornEffect;
import org.example.maniacrevolution.perk.*;

/**
 * Шкура Ёжика / BladeMail (Активный) (Все)
 * Даёт эффект ThornEffect на DURATION_SECONDS секунд.
 * Эффект сам отражает 50% урона и показывает частицы.
 */
public class HedgehogSkinPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int   DURATION_SECONDS = 5;
    private static final int   DURATION_TICKS   = DURATION_SECONDS * 20;
    private static final int   COOLDOWN_SEC     = 100;
    private static final float MANA_COST        = 10f;

    public HedgehogSkinPerk() {
        super(new Builder("hedgehog_skin")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("При активации в течение ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(DURATION_SECONDS + " сек.")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" каждый удар по тебе дополнительно наносит ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int)(ThornEffect.REFLECT_PERCENT * 100) + "% урона")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" обратно атакующему. Кулдаун: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(COOLDOWN_SEC + " сек.")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public void onActivate(ServerPlayer player) {
        // Вешаем эффект — он сам занимается частицами и отражением
        player.addEffect(new MobEffectInstance(
                ModEffects.THORN.get(),
                DURATION_TICKS,
                0,
                false, // ambient
                false, // visible particles (управляем сами)
                false  // show icon
        ));

        // Звук активации
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PUFFER_FISH_BLOW_UP,
                SoundSource.PLAYERS,
                1.0f, 1.2f
        );

        player.displayClientMessage(
                Component.literal("🦔 Шкура Ёжика активирована!")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }
}