package org.example.maniacrevolution.settings;

public class ClientGameSettings {
    // ── Игра ──────────────────────────────────────────────────────────────
    private static int   hpBoost       = 0;
    private static int   maniacCount   = 1;
    private static int   gameTime      = 10;
    private static int   selectedMap   = 0;

    // ── Компьютеры ────────────────────────────────────────────────────────
    private static float hackPointsRequired   = 15.0f;
    private static float pointsPerPlayer      = 0.1f;
    private static float pointsPerSpecialist  = 0.11f;
    private static int   maxBonusPlayers      = 4;
    private static float hackerRadius         = 1.7f;
    private static float supportRadius        = 3.0f;
    private static int   qteIntervalMin       = 3;
    private static int   qteIntervalMax       = 5;
    private static float qteSuccessBonus      = 0.4f;
    private static float qteCritBonus         = 0.6f;
    private static int   computersNeededForWin = 3;

    // ── Сеттер — Игра ─────────────────────────────────────────────────────
    public static void setSettings(int hp,
                                   int maniacs, int time, int map) {
        hpBoost       = hp;
        maniacCount   = maniacs;
        gameTime      = time;
        selectedMap   = map;
    }

    // ── Сеттер — Компьютеры ───────────────────────────────────────────────
    public static void setComputerSettings(float hackPtsReq, float ptsPlayer, float ptsSpc,
                                           int maxBonus, float hackerR, float supportR,
                                           int qteMin, int qteMax, float qteSuccess,
                                           float qteCrit, int computersNeeded) {
        hackPointsRequired   = hackPtsReq;
        pointsPerPlayer      = ptsPlayer;
        pointsPerSpecialist  = ptsSpc;
        maxBonusPlayers      = maxBonus;
        hackerRadius         = hackerR;
        supportRadius        = supportR;
        qteIntervalMin       = qteMin;
        qteIntervalMax       = qteMax;
        qteSuccessBonus      = qteSuccess;
        qteCritBonus         = qteCrit;
        computersNeededForWin = computersNeeded;
    }

    // ── Геттеры — Игра ────────────────────────────────────────────────────
    public static int   getHpBoost()       { return hpBoost; }
    public static int   getManiacCount()   { return maniacCount; }
    public static int   getGameTime()      { return gameTime; }
    public static int   getSelectedMap()   { return selectedMap; }

    // ── Геттеры — Компьютеры ──────────────────────────────────────────────
    public static float getHackPointsRequired()    { return hackPointsRequired; }
    public static float getPointsPerPlayer()       { return pointsPerPlayer; }
    public static float getPointsPerSpecialist()   { return pointsPerSpecialist; }
    public static int   getMaxBonusPlayers()       { return maxBonusPlayers; }
    public static float getHackerRadius()          { return hackerRadius; }
    public static float getSupportRadius()         { return supportRadius; }
    public static int   getQteIntervalMin()        { return qteIntervalMin; }
    public static int   getQteIntervalMax()        { return qteIntervalMax; }
    public static float getQteSuccessBonus()       { return qteSuccessBonus; }
    public static float getQteCritBonus()          { return qteCritBonus; }
    public static int   getComputersNeededForWin() { return computersNeededForWin; }
}
