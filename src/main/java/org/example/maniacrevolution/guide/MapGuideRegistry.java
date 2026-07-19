package org.example.maniacrevolution.guide;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;

import java.util.List;

/** Shared source for the map facts displayed by the guide and contextual UI cards. */
public final class MapGuideRegistry {
    private static final List<Entry> ENTRIES = List.of(
            entry(
                    "mansion",
                    "Особняк",
                    "Классическая многоэтажная карта с коридорами и потайными проходами.",
                    "Средний",
                    3,
                    List.of(
                            "§e● Особенности:",
                            "  - Много этажей",
                            "  - Коридорность",
                            "  - Потайные проходы",
                            "",
                            "§e● Советы:",
                            "  §aВыжившим:§r Сокращайте путь через потайные проходы и не забывайте их закрывать",
                            "  §cМаньякам:§r Ловите выживших в узких проходах"
                    )
            ),
            entry(
                    "pizzeria",
                    "Пиццерия Фрэдэ",
                    "Открытая карта по мотивам первых двух частей франшизы с вентиляциями и канализациями.",
                    "Большой",
                    4,
                    List.of(
                            "§e● Особенности:",
                            "  - Открытая карта",
                            "  - Вентиляции",
                            "  - Канализации",
                            "  - Есть места взаимодействия с окружением",
                            "",
                            "§e● Советы:",
                            "  §aВыжившим:§r Используйте вентиляции для быстрого перемещения к генераторам",
                            "  §cМаньякам:§r Ловите выживших возле генераторов"
                    )
            ),
            entry(
                    "fort",
                    "Промзона",
                    "Заброшенный завод с очень тесным пространством для небольшого состава игроков.",
                    "Маленький",
                    4,
                    List.of(
                            "§e● Особенности:",
                            "  - Очень узкое пространство",
                            "  - Карта для малого количества игроков"
                    )
            )
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

    private static Entry entry(String id, String name, String description, String size,
                               int difficulty, List<String> details) {
        return new Entry(
                id,
                name,
                description,
                size,
                difficulty,
                List.copyOf(details),
                new ResourceLocation(Maniacrev.MODID, "textures/gui/maps/" + id + ".png")
        );
    }

    public record Entry(String id, String name, String description, String size,
                        int difficulty, List<String> details, ResourceLocation previewTexture) {
        public String difficultyStars() {
            int value = Math.max(0, Math.min(5, difficulty));
            return "★".repeat(value) + "☆".repeat(5 - value);
        }
    }
}
