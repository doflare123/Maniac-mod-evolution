package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.preset.PerkPreset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class SyncPlayerDataPacket {
    private final ProfileSyncData profile;
    private final List<PerkSyncData> selectedPerks;
    private final int activePerkIndex;

    public SyncPlayerDataPacket(PlayerData data) {
        this(ProfileSyncData.capture(data), capturePerks(data.getSelectedPerks()), data.getActivePerkIndex());
    }

    private SyncPlayerDataPacket(ProfileSyncData profile, List<PerkSyncData> perks, int activePerkIndex) {
        this.profile = profile;
        this.selectedPerks = perks;
        this.activePerkIndex = activePerkIndex;
    }

    private static List<PerkSyncData> capturePerks(List<PerkInstance> perks) {
        List<PerkSyncData> snapshot = new ArrayList<>(perks.size());
        for (PerkInstance inst : perks) {
            snapshot.add(new PerkSyncData(
                    inst.getPerk().getId(),
                    inst.getCooldownRemaining(),
                    inst.getPerk().getCooldownTicks(),
                    inst.getPerk().getType().ordinal(),
                    inst.getPerk().getManaCost(),
                    inst.getChargeCount(),
                    inst.getChargeRemainingTicks(),
                    inst.getChargeDurationTicks()
            ));
        }
        return snapshot;
    }

    /** Called with the last snapshot sent to this player, or null for the initial sync. */
    public static SyncPlayerDataPacket periodicUpdate(PlayerData data, SyncPlayerDataPacket previous) {
        if (previous == null || !previous.profile.matches(data)) return new SyncPlayerDataPacket(data);

        List<PerkInstance> perks = data.getSelectedPerks();
        int activeIndex = data.getActivePerkIndex();
        if (activeIndex != previous.activePerkIndex || perks.size() != previous.selectedPerks.size()) {
            return new SyncPlayerDataPacket(previous.profile, capturePerks(perks), activeIndex);
        }

        boolean refreshChargeClock = false;
        for (int i = 0; i < perks.size(); i++) {
            PerkSyncData synced = previous.selectedPerks.get(i);
            if (!synced.matches(perks.get(i))) {
                return new SyncPlayerDataPacket(previous.profile, capturePerks(perks), activeIndex);
            }
            // The client extrapolates the charge timer even while the server pauses it.
            refreshChargeClock |= synced.chargeCount() > 0 && synced.chargeRemaining() > 0;
        }
        return refreshChargeClock ? previous : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(profile.level);
        buf.writeInt(profile.experience);
        buf.writeInt(profile.expForNext);
        buf.writeInt(profile.coins);
        buf.writeInt(activePerkIndex);
        buf.writeInt(profile.maxPresets);

        // Перки
        buf.writeInt(selectedPerks.size());
        for (PerkSyncData perk : selectedPerks) {
            buf.writeUtf(perk.id, 64);
            buf.writeInt(perk.cooldown);
            buf.writeInt(perk.maxCooldown);
            buf.writeInt(perk.typeOrdinal);
            buf.writeFloat(perk.manaCost);
            buf.writeInt(perk.chargeCount);
            buf.writeInt(perk.chargeRemaining);
            buf.writeInt(perk.chargeDuration);
        }

        // Пресеты
        buf.writeInt(profile.presets.size());
        for (PresetSyncData preset : profile.presets) {
            buf.writeUtf(preset.name, 64);
            buf.writeInt(preset.perkIds.size());
            for (String id : preset.perkIds) {
                buf.writeUtf(id, 64);
            }
        }

        // Косметика - купленная
        buf.writeInt(profile.purchasedCosmetics.size());
        for (String id : profile.purchasedCosmetics) {
            buf.writeUtf(id, 64);
        }

        // Косметика - включённая
        buf.writeInt(profile.enabledCosmetics.size());
        for (String id : profile.enabledCosmetics) {
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
                    buf.readUtf(64), buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(),
                    buf.readInt(), buf.readInt(), buf.readInt()
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

        return new SyncPlayerDataPacket(new ProfileSyncData(level, exp, expNext, coins,
                presets, maxPresets, purchased, enabled), perks, activeIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPlayerData.update(profile.level, profile.experience, profile.expForNext, profile.coins,
                    selectedPerks, activePerkIndex, profile.presets, profile.maxPresets,
                    profile.purchasedCosmetics, profile.enabledCosmetics);
        });
        ctx.get().setPacketHandled(true);
    }

    public record PerkSyncData(String id, int cooldown, int maxCooldown, int typeOrdinal,
                               float manaCost, int chargeCount, int chargeRemaining,
                               int chargeDuration) {
        private boolean matches(PerkInstance instance) {
            var perk = instance.getPerk();
            return id.equals(perk.getId()) && cooldown == instance.getCooldownRemaining()
                    && maxCooldown == perk.getCooldownTicks() && typeOrdinal == perk.getType().ordinal()
                    && Float.compare(manaCost, perk.getManaCost()) == 0
                    && chargeCount == instance.getChargeCount() && chargeRemaining == instance.getChargeRemainingTicks()
                    && chargeDuration == instance.getChargeDurationTicks();
        }
    }
    public record PresetSyncData(String name, List<String> perkIds) {
        public PresetSyncData {
            perkIds = List.copyOf(perkIds);
        }

        private boolean matches(PerkPreset preset) {
            List<String> currentIds = preset.getPerkIds();
            if (!Objects.equals(name, preset.getName()) || perkIds.size() != currentIds.size()) return false;
            for (int i = 0; i < perkIds.size(); i++) {
                if (!perkIds.get(i).equals(currentIds.get(i))) return false;
            }
            return true;
        }
    }

    private record ProfileSyncData(int level, int experience, int expForNext, int coins,
                                   List<PresetSyncData> presets, int maxPresets,
                                   Set<String> purchasedCosmetics, Set<String> enabledCosmetics) {
        private static ProfileSyncData capture(PlayerData data) {
            List<PresetSyncData> presets = new ArrayList<>(data.getPresets().size());
            for (PerkPreset preset : data.getPresets()) {
                presets.add(new PresetSyncData(preset.getName(), preset.getPerkIds()));
            }
            var cosmetics = data.getCosmeticData();
            return new ProfileSyncData(data.getLevel(), data.getExperience(), data.getExperienceForNextLevel(),
                    data.getCoins(), presets, data.getMaxPresets(),
                    cosmetics.getPurchasedSnapshot(), cosmetics.getEnabledSnapshot());
        }

        private boolean matches(PlayerData data) {
            var cosmetics = data.getCosmeticData();
            List<PerkPreset> currentPresets = data.getPresets();
            if (level != data.getLevel() || experience != data.getExperience() || coins != data.getCoins()
                    || expForNext != data.getExperienceForNextLevel() || maxPresets != data.getMaxPresets()
                    || purchasedCosmetics != cosmetics.getPurchasedSnapshot()
                    || enabledCosmetics != cosmetics.getEnabledSnapshot() || presets.size() != currentPresets.size()) {
                return false;
            }
            for (int i = 0; i < presets.size(); i++) {
                if (!presets.get(i).matches(currentPresets.get(i))) return false;
            }
            return true;
        }
    }
}
