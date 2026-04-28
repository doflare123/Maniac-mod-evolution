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
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.PlagueOrbEntity;
import org.example.maniacrevolution.mana.ManaData;
import org.example.maniacrevolution.mana.ManaProvider;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.UUID;

public class PlagueLanternItem extends Item implements ITimedAbility {

    public static final float MANA_COST = 25f;
    public static final int COOLDOWN_TICKS = 400;
    public static final int COOLDOWN_SECONDS = 20;
    private static final String NBT_COOLDOWN_KEY = "PlagueLanternCooldownTick";
    // Добавь эти константы в класс:
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ATTACK_SPEED_UUID  = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Урон: базовый урон игрока = 1, модификатор = 0 → итого 1 урон (0.5 сердца)
            // Если хочешь ровно 1 сердце (2 HP) — поставь 1.0 вместо 0.0
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    ATTACK_DAMAGE_UUID,
                    "Weapon modifier",
                    -0.5,  // 1 - 0.5 базового = 0.5 сердца урона. Поставь 0.0 для 1 сердца
                    AttributeModifier.Operation.ADDITION
            ));

            // Скорость атаки: стандарт = 4.0, чем меньше — тем медленнее
            // 0.5 = очень медленно (как у кирки с зачарованием усталости)
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    ATTACK_SPEED_UUID,
                    "Weapon modifier",
                    -3.6,  // 4.0 + (-3.6) = 0.4 — очень медленно
                    AttributeModifier.Operation.ADDITION
            ));

            return builder.build();
        }
        return super.getAttributeModifiers(slot, stack);
    }


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
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/plague_lantern.png");
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