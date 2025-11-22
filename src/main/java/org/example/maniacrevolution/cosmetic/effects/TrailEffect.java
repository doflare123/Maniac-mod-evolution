package org.example.maniacrevolution.cosmetic.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.cosmetic.CosmeticEffect;
import org.example.maniacrevolution.cosmetic.CosmeticType;

public class TrailEffect extends CosmeticEffect {
    private final String trailParticle;

    protected TrailEffect(Builder builder) {
        super(builder);
        this.trailParticle = builder.trailParticle;
    }

    public String getTrailParticle() { return trailParticle; }

    @Override
    public void onTick(ServerPlayer player) {
        // Только если игрок двигается
        if (player.getDeltaMovement().lengthSqr() < 0.01) return;
        if (player.tickCount % 2 != 0) return;

        ServerLevel level = player.serverLevel();
        SimpleParticleType particle = getParticleByName(trailParticle);

        if (particle != null) {
            level.sendParticles(particle,
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    1, 0.1, 0, 0.1, 0
            );
        }
    }

    private SimpleParticleType getParticleByName(String name) {
        return switch (name) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "smoke" -> ParticleTypes.SMOKE;
            default -> ParticleTypes.FLAME;
        };
    }

    // ===== ИСПРАВЛЕННЫЙ BUILDER =====
    public static class Builder {
        private final String id;
        private CosmeticType type = CosmeticType.TRAIL;
        private int price = 80;
        private String trailParticle = "flame";

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

        public Builder trailParticle(String particle) {
            this.trailParticle = particle;
            return this;
        }

        // Геттеры для CosmeticEffect
        public String getId() { return id; }
        public CosmeticType getType() { return type; }
        public int getPrice() { return price; }

        public TrailEffect build() {
            return new TrailEffect(this);
        }
    }
}
