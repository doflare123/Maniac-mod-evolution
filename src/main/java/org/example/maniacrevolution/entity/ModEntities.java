package org.example.maniacrevolution.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

import static net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES;

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

    public static final RegistryObject<EntityType<BloodMarkerEntity>> BLOOD_MARKER =
            ENTITIES.register("blood_marker",
                    () -> EntityType.Builder.<BloodMarkerEntity>of(BloodMarkerEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.1F) // Маленький размер
                            .clientTrackingRange(64) // Дистанция отслеживания
                            .updateInterval(20) // Интервал обновления
                            .noSave() // Не сохраняется в мир
                            .noSummon() // Нельзя призвать командой
                            .fireImmune() // Не горит
                            .build("blood_marker")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
