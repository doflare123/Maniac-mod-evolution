package org.example.maniacrevolution.cosmetic.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.cosmetic.CosmeticEffect;
import org.example.maniacrevolution.cosmetic.CosmeticType;

public class ParticleEffect extends CosmeticEffect {
    private final String particleType;
    private final int particleCount;

    protected ParticleEffect(Builder builder) {
        super(builder);
        this.particleType = builder.particleType;
        this.particleCount = builder.particleCount;
    }

    public String getParticleType() { return particleType; }
    public int getParticleCount() { return particleCount; }

    @Override
    public void onTick(ServerPlayer player) {
        if (player.tickCount % 10 != 0) return; // Каждые 0.5 сек

        ServerLevel level = player.serverLevel();
        SimpleParticleType particle = getParticleByName(particleType);

        if (particle != null) {
            level.sendParticles(particle,
                    player.getX(), player.getY() + 1, player.getZ(),
                    particleCount,
                    0.3, 0.5, 0.3,
                    0.01
            );
        }
    }

    private SimpleParticleType getParticleByName(String name) {
        return switch (name) {
            case "flame" -> ParticleTypes.FLAME;
            case "heart" -> ParticleTypes.HEART;
            case "smoke" -> ParticleTypes.SMOKE;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "portal" -> ParticleTypes.PORTAL;
            default -> null;
        };
    }

    // ===== ИСПРАВЛЕННЫЙ BUILDER =====
    public static class Builder {
        private final String id;
        private CosmeticType type = CosmeticType.PARTICLE;
        private int price = 50;
        private String particleType = "flame";
        private int particleCount = 3;

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

        public Builder particleType(String type) {
            this.particleType = type;
            return this;
        }

        public Builder particleCount(int count) {
            this.particleCount = count;
            return this;
        }

        // Геттеры для CosmeticEffect
        public String getId() { return id; }
        public CosmeticType getType() { return type; }
        public int getPrice() { return price; }

        public ParticleEffect build() {
            return new ParticleEffect(this);
        }
    }
}
