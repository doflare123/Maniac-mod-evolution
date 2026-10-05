package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.WardenShriekSoundPacket;
import org.example.maniacrevolution.warden.WardenShriekerRules;
import java.util.HashMap;
import java.util.Map;

/** Independently stoppable vanilla screams; never stops unrelated vanilla shriekers. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenShriekerSounds {
    private static final Map<BlockPos, Shriek> SOUNDS = new HashMap<>();
    private static ClientLevel level;
    private static final class Shriek extends AbstractTickableSoundInstance {
        private final ClientLevel world;
        private final BlockPos pos;
        private int age;
        Shriek(ClientLevel world, BlockPos pos) {
            super(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.world = world; this.pos = pos.immutable();
            x = pos.getX() + 0.5; y = pos.getY() + 0.5; z = pos.getZ() + 0.5;
            volume = 2; pitch = 0.6F;
        }
        @Override public void tick() {
            var mc = Minecraft.getInstance();
            if (mc.level != world || !world.hasChunkAt(pos) || ++age >= WardenShriekerRules.SHRIEK_TICKS) { stop(); return; }
            // The start packet can arrive before the chunk's block-state update. Server stop packets
            // handle disable/removal; do not cancel a confirmed scream from stale client SHRIEKING.
        }
    }
    public static void accept(WardenShriekSoundPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        if (level != mc.level) { clear(); level = mc.level; }
        var old = SOUNDS.remove(packet.pos()); if (old != null) mc.getSoundManager().stop(old);
        if (packet.start() && mc.level.hasChunkAt(packet.pos()) && SOUNDS.size() < 1024) {
            var sound = new Shriek(mc.level, packet.pos()); SOUNDS.put(packet.pos().immutable(), sound); mc.getSoundManager().play(sound);
        }
    }
    private static void clear() {
        var manager = Minecraft.getInstance().getSoundManager(); SOUNDS.values().forEach(manager::stop); SOUNDS.clear(); level = null;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (level != Minecraft.getInstance().level) { clear(); return; }
        SOUNDS.values().removeIf(Shriek::isStopped);
    }
}
