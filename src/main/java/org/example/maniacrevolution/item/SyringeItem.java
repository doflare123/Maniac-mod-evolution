package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * Смерть от передоза: 3 подряд без 20-сек перерыва.
 * Смерть на стадии 3: если totalSyringes >= 3 → 10%/сек.
 */
public class SyringeItem extends Item {

    private static final int BASE_DURATION_SECS = 8;
    private static final int BASE_AMPLIFIER     = 2; // скорость 3

    public SyringeItem(Properties props) { super(props); }

    // ── Использование ────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);

        if (!AddictionEventHandler.isAddictClass(sp)) {
            sp.displayClientMessage(
                    Component.literal("§cЭтот предмет только для зависимого"), true);
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
        if (cap.getConsecSyringes() >= 3) {
            cap.setConsecSyringes(0);
            cap.setTotalSyringeCount(cap.getTotalSyringeCount() + 1);
            cap.syncToClient(sp);
            if (!sp.isCreative()) stack.shrink(1);
            AddictionEventHandler.killWithMessage(sp,
                    "§4§l" + sp.getName().getString() + " §c— сердце не выдержало давления");
            return InteractionResultHolder.consume(stack);
        }

        int usedBefore = cap.getTotalSyringeCount();
        cap.setTotalSyringeCount(usedBefore + 1);

        // ── Снижаем шкалу зависимости на 20% ─────────────────────────────────
        float reduction = cap.getAddiction() * AddictionCapability.SYRINGE_REDUCE_PCT;
        cap.setAddiction(cap.getAddiction() - reduction);

        // ── Эффект скорости (СКЛАДЫВАЕТСЯ с уже активным) ────────────────────
        int amplifier    = Math.max(0, BASE_AMPLIFIER - (usedBefore / 3));
        int durationSecs = Math.max(1, BASE_DURATION_SECS - usedBefore);
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

        sp.displayClientMessage(Component.literal(
                String.format("§bАдреналин! §7Скорость %d на %d сек",
                        amplifier + 1, durationSecs)), true);

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
        tooltip.add(Component.literal("§b⚡ Адреналин")
                .withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Снижает шкалу зависимости §aна 20%")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Даёт скорость §f(эффекты складываются)")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Деградация
        tooltip.add(Component.literal("§e⚠ Деградация эффекта:")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  1-й шприц: §fСкорость 3 §7на §f8 сек")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Каждый следующий: §c-1 сек §7длительности")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Каждые 3 шприца: §c-1 уровень §7скорости")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Опасность
        tooltip.add(Component.literal("§4☠ Опасность:")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  §c3 шприца подряд §7(< 20 сек) = §4СМЕРТЬ")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §cСтадия 3 + 3 общих §7= 10%/сек шанс смерти")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Подсказка по ускорению ломки
        tooltip.add(Component.literal("§8⟳ Каждый шприц ускоряет ломку на §c"
                        + (int)(AddictionCapability.SYRINGE_SPEED_BONUS * 100) + "%")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}