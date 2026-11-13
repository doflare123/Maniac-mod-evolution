package org.example.maniacrevolution.lich;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Shared, deterministic gameplay values; rendering never drives these clocks. */
public final class LichRules {
    public static final double SKELETON_ATTACK_RANGE=28,SKELETON_TRACK_RANGE=32;
    public static final float SKELETON_SPREAD=2;
    public static final int PATROL_RADIUS=10;
    private LichRules() {}
    public static boolean safeSurface(Level level,BlockPos pos,double width,double height) {
        var floor=pos.below();
        return level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)
                &&level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)
                &&level.noCollision(new AABB(pos.getX()+.5-width/2,pos.getY(),pos.getZ()+.5-width/2,
                pos.getX()+.5+width/2,pos.getY()+height,pos.getZ()+.5+width/2));
    }
    public enum Kind {
        ZOMBIE("Зомби", 20, 4, 5, .23, 20),
        SKELETON("Скелет", 10, 4, 5, .23, 40),
        GHOUL("Гуль", 15, 2, 5, .30, 20),
        BUTCHER("Мясник", 40, 8, 15, .21, 40);
        public final String title;
        public final int hp, damage, cost, interval;
        public final double speed;
        Kind(String title, int hp, int damage, int cost, double speed, int interval) {
            this.title = title; this.hp = hp; this.damage = damage; this.cost = cost;
            this.speed = speed; this.interval = interval;
        }
    }
    public enum Order {
        FOLLOW("Следовать"), STAND("Стоять"), GUARD("Охранять ПК"), PATROL("Патруль"),
        SEARCH("Поиск"), CHASE("Преследовать"), RETURN("Вернуться"), MOVE("Перейти");
        public final String title;
        Order(String title) { this.title = title; }
    }
    public static int missing(int army, int corpses, long now, long delayedUntil) {
        return now < delayedUntil ? 0 : Math.max(0, 3 - army - corpses);
    }
    public static int hitCooldown(int phase) { return phase >= 3 ? 40 : 100; }
    public static boolean castInterrupted(double movementSquared, boolean hit, boolean stunned) {
        return movementSquared > .0025 || hit || stunned;
    }
}
