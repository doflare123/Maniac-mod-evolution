package org.example.maniacrevolution.helsing;

import net.minecraft.world.item.Item;
import org.example.maniacrevolution.ModItems;

public final class HelsingBoltItem extends Item {
    public enum Kind {
        IRON, RUNE, SILVER, ENCHANTED;
        public Item item() {
            return switch (this) {
                case IRON -> ModItems.HELSING_IRON_BOLT.get();
                case RUNE -> ModItems.HELSING_RUNE_BOLT.get();
                case SILVER -> ModItems.HELSING_SILVER_BOLT.get();
                case ENCHANTED -> ModItems.HELSING_ENCHANTED_BOLT.get();
            };
        }
        public String key() { return "item.maniacrev.helsing_" + name().toLowerCase(java.util.Locale.ROOT) + "_bolt"; }
    }
    public final Kind kind;
    public HelsingBoltItem(Kind kind) { super(new Properties().stacksTo(64)); this.kind = kind; }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                          java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        Object[] values = switch (kind) {
            case IRON -> new Object[]{HelsingConfig.BOLT_DAMAGE.get(), HelsingConfig.IRON_DURATION.get() / 20.0};
            case RUNE -> new Object[]{HelsingConfig.BOLT_DAMAGE.get(), HelsingConfig.RUNE_MULTIPLIER.get(), HelsingConfig.RUNE_DURATION.get() / 20.0};
            case SILVER -> new Object[]{HelsingConfig.BOLT_DAMAGE.get() * 2.0, HelsingConfig.BOLT_DAMAGE.get() * 1.5};
            case ENCHANTED -> new Object[]{HelsingConfig.BOLT_DAMAGE.get(), Math.round(HelsingConfig.DAMAGE_REDUCTION.get() * 100), HelsingConfig.BOLT_WEAKNESS_DURATION.get() / 20.0};
        };
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.maniacrev.helsing.bolt." + kind.name().toLowerCase(java.util.Locale.ROOT), values));
    }
}
