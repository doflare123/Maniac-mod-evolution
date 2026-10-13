package org.example.maniacrevolution.network.packets;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Regression checks for periodic snapshot suppression and charge clock refreshes. */
public final class PlayerDataSyncTest {
    public static void main(String[] args) {
        int[] values = {1, 0, 100, 0, 0, 2, 0, 200, 0, 0, 0, 0};
        SyncPlayerDataPacket idle = snapshot(values, "perk", "preset", "cosmetic", false);
        check(idle.needsPeriodicSync(null), "Initial synchronization");
        check(!snapshot(values, "perk", "preset", "cosmetic", false).needsPeriodicSync(idle),
                "Identical independently constructed snapshots must be suppressed");
        for (int field = 0; field < values.length; field++) {
            int[] changed = values.clone();
            changed[field]++;
            check(snapshot(changed, "perk", "preset", "cosmetic", false).needsPeriodicSync(idle),
                    "Changed numeric field " + field);
        }
        check(snapshot(values, "other", "preset", "cosmetic", false).needsPeriodicSync(idle), "Perk change");
        check(snapshot(values, "perk", "other", "cosmetic", false).needsPeriodicSync(idle), "Preset change");
        check(snapshot(values, "perk", "preset", "other", false).needsPeriodicSync(idle), "Purchase change");
        check(snapshot(values, "perk", "preset", "cosmetic", true).needsPeriodicSync(idle), "Enabled cosmetic change");

        int[] charged = values.clone();
        charged[9] = 1;
        charged[10] = 100;
        charged[11] = 200;
        SyncPlayerDataPacket active = snapshot(charged, "perk", "preset", "cosmetic", false);
        check(active.needsPeriodicSync(active), "Paused active charge must refresh the client clock");
        check(idle.needsPeriodicSync(active), "Final charge expiration must be sent");
        int[] cooldown = values.clone();
        cooldown[6] = 10;
        check(idle.needsPeriodicSync(snapshot(cooldown, "perk", "preset", "cosmetic", false)),
                "Final cooldown expiration must be sent");

        List<String> ids = new ArrayList<>(List.of("first"));
        var preset = new SyncPlayerDataPacket.PresetSyncData("saved", ids);
        ids.add("second");
        check(preset.perkIds().equals(List.of("first")), "Cached presets must be immutable snapshots");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            idle.encode(buffer);
            check(!SyncPlayerDataPacket.decode(buffer).needsPeriodicSync(idle), "Wire round trip");
            check(!buffer.isReadable(), "Round trip must consume the complete payload");
        } finally {
            buffer.release();
        }
        System.out.println("PlayerDataSyncTest: all checks passed");
    }

    private static SyncPlayerDataPacket snapshot(int[] values, String perk, String preset,
                                                 String cosmetic, boolean enabled) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (int i = 0; i < 6; i++) buffer.writeInt(values[i]);
            buffer.writeInt(1);
            buffer.writeUtf(perk);
            for (int i = 6; i < 9; i++) buffer.writeInt(values[i]);
            buffer.writeFloat(5.0f);
            for (int i = 9; i < 12; i++) buffer.writeInt(values[i]);
            buffer.writeInt(1);
            buffer.writeUtf(preset);
            buffer.writeInt(1);
            buffer.writeUtf(perk);
            buffer.writeInt(1);
            buffer.writeUtf(cosmetic);
            buffer.writeInt(enabled ? 1 : 0);
            if (enabled) buffer.writeUtf(cosmetic);
            return SyncPlayerDataPacket.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
