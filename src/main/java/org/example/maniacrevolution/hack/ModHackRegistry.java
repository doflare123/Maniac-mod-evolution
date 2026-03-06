package org.example.maniacrevolution.hack;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.Maniacrev;

/**
 * Регистрация блока компьютера, его BlockEntity и предмета.
 **/
public class ModHackRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Maniacrev.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Maniacrev.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Maniacrev.MODID);

    // ── Блок ─────────────────────────────────────────────────────────────────
    public static final RegistryObject<Block> COMPUTER_BLOCK =
            BLOCKS.register("computer", () -> new ComputerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .noOcclusion()
                            .sound(SoundType.METAL)));

    // ── BlockEntity ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("computer", () -> BlockEntityType.Builder
                    .of(ComputerBlockEntity::new, COMPUTER_BLOCK.get())
                    .build(null));

    // ── Предмет (дроп + инвентарь) ────────────────────────────────────────────
    public static final RegistryObject<Item> COMPUTER_ITEM =
            ITEMS.register("computer", () -> new BlockItem(
                    COMPUTER_BLOCK.get(),
                    new Item.Properties()));
}