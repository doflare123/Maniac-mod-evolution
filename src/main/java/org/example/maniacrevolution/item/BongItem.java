package org.example.maniacrevolution.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;
import org.example.maniacrevolution.client.renderer.BongItemRenderer;
import org.example.maniacrevolution.entity.BongCloudEntity;
import org.example.maniacrevolution.mana.ManaProvider;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Бонк.
 *
 * ПКМ: создаёт облако дыма (BongCloudEntity) радиусом 5 блоков на 5 секунд.
 *   - Все игроки (кроме владельца), кто окажется в облаке, получают
 *     плавное падение на 5 сек (эффект обновляется каждый тик пока внутри)
 *   - Владельцу: пауза шкалы зависимости на BONG_PAUSE_TICKS тиков
 */
public class BongItem extends Item implements IItemWithAbility, GeoItem {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final float MANA_COST      = 5f;
    public static final int   COOLDOWN_TICKS = 300;  // 15 секунд
    public static final int   COOLDOWN_SECS  = 15;
    private static final int USING_ANIMATION_TICKS = 48; // using: 2.4 seconds
    // Радиус и длительность облака — см. BongCloudEntity

    private static final String NBT_CD = "BongCooldownTick";
    private static final String NBT_RUNNING = "BongRunning";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation USING = RawAnimation.begin().thenPlay("using");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BongItem(Properties props) {
        super(props);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            boolean held = selected || player.getOffhandItem() == stack;
            // Assign an instance before its first use, so different bongs animate independently.
            if (held) GeoItem.getOrAssignId(stack, serverLevel);
            boolean running = held && player.isSprinting();
            if (running) {
                if (!stack.getOrCreateTag().getBoolean(NBT_RUNNING)) {
                    stack.getOrCreateTag().putBoolean(NBT_RUNNING, true);
                }
            } else if (stack.hasTag()) {
                stack.getTag().remove(NBT_RUNNING);
            }
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new BongItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bong", 3, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            ItemDisplayContext perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            boolean inHand = perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            boolean running = inHand && stack != null && stack.hasTag() && stack.getTag().getBoolean(NBT_RUNNING);
            return state.setAndContinue(running ? RUN : IDLE);
        }).triggerableAnim("using", USING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Running, cooldown and GeckoLib instance NBT updates keep the item in hand.
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    // ── Использование ────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this) || isOnCooldown(player)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            // Block rapid input immediately, before the server's cooldown update arrives.
            player.getCooldowns().addCooldown(this, USING_ANIMATION_TICKS);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);

        // Проверяем и тратим ману
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || !mana.consumeMana(MANA_COST)) {
            // Cancel the predicted input lock if the server rejects the ability.
            sp.getCooldowns().removeCooldown(this);
            sp.displayClientMessage(Component.translatable("message.maniacrev.not_enough_mana", (int) MANA_COST), true);
            return InteractionResultHolder.fail(stack);
        }

        // Спавним длящееся облако (5 сек, радиус 5 блоков)
        if (level instanceof ServerLevel sl) {
            BongCloudEntity.spawn(sl, player);
        }

        // Пауза шкалы зависимости для владельца
        AddictionCapability cap = AddictionCapabilityProvider.get(sp);
        if (cap != null) {
            cap.setBongPauseTicks(AddictionCapability.BONG_PAUSE_TICKS);
            cap.syncToClient(sp);
        }

        // Кулдаун в NBT
        stack.getOrCreateTag().putLong(NBT_CD, level.getGameTime() + COOLDOWN_TICKS);
        sp.getCooldowns().addCooldown(this, USING_ANIMATION_TICKS);
        long instanceId = GeoItem.getOrAssignId(stack, sp.serverLevel());
        sp.inventoryMenu.broadcastChanges();
        triggerAnim(sp, instanceId, "bong", "using");
        return InteractionResultHolder.consume(stack);
    }

    // ── IItemWithAbility ─────────────────────────────────────────────────────

    @Override public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/bong.png");
    }
    @Override public String getAbilityName() {
        return Component.translatable("ability.maniacrev.bong.name").getString();
    }
    @Override public String getAbilityDescription() {
        return Component.translatable("ability.maniacrev.bong.desc",
                BongCloudEntity.DURATION_TICKS / 20, (int) BongCloudEntity.RADIUS).getString();
    }
    @Override public float  getManaCost()           { return MANA_COST; }
    @Override public int    getMaxCooldownSeconds() { return COOLDOWN_SECS; }

    @Override
    public int getCooldownSeconds(Player player) {
        ItemStack s = held(player);
        if (s.isEmpty()) return 0;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0 : (int) Math.ceil(rem / 20.0);
    }

    @Override public boolean isOnCooldown(Player player) { return getCooldownSeconds(player) > 0; }

    @Override
    public float getCooldownProgress(Player player) {
        ItemStack s = held(player);
        if (s.isEmpty()) return 0f;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0f;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0f : Math.min(1f, (float) rem / COOLDOWN_TICKS);
    }

    private ItemStack held(Player p) {
        ItemStack m = p.getMainHandItem();
        if (m.getItem() instanceof BongItem) return m;
        ItemStack o = p.getOffhandItem();
        if (o.getItem() instanceof BongItem) return o;
        return ItemStack.EMPTY;
    }
}
