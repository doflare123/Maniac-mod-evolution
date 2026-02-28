package org.example.maniacrevolution.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.entity.PlagueOrbEntity;
import org.example.maniacrevolution.mana.ManaData;
import org.example.maniacrevolution.mana.ManaProvider;

public class PlagueLanternItem extends Item implements ITimedAbility {

    public static final float MANA_COST = 25f;
    public static final int COOLDOWN_TICKS = 400;
    public static final int COOLDOWN_SECONDS = 20;
    private static final String NBT_COOLDOWN_KEY = "PlagueLanternCooldownTick";

    public PlagueLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (isOnCooldown(player)) return InteractionResultHolder.fail(stack);

        // Проверяем и списываем ману через ManaProvider
        ManaData manaData = player.getCapability(ManaProvider.MANA).orElse(null);
        if (manaData == null || !manaData.consumeMana(MANA_COST)) {
            // Недостаточно маны — показываем сообщение в action bar
            player.displayClientMessage(
                    Component.translatable("item.maniacrev.plague_lantern.no_mana"), true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Спавним снаряд
        PlagueOrbEntity orb = PlagueOrbEntity.create(level, player);
        level.addFreshEntity(orb);

        // Ставим кулдаун
        stack.getOrCreateTag().putLong(NBT_COOLDOWN_KEY, level.getGameTime() + COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    // ─── IItemWithAbility ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation("maniacrev", "textures/gui/ability/plague_lantern.png");
    }

    @Override public String getAbilityName() { return "Сгусток чумы"; }
    @Override public String getAbilityDescription() { return "Бросает сгусток чумы"; }
    @Override public float getManaCost() { return MANA_COST; }
    @Override public int getMaxCooldownSeconds() { return COOLDOWN_SECONDS; }

    @Override
    public int getCooldownSeconds(Player player) {
        ItemStack stack = getHeldStack(player);
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_COOLDOWN_KEY)) return 0;
        long remaining = tag.getLong(NBT_COOLDOWN_KEY) - player.level().getGameTime();
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 20.0);
    }

    @Override public boolean isOnCooldown(Player player) { return getCooldownSeconds(player) > 0; }

    @Override
    public float getCooldownProgress(Player player) {
        ItemStack stack = getHeldStack(player);
        if (stack.isEmpty()) return 0f;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_COOLDOWN_KEY)) return 0f;
        long remaining = tag.getLong(NBT_COOLDOWN_KEY) - player.level().getGameTime();
        if (remaining <= 0) return 0f;
        return Math.min(1.0f, (float) remaining / COOLDOWN_TICKS);
    }

    // ─── ITimedAbility ────────────────────────────────────────────────────────

    @Override public int getDurationSeconds() { return COOLDOWN_SECONDS; }
    @Override public int getRemainingDurationSeconds(Player player) { return 0; }
    @Override public boolean isAbilityActive(Player player) { return false; }

    private ItemStack getHeldStack(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof PlagueLanternItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof PlagueLanternItem) return off;
        return ItemStack.EMPTY;
    }
}