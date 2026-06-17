package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.sound.ModSounds;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class JackpotMusicHandler {
    private static final Set<UUID> ACTIVE_JACKPOTS = new HashSet<>();
    private static JackpotMusicSound activeSound;

    private JackpotMusicHandler() {
    }

    public static void setJackpotActive(UUID playerId, boolean active) {
        if (active) {
            ACTIVE_JACKPOTS.add(playerId);
        } else {
            ACTIVE_JACKPOTS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            stopAll();
            return;
        }

        if (ACTIVE_JACKPOTS.isEmpty()) {
            stopAll();
            return;
        }

        if (activeSound == null || activeSound.isStopped()) {
            activeSound = new JackpotMusicSound();
            minecraft.getSoundManager().play(activeSound);
        }
    }

    private static void stopAll() {
        ACTIVE_JACKPOTS.clear();
        if (activeSound != null) {
            activeSound.stopSound();
            activeSound = null;
        }
    }

    private static final class JackpotMusicSound extends AbstractTickableSoundInstance {
        private JackpotMusicSound() {
            super(ModSounds.SLOT_JACKPOT.get(), SoundSource.PLAYERS, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.35f;
            this.pitch = 1.0f;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (ACTIVE_JACKPOTS.isEmpty()) {
                stop();
            }
        }

        private void stopSound() {
            stop();
        }
    }
}
