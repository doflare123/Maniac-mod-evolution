package org.example.maniacrevolution.character;

import java.util.HashMap;
import java.util.Map;

/**
 * Регистр всех тэгов и их описаний
 */
public class TagRegistry {
    private static final Map<String, String> TAG_DESCRIPTIONS = new HashMap<>();

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
        return TAG_DESCRIPTIONS.getOrDefault(tag, tag);
    }

    /**
     * Проверить существование тэга
     */
    public static boolean hasDescription(String tag) {
        return TAG_DESCRIPTIONS.containsKey(tag);
    }
}