package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
        TrialArea area = new TrialArea(level, origin, NightmareConfig.FEAR_RACE_AREA_WIDTH,
                5, NightmareConfig.FEAR_RACE_LENGTH + 8);
        Random random = new Random(seed);
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState wall = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState obstacle = Blocks.BLACKSTONE.defaultBlockState();

        for (int x = 0; x < area.width; x++) {
            for (int z = 0; z < area.depth; z++) {
                level.setBlock(origin.offset(x, 0, z), floor, 3);
                level.setBlock(origin.offset(x, 4, z), wall, 3);
                for (int y = 1; y <= 3; y++) {
                    level.setBlock(origin.offset(x, y, z), wall, 3);
                }
            }
        }

        int center = area.width / 2;
        int[][] route = {
                {center, 3},
                {center, 20},
                {6, 20},
                {6, 40},
                {24, 40},
                {24, 60},
                {12, 60},
                {12, NightmareConfig.FEAR_RACE_LENGTH - 4}
        };

        for (int i = 0; i < route.length - 1; i++) {
            carveLine(level, origin, route[i][0], route[i][1], route[i + 1][0], route[i + 1][1]);
        }

        for (int i = 1; i < route.length - 1; i++) {
            carveRoom(level, origin, route[i][0], route[i][1], 2);
        }

        for (int i = 14; i < NightmareConfig.FEAR_RACE_LENGTH - 10; i += 9) {
            int[] point = pointOnRoute(route, i);
            if (point == null) continue;
            int side = random.nextBoolean() ? -1 : 1;
            level.setBlock(origin.offset(point[0] + side, 1, point[1]), obstacle, 3);
            if (random.nextBoolean()) {
                level.setBlock(origin.offset(point[0] + side, 2, point[1]), obstacle, 3);
            }
        }

        placeFinishDoor(level, raceFinish(origin));

        FearChaserEntity chaser = new FearChaserEntity(ModEntities.FEAR_CHASER.get(), level);
        chaser.setTargetPlayer(target);
        chaser.setActiveAtGameTime(activeAtGameTime);
        BlockPos chaserPos = raceChaserSpawn(origin);
        chaser.moveTo(chaserPos.getX() + 0.5D, chaserPos.getY(), chaserPos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(chaser);
        area.entities.add(chaser);
        return area;
    }

    static BlockPos arenaSpawn(BlockPos origin) {
        return origin.offset(NightmareConfig.ARENA_SIZE / 2, 1, NightmareConfig.ARENA_SIZE / 2);
    }

    static BlockPos raceSpawn(BlockPos origin) {
        return origin.offset(NightmareConfig.FEAR_RACE_AREA_WIDTH / 2, 1, 4);
    }

    static BlockPos raceFinish(BlockPos origin) {
        return origin.offset(12, 1, NightmareConfig.FEAR_RACE_LENGTH - 2);
    }

    private static BlockPos raceChaserSpawn(BlockPos origin) {
        return origin.offset(NightmareConfig.FEAR_RACE_AREA_WIDTH / 2, 1, 2);
    }

    private static void carveLine(ServerLevel level, BlockPos origin, int x1, int z1, int x2, int z2) {
        int dx = Integer.compare(x2, x1);
        int dz = Integer.compare(z2, z1);
        int x = x1;
        int z = z1;
        while (x != x2 || z != z2) {
            carveCorridor(level, origin, x, z);
            if (x != x2) x += dx;
            if (z != z2) z += dz;
        }
        carveCorridor(level, origin, x2, z2);
    }

    private static void carveCorridor(ServerLevel level, BlockPos origin, int x, int z) {
        int radius = NightmareConfig.FEAR_RACE_CORRIDOR_WIDTH / 2;
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                for (int y = 1; y <= 3; y++) {
                    level.setBlock(origin.offset(x + ox, y, z + oz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void carveRoom(ServerLevel level, BlockPos origin, int x, int z, int radius) {
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                for (int y = 1; y <= 3; y++) {
                    level.setBlock(origin.offset(x + ox, y, z + oz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static int[] pointOnRoute(int[][] route, int distance) {
        int remaining = distance;
        for (int i = 0; i < route.length - 1; i++) {
            int x1 = route[i][0];
            int z1 = route[i][1];
            int x2 = route[i + 1][0];
            int z2 = route[i + 1][1];
            int length = Math.abs(x2 - x1) + Math.abs(z2 - z1);
            if (remaining <= length) {
                return new int[] {
                        x1 + Integer.compare(x2, x1) * remaining,
                        z1 + Integer.compare(z2, z1) * remaining
                };
            }
            remaining -= length;
        }
        return null;
    }

    private static void placeFinishDoor(ServerLevel level, BlockPos worldDoorPos) {
        BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlock(worldDoorPos, lower, 3);
        level.setBlock(worldDoorPos.above(), upper, 3);
    }

    private static Mob createArenaMob(ServerLevel level, Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> new Zombie(EntityType.ZOMBIE, level);
            case 1 -> new Skeleton(EntityType.SKELETON, level);
            default -> new Husk(EntityType.HUSK, level);
        };
    }
}
