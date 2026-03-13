package org.example.maniacrevolution.item;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.NetherSwapProjectile;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import javax.annotation.Nullable;
import java.util.UUID;

public class NetherSwapItem extends Item implements IItemWithAbility {

    public static final int COOLDOWN_TICKS = 600;   // 30 секунд
    public static final float MANA_COST     = 10f;
    public static final int   SCAN_RANGE    = 64;   // блоков

    // NBT ключи
    private static final String NBT_COOLDOWN     = "netherswap_cooldown";
    // Клиентский кэш: кого подсветили (только серверная сторона хранит UUID)
    private static final String NBT_LOCKED_TARGET = "netherswap_target";

    public NetherSwapItem(Properties props) {
        super(props);
    }

    // ── Использование ─────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Начинаем прицеливание — запускаем анимацию использования
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;

        // Каждый тик пока прицеливаемся — сканируем цель
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            UUID prevTarget = getLockedTarget(stack);

            Player target = findAimedPlayer(serverPlayer);
            if (target != null) {
                UUID newId = target.getUUID();
                if (!newId.equals(prevTarget)) {
                    // Новая цель — снимаем подсветку со старой
                    if (prevTarget != null) {
                        Player old = level.getPlayerByUUID(prevTarget);
                        if (old != null) SelectiveGlowingEffect.removeGlowing(old, serverPlayer);
                    }
                    // Подсвечиваем новую только для стрелка
                    SelectiveGlowingEffect.addGlowing(target, serverPlayer, 40); // 2 сек запас
                    setLockedTarget(stack, newId);
                }
            } else {
                // Цель потеряна — убираем подсветку
                if (prevTarget != null) {
                    Player old = level.getPlayerByUUID(prevTarget);
                    if (old != null) SelectiveGlowingEffect.removeGlowing(old, serverPlayer);
                    clearLockedTarget(stack);
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (level.isClientSide()) return;

        UUID targetId = getLockedTarget(stack);
        clearLockedTarget(stack);

        // Снимаем подсветку в любом случае
        if (targetId != null) {
            Player target = level.getPlayerByUUID(targetId);
            if (target != null) SelectiveGlowingEffect.removeGlowing(target, player);
        }

        // Проверки
        if (getCooldownTicks(stack) > 0) return;
        if (targetId == null) return;

        // Проверяем ману
        boolean hasEnoughMana = player.getCapability(ManaProvider.MANA).map(mana -> {
            if (mana.getMana() >= MANA_COST) {
                mana.setMana(mana.getMana() - MANA_COST);
                return true;
            }
            return false;
        }).orElse(false);

        if (!hasEnoughMana) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cНедостаточно маны!"));
            return;
        }

        // Запускаем снаряд в направлении взгляда
        Vec3 look = player.getLookAngle();
        NetherSwapProjectile projectile = new NetherSwapProjectile(level, player, look);
        level.addFreshEntity(projectile);

        // Эффект выстрела у стрелка
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getEyeY(), player.getZ(),
                    10, 0.1, 0.1, 0.1, 0.2
            );
        }

        // Ставим кулдаун
        setCooldownTicks(stack, COOLDOWN_TICKS);
    }

    // ── Тик предмета — уменьшаем кулдаун ─────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;
        int cd = getCooldownTicks(stack);
        if (cd > 0) setCooldownTicks(stack, cd - 1);
    }

    // ── Анимация ──────────────────────────────────────────────────────────

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Анимация натяжения лука — "прицеливание"
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // Бесконечно пока держим
    }

    // ── Поиск игрока в прицеле ────────────────────────────────────────────

    @Nullable
    private Player findAimedPlayer(ServerPlayer shooter) {
        Vec3 eye    = shooter.getEyePosition();
        Vec3 look   = shooter.getLookAngle();
        Vec3 end    = eye.add(look.scale(SCAN_RANGE));

        // Луч трассировки по AABB игроков в радиусе
        double bestDist = Double.MAX_VALUE;
        Player best = null;

        for (Player candidate : shooter.level().players()) {
            if (candidate == shooter) continue;
            AABB box = candidate.getBoundingBox().inflate(0.3);
            var hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = candidate;
                }
            }
        }
        return best;
    }

    // ── NBT хелперы ───────────────────────────────────────────────────────

    public static int getCooldownTicks(ItemStack stack) {
        return stack.getOrCreateTag().getInt(NBT_COOLDOWN);
    }

    public static void setCooldownTicks(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt(NBT_COOLDOWN, ticks);
    }

    @Nullable
    private UUID getLockedTarget(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.hasUUID(NBT_LOCKED_TARGET)) return null;
        return tag.getUUID(NBT_LOCKED_TARGET);
    }

    private void setLockedTarget(ItemStack stack, UUID id) {
        stack.getOrCreateTag().putUUID(NBT_LOCKED_TARGET, id);
    }

    private void clearLockedTarget(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null) tag.remove(NBT_LOCKED_TARGET);
    }

    // ── IItemWithAbility ──────────────────────────────────────────────────

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/ability/nether_swap.png");
    }

    @Override
    public String getAbilityName() { return "Квантовая телепортация"; }

    @Override
    public String getAbilityDescription() { return "Меняет местами с целью"; }

    @Override
    public float getManaCost() { return MANA_COST; }

    @Override
    public int getCooldownSeconds(Player player) {
        // Ищем предмет в руках
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        ItemStack stack = main.getItem() instanceof NetherSwapItem ? main :
                off.getItem()  instanceof NetherSwapItem ? off : ItemStack.EMPTY;
        if (stack.isEmpty()) return 0;
        return getCooldownTicks(stack) / 20;
    }

    @Override
    public int getMaxCooldownSeconds() { return COOLDOWN_TICKS / 20; }
}