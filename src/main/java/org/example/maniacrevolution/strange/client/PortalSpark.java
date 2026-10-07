package org.example.maniacrevolution.strange.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.strange.StrangeParticles;

/** Short-lived emissive sparks orbit the portal instead of leaving a static dust ring. */
public final class PortalSpark extends TextureSheetParticle {
    private final double centerX, centerY, centerZ, yaw, scale;
    private double angle;
    private PortalSpark(ClientLevel level, double x, double y, double z, double angle, double yaw, double scale, SpriteSet sprites) {
        super(level, x, y, z);
        this.angle=angle; this.yaw=yaw; this.scale=scale;
        centerX=x-Math.cos(angle)*.88*scale*Math.cos(yaw);
        centerY=y-Math.sin(angle)*1.3*scale;
        centerZ=z-Math.cos(angle)*.88*scale*Math.sin(yaw);
        lifetime=6+random.nextInt(4); quadSize=.018f+random.nextFloat()*.018f;
        hasPhysics=false; pickSprite(sprites); setColor(1,.35f+random.nextFloat()*.35f,.025f);
    }
    @Override public void tick() {
        xo=x; yo=y; zo=z;
        if (++age>=lifetime) { remove(); return; }
        angle+=.13;
        setPos(centerX+Math.cos(angle)*.88*scale*Math.cos(yaw),centerY+Math.sin(angle)*1.3*scale,
                centerZ+Math.cos(angle)*.88*scale*Math.sin(yaw));
        alpha=1-age/(float)lifetime;
        gCol*=.91f;
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_LIT; }
    @Override protected int getLightColor(float partial) { return 0xf000f0; }
    @Mod.EventBusSubscriber(modid=Maniacrev.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(StrangeParticles.PORTAL_SPARK.get(), sprites ->
                    (SimpleParticleType type, ClientLevel level, double x, double y, double z, double angle, double yaw, double scale) ->
                            new PortalSpark(level,x,y,z,angle,yaw,scale,sprites));
        }
    }
}
