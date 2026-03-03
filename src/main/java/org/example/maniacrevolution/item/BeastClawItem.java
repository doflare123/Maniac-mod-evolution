package org.example.maniacrevolution.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.FurySwipesCapability;
import org.example.maniacrevolution.capability.FurySwipesCapabilityProvider;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncFurySwipesTargetPacket;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Коготь зверя.
 *
 * Атака: базовый урон 1 (0.5 сердца) + 0.5 за каждый стак Fury Swipes на жертве.
 * Способность (ПКМ): прыжок на 1 блок вперёд, при приземлении урон в радиусе 1 блока.
 * Стаки: накладываются только при атаке по team "survivors", каждый живёт 20 сек.
 */
public class BeastClawItem extends Item implements IItemWithAbility {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final float  BASE_DAMAGE         = 1.0f;
    public static final float  LEAP_MANA_COST      = 10f;
    public static final int    LEAP_COOLDOWN_TICKS = 200;  // 10 секунд
    public static final int    LEAP_COOLDOWN_SECS  = 10;
    public static final float  LEAP_LAND_DAMAGE    = 2.0f; // урон при приземлении
    public static final double LEAP_LAND_RADIUS    = 1.5;  // радиус урона при приземлении
    public static final String SURVIVORS_TEAM      = "survivors";

    private static final String NBT_CD         = "BeastClawCooldown";
    private static final String NBT_LEAPING    = "BeastClawLeaping"; // прыгает ли сейчас
    private static final String NBT_PREV_Y     = "BeastClawPrevY";   // Y до прыжка

    private static final UUID DAMAGE_UUID = UUID.fromString("A7B3C5D1-E2F4-4A6B-8C0D-1E2F3A4B5C6D");
    private static final UUID SPEED_UUID  = UUID.fromString("B8C4D6E2-F3A5-5B7C-9D1E-2F3A4B5C6D7E");

    public BeastClawItem(Properties props) { super(props); }

    // ── Атака ─────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                             net.minecraft.world.entity.LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer sp)) return super.hurtEnemy(stack, target, attacker);
        if (!(target instanceof Player targetPlayer)) return super.hurtEnemy(stack, target, attacker);

        // Только по выжившим (team survivors)
        if (!isInSurvivorsTeam(targetPlayer)) return super.hurtEnemy(stack, target, attacker);

        // Добавляем стак на цель
        FurySwipesCapability cap = FurySwipesCapabilityProvider.get(targetPlayer);
        if (cap != null) {
            cap.addStack(attacker.level().getGameTime());
            if (targetPlayer instanceof ServerPlayer targetSP) {
                cap.syncToClient(targetSP);
            }
            // Отправляем данные атакующему для рендера над головой
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncFurySwipesTargetPacket(targetPlayer.getUUID(),
                            getCapabilityExpireTicks(cap))
            );
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // ── Прыжок (ПКМ) ─────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
        if (isOnCooldown(player)) return InteractionResultHolder.fail(stack);

        // Проверяем ману
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || !mana.consumeMana(LEAP_MANA_COST)) {
            sp.displayClientMessage(Component.literal("§cНедостаточно маны!"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Запоминаем Y до прыжка и ставим флаг
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(NBT_LEAPING, true);
        tag.putDouble(NBT_PREV_Y, player.getY());

        // Импульс вперёд и вверх
        Vec3 look = player.getLookAngle();
        double horizontal = 1.5;
        double vertical   = 0.5;
        player.setDeltaMovement(look.x * horizontal, vertical, look.z * horizontal);
        player.hurtMarked = true;

        // Частицы отрыва от земли
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
        }

        // Кулдаун
        tag.putLong(NBT_CD, level.getGameTime() + LEAP_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Вызывается каждый тик — проверяем приземление после прыжка.
     * Зарегистрировать в FurySwipesEventHandler через LivingUpdateEvent.
     */
    public static void onPlayerTick(ServerPlayer player) {
        ItemStack stack = getClawStack(player);
        if (stack == null) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean(NBT_LEAPING)) return;

        // Приземлились?
        if (player.onGround() && player.getDeltaMovement().y <= 0) {
            tag.putBoolean(NBT_LEAPING, false);
            // Урон в радиусе при приземлении
            landingDamage(player);
        }
    }

    private static void landingDamage(ServerPlayer player) {
        Level level = player.level();
        AABB box = new AABB(
                player.getX() - LEAP_LAND_RADIUS, player.getY() - 0.5,
                player.getZ() - LEAP_LAND_RADIUS,
                player.getX() + LEAP_LAND_RADIUS, player.getY() + 2,
                player.getZ() + LEAP_LAND_RADIUS);

        List<Entity> nearby = level.getEntities(player, box);
        for (Entity e : nearby) {
            if (e == player) continue;
            if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                le.hurt(level.damageSources().playerAttack(player), LEAP_LAND_DAMAGE);
            }
        }

        // Частицы удара о землю
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY(), player.getZ(),
                    1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    12, 0.5, 0.3, 0.5, 0.1);
        }
    }

    // ── Атрибуты урона ────────────────────────────────────────────────────────

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // BASE_DAMAGE = 1 урон. Базовый урон игрока = 1, модификатор = BASE_DAMAGE - 1 = 0
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID, "Claw damage",
                BASE_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
        // Скорость атаки — немного быстрее меча
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                SPEED_UUID, "Claw speed",
                -2.4, AttributeModifier.Operation.ADDITION)); // 4 - 2.4 = 1.6
        return builder.build();
    }

    // ── IItemWithAbility ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/beast_claw.png");
    }
    @Override public String getAbilityName()        { return "Прыжок зверя"; }
    @Override public String getAbilityDescription() { return "Прыгает вперёд, сбивая врагов при приземлении"; }
    @Override public float  getManaCost()           { return LEAP_MANA_COST; }
    @Override public int    getMaxCooldownSeconds() { return LEAP_COOLDOWN_SECS; }

    @Override
    public int getCooldownSeconds(Player player) {
        ItemStack s = getClawStack(player);
        if (s == null) return 0;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0 : (int) Math.ceil(rem / 20.0);
    }

    @Override public boolean isOnCooldown(Player player) { return getCooldownSeconds(player) > 0; }

    @Override
    public float getCooldownProgress(Player player) {
        ItemStack s = getClawStack(player);
        if (s == null) return 0f;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0f;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0f : Math.min(1f, (float) rem / LEAP_COOLDOWN_TICKS);
    }

    // ── Тултип ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6⚔ Коготь зверя").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Базовый урон: §f" + (int)BASE_DAMAGE)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §c[Fury Swipes] §7+0.5 урона за каждый стак")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Стак живёт §f20 сек §7независимо от других")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§b[ПКМ] Прыжок зверя")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Прыжок вперёд, урон §f" + (int)LEAP_LAND_DAMAGE
                        + " §7в радиусе §f" + (int)LEAP_LAND_RADIUS + " §7блока")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Кулдаун: §f" + LEAP_COOLDOWN_SECS
                        + " сек §7| Мана: §f" + (int)LEAP_MANA_COST)
                .withStyle(ChatFormatting.GRAY));
    }

    // ── Вспомогательные ──────────────────────────────────────────────────────

    public static boolean isInSurvivorsTeam(Player player) {
        var team = player.getTeam();
        return team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName());
    }

    @Nullable
    public static ItemStack getClawStack(Player player) {
        ItemStack m = player.getMainHandItem();
        if (m.getItem() instanceof BeastClawItem) return m;
        ItemStack o = player.getOffhandItem();
        if (o.getItem() instanceof BeastClawItem) return o;
        return null;
    }

    private static java.util.List<Long> getCapabilityExpireTicks(FurySwipesCapability cap) {
        // Рефлексия через serializeNBT неудобна — добавим прямой геттер в capability
        // Используем временный обходной путь через NBT
        var tag = cap.serializeNBT();
        var list = tag.getList("stacks", 4);
        java.util.List<Long> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(((net.minecraft.nbt.LongTag) list.get(i)).getAsLong());
        }
        return result;
    }
}