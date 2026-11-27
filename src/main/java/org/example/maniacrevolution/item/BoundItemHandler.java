package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.game.GameManager;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class BoundItemHandler {
    private BoundItemHandler() {}

    // Run after roulette awards cards and before the usual perk updates.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || GameManager.getPhaseValue() == 0) return;

        MinecraftServer server = event.getServer();
        RecoveryBatch batch = new RecoveryBatch();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            batch.checkInventory(player);
            batch.checkMenu(player.containerMenu, false);
            batch.checkCursor(player, player.containerMenu, false);
        }
        batch.returnItems(server, null);
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        RecoveryBatch batch = new RecoveryBatch();
        AbstractContainerMenu menu = event.getContainer();
        boolean lobby = GameManager.getPhaseValue() == 0;
        batch.checkMenu(menu, lobby);
        batch.checkCursor(player, menu, lobby);
        batch.returnItems(player.server, player);
    }

    private static final class RecoveryBatch {
        private final Map<UUID, EnumSet<Kind>> inventoryItems = new HashMap<>();
        private final ArrayDeque<Recovery> recoveries = new ArrayDeque<>();

        private void checkInventory(ServerPlayer player) {
            Inventory inventory = player.getInventory();
            UUID playerId = player.getUUID();
            EnumSet<Kind> held = EnumSet.noneOf(Kind.class);
            for (int slot = 0, size = inventory.getContainerSize(); slot < size; slot++) {
                ItemStack stack = inventory.getItem(slot);
                Kind kind = Kind.of(stack);
                if (kind == null) continue;

                UUID ownerId = kind.owner.apply(stack);
                if (ownerId == null) {
                    if (kind == Kind.SPROUT || kind == Kind.CARD) {
                        inventory.setItem(slot, ItemStack.EMPTY);
                    }
                } else if (!ownerId.equals(playerId)) {
                    recoveries.addLast(new Recovery(kind, ownerId, stack.copy()));
                    inventory.setItem(slot, ItemStack.EMPTY);
                } else if ((kind == Kind.SPROUT && held.contains(kind))
                        || (kind == Kind.CARD && !tickCard(player, stack))) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                } else if (kind.limitsReturns()) {
                    held.add(kind);
                }
            }
            if (!held.isEmpty()) inventoryItems.put(playerId, held);
        }

        private void checkMenu(AbstractContainerMenu menu, boolean lobby) {
            for (Slot slot : menu.slots) {
                if (slot.container instanceof Inventory) continue;
                ItemStack stack = slot.getItem();
                Kind kind = Kind.of(stack);
                if (kind == null) continue;

                if (!lobby || kind != Kind.CARD) {
                    UUID ownerId = kind.owner.apply(stack);
                    if (ownerId != null) {
                        recoveries.addLast(new Recovery(kind, ownerId, stack.copy()));
                    }
                }
                slot.set(ItemStack.EMPTY);
            }
        }

        private void checkCursor(ServerPlayer player, AbstractContainerMenu menu, boolean lobby) {
            ItemStack stack = menu.getCarried();
            Kind kind = Kind.of(stack);
            if (kind == null) return;

            UUID ownerId = kind.owner.apply(stack);
            boolean belongsToPlayer = player.getUUID().equals(ownerId);
            if (kind == Kind.PAINT && (ownerId == null || belongsToPlayer)) return;

            menu.setCarried(ItemStack.EMPTY);
            if (ownerId == null || (lobby && kind == Kind.CARD)) return;
            Recovery recovery = new Recovery(kind, ownerId, stack.copy());
            if (kind == Kind.SPROUT && belongsToPlayer) {
                // A sprout on its owner's cursor takes precedence over other recovered copies.
                recoveries.addFirst(recovery);
            } else {
                recoveries.addLast(recovery);
            }
        }

        private void returnItems(MinecraftServer server, ServerPlayer closingPlayer) {
            // Apply returns after the scan so a recovered stack cannot be visited twice.
            for (Recovery recovery : recoveries) {
                ServerPlayer owner = server.getPlayerList().getPlayer(recovery.ownerId());
                if (owner == null) continue;

                // Forge fires Close before the player switches back to the inventory menu.
                AbstractContainerMenu destination = owner == closingPlayer ? owner.inventoryMenu : owner.containerMenu;
                Kind kind = recovery.kind();
                EnumSet<Kind> held = null;
                if (kind.limitsReturns()) {
                    held = inventoryItems.computeIfAbsent(recovery.ownerId(), id ->
                            closingPlayer == null ? EnumSet.noneOf(Kind.class) : findHeldItems(owner));
                    if (held.contains(kind) || (kind == Kind.SPROUT
                            && SberSproutItem.belongsTo(destination.getCarried(), owner.getUUID()))) {
                        continue;
                    }
                }

                ItemStack stack = recovery.stack();
                if (!owner.getInventory().add(stack)) {
                    destination.setCarried(stack);
                } else if (held != null) {
                    held.add(kind);
                }
                if (kind.limitsReturns()) owner.inventoryMenu.broadcastChanges();
            }
        }
    }

    private static EnumSet<Kind> findHeldItems(ServerPlayer player) {
        EnumSet<Kind> held = EnumSet.noneOf(Kind.class);
        Inventory inventory = player.getInventory();
        for (int slot = 0, size = inventory.getContainerSize(); slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            Kind kind = Kind.of(stack);
            if (kind == Kind.CARD || ((kind == Kind.ORCHID || kind == Kind.SPROUT)
                    && player.getUUID().equals(kind.owner.apply(stack)))) {
                held.add(kind);
            }
        }
        return held;
    }

    private static boolean tickCard(ServerPlayer player, ItemStack stack) {
        int remaining = ColorCardItem.getRemainingTicks(stack) - 1;
        if (remaining <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.card_expired"), true);
            return false;
        }
        ColorCardItem.setRemainingTicks(stack, remaining);
        return true;
    }

    private record Recovery(Kind kind, UUID ownerId, ItemStack stack) {}

    private enum Kind {
        PAINT(PaintCanItem::getOwner),
        ORCHID(PinkOrchidItem::getOwner),
        SPROUT(SberSproutItem::getOwner),
        GLASSES(RoseColoredGlassesItem::getOwner),
        CARD(ColorCardItem::getOwner);

        private final Function<ItemStack, UUID> owner;

        Kind(Function<ItemStack, UUID> owner) {
            this.owner = owner;
        }

        private boolean limitsReturns() {
            return this == ORCHID || this == SPROUT || this == CARD;
        }

        private static Kind of(ItemStack stack) {
            if (stack.isEmpty()) return null;
            Item item = stack.getItem();
            if (item instanceof PaintCanItem) return PAINT;
            if (item instanceof PinkOrchidItem) return ORCHID;
            if (item instanceof SberSproutItem) return SPROUT;
            if (item instanceof RoseColoredGlassesItem) return GLASSES;
            if (item instanceof ColorCardItem) return CARD;
            return null;
        }
    }
}
