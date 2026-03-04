package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.mana.ManaProvider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Микрофон.
 *
 * ПКМ: выпускает музыкальную волну — максимальное замедление (IX)
 * на всех игроков team "survivors" в радиусе WAVE_RADIUS блоков на STUN_SECS секунды.
 *
 * Стоимость: MANA_COST маны.
 * Кулдаун: COOLDOWN_SECS секунд.
 *
 */
public class MicrophoneItem extends Item implements IItemWithAbility {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final float MANA_COST      = 5f;
    public static final int   COOLDOWN_SECS  = 15;
    public static final int   COOLDOWN_TICKS = COOLDOWN_SECS * 20; // 300
    public static final double WAVE_RADIUS   = 5.0;
    public static final int   STUN_SECS      = 3;
    public static final int   STUN_TICKS     = STUN_SECS * 20;     // 60
    // Slowness IX (амплификатор 8) = максимальное замедление
    public static final int   SLOWNESS_AMP   = 8;

    private static final String SURVIVORS_TEAM = "survivors";
    private static final String NBT_CD         = "MicCooldown";

    public MicrophoneItem(Properties props) { super(props); }

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
            sp.displayClientMessage(Component.literal("§cНедостаточно маны!"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Применяем эффект и спавним волну
        applyWave(sp, (ServerLevel) level);

        // Ставим кулдаун
        stack.getOrCreateTag().putLong(NBT_CD, level.getGameTime() + COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private void applyWave(ServerPlayer caster, ServerLevel level) {
        AABB box = new AABB(
                caster.getX() - WAVE_RADIUS, caster.getY() - 1,
                caster.getZ() - WAVE_RADIUS,
                caster.getX() + WAVE_RADIUS, caster.getY() + 3,
                caster.getZ() + WAVE_RADIUS
        );

        int hit = 0;
        for (Player target : level.getEntitiesOfClass(Player.class, box)) {
            if (target == caster) continue;
            if (!isInSurvivorsTeam(target)) continue;

            // Максимальное замедление на 3 секунды
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, SLOWNESS_AMP,
                    false, true, true));
            hit++;
        }

        // Частицы волны — расходящиеся кольца из нот
        spawnWaveParticles(caster, level);

        String msg = hit > 0
                ? "§bВолна! §7Станули §f" + hit + " §7выживших"
                : "§7Волна прошла мимо...";
        caster.displayClientMessage(Component.literal(msg), true);
    }

    private void spawnWaveParticles(ServerPlayer caster, ServerLevel level) {
        int rings = 3;
        int notesPerRing = 16;
        for (int r = 1; r <= rings; r++) {
            double radius = (WAVE_RADIUS / rings) * r;
            for (int i = 0; i < notesPerRing; i++) {
                double angle = (2 * Math.PI / notesPerRing) * i;
                double px = caster.getX() + Math.cos(angle) * radius;
                double pz = caster.getZ() + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.NOTE,
                        px, caster.getY() + 1.2, pz,
                        1, 0, 0.1, 0, 0.5);
            }
        }
        // Центральная вспышка
        level.sendParticles(ParticleTypes.EXPLOSION,
                caster.getX(), caster.getY() + 1, caster.getZ(),
                1, 0, 0, 0, 0);
    }

    // ── IItemWithAbility ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/microphone.png");
    }
    @Override public String getAbilityName()        { return "Музыкальная волна"; }
    @Override public String getAbilityDescription() { return "Станит выживших в радиусе 5 блоков"; }
    @Override public float  getManaCost()           { return MANA_COST; }
    @Override public int    getMaxCooldownSeconds() { return COOLDOWN_SECS; }

    @Override
    public int getCooldownSeconds(Player player) {
        ItemStack s = getMicStack(player);
        if (s == null) return 0;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0 : (int) Math.ceil(rem / 20.0);
    }

    @Override public boolean isOnCooldown(Player player) { return getCooldownSeconds(player) > 0; }

    @Override
    public float getCooldownProgress(Player player) {
        ItemStack s = getMicStack(player);
        if (s == null) return 0f;
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(NBT_CD)) return 0f;
        long rem = t.getLong(NBT_CD) - player.level().getGameTime();
        return rem <= 0 ? 0f : Math.min(1f, (float) rem / COOLDOWN_TICKS);
    }

    // ── Тултип ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§b♪ Микрофон").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  §b[ПКМ] Музыкальная волна")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(
                        "  Максимальное замедление на §f" + STUN_SECS + " сек")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                        "  Радиус: §f" + (int) WAVE_RADIUS + " §7блоков | "
                                + "Мана: §f" + (int) MANA_COST + " §7| КД: §f" + COOLDOWN_SECS + " сек")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Действует только на выживших")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // ── Вспомогательные ──────────────────────────────────────────────────────

    private static boolean isInSurvivorsTeam(Player player) {
        var team = player.getTeam();
        return team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName());
    }

    @Nullable
    private static ItemStack getMicStack(Player player) {
        ItemStack m = player.getMainHandItem();
        if (m.getItem() instanceof MicrophoneItem) return m;
        ItemStack o = player.getOffhandItem();
        if (o.getItem() instanceof MicrophoneItem) return o;
        return null;
    }
}