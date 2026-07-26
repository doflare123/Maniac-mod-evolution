package org.example.maniacrevolution.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import org.example.maniacrevolution.Maniacrev;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side preferences for the character selection screen.
 *
 * <p>The card display mode is keyed by the local player's UUID so different
 * accounts using the same installation can keep separate UI settings.</p>
 */
public final class CharacterSelectionPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "maniacrev_character_selection.json";

    private static boolean loaded;
    private static ConfigData data = new ConfigData();

    private CharacterSelectionPreferences() {
    }

    public static boolean useLargeCards(UUID playerId) {
        load();
        return data.largeCardsByPlayer.getOrDefault(playerId.toString(), false);
    }

    public static void setLargeCards(UUID playerId, boolean largeCards) {
        load();
        if (largeCards) {
            data.largeCardsByPlayer.put(playerId.toString(), true);
        } else {
            data.largeCardsByPlayer.remove(playerId.toString());
        }
        save();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
            if (loadedData != null && loadedData.largeCardsByPlayer != null) {
                data = loadedData;
                data.largeCardsByPlayer.values().removeIf(value -> value == null);
            }
        } catch (Exception exception) {
            data = new ConfigData();
            Maniacrev.LOGGER.warn("Could not load character selection preferences", exception);
        }
    }

    private static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception exception) {
            Maniacrev.LOGGER.warn("Could not save character selection preferences", exception);
        }
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private static final class ConfigData {
        private Map<String, Boolean> largeCardsByPlayer = new HashMap<>();
    }
}
