package org.example.maniacrevolution.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

/**
 * Если у вас уже есть ModSounds — просто добавьте строку HEARTBEAT.
 * Если нет — используйте весь класс как шаблон.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Maniacrev.MODID);

    // Добавьте эту строку в ваш существующий ModSounds:
    public static final RegistryObject<SoundEvent> HEARTBEAT =
            SOUND_EVENTS.register("heartbeat",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "heartbeat")));

    // Регистрация в главном классе мода:
    // ModSounds.SOUND_EVENTS.register(modEventBus);
}