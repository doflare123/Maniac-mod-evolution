package org.example.maniacrevolution.cosmetic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.cosmetic.effects.ParticleEffect;
import org.example.maniacrevolution.cosmetic.effects.TrailEffect;
import org.example.maniacrevolution.cosmetic.effects.WeaponEffect;

import java.util.HashSet;
import java.util.Set;

public class CosmeticEffect {
    protected final String id;
    protected final String nameKey;
    protected final String descKey;
    protected final CosmeticType type;
    protected final int price;

    // Конструктор для ParticleEffect.Builder
    protected CosmeticEffect(ParticleEffect.Builder builder) {
        this.id = builder.getId();
        this.nameKey = "cosmetic.maniacrev." + id + ".name";
        this.descKey = "cosmetic.maniacrev." + id + ".desc";
        this.type = builder.getType();
        this.price = builder.getPrice();
    }

    // Конструктор для WeaponEffect.Builder
    protected CosmeticEffect(WeaponEffect.Builder builder) {
        this.id = builder.getId();
        this.nameKey = "cosmetic.maniacrev." + id + ".name";
        this.descKey = "cosmetic.maniacrev." + id + ".desc";
        this.type = builder.getType();
        this.price = builder.getPrice();
    }

    // Конструктор для TrailEffect.Builder
    protected CosmeticEffect(TrailEffect.Builder builder) {
        this.id = builder.getId();
        this.nameKey = "cosmetic.maniacrev." + id + ".name";
        this.descKey = "cosmetic.maniacrev." + id + ".desc";
        this.type = builder.getType();
        this.price = builder.getPrice();
    }

    // Конструктор для простых эффектов (скины перков)
    protected CosmeticEffect(SimpleBuilder builder) {
        this.id = builder.id;
        this.nameKey = "cosmetic.maniacrev." + id + ".name";
        this.descKey = "cosmetic.maniacrev." + id + ".desc";
        this.type = builder.type;
        this.price = builder.price;
    }

    public String getId() { return id; }
    public CosmeticType getType() { return type; }
    public int getPrice() { return price; }

    public Component getName() {
        return Component.translatable(nameKey);
    }

    public Component getDescription() {
        return Component.translatable(descKey);
    }

    // Проверяет, применяется ли эффект к предмету (для WeaponEffect)
    public boolean appliesTo(ItemStack stack) {
        return true; // По умолчанию применяется ко всему
    }

    // Переопределяется в дочерних классах
    public void onTick(ServerPlayer player) {}

    public void onAttack(ServerPlayer player, ItemStack weapon) {}

    // ===== ПРОСТОЙ BUILDER для скинов перков и т.д. =====
    public static class SimpleBuilder {
        private final String id;
        private CosmeticType type = CosmeticType.PERK_SKIN;
        private int price = 50;

        public SimpleBuilder(String id) {
            this.id = id;
        }

        public SimpleBuilder type(CosmeticType type) {
            this.type = type;
            return this;
        }

        public SimpleBuilder price(int price) {
            this.price = price;
            return this;
        }

        public CosmeticEffect build() {
            return new CosmeticEffect(this);
        }
    }
}