package org.example.maniacrevolution.character;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Регистр всех тэгов и их описаний
 */
public class TagRegistry {
    private static final Map<String, String> TAG_DESCRIPTIONS = new HashMap<>();
    private static final Map<String, String> TAG_KEYS = Map.ofEntries(
            Map.entry("Поддержка", "support"),
            Map.entry("Мобильность", "mobility"),
            Map.entry("Отвлечение", "distraction"),
            Map.entry("Хаккер", "hacker"),
            Map.entry("Видение", "vision"),
            Map.entry("Живучесть", "survivability"),
            Map.entry("Побег", "escape"),
            Map.entry("Помеха", "hindrance"),
            Map.entry("Прогресс", "scaling"),
            Map.entry("Неожиданность", "surprise"),
            Map.entry("AoE", "aoe"),
            Map.entry("Контроль", "control"),
            Map.entry("Периодический урон", "damage_over_time"),
            Map.entry("Призыватель", "summoner"),
            Map.entry("Преследователь", "chaser"),
            Map.entry("Ближний бой", "melee"),
            Map.entry("Дальний бой", "ranged"),
            Map.entry("Универсал", "versatile"),
            Map.entry("Техник", "technician"),
            Map.entry("Зелья", "potions"),
            Map.entry("Скорость", "speed"),
            Map.entry("Риск", "risk"),
            Map.entry("Агрессивный", "aggressive"),
            Map.entry("Интеллект", "intelligence"),
            Map.entry("Мистика", "mystic"),
            Map.entry("Скрытность", "stealth"),
            Map.entry("Страх", "fear"),
            Map.entry("Яды", "poisons"),
            Map.entry("Контроль зоны", "zone_control"),
            Map.entry("Дебаффы", "debuffs"),
            Map.entry("Прыгскеры", "jumpscares"),
            Map.entry("Психология", "psychology"),
            Map.entry("Психологическое давление", "psychological_pressure"),
            Map.entry("Испытания", "trials"),
            Map.entry("Агрессор", "aggressor")
    );

    static {
        // Тэги выживших
        TAG_DESCRIPTIONS.put("Поддержка", "Имеет скиллы/предметы которые могут помочь другим выжившим");
        TAG_DESCRIPTIONS.put("Мобильность", "Быстро перемещается по карте");
        TAG_DESCRIPTIONS.put("Отвлечение", "Может запутать маньяка");
        TAG_DESCRIPTIONS.put("Хаккер", "Имеет особенности с компьютерами");
        TAG_DESCRIPTIONS.put("Видение", "Имеет преимущества в видении маньяка");
        TAG_DESCRIPTIONS.put("Живучесть", "Может пережить больше ударов чем другие выжившие");
        TAG_DESCRIPTIONS.put("Побег", "Имеет возможность убежать от маньяка");
        TAG_DESCRIPTIONS.put("Помеха", "Может ослабить маньяка");

        // Тэги маньяков
        TAG_DESCRIPTIONS.put("Прогресс", "Раскрывается в лейте игры");
        TAG_DESCRIPTIONS.put("Неожиданность", "Может появиться внезапно или плохо виден");
        TAG_DESCRIPTIONS.put("AoE", "Может наносить урон сразу нескольким игрокам");
        TAG_DESCRIPTIONS.put("Контроль", "Имеет дебаффы на мобильность выживших");
        TAG_DESCRIPTIONS.put("Периодический урон", "Наносит урон раз в какое-то время при определенных условиях");
        TAG_DESCRIPTIONS.put("Призыватель", "Имеет союзников-миньонов");
        TAG_DESCRIPTIONS.put("Преследователь", "Имеет преимущества в преследовании одного игрока");
        TAG_DESCRIPTIONS.put("Ближний бой", "Основное оружие - ближнее");
        TAG_DESCRIPTIONS.put("Дальний бой", "Основное оружие - дальнее");

        // Общие тэги
        TAG_DESCRIPTIONS.put("Универсал", "Сбалансированный набор способностей");
        TAG_DESCRIPTIONS.put("Техник", "Специалист по технологиям");
        TAG_DESCRIPTIONS.put("Зелья", "Использует зелья и эликсиры");
        TAG_DESCRIPTIONS.put("Скорость", "Повышенная скорость передвижения");
        TAG_DESCRIPTIONS.put("Риск", "Высокий риск - высокая награда");
        TAG_DESCRIPTIONS.put("Агрессивный", "Ориентирован на агрессивный стиль игры");
        TAG_DESCRIPTIONS.put("Интеллект", "Использует ум и стратегию");
        TAG_DESCRIPTIONS.put("Мистика", "Использует мистические силы");
        TAG_DESCRIPTIONS.put("Скрытность", "Может скрываться и действовать незаметно");
        TAG_DESCRIPTIONS.put("Страх", "Использует страх против противников");
        TAG_DESCRIPTIONS.put("Яды", "Использует яды и отравления");
        TAG_DESCRIPTIONS.put("Контроль зоны", "Контролирует определенную область");
        TAG_DESCRIPTIONS.put("Дебаффы", "Накладывает негативные эффекты");
        TAG_DESCRIPTIONS.put("Прыгскеры", "Может пугать неожиданными появлениями");
        TAG_DESCRIPTIONS.put("Психология", "Использует психологическое давление");
        TAG_DESCRIPTIONS.put("Агрессор", "Агрессивный стиль боя");
    }

    /**
     * Получить описание тэга
     */
    public static String getTagDescription(String tag) {
        String suffix = TAG_KEYS.get(tag);
        if (suffix == null) return TAG_DESCRIPTIONS.getOrDefault(tag, tag);
        String key = "character.maniacrev.tag." + suffix + ".description";
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? TAG_DESCRIPTIONS.getOrDefault(tag, tag) : translated;
    }

    public static String getTagDisplayName(String tag) {
        String suffix = TAG_KEYS.get(tag);
        if (suffix == null) return tag;
        String key = "character.maniacrev.tag." + suffix + ".name";
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? tag : translated;
    }

    /**
     * Проверить существование тэга
     */
    public static boolean hasDescription(String tag) {
        return TAG_DESCRIPTIONS.containsKey(tag);
    }
}
