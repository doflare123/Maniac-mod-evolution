package org.example.maniacrevolution.helsing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.client.screen.radial.*;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import java.util.List;

public final class HelsingBoltWheelScreen extends RadialSelectionScreen {
    public HelsingBoltWheelScreen(InteractionHand hand) { super(new Bolts(hand)); }

    private record Bolts(InteractionHand hand) implements RadialSelectionMenu {
        private static final int[] COLORS = {0xA3B4CC, 0xBA8DF0, 0xE5F6FF, 0xF46C87};
        private ItemStack crossbow() {
            var player = Minecraft.getInstance().player;
            return player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        }
        public Component title() { return Component.translatable("screen.maniacrev.helsing.bolts"); }
        public KeyMapping binding() { return ModKeybinds.HELSING_SWITCH_BOLT; }
        public int initialSelection() { return HelsingCrossbowItem.selected(crossbow()).ordinal(); }
        public boolean valid() {
            var player = Minecraft.getInstance().player;
            return player != null && HelsingManager.isSurvivor(player) && !player.isUsingItem()
                    && org.example.maniacrevolution.data.ClientPlayerData.isSurvivorClass(HelsingConfig.CLASS_ID)
                    && crossbow().getItem() instanceof HelsingCrossbowItem;
        }
        public List<Entry> entries() {
            var player = Minecraft.getInstance().player;
            var entries = new java.util.ArrayList<Entry>();
            for (var kind : HelsingBoltItem.Kind.values()) {
                int count = 0;
                if (player != null)
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        var stack = player.getInventory().getItem(i);
                        if (stack.is(kind.item())) count += stack.getCount();
                    }
                entries.add(new Entry(new ItemStack(kind.item()), Component.translatable(kind.key()),
                        COLORS[kind.ordinal()], Component.literal(Integer.toString(count)), count > 0));
            }
            return entries;
        }
        public void select(int index) { ModNetworking.sendToServer(new SelectHelsingBoltPacket(index, hand)); }
        public List<Component> footer() {
            var bow = crossbow();
            return HelsingCrossbowItem.loaded(bow)
                    ? List.of(Component.translatable("screen.maniacrev.helsing.next_load",
                            Component.translatable(HelsingCrossbowItem.loadedKind(bow).key()))) : List.of();
        }
    }
}
