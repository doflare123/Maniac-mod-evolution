package org.example.maniacrevolution.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class PenaltySlotManager {

    private static final int PENALTY_SLOT_START = 6; // Слоты 6, 7, 8
    private static final int PENALTY_SLOT_END = 8;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Inventory inv = player.getInventory();

        // Применяем пенальти только на сервере
        if (!player.level().isClientSide()) {
            applySlownessPenalty(player, inv);
        }
    }

    /**
     * Применяет замедление в зависимости от количества предметов в пенальти-слотах
     */
    private static void applySlownessPenalty(Player player, Inventory inv) {
        int filledPenaltySlots = 0;

        // Считаем заполненные пенальти-слоты
        for (int i = PENALTY_SLOT_START; i <= PENALTY_SLOT_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                filledPenaltySlots++;
            }
        }

        // Убираем старый эффект замедления (если он от нас)
        if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance currentEffect = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            // Проверяем, что это наш эффект (длительность 25 тиков = ~1 секунда)
            if (currentEffect != null && currentEffect.getDuration() <= 25) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }

        // Применяем новый эффект, если есть предметы в пенальти-слотах
        if (filledPenaltySlots > 0) {
            // Уровень замедления = количество заполненных слотов - 1 (0, 1, 2)
            int slownessLevel = filledPenaltySlots - 1;

            // Применяем эффект на 25 тиков (~1 секунда), без частиц
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    25, // Длительность
                    slownessLevel, // Уровень (0 = I, 1 = II, 2 = III)
                    false, // ambient
                    false, // visible
                    false  // showIcon
            ));
        }
    }

    /**
     * Проверяет, находится ли игрок в пенальти-слоте
     */
    public static boolean isInPenaltySlot(Player player) {
        int selected = player.getInventory().selected;
        return selected >= PENALTY_SLOT_START && selected <= PENALTY_SLOT_END;
    }

    /**
     * Получает количество заполненных пенальти-слотов
     */
    public static int getFilledPenaltySlots(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();

        for (int i = PENALTY_SLOT_START; i <= PENALTY_SLOT_END; i++) {
            if (!inv.getItem(i).isEmpty()) {
                count++;
            }
        }

        return count;
    }
}