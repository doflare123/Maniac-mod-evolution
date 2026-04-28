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

    public static final RegistryObject<SoundEvent> SCREAM_AUDIO =
            SOUND_EVENTS.register("scream_audio",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "scream_audio")));

    public static final RegistryObject<SoundEvent> QTE_SUCCESS =
            SOUND_EVENTS.register("qte_success",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "qte_success")));

    public static final RegistryObject<SoundEvent> QTE_CRIT =
            SOUND_EVENTS.register("qte_crit",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "qte_crit")));

    public static final RegistryObject<SoundEvent> QTE_FAIL =
            SOUND_EVENTS.register("qte_fail",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "qte_fail")));

    public static final RegistryObject<SoundEvent> COIN_FLIP =
            SOUND_EVENTS.register("coin_flip",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "coin_flip")));

    public static final RegistryObject<SoundEvent> SLOT_INSERT =
            SOUND_EVENTS.register("slot_insert",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "slot_insert")));

    public static final RegistryObject<SoundEvent> SLOT_SPIN =
            SOUND_EVENTS.register("slot_spin",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "slot_spin")));

    public static final RegistryObject<SoundEvent> SLOT_WIN =
            SOUND_EVENTS.register("slot_win",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "slot_win")));

    public static final RegistryObject<SoundEvent> SLOT_JACKPOT =
            SOUND_EVENTS.register("slot_jackpot",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(Maniacrev.MODID, "slot_jackpot")));
}
