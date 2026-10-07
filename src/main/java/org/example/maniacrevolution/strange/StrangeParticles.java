package org.example.maniacrevolution.strange;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

public final class StrangeParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Maniacrev.MODID);
    public static final RegistryObject<SimpleParticleType> PORTAL_SPARK = TYPES.register("portal_spark", () -> new SimpleParticleType(false));
    private StrangeParticles() {}
}
