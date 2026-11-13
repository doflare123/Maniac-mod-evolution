package org.example.maniacrevolution.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.joml.Vector3f;

/** Presence alone gates phylactery hits; this effect grants no player damage resistance. */
public final class LichPhylacteryGuardEffect extends MobEffect {
    public static final int COLOR=0xbd64ff;
    public static final DustParticleOptions HIT_PARTICLE=new DustParticleOptions(new Vector3f(189/255f,100/255f,1),1.15f);
    public LichPhylacteryGuardEffect() {super(MobEffectCategory.BENEFICIAL,COLOR);}
}
