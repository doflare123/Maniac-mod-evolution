package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.GameRulesConfig;

import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ItemDropEvents {

    /**
     * Обрабатывает ручной выброс предмета игроком (клавиша Q)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (!GameRulesConfig.isItemDropAllowed()) {
            if (event.getPlayer().isCreative() || event.getPlayer().isSpectator()) {
                return;
            }

            // Отменяем дроп
            event.setCanceled(true);

            // Возвращаем предмет в инвентарь
            ItemEntity itemEntity = event.getEntity();
            ItemStack stack = itemEntity.getItem();

            if (!event.getPlayer().getInventory().add(stack)) {
                event.getPlayer().containerMenu.setCarried(stack);
            }
        }
    }

    /**
     * Обрабатывает ТОЛЬКО спавн предметов на земле (не в инвентаре!)
     * Например, когда инвентарь полон при /give
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemEntitySpawn(EntityJoinLevelEvent event) {
        // Только на сервере
        if (event.getLevel().isClientSide()) return;

        // Только ItemEntity
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        // Если дроп разрешен - пропускаем
        if (GameRulesConfig.isItemDropAllowed()) return;

        // Проверяем владельца предмета
        UUID ownerUUID = Objects.requireNonNull(itemEntity.getOwner()).getUUID();
        ServerPlayer owner = null;

        if (ownerUUID != null && event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        }

        // Если владелец не найден - ищем ближайшего
        if (owner == null) {
            owner = findNearestNonCreativePlayer(itemEntity);
        }

        // Если нашли владельца и он не в креативе
        if (owner != null && !owner.isCreative() && !owner.isSpectator()) {

            System.out.println("[ItemDrop] Blocked item spawn: " +
                    itemEntity.getItem().getDisplayName().getString() +
                    " x" + itemEntity.getItem().getCount() +
                    " for " + owner.getName().getString());

            // Отменяем спавн
            event.setCanceled(true);

            // Пытаемся добавить в инвентарь
            ItemStack stack = itemEntity.getItem();

            if (!owner.getInventory().add(stack)) {
                // Инвентарь полон - уничтожаем
                owner.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§cИнвентарь полон! Предмет уничтожен: " + stack.getDisplayName().getString()
                        ),
                        true
                );
            }
        }
    }

    private static ServerPlayer findNearestNonCreativePlayer(ItemEntity itemEntity) {
        if (!(itemEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        double searchRadius = 3.0; // Уменьшен радиус до 3 блоков
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            double distance = player.distanceToSqr(itemEntity);
            if (distance < nearestDistance && distance < searchRadius * searchRadius) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}