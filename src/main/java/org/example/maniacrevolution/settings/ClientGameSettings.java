package org.example.maniacrevolution.settings;

public class ClientGameSettings {
    private static int computerCount = 5;
    private static int hackPoints = 12500;
    private static int hpBoost = 0;
    private static int maniacCount = 1;
    private static int gameTime = 10;
    private static int selectedMap = 0;

    public static void setSettings(int computers, int hack, int hp, int maniacs, int time, int map) {
        computerCount = computers;
        hackPoints = hack;
        hpBoost = hp;
        maniacCount = maniacs;
        gameTime = time;
        selectedMap = map;
    }

    public static int getComputerCount() { return computerCount; }
    public static int getHackPoints() { return hackPoints; }
    public static int getHpBoost() { return hpBoost; }
    public static int getManiacCount() { return maniacCount; }
    public static int getGameTime() { return gameTime; }
    public static int getSelectedMap() { return selectedMap; }
}
