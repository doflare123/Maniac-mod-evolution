package org.example.maniacrevolution.warden;

import net.minecraft.sounds.SoundSource;

/** Sound category/ID exclusions shared by the server and the independent client test. */
public final class WardenSoundPolicy {
    private WardenSoundPolicy() {}

    public static boolean eligible(String id, SoundSource source) {
        return id != null && source != null && source != SoundSource.MASTER && source != SoundSource.MUSIC
                && source != SoundSource.RECORDS && source != SoundSource.AMBIENT && source != SoundSource.WEATHER
                && !id.startsWith("minecraft:ui.") && !id.equals("maniacrev:warden_pulse") && !id.equals("maniacrev:warden_echo");
    }
    public static boolean movement(String id) {
        return id.endsWith(".step") || id.endsWith(".small_fall") || id.endsWith(".big_fall")
                || id.endsWith(".land") || id.endsWith(".jump");
    }
    public static boolean coveredPlayerAction(String id, SoundSource source) {
        return source == SoundSource.PLAYERS && (movement(id) || id.startsWith("minecraft:entity.player.attack.")
                || id.startsWith("minecraft:entity.player.hurt"));
    }
}
