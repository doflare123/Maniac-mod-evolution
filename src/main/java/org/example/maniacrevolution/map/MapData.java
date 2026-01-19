package org.example.maniacrevolution.map;

import net.minecraft.resources.ResourceLocation;

public class MapData {
    private final String id;
    private final int numericId; // Числовой ID для scoreboard
    private final String name;
    private final String description;
    private final ResourceLocation logoTexture;

    public MapData(String id, int numericId, String name, String description, ResourceLocation logoTexture) {
        this.id = id;
        this.numericId = numericId;
        this.name = name;
        this.description = description;
        this.logoTexture = logoTexture;
    }

    public String getId() {
        return id;
    }

    public int getNumericId() {
        return numericId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ResourceLocation getLogoTexture() {
        return logoTexture;
    }
}