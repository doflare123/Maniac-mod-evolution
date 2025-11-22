package org.example.maniacrevolution.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Maniacrev.MODID);

    public static final RegistryObject<EntityType<MimicBlockEntity>> MIMIC_BLOCK =
            ENTITIES.register("mimic_block", () ->
                    EntityType.Builder.<MimicBlockEntity>of(MimicBlockEntity::new, MobCategory.MISC)
                            .sized(0.98f, 0.98f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .noSummon() // Нельзя заспавнить командой /summon
                            .build("mimic_block")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
