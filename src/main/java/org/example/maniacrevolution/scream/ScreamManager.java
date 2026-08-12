package org.example.maniacrevolution.scream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ScreamPacket;
import org.example.maniacrevolution.perk.PerkTeam;

/**
 * Единая серверная точка входа для крика выжившего.
 * Будущие механики могут вызывать её независимо от перка «Го некст».
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ScreamManager {
    public static final int TICKS_PER_SECOND = 20;
    public static final int EFFECT_DURATION_SECONDS = 1;
    public static final int EFFECT_DURATION_TICKS = EFFECT_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final int MARKER_DURATION_SECONDS = 5;
    public static final int MARKER_DURATION_TICKS = MARKER_DURATION_SECONDS * TICKS_PER_SECOND;
    public static final double MARKER_CHEST_HEIGHT_FACTOR = 0.65D;

    public static final float SOUND_VOLUME = 1.0F;
    public static final float SOUND_PITCH = 1.0F;

    private ScreamManager() {
    }

    public static void trigger(ServerPlayer screamer) {
        screamer.addEffect(new MobEffectInstance(
                ModEffects.SCREAM.get(),
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));
    }

    @SubscribeEvent
    public static void onScreamEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer screamer)
                || event.getEffectInstance().getEffect() != ModEffects.SCREAM.get()) {
            return;
        }

        broadcastScream(screamer);
    }

    private static void broadcastScream(ServerPlayer screamer) {
        double markerX = screamer.getX();
        double markerY = screamer.getY() + screamer.getBbHeight() * MARKER_CHEST_HEIGHT_FACTOR;
        double markerZ = screamer.getZ();

        for (ServerPlayer viewer : screamer.server.getPlayerList().getPlayers()) {
            if (!viewer.level().dimension().equals(screamer.level().dimension())) {
                continue;
            }

            boolean showMarker = PerkTeam.fromPlayer(viewer) == PerkTeam.MANIAC;
            ModNetworking.sendToPlayer(new ScreamPacket(
                    markerX,
                    markerY,
                    markerZ,
                    MARKER_DURATION_TICKS,
                    showMarker
            ), viewer);
        }
    }
}
