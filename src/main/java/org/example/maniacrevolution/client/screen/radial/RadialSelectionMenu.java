package org.example.maniacrevolution.client.screen.radial;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Class-specific data and actions; rendering and input belong to the shared screen. */
public interface RadialSelectionMenu {
    record Entry(ItemStack icon, Component label, int color, Component badge, boolean available) {}
    Component title();
    KeyMapping binding();
    List<Entry> entries();
    boolean valid();
    void select(int index);
    default int initialSelection() { return 0; }
    default List<Component> details(int index) { return List.of(); }
    default List<Component> footer() { return List.of(); }
}
