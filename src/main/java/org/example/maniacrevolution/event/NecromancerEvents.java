package org.example.maniacrevolution.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.necromancer.NecromancerProvider;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class NecromancerEvents {

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(NecromancerProvider.NECROMANCER).ifPresent(oldStore -> {
            event.getEntity().getCapability(NecromancerProvider.NECROMANCER).ifPresent(newStore -> {
                newStore.copyFrom(oldStore);
            });
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(NecromancerProvider.NECROMANCER).ifPresent(necroData -> {
                if (necroData.hasPassiveProtection()) {
                    // Отменяем смерть
                    event.setCanceled(true);

                    // Восстанавливаем здоровье до 0.5 сердца
                    player.setHealth(1.0f);

                    // Используем пассивку
                    necroData.usePassiveProtection();

                    // Эффекты
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    // Можно добавить частицы
                    spawnDeathProtectionParticles(player);

                    // Сообщение игроку
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§5§lНекромантская защита активирована!"),
                            true
                    );
                }
            });
        }
    }

    private static void spawnDeathProtectionParticles(ServerPlayer player) {
        // Защитная сфера вокруг игрока
        org.example.maniacrevolution.util.ParticleShapes.drawHollowSphere(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                2.0,  // радиус
                12,   // колец
                20    // точек на кольцо
        );

        // Взрывное кольцо на земле
        org.example.maniacrevolution.util.ParticleShapes.drawExplosionRing(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                2.5,  // радиус
                40    // точек
        );

        // Столб душ вокруг игрока
        org.example.maniacrevolution.util.ParticleShapes.drawLightBeam(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                3.0,  // высота
                15    // плотность
        );

        // Вихрь защитных частиц
        org.example.maniacrevolution.util.ParticleShapes.drawVortex(
                player,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                1.5,  // радиус внизу
                0.5,  // радиус вверху
                2.5,  // высота
                15,   // слоёв
                12,   // точек на слой
                0.0   // без вращения
        );
    }
}