package org.example.maniacrevolution.shop;

import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.shop.items.*;

import java.util.*;
import java.util.stream.Collectors;

public class ShopRegistry {
    private static final Map<String, ShopItem> ITEMS = new LinkedHashMap<>();

    public static void init() {
        // === КОСМЕТИКА ===

        // Нимб
        register(new CosmeticShopItem("halo", 2228, "Рулетка",
                "Вращающаяся рулетка над головой с красными, чёрными и зелёным сегментами"));

        // Частицы
        register(new CosmeticShopItem("particles_flame", 500, "Частицы: Пламя",
                "Огненные частицы вокруг игрока"));
        register(new CosmeticShopItem("particles_hearts", 400, "Частицы: Сердца",
                "Сердечки вокруг игрока"));
        register(new CosmeticShopItem("particles_smoke", 300, "Частицы: Дым",
                "Лёгкий дымок вокруг игрока"));
        register(new CosmeticShopItem("particles_enchant", 200, "Частицы: Зачарование",
                "Магические частицы вокруг игрока"));

        // Эффекты оружия
        register(new CosmeticShopItem("bleeding_axe", 400, "Кровотечение (Топор)",
                "Красный след при ударе топором"));
        register(new CosmeticShopItem("frost_sword", 500, "Мороз (Меч)",
                "Ледяной след при ударе мечом"));
        register(new CosmeticShopItem("shadow_blade", 700, "Тень (Всё оружие)",
                "Тёмный след при ударе любым оружием"));

        // Следы
//        register(new CosmeticShopItem("trail_fire", 100, "След: Огонь",
//                "Огненный след за игроком"));
//        register(new CosmeticShopItem("trail_soul", 120, "След: Души",
//                "След синего пламени"));

        // Скины перков
//        register(new CosmeticShopItem("skin_berserker_fire", 50, "Скин: Берсерк (Огонь)",
//                "Огненная рамка для перка Берсерк"));
//        register(new CosmeticShopItem("skin_mimic_shadow", 50, "Скин: Мимик (Тень)",
//                "Тёмная рамка для перка Мимик"));
//        register(new CosmeticShopItem("skin_predator_blood", 75, "Скин: Хищник (Кровь)",
//                "Кровавая рамка для перка Хищник"));

        // === УСИЛЕНИЯ ===
        register(new InfoBoostItem("reveal_maniac_class", 9999, "Разведка: Маньяк",
                "Узнать класс маньяка в начале игры"));
        register(new InfoBoostItem("reveal_survivor_count", 9999, "Разведка: Выжившие",
                "Узнать количество выживших в начале игры"));

        // === УЛУЧШЕНИЯ ===
        register(new PresetSlotItem("extra_preset_slot", 700));

        Maniacrev.LOGGER.info("Registered {} shop items", ITEMS.size());
    }

    public static void register(ShopItem item) {
        ITEMS.put(item.getId(), item);
    }

    public static ShopItem getItem(String id) {
        return ITEMS.get(id);
    }

    public static Collection<ShopItem> getAllItems() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    public static List<ShopItem> getItemsByCategory(ShopCategory category) {
        return ITEMS.values().stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }
}