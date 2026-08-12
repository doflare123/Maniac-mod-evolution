package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

final class NightmareTrialBuilder {
    private NightmareTrialBuilder() {
    }

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
                area.setBlock(x, 0, z, floor);
                area.setBlock(x, NightmareConfig.ARENA_WALL_HEIGHT + 1, z, wall);
                if (border) {
                    for (int y = 1; y <= NightmareConfig.ARENA_WALL_HEIGHT; y++) {
                        area.setBlock(x, y, z, wall);
                    }
                } else {
                    for (int y = 1; y <= NightmareConfig.ARENA_WALL_HEIGHT; y++) {
                        area.setBlock(x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        int coverCount = Math.min(NightmareConfig.ARENA_COVER_COUNT,
                Math.max(1, ((area.width - 2) * (area.depth - 2)) / 8));
        for (int i = 0; i < coverCount; i++) {
            int x = randomInside(random, area.width);
            int z = randomInside(random, area.depth);
            int height = 2 + random.nextInt(3);
            for (int y = 1; y <= height; y++) {
                area.setBlock(x, y, z, cover);
            }
        }

        for (int i = 0; i < NightmareConfig.ARENA_MOB_COUNT; i++) {
            Mob mob = createArenaMob(level, random);
            mob.moveTo(origin.getX() + randomInside(random, area.width) + 0.5D, origin.getY() + 1,
                    origin.getZ() + randomInside(random, area.depth) + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
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

        Set<BlockPos> walkable = new HashSet<>();
        for (int i = 0; i < route.length - 1; i++) {
            addCorridor(walkable, route[i][0], route[i][1], route[i + 1][0], route[i + 1][1],
                    area.width, area.depth);
        }
        for (int i = 1; i < route.length - 1; i++) {
            addRoom(walkable, route[i][0], route[i][1], 2, area.width, area.depth);
        }

        buildRaceShell(area, walkable, floor, wall);

        for (int distance = 14; distance < NightmareConfig.FEAR_RACE_LENGTH - 10; distance += 9) {
            int[] point = pointOnRoute(route, distance);
            if (point == null) {
                continue;
            }
            if (!nearRouteTurn(route, point[0], point[1], 7) && (distance / 9) % 2 == 0) {
                placeJumpBarrier(area, point, obstacle);
                continue;
            }
            int side = random.nextBoolean() ? -1 : 1;
            area.setBlock(point[0] + side, 1, point[1], obstacle);
            if (random.nextBoolean()) {
                area.setBlock(point[0] + side, 2, point[1], obstacle);
            }
        }

        placeFinishDoor(area, raceFinish(origin));

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

    private static void addCorridor(Set<BlockPos> walkable, int x1, int z1, int x2, int z2,
                                    int width, int depth) {
        int dx = Integer.compare(x2, x1);
        int dz = Integer.compare(z2, z1);
        int x = x1;
        int z = z1;
        while (true) {
            addRoom(walkable, x, z, NightmareConfig.FEAR_RACE_CORRIDOR_WIDTH / 2, width, depth);
            if (x == x2 && z == z2) {
                break;
            }
            if (x != x2) {
                x += dx;
            }
            if (z != z2) {
                z += dz;
            }
        }
    }

    private static void addRoom(Set<BlockPos> walkable, int x, int z, int radius, int width, int depth) {
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                int blockX = x + offsetX;
                int blockZ = z + offsetZ;
                if (blockX >= 0 && blockX < width && blockZ >= 0 && blockZ < depth) {
                    walkable.add(new BlockPos(blockX, 0, blockZ));
                }
            }
        }
    }

    private static void buildRaceShell(TrialArea area, Set<BlockPos> walkable,
                                       BlockState floor, BlockState wall) {
        for (BlockPos cell : walkable) {
            int x = cell.getX();
            int z = cell.getZ();
            area.setBlock(x, 0, z, floor);
            area.setBlock(x, 4, z, wall);
            for (int y = 1; y <= 3; y++) {
                area.setBlock(x, y, z, Blocks.AIR.defaultBlockState());
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = cell.relative(direction);
                if (neighbor.getX() < 0 || neighbor.getX() >= area.width
                        || neighbor.getZ() < 0 || neighbor.getZ() >= area.depth
                        || walkable.contains(neighbor)) {
                    continue;
                }
                area.setBlock(neighbor.getX(), 0, neighbor.getZ(), floor);
                for (int y = 1; y <= 3; y++) {
                    area.setBlock(neighbor.getX(), y, neighbor.getZ(), wall);
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
                return new int[]{
                        x1 + Integer.compare(x2, x1) * remaining,
                        z1 + Integer.compare(z2, z1) * remaining,
                        Integer.compare(x2, x1),
                        Integer.compare(z2, z1)
                };
            }
            remaining -= length;
        }
        return null;
    }

    private static boolean nearRouteTurn(int[][] route, int x, int z, int minDistance) {
        int minDistanceSqr = minDistance * minDistance;
        for (int i = 1; i < route.length - 1; i++) {
            int dx = x - route[i][0];
            int dz = z - route[i][1];
            if (dx * dx + dz * dz <= minDistanceSqr) {
                return true;
            }
        }
        return false;
    }

    private static void placeJumpBarrier(TrialArea area, int[] point, BlockState obstacle) {
        if (point[2] != 0) {
            for (int dz = -1; dz <= 1; dz++) {
                area.setBlock(point[0], 1, point[1] + dz, obstacle);
            }
        } else {
            for (int dx = -1; dx <= 1; dx++) {
                area.setBlock(point[0] + dx, 1, point[1], obstacle);
            }
        }
    }

    private static void placeFinishDoor(TrialArea area, BlockPos worldDoorPos) {
        BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos relative = worldDoorPos.subtract(area.origin);
        area.setBlock(relative, lower);
        area.setBlock(relative.above(), upper);
    }

    private static Mob createArenaMob(ServerLevel level, Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> new Zombie(EntityType.ZOMBIE, level);
            case 1 -> new Skeleton(EntityType.SKELETON, level);
            default -> new Husk(EntityType.HUSK, level);
        };
    }

    private static int randomInside(Random random, int size) {
        return 1 + random.nextInt(Math.max(1, size - 2));
    }
}
