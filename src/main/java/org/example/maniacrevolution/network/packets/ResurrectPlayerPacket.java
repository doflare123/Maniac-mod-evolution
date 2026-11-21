package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.necromancer.NecromancerProvider;
import org.example.maniacrevolution.necromancer.NecromancerResurrection;

import java.util.UUID;
import java.util.function.Supplier;

public class ResurrectPlayerPacket {
    private final UUID ritualId;
    private final UUID targetPlayerUUID;

    public ResurrectPlayerPacket(UUID ritualId, UUID targetPlayerUUID) {
        this.ritualId = ritualId;
        this.targetPlayerUUID = targetPlayerUUID;
    }

    public ResurrectPlayerPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUUID());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(ritualId);
        buf.writeUUID(targetPlayerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            var necromancer = sender.getCapability(NecromancerProvider.NECROMANCER).orElse(null);
            int currentTick = sender.server.getTickCount();
            if (necromancer == null || !necromancer.hasPendingResurrection(ritualId, currentTick)
                    || !NecromancerResurrection.canCast(sender)) {
                sender.displayClientMessage(Component.literal("§cРитуал воскрешения недоступен."), false);
                return;
            }

            ServerPlayer target = sender.server.getPlayerList().getPlayer(targetPlayerUUID);
            if (target == null || !NecromancerResurrection.isEligibleTarget(target)) {
                sender.displayClientMessage(Component.literal("§cЭтого игрока уже нельзя воскресить."), false);
                return;
            }

            if (!necromancer.consumeResurrection(ritualId, currentTick)) return;
            resurrectPlayer(target, sender);

            // Сообщения
            sender.displayClientMessage(
                    Component.literal("§aВы воскресили " + target.getName().getString()),
                    false
            );

            target.displayClientMessage(
                    Component.literal("§5Вы были воскрешены некромантом!"),
                    false
            );
        });
        return true;
    }

    private void resurrectPlayer(ServerPlayer target, ServerPlayer necromancer) {
        // Меняем режим игры
        target.setGameMode(GameType.ADVENTURE);

        // Телепортируем к некроманту
        target.teleportTo(
                necromancer.serverLevel(),
                necromancer.getX(),
                necromancer.getY(),
                necromancer.getZ(),
                necromancer.getYRot(),
                necromancer.getXRot()
        );

        // Устанавливаем здоровье (половина)
        target.setHealth(target.getMaxHealth() * 0.5f);

        // Добавляем эффект замедления 2
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                99999, // 30 секунд
                0, // Уровень 2
                false,
                false
        ));

        // Добавляем эффект слабости для "зомби" ощущения
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                99999,
                0,
                false,
                false
        ));

        // Добавляем эффект грации
        target.addEffect(new MobEffectInstance(
                MobEffects.DOLPHINS_GRACE,
                99999,
                0,
                false,
                false
        ));

        // Добавляем эффект дыхания
        target.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING,
                99999,
                0,
                false,
                false
        ));

        // Добавляем эффект дыхания
        target.addEffect(new MobEffectInstance(
                MobEffects.SATURATION,
                99999,
                255,
                false,
                false
        ));

        // Звуки и частицы
        target.serverLevel().playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CURE,
                SoundSource.PLAYERS,
                1.0F,
                0.8F
        );

        // Частицы воскрешения
        spawnResurrectionParticles(target);
    }

    private void spawnResurrectionParticles(ServerPlayer player) {
        // Взрывная волна из частиц душ
        org.example.maniacrevolution.util.ParticleShapes.drawExplosionRing(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                2.0,  // радиус
                50    // количество частиц
        );

        // Большая сфера из душ, расширяющаяся от игрока
        org.example.maniacrevolution.util.ParticleShapes.drawSphere(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                1.5,  // радиус
                100   // плотность
        );

        // Спиральный вихрь вокруг воскресшего игрока
        for (int i = 0; i < 3; i++) {
            org.example.maniacrevolution.util.ParticleShapes.drawSpiral(
                    player,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    2.0,  // максимальный радиус
                    3.0,  // высота
                    40,   // точек
                    i * Math.PI * 2 / 3  // смещение для трёх спиралей
            );
        }

        // Столб света снизу вверх
        org.example.maniacrevolution.util.ParticleShapes.drawLightBeam(
                player,
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                5.0,  // высота
                20    // плотность
        );

        // Пентаграмма под ногами
        org.example.maniacrevolution.util.ParticleShapes.drawPentagram(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                1.5,  // радиус
                15    // точек на линию
        );

        // Дым вокруг
        for (int i = 0; i < 30; i++) {
            double d0 = player.getRandom().nextGaussian() * 0.02D;
            double d1 = player.getRandom().nextGaussian() * 0.02D;
            double d2 = player.getRandom().nextGaussian() * 0.02D;

            player.serverLevel().sendParticles(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    player.getX() + (player.getRandom().nextDouble() - 0.5D) * player.getBbWidth(),
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D) * player.getBbWidth(),
                    1, d0, d1, d2, 0.05
            );
        }
    }
}
