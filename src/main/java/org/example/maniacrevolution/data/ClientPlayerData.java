package org.example.maniacrevolution.data;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.cosmetic.CosmeticData;
import org.example.maniacrevolution.network.packets.SyncPlayerDataPacket;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.preset.PerkPreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Клиентская копия данных игрока для отображения в HUD/GUI
 */
public class ClientPlayerData {
    private static int level = 1;
    private static int experience = 0;
    private static int expForNextLevel = 100;
    private static int coins = 0;
    private static List<ClientPerkData> selectedPerks = new ArrayList<>();
    private static int activePerkIndex = 0;

    // Пресеты
    private static List<PerkPreset> presets = new ArrayList<>();
    private static int maxPresets = 2;

    // Косметика
    private static CosmeticData cosmeticData = new CosmeticData();

    public static void update(int lvl, int exp, int expNext, int money,
                              List<SyncPlayerDataPacket.PerkSyncData> perks, int activeIndex,
                              List<SyncPlayerDataPacket.PresetSyncData> presetData, int maxPres,
                              Set<String> purchasedCosmetics, Set<String> enabledCosmetics) {
        level = lvl;
        experience = exp;
        expForNextLevel = expNext;
        coins = money;
        activePerkIndex = activeIndex;
        maxPresets = maxPres;

        selectedPerks.clear();
        for (SyncPlayerDataPacket.PerkSyncData perk : perks) {
            selectedPerks.add(new ClientPerkData(
                    perk.id(),
                    perk.cooldown(),
                    perk.maxCooldown(),
                    PerkType.values()[perk.typeOrdinal()]
            ));
        }

        presets.clear();
        for (SyncPlayerDataPacket.PresetSyncData preset : presetData) {
            presets.add(new PerkPreset(preset.name(), preset.perkIds()));
        }

        // Обновляем косметику
        cosmeticData = new CosmeticData();
        for (String id : purchasedCosmetics) {
            cosmeticData.addPurchase(id);
        }
        for (String id : enabledCosmetics) {
            cosmeticData.setEnabled(id, true);
        }
    }

    public static int getLevel() { return level; }
    public static int getExperience() { return experience; }
    public static int getExpForNextLevel() { return expForNextLevel; }
    public static int getCoins() { return coins; }
    public static List<ClientPerkData> getSelectedPerks() { return selectedPerks; }
    public static int getActivePerkIndex() { return activePerkIndex; }
    public static List<PerkPreset> getPresets() { return presets; }
    public static int getMaxPresets() { return maxPresets; }
    public static CosmeticData getCosmeticData() { return cosmeticData; }

    public static float getExpProgress() {
        return expForNextLevel > 0 ? (float) experience / expForNextLevel : 0;
    }

    public static ClientPerkData getActivePerk() {
        if (activePerkIndex >= 0 && activePerkIndex < selectedPerks.size()) {
            return selectedPerks.get(activePerkIndex);
        }
        return null;
    }

    public record ClientPerkData(String id, int cooldown, int maxCooldown, PerkType type) {
        public boolean isOnCooldown() { return cooldown > 0; }
        public int getCooldownSeconds() { return (cooldown + 19) / 20; }
        public float getCooldownProgress() {
            return maxCooldown > 0 ? (float) cooldown / maxCooldown : 0;
        }

        public String getDisplayName() {
            var perk = PerkRegistry.getPerk(id);
            return perk != null ? perk.getName().getString() : id;
        }

        public ResourceLocation getIcon() {
            return new ResourceLocation("maniacrev", "textures/perks/" + id + ".png");
        }
    }
}

