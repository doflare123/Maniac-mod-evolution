package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.BudDispatcherClientData;
import org.example.maniacrevolution.perk.perks.maniac.BudDispatcherPerk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Синхронизирует назначенные цветы и их дискретные стадии увядания. */
public class BudDispatcherPacket {
    private static final int MAX_NETWORK_ENTRIES = BudDispatcherPerk.COMPUTER_COUNT;

    private final boolean replaceState;
    private final boolean openScreen;
    private final List<Entry> entries;

    private BudDispatcherPacket(boolean replaceState, boolean openScreen, List<Entry> entries) {
        this.replaceState = replaceState;
        this.openScreen = openScreen;
        this.entries = List.copyOf(entries);
    }

    public static BudDispatcherPacket full(List<Entry> entries, boolean openScreen) {
        return new BudDispatcherPacket(true, openScreen, entries);
    }

    public static BudDispatcherPacket update(int computerId, int flowerIndex, int stage) {
        return new BudDispatcherPacket(false, false,
                List.of(new Entry(computerId, flowerIndex, stage)));
    }

    public static void encode(BudDispatcherPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.replaceState);
        buffer.writeBoolean(packet.openScreen);
        int count = Math.min(MAX_NETWORK_ENTRIES, packet.entries.size());
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            Entry entry = packet.entries.get(index);
            buffer.writeVarInt(entry.computerId());
            buffer.writeByte(entry.flowerIndex());
            buffer.writeByte(entry.stage());
        }
    }

    public static BudDispatcherPacket decode(FriendlyByteBuf buffer) {
        boolean replaceState = buffer.readBoolean();
        boolean openScreen = buffer.readBoolean();
        int encodedCount = Math.max(0, buffer.readVarInt());
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < encodedCount; index++) {
            Entry entry = new Entry(buffer.readVarInt(), buffer.readUnsignedByte(),
                    buffer.readUnsignedByte());
            if (index < MAX_NETWORK_ENTRIES) {
                entries.add(entry);
            }
        }
        return new BudDispatcherPacket(replaceState, openScreen, entries);
    }

    public static void handle(BudDispatcherPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BudDispatcherClientData.apply(packet.entries, packet.replaceState,
                        packet.openScreen)));
        context.setPacketHandled(true);
    }

    public record Entry(int computerId, int flowerIndex, int stage) {
    }
}
