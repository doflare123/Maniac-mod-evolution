package org.example.maniacrevolution.character;

import net.minecraft.resources.ResourceLocation;
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

    protected CharacterClass(String id, String name, CharacterType type, String description, int scoreboardId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.scoreboardId = scoreboardId;
        this.tags = new ArrayList<>();
        this.features = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CharacterType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

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

    /**
     * Получить путь к текстуре фрески персонажа
     */
    public ResourceLocation getFrescoTexture() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/frescos/" + id + ".png");
    }

    // Builders для удобного добавления данных
    protected void addTag(String tag) {
        this.tags.add(tag);
    }

    protected void addFeature(String name, String description) {
        this.features.add(new Feature(name, description));
    }

    protected void addItem(String name, String description) {
        this.items.add(new Item(name, description));
    }

    // Вложенные классы для особенностей и предметов
    public static class Feature {
        private final String name;
        private final String description;

        public Feature(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Item {
        private final String name;
        private final String description;

        public Item(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}