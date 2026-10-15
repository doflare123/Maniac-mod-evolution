package org.example.maniacrevolution.techies.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.client.screen.radial.*;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.mana.ClientManaData;
import org.example.maniacrevolution.techies.*;
import java.util.List;

public final class TechiesRadialScreen extends RadialSelectionScreen {
    public TechiesRadialScreen() { super(new Mines()); }

    private static final class Mines implements RadialSelectionMenu {
        private static final int[] COLORS = {0xF46C87, 0x78CB95, 0x80AEF4};
        public Component title() { return Component.translatable("screen.maniacrev.techies.mines"); }
        public KeyMapping binding() { return ModKeybinds.ACTIVATE_ARMOR_ABILITY; }
        public boolean valid() {
            return TechiesClient.eligible() && TechiesClient.selected < 0 && TechiesClient.installStart < 0
                    && !org.example.maniacrevolution.client.QTEClientHandler.isQTEActive();
        }
        public List<Entry> entries() {
            var entries = new java.util.ArrayList<Entry>();
            for (var type : MineType.values()) {
                long ticks = Math.max(0, TechiesClient.cooldowns[type.ordinal()] - (long) TechiesClient.time());
                boolean mana = ClientManaData.getMana() >= type.cost;
                Component badge = ticks > 0
                        ? Component.translatable("screen.maniacrev.radial.cooldown", (int) Math.ceil(ticks / 20.0))
                        : mana ? Component.empty() : Component.translatable("screen.maniacrev.radial.no_mana");
                entries.add(new Entry(new ItemStack(type.item()), Component.translatable("item.maniacrev.techies_mine_" + type.color),
                        COLORS[type.ordinal()], badge, ticks == 0 && mana));
            }
            return entries;
        }
        public List<Component> details(int index) {
            var type = MineType.values()[index];
            return List.of(Component.translatable("screen.maniacrev.radial.mana", type.cost),
                    Component.translatable("screen.maniacrev.radial.install", type.install / 20.0));
        }
        public void select(int index) { TechiesClient.request(TechiesActionPacket.SELECT, index); }
    }
}
