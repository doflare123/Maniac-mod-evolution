package org.example.maniacrevolution.techies;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.sound.ModSounds;

public final class TechiesSounds {
    private static RegistryObject<SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name,()->SoundEvent.createVariableRangeEvent(Maniacrev.loc(name)));
    }
    public static final RegistryObject<SoundEvent> RED_WARNING=register("techies_mine_red_warning");
    public static final RegistryObject<SoundEvent> RED_EXPLODE=register("techies_mine_red_explode");
    public static final RegistryObject<SoundEvent> GREEN_ACTIVATE=register("techies_mine_green_activate");
    public static final RegistryObject<SoundEvent> GREEN_EXPLODE=register("techies_mine_green_explode");
    public static final RegistryObject<SoundEvent> BLUE_WARNING=register("techies_mine_blue_warning");
    public static final RegistryObject<SoundEvent> BLUE_DRAIN=register("techies_mine_blue_drain");
    public static final RegistryObject<SoundEvent> INSTALL=register("techies_install_click");
    public static void init() {}
    public static SoundEvent warning(MineType type) { return switch(type) { case RED->RED_WARNING.get(); case GREEN->GREEN_ACTIVATE.get(); case BLUE->BLUE_WARNING.get(); }; }
    public static SoundEvent explosion(MineType type) { return switch(type) { case RED->RED_EXPLODE.get(); case GREEN->GREEN_EXPLODE.get(); case BLUE->BLUE_DRAIN.get(); }; }
}
