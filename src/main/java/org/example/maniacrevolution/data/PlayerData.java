package org.example.maniacrevolution.data;

import org.example.maniacrevolution.cosmetic.CosmeticData;
import org.example.maniacrevolution.perk.*;
import org.example.maniacrevolution.preset.PerkPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class PlayerData {
    private final UUID playerUuid;

    // Система уровней
    private int level = 1;
    private int experience = 0;
    private int coins = 0;

    // Выбранные перки на текущую игру (максимум 2)
    private final List<PerkInstance> selectedPerks = new ArrayList<>(2);
    private int activePerkIndex = 0;

    // Пресеты (максимум 2 по умолчанию, можно докупить)
    private final List<PerkPreset> presets = new ArrayList<>();
    private int maxPresets = 2;

    // Косметика
    private final CosmeticData cosmeticData = new CosmeticData();

    public PlayerData(UUID uuid) {
        this.playerUuid = uuid;
    }

    // Купленные товары в магазине
    private final Set<String> purchasedItems = new HashSet<>();

    // === Система уровней ===

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getCoins() { return coins; }

    public int getExperienceForNextLevel() {
        return 100 + (level - 1) * 25;
    }

    public float getExperienceProgress() {
        return (float) experience / getExperienceForNextLevel();
    }

    public void addExperience(int amount) {
        experience += amount;
        while (experience >= getExperienceForNextLevel()) {
            experience -= getExperienceForNextLevel();
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        coins += 10;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    // === Магазин ===

    public Set<String> getPurchasedItems() {
        return Collections.unmodifiableSet(purchasedItems);
    }

    public boolean hasPurchased(String itemId) {
        return purchasedItems.contains(itemId);
    }

    public void addPurchase(String itemId) {
        purchasedItems.add(itemId);
    }

    // === Перки ===

    public List<PerkInstance> getSelectedPerks() {
        return Collections.unmodifiableList(selectedPerks);
    }

    public int getActivePerkIndex() { return activePerkIndex; }

    public PerkInstance getActivePerk() {
        if (activePerkIndex >= 0 && activePerkIndex < selectedPerks.size()) {
            return selectedPerks.get(activePerkIndex);
        }
        return null;
    }

    public boolean selectPerk(Perk perk, ServerPlayer player) {
        if (selectedPerks.size() >= 2) return false;

        for (PerkInstance inst : selectedPerks) {
            if (inst.getPerk().getId().equals(perk.getId())) return false;
        }

        PerkTeam playerTeam = PerkTeam.fromPlayer(player);
        if (playerTeam != null && !perk.isAvailableForTeam(playerTeam)) {
            return false;
        }

        selectedPerks.add(new PerkInstance(perk));

        return true;
    }

    public void deselectPerk(int index, ServerPlayer player) {
        if (index >= 0 && index < selectedPerks.size()) {
            selectedPerks.get(index).onRemove(player);
            selectedPerks.remove(index);
            if (activePerkIndex >= selectedPerks.size()) {
                activePerkIndex = Math.max(0, selectedPerks.size() - 1);
            }

        }
    }

    public void clearPerks(ServerPlayer player) {
        for (PerkInstance inst : selectedPerks) {
            inst.onRemove(player);
        }
        selectedPerks.clear();
        activePerkIndex = 0;

    }

    public void switchActivePerk() {
        if (selectedPerks.size() > 1) {
            activePerkIndex = (activePerkIndex + 1) % selectedPerks.size();
        }
    }

    // === Пресеты ===

    public List<PerkPreset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public int getMaxPresets() { return maxPresets; }

    public void increaseMaxPresets() { maxPresets++; }

    public boolean createPreset(String name, List<String> perkIds) {
        if (presets.size() >= maxPresets) return false;
        if (perkIds.isEmpty() || perkIds.size() > 2) return false;

        presets.add(new PerkPreset(name, perkIds));
        return true;
    }

    public boolean deletePreset(int index) {
        if (index >= 0 && index < presets.size()) {
            presets.remove(index);
            return true;
        }
        return false;
    }

    public boolean applyPreset(int index, ServerPlayer player) {
        if (index < 0 || index >= presets.size()) return false;

        PerkPreset preset = presets.get(index);
        clearPerks(player);

        for (String perkId : preset.getPerkIds()) {
            Perk perk = PerkRegistry.getPerk(perkId);
            if (perk != null) {
                selectPerk(perk, player);
            }
        }
        return true;
    }

    // === Косметика ===

    public CosmeticData getCosmeticData() {
        return cosmeticData;
    }

    // === Тик ===

    public void tick(ServerPlayer player, PerkPhase currentPhase) {
        for (PerkInstance inst : selectedPerks) {
            inst.tick(player, currentPhase);
        }
    }

    // === Игровые события ===

    public void onGameStart(ServerPlayer player) {
        for (PerkInstance inst : selectedPerks) {
            inst.onGameStart(player);
        }
    }

    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        for (PerkInstance inst : selectedPerks) {
            inst.onPhaseChange(player, newPhase);
        }
    }

    // === Сериализация ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("experience", experience);
        tag.putInt("coins", coins);
        tag.putInt("maxPresets", maxPresets);

        // Пресеты
        ListTag presetsTag = new ListTag();
        for (PerkPreset preset : presets) {
            presetsTag.add(preset.save());
        }
        tag.put("presets", presetsTag);

        // ФИКС: Сохранение косметики
        tag.put("cosmetics", cosmeticData.save());

        // Выбранные перки
        ListTag perksTag = new ListTag();
        for (PerkInstance inst : selectedPerks) {
            perksTag.add(inst.save());
        }
        tag.put("selectedPerks", perksTag);
        tag.putInt("activePerkIndex", activePerkIndex);

        return tag;
    }

    public static PlayerData load(UUID uuid, CompoundTag tag) {
        PlayerData data = new PlayerData(uuid);
        data.level = tag.getInt("level");
        if (data.level < 1) data.level = 1;
        data.experience = tag.getInt("experience");
        data.coins = tag.getInt("coins");
        data.maxPresets = tag.getInt("maxPresets");
        if (data.maxPresets < 2) data.maxPresets = 2;

        // Пресеты
        ListTag presetsTag = tag.getList("presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < presetsTag.size(); i++) {
            PerkPreset preset = PerkPreset.load(presetsTag.getCompound(i));
            if (preset != null) data.presets.add(preset);
        }

        // ФИКС: Загрузка косметики - правильное восстановление данных
        if (tag.contains("cosmetics")) {
            CompoundTag cosmeticsTag = tag.getCompound("cosmetics");

            // Загружаем купленные
            ListTag purchasedTag = cosmeticsTag.getList("purchased", Tag.TAG_STRING);
            for (int i = 0; i < purchasedTag.size(); i++) {
                data.cosmeticData.addPurchase(purchasedTag.getString(i));
            }

            // Загружаем включённые
            ListTag enabledTag = cosmeticsTag.getList("enabled", Tag.TAG_STRING);
            for (int i = 0; i < enabledTag.size(); i++) {
                String id = enabledTag.getString(i);
                // Включаем только если куплено
                if (data.cosmeticData.hasPurchased(id)) {
                    data.cosmeticData.setEnabled(id, true);
                }
            }
        }

        // Выбранные перки
        ListTag perksTag = tag.getList("selectedPerks", Tag.TAG_COMPOUND);
        for (int i = 0; i < perksTag.size(); i++) {
            PerkInstance inst = PerkInstance.load(perksTag.getCompound(i));
            if (inst != null) data.selectedPerks.add(inst);
        }
        data.activePerkIndex = tag.getInt("activePerkIndex");

        return data;
    }
}