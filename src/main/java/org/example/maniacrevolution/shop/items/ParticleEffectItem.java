package org.example.maniacrevolution.shop.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.shop.ShopCategory;
import org.example.maniacrevolution.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

public class ParticleEffectItem extends ShopItem {
    private final String particleType;

    public ParticleEffectItem(String id, String particleType, int price) {
        super(id, price, ShopCategory.COSMETICS, true);
        this.particleType = particleType;
    }

    @Override
    protected void onPurchase(ServerPlayer player, PlayerData data) {
        player.displayClientMessage(
                Component.literal("§dВы приобрели эффект частиц: " + particleType), false);
    }

    @Override
    public List<Component> getTooltip(PlayerData data) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(getDescription().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Эффект: " + particleType).withStyle(ChatFormatting.DARK_GRAY));

        if (data.hasPurchased(id)) {
            tooltip.add(Component.literal("✓ Куплено").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Цена: " + price + " монет").withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}

