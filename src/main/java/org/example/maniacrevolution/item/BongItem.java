package org.example.maniacrevolution.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;
import org.example.maniacrevolution.entity.BongCloudEntity;
import org.example.maniacrevolution.mana.ManaProvider;

/**
 * Бонк.
 *
 * ПКМ: создаёт облако дыма (BongCloudEntity) радиусом 5 блоков на 5 секунд.
 *   - Все игроки (кроме владельца), кто окажется в облаке, получают
 *     плавное падение на 5 сек (эффект обновляется каждый тик пока внутри)
 *   - Владельцу: пауза шкалы зависимости на BONG_PAUSE_TICKS тиков
 */
public class BongItem extends Item implements IItemWithAbility {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final float MANA_COST      = 20f;
    public static final int   COOLDOWN_TICKS = 300;  // 15 секунд
    public static final int   COOLDOWN_SECS  = 15;
    // Радиус и длительность облака — см. BongCloudEntity

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
        return InteractionResultHolder.consume(stack);
    }

    // ── IItemWithAbility ─────────────────────────────────────────────────────

    @Override public ResourceLocation getAbilityIcon() {
        return new ResourceLocation("maniacrev", "textures/gui/ability/bong.png");
    }
    @Override public String getAbilityName()        { return "Облако дыма"; }
    @Override public String getAbilityDescription() {
        return "Создаёт облако дыма на 5 сек. Все в радиусе 5 блоков получают плавное падение.";
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