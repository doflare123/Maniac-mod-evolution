package org.example.maniacrevolution.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Утилитный класс для создания геометрических фигур из частиц
 */
public class ParticleShapes {

    /**
     * Рисует пентаграмму на земле вокруг игрока
     */
    public static void drawPentagram(ServerPlayer player, ParticleOptions particle, double radius, int pointsPerLine) {
        Vec3 center = player.position();

        // Пять вершин пентаграммы
        Vec3[] vertices = new Vec3[5];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72); // -90 чтобы одна вершина смотрела вверх
            vertices[i] = new Vec3(
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.1, // Чуть выше земли
                    center.z + Math.sin(angle) * radius
            );
        }

        // Рисуем линии пентаграммы (соединяем каждую вершину через одну)
        int[] connectionOrder = {0, 2, 4, 1, 3, 0}; // Порядок соединения для звезды

        for (int i = 0; i < connectionOrder.length - 1; i++) {
            Vec3 start = vertices[connectionOrder[i]];
            Vec3 end = vertices[connectionOrder[i + 1]];
            drawLine(player.serverLevel(), start, end, particle, pointsPerLine);
        }
    }

    /**
     * Рисует вращающуюся пентаграмму
     */
    public static void drawRotatingPentagram(ServerPlayer player, ParticleOptions particle, double radius, int pointsPerLine, double rotation) {
        Vec3 center = player.position();

        Vec3[] vertices = new Vec3[5];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72 + rotation);
            vertices[i] = new Vec3(
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.1,
                    center.z + Math.sin(angle) * radius
            );
        }

        int[] connectionOrder = {0, 2, 4, 1, 3, 0};

        for (int i = 0; i < connectionOrder.length - 1; i++) {
            Vec3 start = vertices[connectionOrder[i]];
            Vec3 end = vertices[connectionOrder[i + 1]];
            drawLine(player.serverLevel(), start, end, particle, pointsPerLine);
        }
    }

    /**
     * Рисует магический круг вокруг игрока
     */
    public static void drawCircle(ServerPlayer player, ParticleOptions particle, double radius, int points) {
        Vec3 center = player.position();

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            player.serverLevel().sendParticles(
                    particle,
                    x, center.y + 0.1, z,
                    1, 0, 0, 0, 0
            );
        }
    }

    /**
     * Рисует спираль вокруг игрока
     */
    public static void drawSpiral(ServerPlayer player, ParticleOptions particle, double maxRadius, double height, int points, double offset) {
        Vec3 center = player.position();

        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = (4 * Math.PI * progress) + offset; // 2 полных оборота
            double radius = maxRadius * progress;
            double y = center.y + height * progress;

            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            player.serverLevel().sendParticles(
                    particle,
                    x, y, z,
                    1, 0, 0, 0, 0
            );
        }
    }

    /**
     * Рисует спиральный столб
     */
    public static void drawHelixPillar(ServerPlayer player, ParticleOptions particle, double radius, double height, int helixes, int pointsPerHelix, double offset) {
        Vec3 center = player.position();

        for (int h = 0; h < helixes; h++) {
            double helixOffset = (2 * Math.PI * h) / helixes;

            for (int i = 0; i < pointsPerHelix; i++) {
                double progress = (double) i / pointsPerHelix;
                double angle = (4 * Math.PI * progress) + offset + helixOffset;
                double y = center.y + height * progress;

                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;

                player.serverLevel().sendParticles(
                        particle,
                        x, y, z,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    /**
     * Рисует сферу вокруг игрока
     */
    public static void drawSphere(ServerPlayer player, ParticleOptions particle, double radius, int density) {
        Vec3 center = player.position();

        // Используем метод золотого сечения для равномерного распределения точек на сфере
        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // Золотой угол

        for (int i = 0; i < density; i++) {
            double y = 1 - (i / (double) (density - 1)) * 2; // От 1 до -1
            double radiusAtY = Math.sqrt(1 - y * y);

            double theta = phi * i;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            player.serverLevel().sendParticles(
                    particle,
                    center.x + x * radius,
                    center.y + y * radius + 1.0, // +1 чтобы центр был на уровне игрока
                    center.z + z * radius,
                    1, 0, 0, 0, 0
            );
        }
    }

    /**
     * Рисует полую сферу (только оболочка)
     */
    public static void drawHollowSphere(ServerPlayer player, ParticleOptions particle, double radius, int rings, int pointsPerRing) {
        Vec3 center = player.position().add(0, 1, 0);

        for (int r = 0; r < rings; r++) {
            double phi = Math.PI * r / (rings - 1); // От 0 до PI
            double ringRadius = Math.sin(phi) * radius;
            double y = Math.cos(phi) * radius;

            for (int p = 0; p < pointsPerRing; p++) {
                double theta = 2 * Math.PI * p / pointsPerRing;
                double x = Math.cos(theta) * ringRadius;
                double z = Math.sin(theta) * ringRadius;

                player.serverLevel().sendParticles(
                        particle,
                        center.x + x,
                        center.y + y,
                        center.z + z,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    /**
     * Рисует руны по кругу
     */
    public static void drawRuneCircle(ServerPlayer player, ParticleOptions particle, double radius, int runes) {
        Vec3 center = player.position();

        for (int i = 0; i < runes; i++) {
            double angle = (2 * Math.PI * i) / runes;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            // Рисуем маленький символ (крест)
            drawSmallCross(player.serverLevel(), new Vec3(x, center.y + 0.1, z), particle, 0.2, 5);
        }
    }

    /**
     * Рисует взрыв частиц (расширяющееся кольцо)
     */
    public static void drawExplosionRing(ServerPlayer player, ParticleOptions particle, double radius, int points) {
        Vec3 center = player.position();

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            // Частицы движутся наружу
            double velocityX = Math.cos(angle) * 0.2;
            double velocityZ = Math.sin(angle) * 0.2;

            player.serverLevel().sendParticles(
                    particle,
                    x, center.y + 1, z,
                    1, velocityX, 0.1, velocityZ, 0.1
            );
        }
    }

    /**
     * Рисует столб света
     */
    public static void drawLightBeam(ServerPlayer player, ParticleOptions particle, double height, int density) {
        Vec3 center = player.position();

        for (int i = 0; i < density; i++) {
            double y = center.y + (height * i / density);

            player.serverLevel().sendParticles(
                    particle,
                    center.x, y, center.z,
                    1, 0, 0.05, 0, 0
            );
        }
    }

    /**
     * Рисует вихрь (торнадо из частиц)
     */
    public static void drawVortex(ServerPlayer player, ParticleOptions particle, double baseRadius, double topRadius, double height, int layers, int pointsPerLayer, double rotation) {
        Vec3 center = player.position();

        for (int layer = 0; layer < layers; layer++) {
            double progress = (double) layer / layers;
            double y = center.y + height * progress;
            double currentRadius = baseRadius + (topRadius - baseRadius) * progress;

            for (int p = 0; p < pointsPerLayer; p++) {
                double angle = (2 * Math.PI * p / pointsPerLayer) + (rotation * layer);
                double x = center.x + Math.cos(angle) * currentRadius;
                double z = center.z + Math.sin(angle) * currentRadius;

                player.serverLevel().sendParticles(
                        particle,
                        x, y, z,
                        1, 0, 0.02, 0, 0
                );
            }
        }
    }

    /**
     * Рисует куб из частиц
     */
    public static void drawCube(ServerPlayer player, ParticleOptions particle, double size, int pointsPerEdge) {
        Vec3 center = player.position().add(0, size / 2, 0);
        double half = size / 2;

        // 12 рёбер куба
        Vec3[][] edges = {
                // Нижние рёбра
                {new Vec3(-half, -half, -half), new Vec3(half, -half, -half)},
                {new Vec3(half, -half, -half), new Vec3(half, -half, half)},
                {new Vec3(half, -half, half), new Vec3(-half, -half, half)},
                {new Vec3(-half, -half, half), new Vec3(-half, -half, -half)},

                // Верхние рёбра
                {new Vec3(-half, half, -half), new Vec3(half, half, -half)},
                {new Vec3(half, half, -half), new Vec3(half, half, half)},
                {new Vec3(half, half, half), new Vec3(-half, half, half)},
                {new Vec3(-half, half, half), new Vec3(-half, half, -half)},

                // Вертикальные рёбра
                {new Vec3(-half, -half, -half), new Vec3(-half, half, -half)},
                {new Vec3(half, -half, -half), new Vec3(half, half, -half)},
                {new Vec3(half, -half, half), new Vec3(half, half, half)},
                {new Vec3(-half, -half, half), new Vec3(-half, half, half)},
        };

        for (Vec3[] edge : edges) {
            drawLine(player.serverLevel(),
                    center.add(edge[0]),
                    center.add(edge[1]),
                    particle,
                    pointsPerEdge);
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    /**
     * Рисует линию между двумя точками
     */
    private static void drawLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            Vec3 point = start.add(end.subtract(start).scale(progress));

            level.sendParticles(
                    particle,
                    point.x, point.y, point.z,
                    1, 0, 0, 0, 0
            );
        }
    }

    /**
     * Рисует маленький крест (для рун)
     */
    private static void drawSmallCross(ServerLevel level, Vec3 center, ParticleOptions particle, double size, int pointsPerLine) {
        // Горизонтальная линия
        drawLine(level,
                center.add(-size, 0, 0),
                center.add(size, 0, 0),
                particle,
                pointsPerLine);

        // Вертикальная линия
        drawLine(level,
                center.add(0, 0, -size),
                center.add(0, 0, size),
                particle,
                pointsPerLine);
    }

    /**
     * Рисует двойную спираль ДНК
     */
    public static void drawDoubleHelix(ServerPlayer player, ParticleOptions particle1, ParticleOptions particle2, double radius, double height, int points, double offset) {
        Vec3 center = player.position();

        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = (4 * Math.PI * progress) + offset;
            double y = center.y + height * progress;

            // Первая спираль
            double x1 = center.x + Math.cos(angle) * radius;
            double z1 = center.z + Math.sin(angle) * radius;
            player.serverLevel().sendParticles(particle1, x1, y, z1, 1, 0, 0, 0, 0);

            // Вторая спираль (противоположная)
            double x2 = center.x + Math.cos(angle + Math.PI) * radius;
            double z2 = center.z + Math.sin(angle + Math.PI) * radius;
            player.serverLevel().sendParticles(particle2, x2, y, z2, 1, 0, 0, 0, 0);

            // Соединительные линии каждые 10 точек
            if (i % 10 == 0) {
                drawLine(player.serverLevel(),
                        new Vec3(x1, y, z1),
                        new Vec3(x2, y, z2),
                        particle1,
                        3);
            }
        }
    }
}