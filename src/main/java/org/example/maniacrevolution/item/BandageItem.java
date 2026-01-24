package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BandageItem extends Item {

    public BandageItem(Properties properties) {
        super(properties.stacksTo(4));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6Способность: §e" + "Бинт в беде не бросит").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("При применении на союзника восстанавливает ему 4 хп, если применить на себя (направив в пустоту) восстановить 2 хп"));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Используем анимацию натягивания лука для визуального эффекта
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 90; // 4.5 секунды (90 тиков) для самолечения
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        // Проверяем, есть ли рядом другой игрок для лечения
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 startPos = player.getEyePosition();
        Vec3 endPos = startPos.add(lookVec.scale(3.0)); // Радиус 3 блока

        AABB searchBox = new AABB(startPos, endPos).inflate(1.0);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox,
                p -> p != player && p.distanceTo(player) <= 3.0 && !p.isSpectator());

        Player targetPlayer = null;
        if (!nearbyPlayers.isEmpty()) {
            // Находим ближайшего игрока, на которого смотрит медик
            for (Player nearby : nearbyPlayers) {
                Vec3 toTarget = nearby.position().add(0, nearby.getEyeHeight() / 2, 0).subtract(startPos).normalize();
                double dot = lookVec.dot(toTarget);
                if (dot > 0.95) { // Игрок должен смотреть прямо на цель
                    targetPlayer = nearby;
                    break;
                }
            }
        }

        // Определяем длительность в зависимости от цели
        int totalDuration = targetPlayer != null ? 60 : 90; // 3 сек для других, 4.5 сек для себя
        int usedDuration = totalDuration - remainingUseDuration;

        // Звуковой эффект каждые 10 тиков
        if (usedDuration % 10 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE,
                    SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return stack;
        }

        // Проверяем, есть ли рядом другой игрок для лечения
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 startPos = player.getEyePosition();
        Vec3 endPos = startPos.add(lookVec.scale(3.0));

        AABB searchBox = new AABB(startPos, endPos).inflate(1.0);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox,
                p -> p != player && p.distanceTo(player) <= 3.0 && !p.isSpectator());

        Player targetPlayer = null;
        if (!nearbyPlayers.isEmpty()) {
            for (Player nearby : nearbyPlayers) {
                Vec3 toTarget = nearby.position().add(0, nearby.getEyeHeight() / 2, 0).subtract(startPos).normalize();
                double dot = lookVec.dot(toTarget);
                if (dot > 0.95) {
                    targetPlayer = nearby;
                    break;
                }
            }
        }

        // Лечим цель или себя
        if (targetPlayer != null) {
            targetPlayer.heal(4.0F); // 1 сердце = 2 HP
            level.playSound(null, targetPlayer.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.5F, 1.5F);
            player.displayClientMessage(Component.literal("§aВы вылечили " + targetPlayer.getName().getString()), true);
        } else {
            player.heal(2.0F);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.5F, 1.5F);
            player.displayClientMessage(Component.literal("§aВы вылечили себя"), true);
        }

        // Уменьшаем количество бинтов
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Если игрок отпустил кнопку раньше времени - отменяем лечение
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("§cЛечение прервано"), true);
        }
    }
}