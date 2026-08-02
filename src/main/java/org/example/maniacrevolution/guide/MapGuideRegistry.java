package org.example.maniacrevolution.guide;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.Maniacrev;

import java.util.List;

/** Shared source for the map facts displayed by the guide and contextual UI cards. */
public final class MapGuideRegistry {
    private static final List<Entry> ENTRIES = List.of(
            entry("mansion", 3, 8),
            entry("pizzeria", 4, 9),
            entry("fort", 4, 3)
    );

    private MapGuideRegistry() {}

    public static List<Entry> getAll() {
        return ENTRIES;
    }

    public static Entry getById(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static Entry entry(String id, int difficulty, int detailCount) {
        return new Entry(
                id,
                difficulty,
                detailCount,
                new ResourceLocation(Maniacrev.MODID, "textures/gui/maps/" + id + ".png")
        );
    }

    public record Entry(String id, int difficulty, int detailCount, ResourceLocation previewTexture) {
        private String text(String suffix) {
            return Component.translatable("guide.maniacrev.map." + id + "." + suffix).getString();
        }

        public String name() { return text("name"); }
        public String description() { return text("description"); }
        public String size() { return text("size"); }
        public List<String> details() {
            java.util.ArrayList<String> values = new java.util.ArrayList<>();
            for (int i = 0; i < detailCount; i++) values.add(text("detail." + i));
            return List.copyOf(values);
        }

        public String difficultyStars() {
            int value = Math.max(0, Math.min(5, difficulty));
            return "★".repeat(value) + "☆".repeat(5 - value);
        }
    }
}
