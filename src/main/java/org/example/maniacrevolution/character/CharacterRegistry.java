package org.example.maniacrevolution.character;

import org.example.maniacrevolution.character.maniac.*;
import org.example.maniacrevolution.character.survivor.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Регистр всех классов персонажей
 */
public class CharacterRegistry {
    private static final Map<String, CharacterClass> CHARACTERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * Инициализация всех классов персонажей
     */
    public static void init() {
        if (initialized) return;

        // Регистрация выживших
        register(new AlchemistClass());
        register(new ShamanClass());
        register(new MefedronshchikClass());
        register(new ScientistClass());

        // Регистрация маньяков
        register(new DeathClass());
        register(new UrsaClass());
        register(new PlagueDoctorClass());
        register(new FreddyBearClass());

        initialized = true;
    }

    /**
     * Регистрация класса персонажа
     */
    private static void register(CharacterClass characterClass) {
        CHARACTERS.put(characterClass.getId(), characterClass);
    }

    /**
     * Получить класс персонажа по ID
     */
    public static CharacterClass getClass(String id) {
        return CHARACTERS.get(id);
    }

    /**
     * Получить все классы
     */
    public static Collection<CharacterClass> getAllClasses() {
        return CHARACTERS.values();
    }

    /**
     * Получить все классы определённого типа
     */
    public static List<CharacterClass> getClassesByType(CharacterType type) {
        return CHARACTERS.values().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Получить список ID всех выживших
     */
    public static List<String> getSurvivorIds() {
        return getClassesByType(CharacterType.SURVIVOR).stream()
                .map(CharacterClass::getId)
                .collect(Collectors.toList());
    }

    /**
     * Получить список ID всех маньяков
     */
    public static List<String> getManiacIds() {
        return getClassesByType(CharacterType.MANIAC).stream()
                .map(CharacterClass::getId)
                .collect(Collectors.toList());
    }

    /**
     * Проверка существования класса
     */
    public static boolean exists(String id) {
        return CHARACTERS.containsKey(id);
    }
}