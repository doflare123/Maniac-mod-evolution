package org.example.maniacrevolution.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

/**
 * Регистрация кастомных типов частиц для некроманта
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Maniacrev.MODID);

//    public static final RegistryObject<SimpleParticleType> NECROMANCER_SOUL =
//            PARTICLE_TYPES.register("necromancer_soul",
//                    () -> new SimpleParticleType(true));
//
//    public static final RegistryObject<SimpleParticleType> NECROMANCER_PENTAGRAM =
//            PARTICLE_TYPES.register("necromancer_pentagram",
//                    () -> new SimpleParticleType(true));
//
//    public static final RegistryObject<SimpleParticleType> RESURRECTION_ENERGY =
//            PARTICLE_TYPES.register("resurrection_energy",
//                    () -> new SimpleParticleType(true));
}
