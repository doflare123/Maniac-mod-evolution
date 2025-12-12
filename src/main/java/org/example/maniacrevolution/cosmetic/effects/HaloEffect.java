package org.example.maniacrevolution.cosmetic.effects;

import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.cosmetic.CosmeticEffect;
import org.example.maniacrevolution.cosmetic.CosmeticType;

/**
 * Косметический эффект "Нимб" - золотое кольцо над головой игрока
 * Рендерится на клиенте через HaloClientRenderer
 */
public class HaloEffect extends CosmeticEffect {
    private final double haloHeight;
    private final double haloRadius;

    protected HaloEffect(Builder builder) {
        super(builder);
        this.haloHeight = builder.haloHeight;
        this.haloRadius = builder.haloRadius;
    }

    public double getHaloHeight() { return haloHeight; }
    public double getHaloRadius() { return haloRadius; }

    @Override
    public void onTick(ServerPlayer player) {
        // ПУСТОЙ МЕТОД
        // Нимб рендерится полностью на клиенте через HaloClientRenderer
        // Это предотвращает создание "полосы" из частиц при движении
    }

    // ===== BUILDER =====
    public static class Builder {
        private final String id;
        private CosmeticType type = CosmeticType.PARTICLE;
        private int price = 250;
        private double haloHeight = 0.6;
        private double haloRadius = 0.4;

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

        public Builder haloHeight(double height) {
            this.haloHeight = height;
            return this;
        }

        public Builder haloRadius(double radius) {
            this.haloRadius = radius;
            return this;
        }

        // Геттеры для CosmeticEffect
        public String getId() { return id; }
        public CosmeticType getType() { return type; }
        public int getPrice() { return price; }

        public HaloEffect build() {
            return new HaloEffect(this);
        }
    }
}