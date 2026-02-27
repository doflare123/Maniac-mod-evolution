package org.example.maniacrevolution.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.entity.PlagueOrbEntity;
import org.example.maniacrevolution.mana.ManaProvider; // ← ваш существующий класс работы с маной
import org.example.maniacrevolution.util.ManaUtil;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Чумная лампа.
 *
 * Пассивная аура: каждый тик наносит эффект чумы на survivors/Adventure в радиусе 5 блоков
 *                 (логика в PlagueLanternEventHandler).
 *
 * Активная способность (ПКМ): бросает зелёный сгусток чумы.
 *   - Кулдаун: 20 секунд (400 тиков), хранится в NBT предмета.
 *   - Стоимость: MANA_COST маны (см. константу ниже).
 *   - При попадании: мгновенно заполняет шкалу чумы цели до порога.
 *
 * ITimedAbility используется HUD для отображения оставшегося кулдауна.
 * ВАЖНО: здесь "длительность" = длительность кулдауна (нет активной фазы с таймером),
 * поэтому isAbilityActive всегда false, а ITimedAbility отражает отсчёт кулдауна.
 */
public class PlagueLanternItem extends Item implements GeoItem, ITimedAbility {

    // ── Константы способности ─────────────────────────────────────────────────
    /** Стоимость броска сгустка в единицах маны. Замените на нужное значение. */
    public static final float MANA_COST = 25f;

    /** Кулдаун в тиках (20 сек × 20 TPS = 400). */
    public static final int COOLDOWN_TICKS = 400;

    /** Кулдаун в секундах для интерфейса. */
    public static final int COOLDOWN_SECONDS = 20;

    private static final String NBT_COOLDOWN_KEY = "PlagueLanternCooldownTick";

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    private static final RawAnimation ANIM_RUN  = RawAnimation.begin().thenLoop("animation.plague_lantern.run");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.plague_lantern.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PlagueLanternItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // ─── Активная способность: бросок сгустка (ПКМ) ──────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Работаем только на сервере
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        // Проверяем кулдаун
        if (isOnCooldown(player)) {
            return InteractionResultHolder.fail(stack);
        }

        // Проверяем ману
        // Адаптируйте вызов под ваш реальный ManaHelper / ManaCapability
        if (!ManaUtil.hasMana(player, MANA_COST)) {
            // Опционально: отправить игроку сообщение "Недостаточно маны"
            return InteractionResultHolder.fail(stack);
        }

        // Списываем ману
        ManaUtil.consumeMana(player, MANA_COST);

        // Спавним снаряд
        PlagueOrbEntity orb = PlagueOrbEntity.create(level, player);
        level.addFreshEntity(orb);

        // Ставим кулдаун (сохраняем игровой тик истечения)
        long expireTick = level.getGameTime() + COOLDOWN_TICKS;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(NBT_COOLDOWN_KEY, expireTick);

        // Анимация броска (если есть в вашей модели)
        triggerRunAnimation(stack, player);

        return InteractionResultHolder.consume(stack);
    }

    // ─── IItemWithAbility ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation("maniacrev", "textures/gui/ability/plague_lantern.png");
    }

    @Override
    public String getAbilityName() {
        return "Сгусток чумы";
    }

    @Override
    public String getAbilityDescription() {
        return "Бросает сгусток чумы, мгновенно заполняющий шкалу чумы цели";
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public int getMaxCooldownSeconds() {
        return COOLDOWN_SECONDS;
    }

    /**
     * Оставшийся кулдаун в секундах.
     * Читается из NBT предмета в основной руке игрока.
     */
    @Override
    public int getCooldownSeconds(Player player) {
        ItemStack stack = getHeldStack(player);
        if (stack.isEmpty()) return 0;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_COOLDOWN_KEY)) return 0;

        long expireTick = tag.getLong(NBT_COOLDOWN_KEY);
        long remaining  = expireTick - player.level().getGameTime();

        if (remaining <= 0) return 0;
        // Переводим тики в секунды, округляем вверх
        return (int) Math.ceil(remaining / 20.0);
    }

    @Override
    public boolean isOnCooldown(Player player) {
        return getCooldownSeconds(player) > 0;
    }

    @Override
    public float getCooldownProgress(Player player) {
        ItemStack stack = getHeldStack(player);
        if (stack.isEmpty()) return 0f;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_COOLDOWN_KEY)) return 0f;

        long expireTick   = tag.getLong(NBT_COOLDOWN_KEY);
        long remainTicks  = expireTick - player.level().getGameTime();

        if (remainTicks <= 0) return 0f;
        return Math.min(1.0f, (float) remainTicks / COOLDOWN_TICKS);
    }

    // ─── ITimedAbility ────────────────────────────────────────────────────────
    // HUD использует это для отображения иконки без "активной фазы"

    @Override
    public int getDurationSeconds() {
        return COOLDOWN_SECONDS;
    }

    @Override
    public int getRemainingDurationSeconds(Player player) {
        // Для лампы нет активной фазы — возвращаем 0
        return 0;
    }

    /** Лампа не имеет активной временной фазы — всегда false */
    @Override
    public boolean isAbilityActive(Player player) {
        return false;
    }

    // ─── Хелпер: получить ItemStack лампы из рук игрока ──────────────────────

    private ItemStack getHeldStack(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof PlagueLanternItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof PlagueLanternItem) return off;
        return ItemStack.EMPTY;
    }

    // ─── GeckoLib ────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, state ->
                state.setAndContinue(ANIM_IDLE)));

        controllers.add(new AnimationController<>(this, "run_controller", 2, state ->
                PlayState.STOP).triggerableAnim("run", ANIM_RUN));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object itemStack) {
        return System.currentTimeMillis() / 50.0;
    }

    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {});
    }

    @Override
    public Supplier<Object> getRenderProviderId() {
        return renderProvider;
    }

    public void triggerRunAnimation(ItemStack stack, Player player) {
        triggerAnim(player, GeckoLibUtil.getIDFromStack(stack), "run_controller", "run");
    }
}