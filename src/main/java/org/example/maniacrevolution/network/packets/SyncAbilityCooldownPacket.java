package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.client.ClientAbilityData;

import java.util.function.Supplier;

public class SyncAbilityCooldownPacket {
    private final ResourceLocation itemId;
    private final int cooldownSeconds;
    private final int maxCooldownSeconds;
    private final int remainingDuration; // НОВОЕ

    public SyncAbilityCooldownPacket(Item item, int cooldownSeconds, int maxCooldownSeconds, int remainingDuration) {
        this.itemId = ForgeRegistries.ITEMS.getKey(item);
        this.cooldownSeconds = cooldownSeconds;
        this.maxCooldownSeconds = maxCooldownSeconds;
        this.remainingDuration = remainingDuration;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(itemId);
        buffer.writeInt(cooldownSeconds);
        buffer.writeInt(maxCooldownSeconds);
        buffer.writeInt(remainingDuration);
    }

    public static SyncAbilityCooldownPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation itemId = buffer.readResourceLocation();
        int cooldownSeconds = buffer.readInt();
        int maxCooldownSeconds = buffer.readInt();
        int remainingDuration = buffer.readInt();

        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return new SyncAbilityCooldownPacket(item, cooldownSeconds, maxCooldownSeconds, remainingDuration);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                ClientAbilityData.setAbilityData(item, cooldownSeconds, maxCooldownSeconds, remainingDuration);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}