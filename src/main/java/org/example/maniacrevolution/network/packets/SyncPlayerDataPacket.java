package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.preset.PerkPreset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SyncPlayerDataPacket {
    private final int level;
    private final int experience;
    private final int expForNext;
    private final int coins;
    private final List<PerkSyncData> selectedPerks;
    private final int activePerkIndex;
    private final List<PresetSyncData> presets;
    private final int maxPresets;
    private final Set<String> purchasedCosmetics;
    private final Set<String> enabledCosmetics;

    public SyncPlayerDataPacket(PlayerData data) {
        this.level = data.getLevel();
        this.experience = data.getExperience();
        this.expForNext = data.getExperienceForNextLevel();
        this.coins = data.getCoins();
        this.activePerkIndex = data.getActivePerkIndex();
        this.maxPresets = data.getMaxPresets();

        this.selectedPerks = new ArrayList<>();
        for (var inst : data.getSelectedPerks()) {
            selectedPerks.add(new PerkSyncData(
                    inst.getPerk().getId(),
                    inst.getCooldownRemaining(),
                    inst.getPerk().getCooldownTicks(),
                    inst.getPerk().getType().ordinal()
            ));
        }

        this.presets = new ArrayList<>();
        for (PerkPreset preset : data.getPresets()) {
            presets.add(new PresetSyncData(preset.getName(), preset.getPerkIds()));
        }

        this.purchasedCosmetics = new HashSet<>(data.getCosmeticData().getPurchasedCosmetics());
        this.enabledCosmetics = new HashSet<>(data.getCosmeticData().getEnabledCosmetics());
    }

    private SyncPlayerDataPacket(int level, int exp, int expNext, int coins,
                                 List<PerkSyncData> perks, int activeIndex,
                                 List<PresetSyncData> presets, int maxPresets,
                                 Set<String> purchased, Set<String> enabled) {
        this.level = level;
        this.experience = exp;
        this.expForNext = expNext;
        this.coins = coins;
        this.selectedPerks = perks;
        this.activePerkIndex = activeIndex;
        this.presets = presets;
        this.maxPresets = maxPresets;
        this.purchasedCosmetics = purchased;
        this.enabledCosmetics = enabled;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(expForNext);
        buf.writeInt(coins);
        buf.writeInt(activePerkIndex);
        buf.writeInt(maxPresets);

        // Перки
        buf.writeInt(selectedPerks.size());
        for (PerkSyncData perk : selectedPerks) {
            buf.writeUtf(perk.id, 64);
            buf.writeInt(perk.cooldown);
            buf.writeInt(perk.maxCooldown);
            buf.writeInt(perk.typeOrdinal);
        }

        // Пресеты
        buf.writeInt(presets.size());
        for (PresetSyncData preset : presets) {
            buf.writeUtf(preset.name, 64);
            buf.writeInt(preset.perkIds.size());
            for (String id : preset.perkIds) {
                buf.writeUtf(id, 64);
            }
        }

        // Косметика - купленная
        buf.writeInt(purchasedCosmetics.size());
        for (String id : purchasedCosmetics) {
            buf.writeUtf(id, 64);
        }

        // Косметика - включённая
        buf.writeInt(enabledCosmetics.size());
        for (String id : enabledCosmetics) {
            buf.writeUtf(id, 64);
        }
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        int level = buf.readInt();
        int exp = buf.readInt();
        int expNext = buf.readInt();
        int coins = buf.readInt();
        int activeIndex = buf.readInt();
        int maxPresets = buf.readInt();

        // Перки
        int perkCount = buf.readInt();
        List<PerkSyncData> perks = new ArrayList<>();
        for (int i = 0; i < perkCount; i++) {
            perks.add(new PerkSyncData(
                    buf.readUtf(64), buf.readInt(), buf.readInt(), buf.readInt()
            ));
        }

        // Пресеты
        int presetCount = buf.readInt();
        List<PresetSyncData> presets = new ArrayList<>();
        for (int i = 0; i < presetCount; i++) {
            String name = buf.readUtf(64);
            int perkIdCount = buf.readInt();
            List<String> perkIds = new ArrayList<>();
            for (int j = 0; j < perkIdCount; j++) {
                perkIds.add(buf.readUtf(64));
            }
            presets.add(new PresetSyncData(name, perkIds));
        }

        // Косметика - купленная
        int purchasedCount = buf.readInt();
        Set<String> purchased = new HashSet<>();
        for (int i = 0; i < purchasedCount; i++) {
            purchased.add(buf.readUtf(64));
        }

        // Косметика - включённая
        int enabledCount = buf.readInt();
        Set<String> enabled = new HashSet<>();
        for (int i = 0; i < enabledCount; i++) {
            enabled.add(buf.readUtf(64));
        }

        return new SyncPlayerDataPacket(level, exp, expNext, coins, perks, activeIndex,
                presets, maxPresets, purchased, enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPlayerData.update(level, experience, expForNext, coins,
                    selectedPerks, activePerkIndex, presets, maxPresets,
                    purchasedCosmetics, enabledCosmetics);
        });
        ctx.get().setPacketHandled(true);
    }

    public record PerkSyncData(String id, int cooldown, int maxCooldown, int typeOrdinal) {}
    public record PresetSyncData(String name, List<String> perkIds) {}
}
