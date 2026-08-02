package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;
import org.example.maniacrevolution.event.AddictionEventHandler;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.sound.ModSounds;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Шприц адреналина.
 *
 * Эффект скорости СКЛАДЫВАЕТСЯ (каждый новый шприц добавляет к уже активному).
 * Базовая длительность: 8 сек → каждый следующий -1 сек (мин 1 сек).
 * Базовый уровень: скорость 3 → каждые 3 шприца -1 (мин скорость 1).
 *
 * Смерть от передоза: 4 подряд без 20-сек перерыва.
 * Смерть на стадии 3: если totalSyringes >= 3 → 1%/сек.
 */
public class SyringeItem extends Item {

    public static final int BASE_DURATION_SECS = 8;
    public static final int BASE_AMPLIFIER = 2; // скорость 3
    public static final int MIN_DURATION_SECS = 1;
    public static final int AMPLIFIER_LOSS_INTERVAL = 3;
    public static final int OVERDOSE_SYRINGES = 4;

    public SyringeItem(Properties props) { super(props); }

    // ── Использование ────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);

        if (!AddictionEventHandler.isAddictClass(sp)) {
            sp.displayClientMessage(Component.translatable("message.maniacrev.syringe.wrong_class"), true);
            return InteractionResultHolder.fail(stack);
        }

        AddictionCapability cap = AddictionCapabilityProvider.get(sp);
        if (cap == null) return InteractionResultHolder.fail(stack);

        long now = level.getGameTime();

        // ── Счётчик «подряд» ─────────────────────────────────────────────────
        if (now - cap.getLastSyringeTick() <= AddictionCapability.SYRINGE_WINDOW_TICKS) {
            cap.setConsecSyringes(cap.getConsecSyringes() + 1);
        } else {
            cap.setConsecSyringes(1);
        }
        cap.setLastSyringeTick(now);

        // ── Смерть от передоза: 3 подряд ─────────────────────────────────────
        if (cap.getConsecSyringes() >= OVERDOSE_SYRINGES) {
            cap.setConsecSyringes(0);
            cap.setTotalSyringeCount(cap.getTotalSyringeCount() + 1);
            cap.syncToClient(sp);
            if (!sp.isCreative()) stack.shrink(1);
            AddictionEventHandler.killWithMessage(sp,
                    Component.translatable("message.maniacrev.syringe.death.overdose", sp.getDisplayName()));
            return InteractionResultHolder.consume(stack);
        }

        int usedBefore = cap.getTotalSyringeCount();
        cap.setTotalSyringeCount(usedBefore + 1);

        // ── Снижаем шкалу зависимости на заданную долю ───────────────────────
        float reduction = cap.getAddiction() * AddictionCapability.SYRINGE_REDUCE_PCT;
        cap.setAddiction(cap.getAddiction() - reduction);

        // ── Эффект скорости (СКЛАДЫВАЕТСЯ с уже активным) ────────────────────
        int amplifier    = Math.max(0, BASE_AMPLIFIER - (usedBefore / AMPLIFIER_LOSS_INTERVAL));
        int durationSecs = Math.max(MIN_DURATION_SECS, BASE_DURATION_SECS - usedBefore);
        int newTicks     = durationSecs * 20;

        // Получаем уже активный эффект скорости
        MobEffectInstance existing = sp.getEffect(MobEffects.MOVEMENT_SPEED);
        if (existing != null) {
            // Складываем длительность к оставшемуся времени
            // Уровень берём максимальный из текущего и нового
            int combinedAmplifier = Math.max(existing.getAmplifier(), amplifier);
            int combinedTicks     = existing.getDuration() + newTicks;
            sp.removeEffect(MobEffects.MOVEMENT_SPEED);
            sp.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, combinedTicks, combinedAmplifier,
                    false, true, true));
        } else {
            sp.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, newTicks, amplifier,
                    false, true, true));
        }

        sp.displayClientMessage(Component.translatable("message.maniacrev.syringe.used",
                amplifier + 1, durationSecs), true);

        // ── Звук стука сердца при высоком totalSyringeCount ───────────────────
        // Порог: 2+ шприца (предупреждение перед возможной смертью)
        // Звук нужно зарегистрировать в ModSounds как HEARTBEAT
        if (cap.getTotalSyringeCount() >= 2) {
            playHeartbeatSound(sp, cap.getTotalSyringeCount());
        }

        cap.syncToClient(sp);
        if (!sp.isCreative()) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Воспроизводит звук стука сердца.
     * Чем больше шприцов — тем громче.
     *
     * Замените ModSounds.HEARTBEAT.get() на вашу SoundEvent когда добавите звук.
     */
    private void playHeartbeatSound(ServerPlayer player, int syringeCount) {
        try {
            SoundEvent heartbeat = ModSounds.HEARTBEAT.get();
            float volume = Math.min(1.0f, 0.4f + (syringeCount - 2) * 0.2f);
            float pitch  = Math.max(0.5f, 1.2f - (syringeCount - 2) * 0.15f); // тише и медленнее при большем кол-ве
            player.level().playSound(
                    null, // null = слышат все включая самого игрока
                    player.getX(), player.getY(), player.getZ(),
                    heartbeat, SoundSource.PLAYERS,
                    volume, pitch
            );
        } catch (Exception ignored) {
            // Звук ещё не зарегистрирован — просто пропускаем
        }
    }

    // ── Тултип ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.empty());

        // Описание эффекта
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.title")
                .withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.reduction",
                        Math.round(AddictionCapability.SYRINGE_REDUCE_PCT * 100))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.speed")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Деградация
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.degradation")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.first",
                        BASE_AMPLIFIER + 1, BASE_DURATION_SECS)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.duration_loss",
                        BASE_DURATION_SECS - Math.max(MIN_DURATION_SECS, BASE_DURATION_SECS - 1),
                        MIN_DURATION_SECS)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.level_loss", AMPLIFIER_LOSS_INTERVAL)
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Опасность
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.danger")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.overdose",
                        OVERDOSE_SYRINGES, AddictionCapability.SYRINGE_WINDOW_TICKS / 20)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.stage_death",
                        AddictionCapability.DANGER_STAGE,
                        AddictionCapability.DANGER_MIN_TOTAL_SYRINGES,
                        Math.round(AddictionCapability.STAGE3_DEATH_CHANCE * 100.0f),
                        AddictionCapability.DEATH_CHECK_INTERVAL / 20)
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Подсказка по ускорению ломки
        tooltip.add(Component.translatable("tooltip.maniacrev.syringe.addiction_speed",
                        Math.round(AddictionCapability.SYRINGE_SPEED_BONUS * 100.0f))
                .withStyle(ChatFormatting.GRAY));
    }
}
