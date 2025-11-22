package org.example.maniacrevolution.cosmetic.effects;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.cosmetic.CosmeticEffect;
import org.example.maniacrevolution.cosmetic.CosmeticType;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class WeaponEffect extends CosmeticEffect {
    private final int effectColor;
    private final int trailLength;
    private final Set<Item> triggerItems;
    private final Set<String> triggerTags;

    protected WeaponEffect(Builder builder) {
        super(builder);
        this.effectColor = builder.effectColor;
        this.trailLength = builder.trailLength;
        this.triggerItems = new HashSet<>(builder.triggerItems);
        this.triggerTags = new HashSet<>(builder.triggerTags);
    }

    public int getEffectColor() { return effectColor; }
    public int getTrailLength() { return trailLength; }

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Если нет ограничений - не применяется (это эффект оружия)
        if (triggerItems.isEmpty() && triggerTags.isEmpty()) {
            return false;
        }

        // Проверка по конкретным предметам
        if (triggerItems.contains(stack.getItem())) {
            return true;
        }

        // Проверка по тегам
        for (String tag : triggerTags) {
            if (stack.getTags().anyMatch(t -> t.location().toString().contains(tag))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onAttack(ServerPlayer player, ItemStack weapon) {
        if (!appliesTo(weapon)) return;

        ServerLevel level = player.serverLevel();

        // Создаём цветные частицы
        float r = ((effectColor >> 16) & 0xFF) / 255f;
        float g = ((effectColor >> 8) & 0xFF) / 255f;
        float b = (effectColor & 0xFF) / 255f;

        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 1.0f);

        // Спавним частицы в направлении взгляда
        double lookX = -Math.sin(Math.toRadians(player.getYRot()));
        double lookZ = Math.cos(Math.toRadians(player.getYRot()));

        for (int i = 0; i < trailLength; i++) {
            level.sendParticles(dust,
                    player.getX() + lookX * (i * 0.5),
                    player.getY() + 1.2,
                    player.getZ() + lookZ * (i * 0.5),
                    3, 0.1, 0.1, 0.1, 0
            );
        }
    }

    // ===== ИСПРАВЛЕННЫЙ BUILDER =====
    public static class Builder {
        private final String id;
        private CosmeticType type = CosmeticType.WEAPON_EFFECT;
        private int price = 100;
        private int effectColor = 0xFFFFFF;
        private int trailLength = 3;
        private final Set<Item> triggerItems = new HashSet<>();
        private final Set<String> triggerTags = new HashSet<>();

        public Builder(String id) {
            this.id = id;
        }

        public Builder type(CosmeticType type) {
            this.type = type;
            return this;
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder effectColor(int color) {
            this.effectColor = color;
            return this;
        }

        public Builder trailLength(int length) {
            this.trailLength = length;
            return this;
        }

        public Builder triggerItem(Item item) {
            this.triggerItems.add(item);
            return this;
        }

        public Builder triggerItem(String itemId) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item != null) {
                this.triggerItems.add(item);
            }
            return this;
        }

        public Builder triggerTag(String tag) {
            this.triggerTags.add(tag);
            return this;
        }

        // Геттеры для CosmeticEffect
        public String getId() { return id; }
        public CosmeticType getType() { return type; }
        public int getPrice() { return price; }
        public Set<Item> getTriggerItems() { return triggerItems; }
        public Set<String> getTriggerTags() { return triggerTags; }

        public WeaponEffect build() {
            return new WeaponEffect(this);
        }
    }
}
