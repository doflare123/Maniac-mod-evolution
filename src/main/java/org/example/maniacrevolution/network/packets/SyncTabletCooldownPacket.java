package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class SyncTabletCooldownPacket {

    private final int cooldownSeconds;

    public SyncTabletCooldownPacket(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(cooldownSeconds);
    }

    public static SyncTabletCooldownPacket decode(FriendlyByteBuf buffer) {
        return new SyncTabletCooldownPacket(buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientOnlyExecutor.showTabletCooldown(cooldownSeconds));
        context.setPacketHandled(true);
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}
