package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.sound.ModSounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class JackpotMusicHandler {
    private static final Map<UUID, JackpotMusicSound> ACTIVE_SOUNDS = new HashMap<>();

    private JackpotMusicHandler() {
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

        Set<UUID> activePlayers = new HashSet<>();
        for (Player player : minecraft.level.players()) {
            if (!player.hasEffect(ModEffects.JACKPOT.get())) {
                continue;
            }

            activePlayers.add(player.getUUID());
            JackpotMusicSound sound = ACTIVE_SOUNDS.get(player.getUUID());
            if (sound == null || sound.isStopped()) {
                sound = new JackpotMusicSound(player);
                ACTIVE_SOUNDS.put(player.getUUID(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }

        ACTIVE_SOUNDS.entrySet().removeIf(entry -> {
            if (activePlayers.contains(entry.getKey()) && !entry.getValue().isStopped()) {
                return false;
            }
            entry.getValue().stopSound();
            return true;
        });
    }

    private static void stopAll() {
        for (JackpotMusicSound sound : ACTIVE_SOUNDS.values()) {
            sound.stopSound();
        }
        ACTIVE_SOUNDS.clear();
    }

    private static final class JackpotMusicSound extends AbstractTickableSoundInstance {
        private final Player player;

        private JackpotMusicSound(Player player) {
            super(ModSounds.SLOT_JACKPOT.get(), SoundSource.PLAYERS, RandomSource.create());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.35f;
            this.pitch = 1.0f;
            this.relative = false;
            updatePosition();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !player.hasEffect(ModEffects.JACKPOT.get())) {
                stop();
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        private void stopSound() {
            stop();
        }
    }
}
