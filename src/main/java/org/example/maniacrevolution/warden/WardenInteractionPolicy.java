package org.example.maniacrevolution.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;

/** Shared block priority for wave input and vision. Never executes a block's use method. */
public final class WardenInteractionPolicy {
    private static final Class<?>[] SIGNATURE = {BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class};
    private static final ClassValue<Boolean> INTERACTIVE = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            if (WardenShriekerBlock.class.isAssignableFrom(type)) return true;
            for (var method : type.getMethods()) if (method.getReturnType() == InteractionResult.class && Arrays.equals(method.getParameterTypes(), SIGNATURE))
                return method.getDeclaringClass() != net.minecraft.world.level.block.state.BlockBehaviour.class
                        && method.getDeclaringClass() != Block.class && method.getDeclaringClass() != StairBlock.class;
            return false;
        }
    };
    private static final ClassValue<Boolean> ENTITY_INTERACTION = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            for (var current = type; current != null; current = current.getSuperclass()) {
                if (current == Entity.class || current == LivingEntity.class || current == Mob.class || current == Player.class) continue;
                for (var method : current.getDeclaredMethods()) if (method.getReturnType() == InteractionResult.class
                        && (Arrays.equals(method.getParameterTypes(), new Class<?>[]{Player.class, InteractionHand.class})
                        || Arrays.equals(method.getParameterTypes(), new Class<?>[]{Player.class, Vec3.class, InteractionHand.class}))) return true;
            }
            return false;
        }
    };
    private WardenInteractionPolicy() {}
    public static boolean hasInteraction(Class<?> type) { return INTERACTIVE.get(type); }
    public static boolean hasEntityInteraction(Class<?> type) { return ENTITY_INTERACTION.get(type); }
    public static boolean interactive(Entity entity) { return hasEntityInteraction(entity.getClass()); }
    public static boolean interactive(BlockState state, Level level, BlockPos pos) {
        return hasInteraction(state.getBlock().getClass()) || state.getMenuProvider(level, pos) != null;
    }
}
