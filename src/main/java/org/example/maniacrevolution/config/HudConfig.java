package org.example.maniacrevolution.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Конфигурация HUD с сохранением настроек
 */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Paths.get("config", "maniacrev_hud.json");

    private static boolean customHudEnabled = true;
    private static ConfigData currentConfig = new ConfigData();

    /**
     * Класс для сериализации конфига
     */
    private static class ConfigData {
        public boolean customHudEnabled = true;
    }

    /**
     * Загружает конфигурацию из файла
     */
    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Reader reader = Files.newBufferedReader(CONFIG_FILE);
                currentConfig = GSON.fromJson(reader, ConfigData.class);
                reader.close();

                if (currentConfig != null) {
                    customHudEnabled = currentConfig.customHudEnabled;
                    System.out.println("[HudConfig] Loaded: Custom HUD " + (customHudEnabled ? "enabled" : "disabled"));
                } else {
                    currentConfig = new ConfigData();
                }
            } else {
                // Создаем конфиг по умолчанию
                save();
                System.out.println("[HudConfig] Created default config");
            }
        } catch (Exception e) {
            System.err.println("[HudConfig] Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Сохраняет конфигурацию в файл
     */
    public static void save() {
        try {
            // Создаем директорию если не существует
            Files.createDirectories(CONFIG_FILE.getParent());

            currentConfig.customHudEnabled = customHudEnabled;

            Writer writer = Files.newBufferedWriter(CONFIG_FILE);
            GSON.toJson(currentConfig, writer);
            writer.close();

            System.out.println("[HudConfig] Saved: Custom HUD " + (customHudEnabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            System.err.println("[HudConfig] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isCustomHudEnabled() {
        return customHudEnabled;
    }

    public static void setCustomHudEnabled(boolean enabled) {
        customHudEnabled = enabled;
        save(); // Автоматически сохраняем при изменении
        System.out.println("[HudConfig] Custom HUD " + (enabled ? "enabled" : "disabled"));
    }

    public static void toggleCustomHud() {
        setCustomHudEnabled(!customHudEnabled);
    }
}