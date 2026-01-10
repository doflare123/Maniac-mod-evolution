package org.example.maniacrevolution.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.armor.NecromancerArmorItem;
import org.example.maniacrevolution.necromancer.NecromancerProvider;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class NecromancerEventsUpdated {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(NecromancerProvider.NECROMANCER).isPresent()) {
                event.addCapability(
                        new ResourceLocation(Maniacrev.MODID, "necromancer"),
                        new NecromancerProvider()
                );
            }
        }
    }

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

                    // Повреждаем броню некроманта
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

    private static void damageNecromancerArmor(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (armorPiece.getItem() instanceof NecromancerArmorItem necroArmor) {
                    necroArmor.onPassiveActivated(player, armorPiece);
                }
            }
        }
    }

    private static void spawnDeathProtectionParticles(ServerPlayer player) {
        // Массивная защитная сфера
        org.example.maniacrevolution.util.ParticleShapes.drawSphere(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                2.5,  // радиус
                150   // большая плотность
        );

        // Кольцо тёмного пламени на земле
        org.example.maniacrevolution.util.ParticleShapes.drawCircle(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                3.0,  // большой радиус
                60    // много точек
        );

        // Пентаграмма защиты
        org.example.maniacrevolution.util.ParticleShapes.drawPentagram(
                player,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                2.5,  // радиус
                20    // точек на линию
        );

        // Множественные взрывные кольца на разных высотах
        for (int i = 0; i < 3; i++) {
            double heightOffset = i * 0.5;
            ServerPlayer offsetPlayer = player; // Используем того же игрока
            org.example.maniacrevolution.util.ParticleShapes.drawExplosionRing(
                    player,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    2.0 + i * 0.3,  // увеличивающийся радиус
                    30
            );
        }

        // Спиральные столбы вокруг
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            double offsetX = Math.cos(angle) * 2.0;
            double offsetZ = Math.sin(angle) * 2.0;

            // Создаём спираль со смещением
            org.example.maniacrevolution.util.ParticleShapes.drawSpiral(
                    player,
                    net.minecraft.core.particles.ParticleTypes.WITCH,
                    0.5,  // радиус спирали
                    3.0,  // высота
                    30,   // точек
                    i * Math.PI / 2  // смещение для каждой спирали
            );
        }
    }

    /**
     * Отслеживание надевания/снятия брони для применения/снятия бонусов сета
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            ItemStack from = event.getFrom();
            ItemStack to = event.getTo();

            boolean wasNecroArmor = from.getItem() instanceof NecromancerArmorItem;
            boolean isNecroArmor = to.getItem() instanceof NecromancerArmorItem;

            if (wasNecroArmor != isNecroArmor) {
                // Проверяем, есть ли полный сет
                if (hasFullNecromancerSet(player)) {
                    NecromancerArmorItem.applySetBonus(player);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§5✦ Сила некроманта пробуждена ✦"),
                            true
                    );
                } else {
                    NecromancerArmorItem.removeSetBonus(player);
                }
            }
        }
    }

    /**
     * Периодическая проверка пассивки и отображение статуса
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Каждые 5 секунд проверяем возможность восстановления пассивки
            if (player.tickCount % 100 == 0) {
                player.getCapability(NecromancerProvider.NECROMANCER).ifPresent(necroData -> {
                    if (hasFullNecromancerSet(player) && necroData.canRestorePassive()) {
                        necroData.restorePassiveProtection();
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§a✦ Пассивная защита восстановлена ✦"),
                                true
                        );
                    }
                });
            }
        }
    }

    private static boolean hasFullNecromancerSet(Player player) {
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (!(armorSlot.getItem() instanceof NecromancerArmorItem)) {
                return false;
            }
        }
        return true;
    }
}