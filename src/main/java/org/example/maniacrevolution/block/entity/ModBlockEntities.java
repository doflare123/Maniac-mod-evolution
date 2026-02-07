package org.example.maniacrevolution.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.block.ModBlocks;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Maniacrev.MODID);

    public static final RegistryObject<BlockEntityType<FNAFGeneratorBlockEntity>> FNAF_GENERATOR =
            BLOCK_ENTITIES.register("fnaf_generator", () ->
                    BlockEntityType.Builder.of(FNAFGeneratorBlockEntity::new,
                            ModBlocks.FNAF_GENERATOR.get()).build(null));
}