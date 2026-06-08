package org.example.maniacrevolution.guide;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GuideProgressClient {
    public static final int GUIDE_CONTENT_WEIGHT = 2026060801;

    private static final String FILE_NAME = "maniacrev_guide.properties";
    private static final String SEEN_WEIGHT_KEY = "seenGuideWeight";

    private static boolean loaded;
    private static int seenGuideWeight;

    private GuideProgressClient() {}

    public static boolean shouldShowUpdateIndicator() {
        load();
        return seenGuideWeight < GUIDE_CONTENT_WEIGHT;
    }

    public static void markCurrentGuideSeen() {
        load();
        if (seenGuideWeight >= GUIDE_CONTENT_WEIGHT) {
            return;
        }

        seenGuideWeight = GUIDE_CONTENT_WEIGHT;
        save();
    }

    private static void load() {
        if (loaded) {
            return;
        }

        loaded = true;
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            seenGuideWeight = 0;
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            seenGuideWeight = Integer.parseInt(properties.getProperty(SEEN_WEIGHT_KEY, "0"));
        } catch (IOException | NumberFormatException ignored) {
            seenGuideWeight = 0;
        }
    }

    private static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty(SEEN_WEIGHT_KEY, Integer.toString(seenGuideWeight));
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "Maniac Revolution guide progress");
            }
        } catch (IOException ignored) {
        }
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
