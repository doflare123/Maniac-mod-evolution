package org.example.maniacrevolution.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class PenaltyItemBlocker {

    private static final String BLOCK_MESSAGE = "§c⚠ Этот слот только для хранения!";

    /**
     * Блокирует правый клик (использование предметов)
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();

        if (PenaltySlotManager.isInPenaltySlot(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Блокирует правый клик по блокам
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (PenaltySlotManager.isInPenaltySlot(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Блокирует левый клик (атаки)
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();

        if (PenaltySlotManager.isInPenaltySlot(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Блокирует атаки по сущностям
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (PenaltySlotManager.isInPenaltySlot(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Блокирует пустой клик
     */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();

        if (PenaltySlotManager.isInPenaltySlot(player)) {
            event.setCanceled(true);
        }
    }
}