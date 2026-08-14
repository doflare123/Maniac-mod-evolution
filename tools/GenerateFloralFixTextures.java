import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One-shot deterministic generator for the small floral UI/world textures. */
public final class GenerateFloralFixTextures {
    private static final int TRANSPARENT = 0x00000000;

    public static void main(String[] args) throws IOException {
        File resources = new File("src/main/resources/assets/maniacrev/textures");
        File flowerAtlas = new File(resources, "bud_dispatcher/flowers.png");
        cleanChromaFringe(flowerAtlas);
        writeCardAtlas(flowerAtlas,
                new File(resources, "bud_dispatcher/flowers_cards.png"));
        writeOrchidBase(new File(resources, "gui/pink_orchid_base.png"));
        writeOrchidPetal(new File(resources, "gui/pink_orchid_petal.png"));
        writeVine(new File(resources, "bud_dispatcher/vine.png"));
        writeBudAtlas(flowerAtlas, new File(resources, "bud_dispatcher/bud.png"));
    }

    private static void cleanChromaFringe(File file) throws IOException {
        BufferedImage source = ImageIO.read(file);
        BufferedImage cleaned = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int color = source.getRGB(x, y);
                int flowerRow = Math.min(8, y * 9 / source.getHeight());
                boolean keepFlowerPalette = flowerRow == 3 || flowerRow == 5;
                if (isChromaFringe(color)
                        && (!keepFlowerPalette || nearTransparency(source, x, y, 2))) {
                    int replacement = nearestOpaqueNonFringe(source, x, y, 7);
                    if (replacement != TRANSPARENT) {
                        color = (color & 0xFF000000) | (replacement & 0x00FFFFFF);
                    } else {
                        color = TRANSPARENT;
                    }
                }
                cleaned.setRGB(x, y, color);
            }
        }
        ImageIO.write(cleaned, "png", file);
    }

    private static boolean isChromaFringe(int argb) {
        if ((argb >>> 24) == 0) return false;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return red > 90 && blue > 75
                && green < Math.min(red, blue) * 0.72F;
    }

    private static boolean nearTransparency(BufferedImage image, int x, int y, int radius) {
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                int sampleX = x + offsetX;
                int sampleY = y + offsetY;
                if (sampleX < 0 || sampleY < 0
                        || sampleX >= image.getWidth() || sampleY >= image.getHeight()
                        || image.getRGB(sampleX, sampleY) >>> 24 == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int nearestOpaqueNonFringe(BufferedImage image, int x, int y, int radius) {
        int best = TRANSPARENT;
        int bestDistance = Integer.MAX_VALUE;
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                int distance = offsetX * offsetX + offsetY * offsetY;
                if (distance == 0 || distance >= bestDistance) continue;
                int sampleX = x + offsetX;
                int sampleY = y + offsetY;
                if (sampleX < 0 || sampleY < 0
                        || sampleX >= image.getWidth() || sampleY >= image.getHeight()) continue;
                int sample = image.getRGB(sampleX, sampleY);
                if (sample >>> 24 == 0 || isChromaFringe(sample)) continue;
                best = sample;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void writeOrchidBase(File file) throws IOException {
        BufferedImage image = blank(24, 24);
        // Curved green stem.
        pixels(image, 11, 8, 0xFF2BAE38, 11, 9, 12, 10, 12, 11,
                12, 12, 12, 13, 12, 14, 11, 15, 11, 16, 11, 17,
                11, 18, 11, 19, 11, 20, 11, 21, 11, 22);
        pixels(image, 12, 9, 0xFF63E052, 13, 11, 13, 12, 13, 13,
                12, 15, 12, 16, 12, 17, 12, 18, 12, 19, 12, 20);
        pixels(image, 10, 15, 0xFF167B2E, 10, 16, 10, 17, 10, 18,
                10, 19, 10, 20, 10, 21, 10, 22);
        // Leaves.
        pixels(image, 10, 16, 0xFF37CB40, 9, 16, 8, 15, 7, 14, 7, 15,
                8, 16, 9, 17, 10, 17);
        pixels(image, 13, 17, 0xFF2BAE38, 14, 16, 15, 15, 16, 15,
                15, 16, 14, 17, 13, 18);
        pixels(image, 8, 15, 0xFF72EA58, 9, 16, 15, 16, 14, 17);
        // Fixed ovary/bud to which the six rendered petals attach.
        pixels(image, 11, 5, 0xFFFFCE32, 12, 5, 10, 6, 11, 6, 12, 6,
                13, 6, 11, 7, 12, 7);
        pixels(image, 10, 7, 0xFFD84A91, 13, 7, 11, 8, 12, 8);
        ImageIO.write(image, "png", file);
    }

    private static void writeOrchidPetal(File file) throws IOException {
        BufferedImage image = blank(7, 7);
        pixels(image, 3, 0, 0xFFFFA9E6, 2, 1, 3, 1, 4, 1,
                1, 2, 2, 2, 3, 2, 4, 2, 5, 2,
                1, 3, 2, 3, 3, 3, 4, 3, 5, 3,
                2, 4, 3, 4, 4, 4, 3, 5);
        pixels(image, 2, 1, 0xFFFFD4F1, 2, 2, 3, 2, 2, 3);
        pixels(image, 4, 2, 0xFFF25BC4, 5, 2, 4, 3, 5, 3, 3, 4, 4, 4, 3, 5);
        pixels(image, 3, 6, 0xFFC62B91);
        ImageIO.write(image, "png", file);
    }

    private static void writeVine(File file) throws IOException {
        BufferedImage image = blank(16, 16);
        pixels(image, 7, 0, 0xFFD4FF91, 7, 1, 8, 2, 8, 3,
                7, 4, 6, 5, 6, 6, 7, 7, 8, 8, 8, 9,
                7, 10, 6, 11, 6, 12, 7, 13, 8, 14, 8, 15);
        pixels(image, 8, 0, 0xFF38A92F, 8, 1, 9, 2, 9, 3,
                8, 4, 7, 5, 7, 6, 8, 7, 9, 8, 9, 9,
                8, 10, 7, 11, 7, 12, 8, 13, 9, 14, 9, 15);
        pixels(image, 6, 3, 0xFF75E34B, 5, 2, 4, 2, 5, 3,
                9, 6, 10, 5, 11, 5, 10, 6, 9, 7,
                5, 9, 4, 8, 3, 8, 4, 9, 5, 10,
                9, 12, 10, 11, 11, 11, 10, 12, 9, 13);
        ImageIO.write(image, "png", file);
    }

    private static void writeBudAtlas(File flowersFile, File output) throws IOException {
        BufferedImage flowers = ImageIO.read(flowersFile);
        int variants = 9;
        int stages = 6;
        int spriteSize = 24;
        BufferedImage atlas = blank(spriteSize, spriteSize * variants);
        int cellWidth = flowers.getWidth() / stages;
        int cellHeight = flowers.getHeight() / variants;

        for (int flower = 0; flower < variants; flower++) {
            int cellY = flower * cellHeight;
            int minX = cellWidth;
            int minY = cellHeight;
            int maxX = -1;
            int maxY = -1;
            int scanBottom = cellY + Math.round(cellHeight * 0.68F);
            for (int y = cellY; y < scanBottom; y++) {
                for (int x = 0; x < cellWidth; x++) {
                    int color = flowers.getRGB(x, y);
                    if (!isPetalPixel(color)) continue;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y - cellY);
                    maxY = Math.max(maxY, y - cellY);
                }
            }
            if (maxX < minX || maxY < minY) continue;

            int sourceWidth = maxX - minX + 1;
            int sourceHeight = maxY - minY + 1;
            float scale = Math.min(20.0F / sourceWidth, 20.0F / sourceHeight);
            int targetWidth = Math.max(1, Math.round(sourceWidth * scale));
            int targetHeight = Math.max(1, Math.round(sourceHeight * scale));
            int targetX = (spriteSize - targetWidth) / 2;
            int targetY = flower * spriteSize + (spriteSize - targetHeight) / 2;
            for (int y = 0; y < targetHeight; y++) {
                int sourceY = minY + Math.min(sourceHeight - 1,
                        (int) (y / scale));
                for (int x = 0; x < targetWidth; x++) {
                    int sourceX = minX + Math.min(sourceWidth - 1,
                            (int) (x / scale));
                    int color = flowers.getRGB(sourceX, cellY + sourceY);
                    if (isPetalPixel(color)) {
                        atlas.setRGB(targetX + x, targetY + y, color);
                    }
                }
            }
        }
        ImageIO.write(atlas, "png", output);
    }

    private static void writeCardAtlas(File flowersFile, File output) throws IOException {
        BufferedImage flowers = ImageIO.read(flowersFile);
        int variants = 9;
        int stages = 6;
        int targetCellSize = 96;
        BufferedImage atlas = blank(targetCellSize * stages, targetCellSize * variants);

        for (int flower = 0; flower < variants; flower++) {
            int sourceMinY = Math.round(flower * flowers.getHeight() / (float) variants);
            int sourceMaxY = Math.round((flower + 1) * flowers.getHeight()
                    / (float) variants);
            for (int stage = 0; stage < stages; stage++) {
                int sourceMinX = Math.round(stage * flowers.getWidth() / (float) stages);
                int sourceMaxX = Math.round((stage + 1) * flowers.getWidth()
                        / (float) stages);
                BufferedImage sourceCell = flowers.getSubimage(
                        sourceMinX, sourceMinY,
                        sourceMaxX - sourceMinX, sourceMaxY - sourceMinY);
                BufferedImage cleanedCell = keepIntentionalComponents(sourceCell, stage);
                Bounds bounds = opaqueBounds(cleanedCell);
                if (bounds == null) continue;

                int sourceWidth = bounds.maxX - bounds.minX + 1;
                int sourceHeight = bounds.maxY - bounds.minY + 1;
                float scale = Math.min(76.0F / sourceWidth, 80.0F / sourceHeight);
                int targetWidth = Math.max(1, Math.round(sourceWidth * scale));
                int targetHeight = Math.max(1, Math.round(sourceHeight * scale));
                int targetX = stage * targetCellSize
                        + (targetCellSize - targetWidth) / 2;
                int targetY = flower * targetCellSize + 88 - targetHeight;

                Graphics2D graphics = atlas.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                graphics.drawImage(cleanedCell,
                        targetX, targetY, targetX + targetWidth, targetY + targetHeight,
                        bounds.minX, bounds.minY, bounds.maxX + 1, bounds.maxY + 1,
                        null);
                graphics.dispose();
            }
        }
        ImageIO.write(atlas, "png", output);
    }

    private static BufferedImage keepIntentionalComponents(BufferedImage source, int stage) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[] visited = new boolean[width * height];
        List<Component> components = new ArrayList<>();
        int[] neighborX = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] neighborY = {-1, -1, -1, 0, 0, 1, 1, 1};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int startIndex = y * width + x;
                if (visited[startIndex] || source.getRGB(x, y) >>> 24 == 0) continue;
                Component component = new Component();
                ArrayDeque<Integer> queue = new ArrayDeque<>();
                queue.add(startIndex);
                visited[startIndex] = true;
                while (!queue.isEmpty()) {
                    int packed = queue.removeFirst();
                    int pixelX = packed % width;
                    int pixelY = packed / width;
                    component.pixels.add(packed);
                    component.include(pixelX, pixelY);
                    for (int neighbor = 0; neighbor < neighborX.length; neighbor++) {
                        int nextX = pixelX + neighborX[neighbor];
                        int nextY = pixelY + neighborY[neighbor];
                        if (nextX < 0 || nextY < 0 || nextX >= width || nextY >= height) {
                            continue;
                        }
                        int nextIndex = nextY * width + nextX;
                        if (!visited[nextIndex]
                                && source.getRGB(nextX, nextY) >>> 24 != 0) {
                            visited[nextIndex] = true;
                            queue.addLast(nextIndex);
                        }
                    }
                }
                components.add(component);
            }
        }

        BufferedImage result = blank(width, height);
        if (components.isEmpty()) return result;
        components.sort(Comparator.comparingInt(Component::size).reversed());
        Component main = components.get(0);
        int minimumSize = Math.max(18, Math.round(main.size() * 0.018F));
        for (Component component : components) {
            boolean closeToPlant = component.distanceSquared(main) <= 18 * 18;
            boolean fallenPetal = stage >= 4 && component.size() >= 10;
            if (component != main
                    && !(component.size() >= minimumSize && closeToPlant)
                    && !fallenPetal) {
                continue;
            }
            for (int packed : component.pixels) {
                int x = packed % width;
                int y = packed / width;
                result.setRGB(x, y, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static Bounds opaqueBounds(BufferedImage image) {
        Bounds bounds = null;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 == 0) continue;
                if (bounds == null) bounds = new Bounds();
                bounds.include(x, y);
            }
        }
        return bounds;
    }

    private static class Bounds {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        void include(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
    }

    private static final class Component extends Bounds {
        final List<Integer> pixels = new ArrayList<>();

        int size() {
            return pixels.size();
        }

        int distanceSquared(Component other) {
            int dx = maxX < other.minX ? other.minX - maxX
                    : other.maxX < minX ? minX - other.maxX : 0;
            int dy = maxY < other.minY ? other.minY - maxY
                    : other.maxY < minY ? minY - other.maxY : 0;
            return dx * dx + dy * dy;
        }
    }

    private static boolean isPetalPixel(int argb) {
        if (argb >>> 24 == 0) return false;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        boolean greenStem = green > red * 1.16F && green > blue * 1.10F;
        return !greenStem;
    }

    private static BufferedImage blank(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static void pixels(BufferedImage image, int x, int y, int color,
                               int... moreCoordinates) {
        image.setRGB(x, y, color);
        for (int index = 0; index < moreCoordinates.length; index += 2) {
            image.setRGB(moreCoordinates[index], moreCoordinates[index + 1], color);
        }
    }
}
