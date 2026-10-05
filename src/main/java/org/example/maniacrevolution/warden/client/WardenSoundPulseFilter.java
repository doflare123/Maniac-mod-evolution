package org.example.maniacrevolution.warden.client;

import net.minecraft.sounds.SoundSource;

import org.example.maniacrevolution.warden.WardenSoundPolicy;

/** One policy for all sound IDs, including modded sounds. No per-action pulse producers. */
final class WardenSoundPulseFilter {

    record HeardSound(String id, SoundSource source, double x, double y, double z,
                      float volume, int attenuationDistance, boolean attenuates, boolean relative, boolean looping) {
        HeardSound withVolume(float value) {
            return new HeardSound(id, source, x, y, z, value, attenuationDistance, attenuates, relative, looping);
        }
    }
    record Profile(double radius, double duration) {}

    private WardenSoundPulseFilter() {}

    static boolean eligible(HeardSound sound) {
        if (!WardenSoundPolicy.eligible(sound.id, sound.source) || sound.relative || sound.looping
                || !Double.isFinite(sound.x) || !Double.isFinite(sound.y) || !Double.isFinite(sound.z)) return false;
        return true;
    }

    static Profile profile(HeardSound sound) {
        if (!eligible(sound) || !Float.isFinite(sound.volume) || sound.volume <= 0) return null;
        double strength = Math.sqrt(Math.min(1, sound.volume));
        return new Profile(3 + 9 * strength, 8 + 20 * strength);
    }

    static boolean withinHearingRange(HeardSound sound, double distanceSquared) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0) return false;
        if (!sound.attenuates) return true;
        double range = Math.max(1, sound.volume) * Math.max(1, sound.attenuationDistance);
        return distanceSquared < range * range;
    }

    static boolean suppressOwnMovement(HeardSound sound, boolean sneaking, double distanceToFeetSquared) {
        if (!sneaking || sound.source != SoundSource.PLAYERS || distanceToFeetSquared > 2.25) return false;
        // Source events carry coordinates, not an entity ID. Only nearby movement sounds are suppressed.
        return WardenSoundPolicy.movement(sound.id);
    }
}
