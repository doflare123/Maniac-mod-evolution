package org.example.maniacrevolution.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

public class GameSettings extends SavedData {
    private static final String DATA_NAME = "maniacrev_game_settings";

    // Настройки с базовыми значениями
    private int computerCount = 5;           // Количество компьютеров (1-9, базовое 5)
    private int hackPoints = 12500;          // Очки для хака (мин 500, базовое 12500)
    private int hpBoost = 0;                 // Доп. хп (мин 0, базовое 0)
    private int maniacCount = 1;             // Количество маньяков (мин 1, базовое 1)
    private int gameTime = 10;               // Время игры в минутах (мин 1, базовое 10)
    private int selectedMap = 0;             // ID выбранной карты (0 = голосование)

    // Базовые значения для сброса
    public static final int DEFAULT_COMPUTER_COUNT = 5;
    public static final int DEFAULT_HACK_POINTS = 12500;
    public static final int DEFAULT_HP_BOOST = 0;
    public static final int DEFAULT_MANIAC_COUNT = 1;
    public static final int DEFAULT_GAME_TIME = 10;
    public static final int DEFAULT_SELECTED_MAP = 0;

    public GameSettings() {
    }

    // Геттеры
    public int getComputerCount() { return computerCount; }
    public int getHackPoints() { return hackPoints; }
    public int getHpBoost() { return hpBoost; }
    public int getManiacCount() { return maniacCount; }
    public int getGameTime() { return gameTime; }
    public int getSelectedMap() { return selectedMap; }

    // Сеттеры с валидацией
    public void setComputerCount(int value) {
        this.computerCount = Math.max(1, Math.min(9, value));
        setDirty();
    }

    public void setHackPoints(int value) {
        this.hackPoints = Math.max(500, value);
        setDirty();
    }

    public void setHpBoost(int value) {
        this.hpBoost = Math.max(0, value);
        setDirty();
    }

    public void setManiacCount(int value) {
        this.maniacCount = Math.max(1, value);
        setDirty();
    }

    public void setGameTime(int value) {
        this.gameTime = Math.max(1, value);
        setDirty();
    }

    public void setSelectedMap(int value) {
        this.selectedMap = value;
        setDirty();
    }

    // Сброс к базовым значениям
    public void resetComputerCount() {
        setComputerCount(DEFAULT_COMPUTER_COUNT);
    }

    public void resetHackPoints() {
        setHackPoints(DEFAULT_HACK_POINTS);
    }

    public void resetHpBoost() {
        setHpBoost(DEFAULT_HP_BOOST);
    }

    public void resetManiacCount() {
        setManiacCount(DEFAULT_MANIAC_COUNT);
    }

    public void resetGameTime() {
        setGameTime(DEFAULT_GAME_TIME);
    }

    public void resetSelectedMap() {
        setSelectedMap(DEFAULT_SELECTED_MAP);
    }

    public void resetAll() {
        resetComputerCount();
        resetHackPoints();
        resetHpBoost();
        resetManiacCount();
        resetGameTime();
        resetSelectedMap();
    }

    // Сохранение в NBT
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.putInt("computerCount", computerCount);
        tag.putInt("hackPoints", hackPoints);
        tag.putInt("hpBoost", hpBoost);
        tag.putInt("maniacCount", maniacCount);
        tag.putInt("gameTime", gameTime);
        tag.putInt("selectedMap", selectedMap);
        return tag;
    }

    // Загрузка из NBT
    public static GameSettings load(CompoundTag tag) {
        GameSettings settings = new GameSettings();
        settings.computerCount = tag.getInt("computerCount");
        settings.hackPoints = tag.getInt("hackPoints");
        settings.hpBoost = tag.getInt("hpBoost");
        settings.maniacCount = tag.getInt("maniacCount");
        settings.gameTime = tag.getInt("gameTime");
        settings.selectedMap = tag.getInt("selectedMap");
        return settings;
    }

    // Получение или создание настроек для сервера
    public static GameSettings get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(GameSettings::load, GameSettings::new, DATA_NAME);
    }
}
