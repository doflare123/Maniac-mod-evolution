package org.example.maniacrevolution.map;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapRegistry {
    private static final List<MapData> MAPS = new ArrayList<>();

    public static void init() {
        // Здесь добавляй свои карты
        // Формат: id, числовой_id, название, описание, текстура
        register(new MapData(
                "mansion",
                1,
                "Особняк",
                "Классическая карта. 3 Этажа, много секретных проходов",
                new ResourceLocation(Maniacrev.MODID, "textures/gui/maps/mansion.png")
        ));

        register(new MapData(
                "pizzeria",
                4,
                "Пиццерия",
                "Страшная и большая пиццерия. Уникальная особенность - наличие вентиляций! Всего 1 этаж, но карта длинная",
                new ResourceLocation(Maniacrev.MODID, "textures/gui/maps/pizzeria.png")
        ));

        Maniacrev.LOGGER.info("Registered {} maps", MAPS.size());
    }

    private static void register(MapData map) {
        MAPS.add(map);
    }

    public static List<MapData> getAllMaps() {
        return Collections.unmodifiableList(MAPS);
    }

    public static MapData getMapById(String id) {
        return MAPS.stream()
                .filter(map -> map.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static MapData getMapByNumericId(int numericId) {
        return MAPS.stream()
                .filter(map -> map.getNumericId() == numericId)
                .findFirst()
                .orElse(null);
    }
}