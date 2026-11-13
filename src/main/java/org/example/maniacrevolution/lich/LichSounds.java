package org.example.maniacrevolution.lich;

import java.util.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.*;
import org.example.maniacrevolution.Maniacrev;

public final class LichSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Maniacrev.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> EVENTS = new HashMap<>();
    static {
        for (String id : List.of("book", "command", "cast.raise", "cast.absorb", "cast.repair", "absorb.complete", "repair.complete",
                "emerge", "emerge.ready", "phylactery.hit", "phylactery.carried_hit", "phylactery.destroy",
                "ghoul.attack", "ghoul.hurt", "ghoul.death", "ghoul.step", "butcher.attack", "butcher.hurt", "butcher.death", "butcher.step"))
            EVENTS.put(id, SOUNDS.register("lich." + id, () -> SoundEvent.createVariableRangeEvent(Maniacrev.loc("lich." + id))));
    }
    public static void play(Entity entity, String id, float volume) {
        id = switch (id) {
            case "emerge_ready" -> "emerge.ready";
            case "absorb_complete" -> "absorb.complete";
            case "repair_complete" -> "repair.complete";
            case "phylactery_hit" -> "phylactery.hit";
            case "phylactery_carried_hit" -> "phylactery.carried_hit";
            case "phylactery_destroy" -> "phylactery.destroy";
            default -> id;
        };
        var event = EVENTS.get(id); if (event == null) return;
        entity.level().playSound(null, entity.blockPosition(), event.get(), SoundSource.HOSTILE, volume, 1);
    }
}
