package org.example.maniacrevolution.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;
import org.example.maniacrevolution.mana.ManaProvider;

import java.util.List;

/**
 * Бонк.
 *
 * ПКМ: создаёт облако дыма радиусом CLOUD_RADIUS блоков.
 *   - Всем игрокам (кроме владельца) в облаке: плавное падение 4 сек
 *   - Владельцу: пауза шкалы зависимости на BONG_PAUSE_TICKS тиков
 *
 * Модель: ваш bong.json (parent: maniacweapons:custom/bong)
 * Текстуры: bong_text.png, bong_text2.png (в maniacweapons:block/)
 */
public class BongItem extends Item implements IItemWithAbility {

    // ── Настройте под себя ────────────────────────────────────────────────────
    public static final float MANA_COST       = 20f;
    public static final int   COOLDOWN_TICKS  = 300;   // 15 секунд
    public static final int   COOLDOWN_SECS   = 15;
    private static final double CLOUD_RADIUS  = 4.0;
    private static final int   SLOW_FALL_TICKS = 80;   // 4 секунды

    private static final String NBT_CD = "BongCooldownTick";

    public BongItem(Properties props) { super(props); }

    // ── Использование ────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
        if (isOnCooldown(player)) return InteractionResultHolder.fail(stack);

        // Проверяем и тратим ману
        var mana = player.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || !mana.consumeMana(MANA_COST)) {
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cНедостаточно маны!"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Частицы дыма
        if (level instanceof ServerLevel sl) spawnCloud(sl, player);

        // Эффекты
        applyEffects(sp, level);

        // Кулдаун в NBT
        stack.getOrCreateTag().putLong(NBT_CD, level.getGameTime() + COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private void spawnCloud(ServerLevel level, Player owner) {
        for (int i = 0; i < 90; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r     = Math.random() * CLOUD_RADIUS;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    owner.getX() + Math.cos(angle) * r,
                    owner.getY() + Math.random() * 2.5,
                    owner.getZ() + Math.sin(angle) * r,
                    1, 0, 0.03, 0, 0.01);
        }
    }

    private void applyEffects(ServerPlayer owner, Level level) {
        // Плавное падение всем вокруг кроме владельца
        List<Player> nearby = level.getEntitiesOfClass(Player.class,
                new AABB(
                        owner.getX() - CLOUD_RADIUS, owner.getY() - 1,
                        owner.getZ() - CLOUD_RADIUS,
                        owner.getX() + CLOUD_RADIUS, owner.getY() + 3,
                        owner.getZ() + CLOUD_RADIUS));

        for (Player p : nearby) {
            if (p == owner) continue;
            p.addEffect(new MobEffectInstance(
                    MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, true, true));
        }

        // Пауза шкалы зависимости для владельца
        AddictionCapability cap = AddictionCapabilityProvider.get(owner);
        if (cap != null) {
            cap.setBongPauseTicks(AddictionCapability.BONG_PAUSE_TICKS);
            cap.syncToClient(owner);
        }
    }

    // ── IItemWithAbility ─────────────────────────────────────────────────────

    @Override public ResourceLocation getAbilityIcon() {
        return new ResourceLocation("maniacrev", "textures/gui/ability/bong.png");
    }
    @Override public String getAbilityName()        { return "Облако дыма"; }
    @Override public String getAbilityDescription() { return "Расслабляет окружающих, замедляет зависимость"; }
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