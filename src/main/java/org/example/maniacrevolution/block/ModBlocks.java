package org.example.maniacrevolution.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Maniacrev.MODID);

    public static final RegistryObject<Block> SALT_BLOCK = BLOCKS.register("salt_block",
            () -> new SaltBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.0F)
                    .sound(SoundType.SAND)
                    .noCollission()
                    .noOcclusion()
                    .dynamicShape()
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}