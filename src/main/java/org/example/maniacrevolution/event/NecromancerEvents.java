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

//    @SubscribeEvent
//    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
//        if (event.getObject() instanceof Player) {
//            if (!event.getObject().getCapability(NecromancerProvider.NECROMANCER).isPresent()) {
//                event.addCapability(
//                        new ResourceLocation(Maniacrev.MODID, "necromancer"),
//                        new NecromancerProvider()
//                );
//            }
//        }
//    }

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
            // ПРОВЕРКА 1: Игрок должен быть некромантом (SurvivorClass = 8)
            net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
            net.minecraft.world.scores.Objective classObjective = scoreboard.getObjective("SurvivorClass");

            if (classObjective == null) {
                return; // Нет scoreboard - не некромант
            }

            net.minecraft.world.scores.Score classScore = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), classObjective);
            if (classScore.getScore() != 8) {
                return; // Не некромант - пассивка не работает
            }

            // ПРОВЕРКА 2: Игрок должен быть в команде survivors
            net.minecraft.world.scores.PlayerTeam team = (net.minecraft.world.scores.PlayerTeam) player.getTeam();
            if (team == null || !team.getName().equalsIgnoreCase("survivors")) {
                return; // Не в команде survivors
            }

            // ПРОВЕРКА 3: Игрок должен быть в режиме приключений
            if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE) {
                return; // Не в режиме приключений
            }

            // ПРОВЕРКА 4: Игрок должен носить ПОЛНЫЙ сет брони некроманта
            if (!hasFullNecromancerSet(player)) {
                return; // Нет полного сета - пассивка не работает
            }

            // ПРОВЕРКА 5: Проверяем capability - есть ли защита
            player.getCapability(NecromancerProvider.NECROMANCER).ifPresent(necroData -> {
                if (necroData.hasPassiveProtection()) {
                    // Отменяем смерть
                    event.setCanceled(true);

                    // Восстанавливаем здоровье до 0.5 сердца
                    player.setHealth(1.0f);

                    // Используем пассивку
                    necroData.usePassiveProtection();

                    // ЛОМАЕМ броню некроманта
                    damageNecromancerArmor(player);

                    // Эффекты
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    // Частицы
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

    /**
     * Проверяет, носит ли игрок полный сет брони некроманта
     */
    private static boolean hasFullNecromancerSet(ServerPlayer player) {
        int necroArmorPieces = 0;

        for (net.minecraft.world.item.ItemStack armorSlot : player.getArmorSlots()) {
            if (armorSlot.getItem() instanceof org.example.maniacrevolution.item.armor.NecromancerArmorItem) {
                necroArmorPieces++;
            }
        }

        return necroArmorPieces == 4; // Все 4 части брони
    }

    /**
     * Ломает броню некроманта при использовании пассивки
     */
    private static void damageNecromancerArmor(ServerPlayer player) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                net.minecraft.world.item.ItemStack armorPiece = player.getItemBySlot(slot);
                if (armorPiece.getItem() instanceof org.example.maniacrevolution.item.armor.NecromancerArmorItem necroArmor) {
                    necroArmor.onPassiveActivated(player, armorPiece);
                }
            }
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