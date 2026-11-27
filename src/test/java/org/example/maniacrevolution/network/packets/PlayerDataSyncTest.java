package org.example.maniacrevolution.network.packets;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.perk.ChargedPerk;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerDataSyncTest {
    public static void main(String[] args) {
        PerkRegistry.register(new ChargedPerk(new Perk.Builder("test_charge")
                .type(PerkType.CHARGED).cooldown(10).manaCost(5.0f), 10, 2, 2) {});

        PlayerData data = loadPlayer();
        SyncPlayerDataPacket active = SyncPlayerDataPacket.periodicUpdate(data, null);
        check(active != null, "Initial synchronization");
        check(SyncPlayerDataPacket.periodicUpdate(data, active) == active,
                "Paused active charge must resend the existing snapshot to refresh the client clock");
        verifyWirePayload(active);

        data.onGameStart(null);
        SyncPlayerDataPacket idle = expectChanged(data, active, "Charge removal");
        check(SyncPlayerDataPacket.periodicUpdate(data, idle) == null, "Idle snapshot must be suppressed");

        var perk = data.getPerkInstance("test_charge");
        perk.setCooldownRemaining(10);
        SyncPlayerDataPacket cooldown = expectChanged(data, idle, "Cooldown start");
        check(SyncPlayerDataPacket.periodicUpdate(data, cooldown) == null, "Unchanged cooldown must be suppressed");
        perk.setCooldownRemaining(9);
        cooldown = expectChanged(data, cooldown, "Cooldown tick");
        perk.resetCooldown();
        idle = expectChanged(data, cooldown, "Cooldown expiration");

        data.getPresets().get(0).setName("renamed");
        idle = expectChanged(data, idle, "Preset rename");
        data.increaseMaxPresets();
        idle = expectChanged(data, idle, "Preset limit");
        data.deletePreset(0);
        idle = expectChanged(data, idle, "Preset deletion");

        var cosmetics = data.getCosmeticData();
        cosmetics.addPurchase("second");
        idle = expectChanged(data, idle, "Purchase change");
        cosmetics.addPurchase("second");
        check(SyncPlayerDataPacket.periodicUpdate(data, idle) == null, "Duplicate purchase must be suppressed");
        cosmetics.toggleEnabled("first");
        idle = expectChanged(data, idle, "Enabled cosmetic change");

        data.clearPerks(null);
        idle = expectChanged(data, idle, "Last selected perk removed");
        data.addExperience(100);
        idle = expectChanged(data, idle, "Level and experience changed with no perks selected");
        data.addCoins(1);
        idle = expectChanged(data, idle, "Coins changed with no perks selected");
        check(SyncPlayerDataPacket.periodicUpdate(data, idle) == null, "Unchanged empty loadout");

        List<String> ids = new ArrayList<>(List.of("first"));
        var preset = new SyncPlayerDataPacket.PresetSyncData("saved", ids);
        ids.add("second");
        check(preset.perkIds().equals(List.of("first")), "Cached presets must be immutable snapshots");

        System.out.println("PlayerDataSyncTest: all checks passed");
    }

    private static SyncPlayerDataPacket expectChanged(PlayerData data, SyncPlayerDataPacket previous, String message) {
        SyncPlayerDataPacket next = SyncPlayerDataPacket.periodicUpdate(data, previous);
        check(next != null && next != previous, message);
        return next;
    }

    private static PlayerData loadPlayer() {
        CompoundTag perk = new CompoundTag();
        perk.putString("perkId", "test_charge");
        perk.putIntArray("chargeDurations", new int[]{150, 100});
        ListTag perks = new ListTag();
        perks.add(perk);

        CompoundTag preset = new CompoundTag();
        preset.putString("name", "saved");
        ListTag presetPerks = new ListTag();
        presetPerks.add(StringTag.valueOf("test_charge"));
        preset.put("perks", presetPerks);
        ListTag presets = new ListTag();
        presets.add(preset);

        CompoundTag data = new CompoundTag();
        data.put("selectedPerks", perks);
        data.put("presets", presets);
        PlayerData player = PlayerData.load(UUID.randomUUID(), data);
        player.getCosmeticData().addPurchase("first");
        player.getCosmeticData().setEnabled("first", true);
        return player;
    }

    private static void verifyWirePayload(SyncPlayerDataPacket packet) {
        FriendlyByteBuf expected = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (int value : new int[]{1, 0, 100, 0, 0, 2, 1}) expected.writeInt(value);
            expected.writeUtf("test_charge");
            expected.writeInt(0);
            expected.writeInt(200);
            expected.writeInt(PerkType.CHARGED.ordinal());
            expected.writeFloat(5.0f);
            expected.writeInt(2);
            expected.writeInt(100);
            expected.writeInt(200);
            expected.writeInt(1);
            expected.writeUtf("saved");
            expected.writeInt(1);
            expected.writeUtf("test_charge");
            expected.writeInt(1);
            expected.writeUtf("first");
            expected.writeInt(1);
            expected.writeUtf("first");

            packet.encode(encoded);
            check(encoded.equals(expected), "Wire payload must remain compatible");

            SyncPlayerDataPacket decoded = SyncPlayerDataPacket.decode(encoded);
            check(!encoded.isReadable(), "Decoding must consume the complete payload");
            encoded.clear();
            decoded.encode(encoded);
            check(encoded.equals(expected), "Wire round trip");
        } finally {
            encoded.release();
            expected.release();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
