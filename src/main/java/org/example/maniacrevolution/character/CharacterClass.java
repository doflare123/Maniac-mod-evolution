package org.example.maniacrevolution.character;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.Maniacrev;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для всех персонажей (выживших и маньяков)
 */
public abstract class CharacterClass {
    private final String id;
    private final String name;
    private final CharacterType type;
    private final String description;
    private final int scoreboardId; // ID для scoreboard
    private final List<String> tags;
    private final List<Feature> features;
    private final List<Item> items;
    private final int difficulty;

    protected CharacterClass(String id, String name, CharacterType type, String description, int scoreboardId, int difficulty) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.scoreboardId = scoreboardId;
        this.difficulty = difficulty;
        this.tags = new ArrayList<>();
        this.features = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() { return localize("character.maniacrev." + id + ".name", name); }

    public CharacterType getType() {
        return type;
    }

    public String getDescription() { return localize("character.maniacrev." + id + ".description", description); }

    public List<String> getTags() {
        return tags;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<Item> getItems() {
        return items;
    }

    public int getScoreboardId() {
        return scoreboardId;
    }

    public int getDifficulty() { return difficulty; }

    /**
     * Получить путь к текстуре фрески персонажа
     */
    public ResourceLocation getFrescoTexture() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/frescos/" + id + ".png");
    }

    public String getDifficultyStars() {
        StringBuilder stars = new StringBuilder("§6");
        for (int i = 0; i < 5; i++) {
            stars.append(i < difficulty ? "★" : "☆");
        }
        return stars.toString();
    }

    // Builders для удобного добавления данных
    protected void addTag(String tag) {
        this.tags.add(tag);
    }

    protected void addFeature(String name, String description) {
        int index = this.features.size();
        this.features.add(new Feature(
                "character.maniacrev." + id + ".feature." + index,
                name, description, new Object[0]));
    }

    protected void addItem(String name, String description) {
        addItem(name, description, "");
    }

    protected void addItem(String name, String description, String lore) {
        int index = this.items.size();
        this.items.add(new Item(
                "character.maniacrev." + id + ".item." + index,
                name, description, lore, new Object[0]));
    }

    private static String localize(String key, String fallback, Object... args) {
        String value = Component.translatable(key, args).getString();
        return value.equals(key) ? fallback : value;
    }

    // Вложенные классы для особенностей и предметов
    public static class Feature {
        private final String translationKey;
        private final String name;
        private final String description;
        private final Object[] descriptionArgs;

        public Feature(String translationKey, String name, String description, Object[] descriptionArgs) {
            this.translationKey = translationKey;
            this.name = name;
            this.description = description;
            this.descriptionArgs = descriptionArgs;
        }

        public String getName() {
            return localize(translationKey + ".name", name);
        }

        public String getDescription() {
            return localize(translationKey + ".description", description, descriptionArgs);
        }

        public boolean hasSourceName(String sourceName) { return name.equals(sourceName); }
    }

    public static class Item {
        private final String translationKey;
        private final String name;
        private final String description;
        private final String lore;
        private final Object[] descriptionArgs;

        public Item(String translationKey, String name, String description, String lore, Object[] descriptionArgs) {
            this.translationKey = translationKey;
            this.name = name;
            this.description = description;
            this.lore = lore == null ? "" : lore;
            this.descriptionArgs = descriptionArgs;
        }

        public String getName() {
            return localize(translationKey + ".name", name);
        }

        public String getDescription() {
            return localize(translationKey + ".description", description, descriptionArgs);
        }

        public String getLore() {
            return lore.isBlank() ? "" : localize(translationKey + ".lore", lore);
        }
    }
}
