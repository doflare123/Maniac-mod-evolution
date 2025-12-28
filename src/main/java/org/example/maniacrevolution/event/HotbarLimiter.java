package org.example.maniacrevolution.event;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class HotbarLimiter {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        // Ограничиваем выбор слота до 0-5
        if (player.getInventory().selected > 5) {
            player.getInventory().selected = 0; // Возврат на первый слот
        }

        // Перемещаем предметы из слотов 6, 7, 8 в основной инвентарь
        if (!player.level().isClientSide() && event.phase == TickEvent.Phase.END) {
            Inventory inv = player.getInventory();

            for (int i = 6; i <= 8; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    // Пытаемся добавить в основной инвентарь (слоты 9-35)
                    boolean added = false;
                    for (int j = 9; j < 36; j++) {
                        ItemStack slotStack = inv.getItem(j);
                        if (slotStack.isEmpty()) {
                            inv.setItem(j, stack.copy());
                            inv.setItem(i, ItemStack.EMPTY);
                            added = true;
                            break;
                        } else if (ItemStack.isSameItemSameTags(slotStack, stack) &&
                                slotStack.getCount() < slotStack.getMaxStackSize()) {
                            int space = slotStack.getMaxStackSize() - slotStack.getCount();
                            int toMove = Math.min(space, stack.getCount());
                            slotStack.grow(toMove);
                            stack.shrink(toMove);
                            if (stack.isEmpty()) {
                                inv.setItem(i, ItemStack.EMPTY);
                                added = true;
                                break;
                            }
                        }
                    }

                    // Если не поместилось - выбрасываем
                    if (!added && !stack.isEmpty()) {
                        player.drop(stack, false);
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }
}