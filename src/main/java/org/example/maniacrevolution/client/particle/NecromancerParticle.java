package org.example.maniacrevolution.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Кастомная частица для эффектов некроманта
 * Можно зарегистрировать через DeferredRegister<ParticleType<?>>
 */
@OnlyIn(Dist.CLIENT)
public class NecromancerParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected NecromancerParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.lifetime = 40 + this.random.nextInt(20);
        this.gravity = -0.01F; // Частицы поднимаются вверх
        this.hasPhysics = false;

        // Фиолетовый цвет с вариацией
        float colorVariation = this.random.nextFloat() * 0.3F;
        this.rCol = 0.54F + colorVariation;
        this.gCol = 0.0F + colorVariation * 0.2F;
        this.bCol = 1.0F;

        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);

        // Спиральное движение
        if (this.age < this.lifetime) {
            double angle = this.age * 0.2;
            this.xd = Math.cos(angle) * 0.02;
            this.zd = Math.sin(angle) * 0.02;

            // Затухание альфа канала
            this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new NecromancerParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}