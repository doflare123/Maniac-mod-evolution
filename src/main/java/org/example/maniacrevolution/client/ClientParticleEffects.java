package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.ClientParticleEffectPacket;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ClientParticleEffects {
    private static final DustParticleOptions FEAR_PARTICLE =
            new DustParticleOptions(new Vector3f(0.05F, 0.15F, 0.05F), 1.0F);
    private static final List<FearWave> FEAR_WAVES = new ArrayList<>();
    private static final List<GuidingLight> GUIDING_LIGHTS = new ArrayList<>();

    private ClientParticleEffects() {
    }

    public static void accept(ClientParticleEffectPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long now = minecraft.level.getGameTime();
        switch (packet.type()) {
            case FEAR_WAVE -> FEAR_WAVES.add(new FearWave(
                    new Vec3(packet.x(), packet.y(), packet.z()),
                    packet.primaryRadius(), packet.durationTicks(), now));
            case GUIDING_LIGHT -> GUIDING_LIGHTS.add(new GuidingLight(
                    packet.targetEntityId(), packet.targetUuid(), now + packet.durationTicks()));
            case HACK_RADIUS -> spawnHackRadius(
                    new Vec3(packet.x(), packet.y(), packet.z()),
                    packet.primaryRadius(), packet.secondaryRadius());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.level == null || minecraft.player == null) {
            return;
        }

        long now = minecraft.level.getGameTime();
        Iterator<FearWave> waves = FEAR_WAVES.iterator();
        while (waves.hasNext()) {
            FearWave wave = waves.next();
            int elapsed = (int) (now - wave.startedAt);
            if (elapsed > wave.durationTicks) {
                waves.remove();
            } else {
                spawnFearRing(wave, elapsed);
            }
        }

        Iterator<GuidingLight> trails = GUIDING_LIGHTS.iterator();
        while (trails.hasNext()) {
            GuidingLight trail = trails.next();
            if (now >= trail.expiresAt) {
                trails.remove();
            } else if ((trail.expiresAt - now) % 2 == 0) {
                spawnGuidingLight(minecraft, trail, now);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FEAR_WAVES.clear();
        GUIDING_LIGHTS.clear();
    }

    private static void spawnFearRing(FearWave wave, int elapsed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || elapsed <= 0) {
            return;
        }

        double radius = wave.maxRadius * elapsed / wave.durationTicks;
        int points = Math.max(16, Math.min(120, (int) Math.ceil(radius * 12.0)));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = wave.center.x + Math.cos(angle) * radius;
            double z = wave.center.z + Math.sin(angle) * radius;
            double y = wave.center.y + 0.5 + (i % 4 == 0 ? 0.8 : 0.0);
            minecraft.level.addParticle(FEAR_PARTICLE, x, y, z, 0.0, 0.01, 0.0);
        }

        if (elapsed == 1 || elapsed == wave.durationTicks / 2 || elapsed == wave.durationTicks) {
            minecraft.level.playLocalSound(wave.center.x, wave.center.y, wave.center.z,
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.3F, 0.8F, false);
        }
    }

    private static void spawnGuidingLight(Minecraft minecraft, GuidingLight trail, long now) {
        Entity target = minecraft.level.getEntity(trail.targetEntityId);
        if (target == null || !target.getUUID().equals(trail.targetUuid)) {
            return;
        }

        Vec3 start = minecraft.player.position().add(0.0, 1.0, 0.0);
        Vec3 direction = target.position().add(0.0, 1.0, 0.0).subtract(start);
        double length = direction.length();
        if (length < 0.1) {
            return;
        }

        int points = Math.min(64, Math.max(1, (int) Math.ceil(length / 0.75)));
        Vec3 step = direction.scale(1.0 / points);
        for (int i = 0; i <= points; i++) {
            Vec3 point = start.add(step.scale(i));
            minecraft.level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                    point.x, point.y, point.z, 0.0, 0.0, 0.0);
        }

        if (now % 4 == 0) {
            minecraft.level.addParticle(ParticleTypes.HEART,
                    target.getX(), target.getY() + 2.0, target.getZ(), 0.0, 0.02, 0.0);
        }
    }

    private static void spawnHackRadius(Vec3 center, float supportRadius, float hackerRadius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        spawnCircle(center.add(0.0, -1.0, 0.0), supportRadius, 24, true);
        spawnCircle(center, hackerRadius, 12, false);
    }

    private static void spawnCircle(Vec3 center, double radius, int points, boolean outer) {
        Minecraft minecraft = Minecraft.getInstance();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            minecraft.level.addParticle(outer ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    0.0, 0.01, 0.0);
        }
    }

    private record FearWave(Vec3 center, float maxRadius, int durationTicks, long startedAt) {
    }

    private record GuidingLight(int targetEntityId, java.util.UUID targetUuid, long expiresAt) {
    }
}
