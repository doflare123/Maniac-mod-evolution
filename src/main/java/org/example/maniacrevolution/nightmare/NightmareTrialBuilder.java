package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.entity.FearChaserEntity;
import org.example.maniacrevolution.entity.ModEntities;

import java.util.Random;
import java.util.UUID;

final class NightmareTrialBuilder {
    private NightmareTrialBuilder() {}

    static TrialArea buildArena(ServerLevel level, BlockPos origin, long seed) {
        TrialArea area = new TrialArea(level, origin, NightmareConfig.ARENA_SIZE,
                NightmareConfig.ARENA_WALL_HEIGHT + 2, NightmareConfig.ARENA_SIZE);
        Random random = new Random(seed);
        BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState cover = Blocks.BLACKSTONE.defaultBlockState();

        for (int x = 0; x < area.width; x++) {
            for (int z = 0; z < area.depth; z++) {
                boolean border = x == 0 || z == 0 || x == area.width - 1 || z == area.depth - 1;
                level.setBlock(origin.offset(x, 0, z), floor, 3);
                level.setBlock(origin.offset(x, NightmareConfig.ARENA_WALL_HEIGHT + 1, z), wall, 3);
                if (border) {
                    for (int y = 1; y <= NightmareConfig.ARENA_WALL_HEIGHT; y++) {
                        level.setBlock(origin.offset(x, y, z), wall, 3);
                    }
                }
            }
        }

        for (int i = 0; i < NightmareConfig.ARENA_COVER_COUNT; i++) {
            int x = 3 + random.nextInt(area.width - 6);
            int z = 3 + random.nextInt(area.depth - 6);
            int h = 2 + random.nextInt(3);
            for (int y = 1; y <= h; y++) {
                level.setBlock(origin.offset(x, y, z), cover, 3);
            }
        }

        for (int i = 0; i < NightmareConfig.ARENA_MOB_COUNT; i++) {
            Mob mob = createArenaMob(level, random);
            mob.moveTo(origin.getX() + 4 + random.nextInt(area.width - 8), origin.getY() + 1,
                    origin.getZ() + 4 + random.nextInt(area.depth - 8), random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
            area.entities.add(mob);
        }

        return area;
    }

    static TrialArea buildRace(ServerLevel level, BlockPos origin, UUID target, long seed, long activeAtGameTime) {
        TrialArea area = new TrialArea(level, origin, NightmareConfig.FEAR_RACE_WIDTH,
                5, NightmareConfig.FEAR_RACE_LENGTH + 8);
        Random random = new Random(seed);
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState wall = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState obstacle = Blocks.BLACKSTONE.defaultBlockState();

        for (int x = 0; x < area.width; x++) {
            for (int z = 0; z < area.depth; z++) {
                level.setBlock(origin.offset(x, 0, z), floor, 3);
                level.setBlock(origin.offset(x, 4, z), wall, 3);
                if (x == 0 || x == area.width - 1) {
                    for (int y = 1; y <= 3; y++) {
                        level.setBlock(origin.offset(x, y, z), wall, 3);
                    }
                }
            }
        }

        for (int z = 12; z < NightmareConfig.FEAR_RACE_LENGTH - 8; z += 8) {
            int gap = 1 + random.nextInt(area.width - 2);
            for (int x = 1; x < area.width - 1; x++) {
                if (x == gap) continue;
                level.setBlock(origin.offset(x, 1, z), obstacle, 3);
                if (random.nextBoolean()) level.setBlock(origin.offset(x, 2, z), obstacle, 3);
            }
        }

        FearChaserEntity chaser = new FearChaserEntity(ModEntities.FEAR_CHASER.get(), level);
        chaser.setTargetPlayer(target);
        chaser.setActiveAtGameTime(activeAtGameTime);
        chaser.moveTo(origin.getX() + area.width / 2.0D, origin.getY() + 1,
                origin.getZ() + 1.5D, 0.0F, 0.0F);
        level.addFreshEntity(chaser);
        area.entities.add(chaser);
        return area;
    }

    static BlockPos arenaSpawn(BlockPos origin) {
        return origin.offset(NightmareConfig.ARENA_SIZE / 2, 1, NightmareConfig.ARENA_SIZE / 2);
    }

    static BlockPos raceSpawn(BlockPos origin) {
        return origin.offset(NightmareConfig.FEAR_RACE_WIDTH / 2, 1, 4);
    }

    static BlockPos raceFinish(BlockPos origin) {
        return origin.offset(NightmareConfig.FEAR_RACE_WIDTH / 2, 1, NightmareConfig.FEAR_RACE_LENGTH);
    }

    private static Mob createArenaMob(ServerLevel level, Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> new Zombie(EntityType.ZOMBIE, level);
            case 1 -> new Skeleton(EntityType.SKELETON, level);
            default -> new Husk(EntityType.HUSK, level);
        };
    }
}
