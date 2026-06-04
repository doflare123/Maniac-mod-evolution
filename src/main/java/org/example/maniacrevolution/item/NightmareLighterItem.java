package org.example.maniacrevolution.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.nightmare.NightmareConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NightmareLighterItem extends Item {
    private static final Map<UUID, BlockPos> LIGHT_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> LIGHTER_FUEL = new HashMap<>();

    public NightmareLighterItem() {
        super(new Properties().stacksTo(1));
    }

    public static void tickHeld(ServerPlayer player, ItemStack stack, EquipmentSlot breakSlot) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!(stack.getItem() instanceof NightmareLighterItem)) {
            removeLight(player);
            return;
        }

        moveLight(player, level, findLightPos(player));

        if (player.tickCount % NightmareConfig.LIGHTER_DAMAGE_INTERVAL_TICKS == 0) {
            UUID uuid = player.getUUID();
            int fuel = LIGHTER_FUEL.getOrDefault(uuid, NightmareConfig.LIGHTER_DURABILITY) - 1;
            if (fuel <= 0) {
                LIGHTER_FUEL.remove(uuid);
                stack.shrink(1);
                player.broadcastBreakEvent(breakSlot);
                removeLight(player);
            } else {
                LIGHTER_FUEL.put(uuid, fuel);
            }
        }
    }

    public static void removeLight(ServerPlayer player) {
        BlockPos old = LIGHT_POSITIONS.remove(player.getUUID());
        if (old != null && player.level().getBlockState(old).is(Blocks.LIGHT)) {
            player.level().setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void resetFuel(ServerPlayer player) {
        LIGHTER_FUEL.put(player.getUUID(), NightmareConfig.LIGHTER_DURABILITY);
    }

    public static void clearState(ServerPlayer player) {
        removeLight(player);
        LIGHTER_FUEL.remove(player.getUUID());
    }

    private static BlockPos findLightPos(ServerPlayer player) {
        BlockPos head = player.blockPosition().above();
        if (canReplaceWithLight(player.level(), head)) return head;

        BlockPos feet = player.blockPosition();
        if (canReplaceWithLight(player.level(), feet)) return feet;

        return head;
    }

    private static boolean canReplaceWithLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.LIGHT);
    }

    private static void moveLight(ServerPlayer player, ServerLevel level, BlockPos lightPos) {
        BlockPos old = LIGHT_POSITIONS.get(player.getUUID());
        if (old != null && !old.equals(lightPos) && level.getBlockState(old).is(Blocks.LIGHT)) {
            level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        }

        if (canReplaceWithLight(level, lightPos)) {
            level.setBlock(lightPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 6), 3);
            LIGHT_POSITIONS.put(player.getUUID(), lightPos.immutable());
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
