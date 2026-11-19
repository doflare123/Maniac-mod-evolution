package org.example.maniacrevolution.nightmare;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.example.maniacrevolution.network.packets.KeeperVisionPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class KeeperVisionDataTest {
    static void run() {
        UUID id = UUID.randomUUID();
        check(new KeeperSoulState(id, Float.NaN, false).sanity() == 1, "Malformed sanity stays safe");
        check(new KeeperSoulState(id, -1, false).sanity() == 0, "Sanity lower bound");
        check(new KeeperSoulState(id, 2, false).sanity() == 1, "Sanity upper bound");
        var souls = new ArrayList<>(List.of(new KeeperSoulState(id, .35F, true)));
        ClientKeeperVisionData.update(souls);
        souls.clear();
        check(ClientKeeperVisionData.souls().size() == 1, "Snapshot owns its list");
        for (int i = 0; i < KeeperSoulState.TIMEOUT_TICKS - 1; i++) ClientKeeperVisionData.tick();
        check(ClientKeeperVisionData.souls().get(0).feeding(), "Feeding survives ordinary refresh intervals");
        ClientKeeperVisionData.tick();
        check(ClientKeeperVisionData.souls().isEmpty(), "A missing server expires feeding and souls");
        ClientKeeperVisionData.update(List.of(new KeeperSoulState(id, .2F, true)));
        ClientKeeperVisionData.update(List.of());
        check(ClientKeeperVisionData.souls().isEmpty(), "Losing every target clears the old snapshot immediately");
        var packet = new KeeperVisionPacket(List.of(new KeeperSoulState(id, .35F, true)));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            KeeperVisionPacket.encode(packet, buffer);
            check(KeeperVisionPacket.decode(buffer).equals(packet), "Wire round trip preserves target, sanity and feeding");
            buffer.clear();
            buffer.writeVarInt(KeeperSoulState.MAX_SOULS + 1);
            boolean rejected = false;
            try { KeeperVisionPacket.decode(buffer); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Wire allocation has a hard bound");
        } finally { buffer.release(); }
        ClientKeeperVisionData.clear();
        System.out.println("Keeper vision: bounds, snapshot lifetime and packet round trip passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
