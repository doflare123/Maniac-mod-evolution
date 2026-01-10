package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenResurrectionGuiPacket;

public class NecromancerStaffItem extends Item {
    private static final int USE_DURATION = 100; // 5 секунд (20 тиков = 1 секунда)
    private static final float MANA_COST = 50.0f;

    private static final int MAX_DURABILITY = 100;
    private static final int DAMAGE_ON_SUCCESS = 30;
    private static final int DAMAGE_ON_INTERRUPT = 10;

    public NecromancerStaffItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Проверяем ману
            boolean hasMana = player.getCapability(ManaProvider.MANA).map(mana ->
                    mana.getMana() >= MANA_COST
            ).orElse(false);

            if (!hasMana) {
                player.displayClientMessage(
                        Component.literal("§cНедостаточно маны! Требуется: " + MANA_COST),
                        true
                );
                return InteractionResultHolder.fail(itemStack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (entity instanceof Player player) {
            if (!level.isClientSide()) {
                // Спавним частицы во время использования
                spawnChannelingParticles((ServerPlayer) player, USE_DURATION - remainingUseDuration);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            // Потребляем ману
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                if (mana.consumeMana(MANA_COST)) {
                    // Повреждаем посох за успешное использование
                    stack.hurtAndBreak(DAMAGE_ON_SUCCESS, player, (p) -> {
                        p.broadcastBreakEvent(player.getUsedItemHand());
                    });

                    // Открываем GUI для выбора игрока
                    ModNetworking.sendToPlayer(new OpenResurrectionGuiPacket(), player);

                    player.displayClientMessage(
                            Component.literal("§5Выберите душу для воскрешения..."),
                            true
                    );
                }
            });
        }

        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player && !level.isClientSide()) {
            int usedTime = this.getUseDuration(stack) - timeLeft;

            if (usedTime < USE_DURATION) {
                // Повреждаем посох за прерывание
                stack.hurtAndBreak(DAMAGE_ON_INTERRUPT, player, (p) -> {
                    p.broadcastBreakEvent(player.getUsedItemHand());
                });

                player.displayClientMessage(
                        Component.literal("§cРитуал прерван!"),
                        true
                );
            }
        }
    }

    private void spawnChannelingParticles(ServerPlayer player, int ticksUsed) {
        // Вращающаяся пентаграмма на земле
        double pentagramRotation = (ticksUsed * 5);
        org.example.maniacrevolution.util.ParticleShapes.drawRotatingPentagram(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                2.5, // радиус
                20,  // точек на линию
                1
        );

        // Внешний магический круг
        if (ticksUsed % 3 == 0) {
            org.example.maniacrevolution.util.ParticleShapes.drawCircle(
                    player,
                    net.minecraft.core.particles.ParticleTypes.WITCH,
                    3.0, // радиус больше пентаграммы
                    60   // количество точек
            );
        }

        // Двойная спираль вокруг игрока (ДНК-эффект)
        double spiralOffset = ticksUsed * 0.1;
        org.example.maniacrevolution.util.ParticleShapes.drawDoubleHelix(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                0.7,  // радиус спирали
                3.0,  // высота
                30,   // количество точек
                spiralOffset
        );

        // Вихрь (торнадо) из темных частиц
        if (ticksUsed % 2 == 0) {
            org.example.maniacrevolution.util.ParticleShapes.drawVortex(
                    player,
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    1.5,  // радиус внизу
                    0.3,  // радиус вверху
                    4.0,  // высота
                    20,   // слоёв
                    8,    // точек на слой
                    spiralOffset * 2
            );
        }

        // Руны по кругу (появляются постепенно)
        int runeCount = Math.min(8, ticksUsed / 10);
        if (runeCount > 0 && ticksUsed % 5 == 0) {
            org.example.maniacrevolution.util.ParticleShapes.drawRuneCircle(
                    player,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    2.0,
                    runeCount
            );
        }

        // Столб света в центре (когда ритуал почти завершён)
        if (ticksUsed > 60) {
            org.example.maniacrevolution.util.ParticleShapes.drawLightBeam(
                    player,
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    5.0,  // высота
                    15    // плотность
            );
        }

        // Пульсирующая сфера (финальная фаза)
        if (ticksUsed > 80) {
            double sphereRadius = 1.5 + Math.sin(ticksUsed * 0.2) * 0.3;
            org.example.maniacrevolution.util.ParticleShapes.drawHollowSphere(
                    player,
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    sphereRadius,
                    10, // колец
                    15  // точек на кольцо
            );
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}