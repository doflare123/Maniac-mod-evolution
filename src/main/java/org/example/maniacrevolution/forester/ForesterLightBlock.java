package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Invisible temporary light; scheduled cleanup also survives a world reload. */
public final class ForesterLightBlock extends Block {
    private static final Map<net.minecraft.core.GlobalPos, Long> EXPIRY = new HashMap<>();
    public ForesterLightBlock() {
        super(Properties.of().air().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(s -> 15).strength(-1, 3600000));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    public void illuminate(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        if (state.getBlock() != this && !state.is(Blocks.AIR)) return;
        if (state.getBlock() != this) level.setBlockAndUpdate(pos, defaultBlockState());
        EXPIRY.put(net.minecraft.core.GlobalPos.of(level.dimension(), pos.immutable()), level.getGameTime() + 3);
        level.scheduleTick(pos, this, 3);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var key = net.minecraft.core.GlobalPos.of(level.dimension(), pos);
        if (EXPIRY.getOrDefault(key, 0L) <= level.getGameTime()) { EXPIRY.remove(key); level.removeBlock(pos, false); }
        else level.scheduleTick(pos, this, 3);
    }
    public static void clear() { EXPIRY.clear(); }
}
