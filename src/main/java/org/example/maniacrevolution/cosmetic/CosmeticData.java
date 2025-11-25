package org.example.maniacrevolution.cosmetic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class CosmeticData {
    // Купленные косметические эффекты
    private final Set<String> purchasedCosmetics = new HashSet<>();

    // Активированные эффекты (включённые)
    private final Set<String> enabledCosmetics = new HashSet<>();

    // === Покупка ===

    public boolean hasPurchased(String cosmeticId) {
        return purchasedCosmetics.contains(cosmeticId);
    }

    public void addPurchase(String cosmeticId) {
        purchasedCosmetics.add(cosmeticId);
    }

    public Set<String> getPurchasedCosmetics() {
        return Collections.unmodifiableSet(purchasedCosmetics);
    }

    // === Включение/выключение ===

    public boolean isEnabled(String cosmeticId) {
        return enabledCosmetics.contains(cosmeticId);
    }

    public void setEnabled(String cosmeticId, boolean enabled) {
        if (!purchasedCosmetics.contains(cosmeticId)) return;

        if (enabled) {
            enabledCosmetics.add(cosmeticId);
        } else {
            enabledCosmetics.remove(cosmeticId);
        }
    }

    public void toggleEnabled(String cosmeticId) {
        if (!purchasedCosmetics.contains(cosmeticId)) return;

        if (enabledCosmetics.contains(cosmeticId)) {
            enabledCosmetics.remove(cosmeticId);
        } else {
            enabledCosmetics.add(cosmeticId);
        }
    }

    public Set<String> getEnabledCosmetics() {
        return Collections.unmodifiableSet(enabledCosmetics);
    }

    // === Проверка активного эффекта для предмета ===

    public boolean hasActiveEffectForItem(ItemStack stack, CosmeticType type) {
        if (stack.isEmpty()) return false;

        for (String cosmeticId : enabledCosmetics) {
            CosmeticEffect effect = CosmeticRegistry.getEffect(cosmeticId);
            if (effect != null && effect.getType() == type && effect.appliesTo(stack)) {
                return true;
            }
        }
        return false;
    }

    public List<CosmeticEffect> getActiveEffectsForItem(ItemStack stack) {
        List<CosmeticEffect> effects = new ArrayList<>();

        for (String cosmeticId : enabledCosmetics) {
            CosmeticEffect effect = CosmeticRegistry.getEffect(cosmeticId);
            if (effect != null && effect.appliesTo(stack)) {
                effects.add(effect);
            }
        }

        return effects;
    }

    // === Сериализация ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        ListTag purchasedTag = new ListTag();
        for (String id : purchasedCosmetics) {
            purchasedTag.add(StringTag.valueOf(id));
        }
        tag.put("purchased", purchasedTag);

        ListTag enabledTag = new ListTag();
        for (String id : enabledCosmetics) {
            enabledTag.add(StringTag.valueOf(id));
        }
        tag.put("enabled", enabledTag);

        return tag;
    }

    public static CosmeticData load(CompoundTag tag) {
        CosmeticData data = new CosmeticData();

        // Загружаем купленные
        ListTag purchasedTag = tag.getList("purchased", Tag.TAG_STRING);
        for (int i = 0; i < purchasedTag.size(); i++) {
            data.purchasedCosmetics.add(purchasedTag.getString(i));
        }

        // Загружаем включённые (только если куплено)
        ListTag enabledTag = tag.getList("enabled", Tag.TAG_STRING);
        for (int i = 0; i < enabledTag.size(); i++) {
            String id = enabledTag.getString(i);
            if (data.purchasedCosmetics.contains(id)) {
                data.enabledCosmetics.add(id);
            }
        }

        return data;
    }
}
