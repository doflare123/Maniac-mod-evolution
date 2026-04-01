package org.example.maniacrevolution.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

public class GameSettings extends SavedData {
    private static final String DATA_NAME = "maniacrev_game_settings";

    // ── Игровые настройки ─────────────────────────────────────────────────
    private int hpBoost       = 0;
    private int maniacCount   = 1;
    private int gameTime      = 10;
    private int selectedMap   = 0;

    // ── Настройки компьютеров (из HackConfig) ────────────────────────────
    private float hackPointsRequired         = 15.0f;
    private float pointsPerPlayer            = 0.1f;
    private float pointsPerSpecialist        = 0.11f;
    private int   maxBonusPlayers            = 4;
    private float hackerRadius               = 1.7f;
    private float supportRadius              = 3.0f;
    private int   qteIntervalMin             = 3;
    private int   qteIntervalMax             = 5;
    private float qteSuccessBonus            = 0.4f;
    private float qteCritBonus               = 0.6f;
    private int   computersNeededForWin      = 3;

    // ── Дефолты — Игра ────────────────────────────────────────────────────
    public static final int   DEFAULT_HP_BOOST               = 0;
    public static final int   DEFAULT_MANIAC_COUNT           = 1;
    public static final int   DEFAULT_GAME_TIME              = 10;
    public static final int   DEFAULT_SELECTED_MAP           = 0;

    // ── Дефолты — Компьютеры ──────────────────────────────────────────────
    public static final float DEFAULT_HACK_POINTS_REQUIRED   = 15.0f;
    public static final float DEFAULT_POINTS_PER_PLAYER      = 0.1f;
    public static final float DEFAULT_POINTS_PER_SPECIALIST  = 0.11f;
    public static final int   DEFAULT_MAX_BONUS_PLAYERS      = 4;
    public static final float DEFAULT_HACKER_RADIUS          = 1.7f;
    public static final float DEFAULT_SUPPORT_RADIUS         = 3.0f;
    public static final int   DEFAULT_QTE_INTERVAL_MIN       = 3;
    public static final int   DEFAULT_QTE_INTERVAL_MAX       = 5;
    public static final float DEFAULT_QTE_SUCCESS_BONUS      = 0.4f;
    public static final float DEFAULT_QTE_CRIT_BONUS         = 0.6f;
    public static final int   DEFAULT_COMPUTERS_NEEDED       = 3;

    public GameSettings() {}

    // ── Геттеры — Игра ────────────────────────────────────────────────────
    public int   getHpBoost()       { return hpBoost; }
    public int   getManiacCount()   { return maniacCount; }
    public int   getGameTime()      { return gameTime; }
    public int   getSelectedMap()   { return selectedMap; }

    // ── Геттеры — Компьютеры ──────────────────────────────────────────────
    public float getHackPointsRequired()    { return hackPointsRequired; }
    public float getPointsPerPlayer()       { return pointsPerPlayer; }
    public float getPointsPerSpecialist()   { return pointsPerSpecialist; }
    public int   getMaxBonusPlayers()       { return maxBonusPlayers; }
    public float getHackerRadius()          { return hackerRadius; }
    public float getSupportRadius()         { return supportRadius; }
    public int   getQteIntervalMin()        { return qteIntervalMin; }
    public int   getQteIntervalMax()        { return qteIntervalMax; }
    public float getQteSuccessBonus()       { return qteSuccessBonus; }
    public float getQteCritBonus()          { return qteCritBonus; }
    public int   getComputersNeededForWin() { return computersNeededForWin; }

    // ── Сеттеры — Игра ────────────────────────────────────────────────────
    public void setHpBoost(int v)         { hpBoost       = Math.max(0, v);               setDirty(); }
    public void setManiacCount(int v)     { maniacCount   = Math.max(1, v);               setDirty(); }
    public void setGameTime(int v)        { gameTime      = Math.max(1, v);               setDirty(); }
    public void setSelectedMap(int v)     { selectedMap   = v;                            setDirty(); }

    // ── Сеттеры — Компьютеры ──────────────────────────────────────────────
    public void setHackPointsRequired(float v)   { hackPointsRequired  = Math.max(1f, v);         setDirty(); }
    public void setPointsPerPlayer(float v)      { pointsPerPlayer     = Math.max(0.01f, v);      setDirty(); }
    public void setPointsPerSpecialist(float v)  { pointsPerSpecialist = Math.max(0.01f, v);      setDirty(); }
    public void setMaxBonusPlayers(int v)        { maxBonusPlayers     = Math.max(1, v);           setDirty(); }
    public void setHackerRadius(float v)         { hackerRadius        = Math.max(0.5f, v);        setDirty(); }
    public void setSupportRadius(float v)        { supportRadius       = Math.max(0.5f, v);        setDirty(); }
    public void setQteIntervalMin(int v)         { qteIntervalMin      = Math.max(1, v);           setDirty(); }
    public void setQteIntervalMax(int v)         { qteIntervalMax      = Math.max(qteIntervalMin, v); setDirty(); }
    public void setQteSuccessBonus(float v)      { qteSuccessBonus     = Math.max(0f, v);          setDirty(); }
    public void setQteCritBonus(float v)         { qteCritBonus        = Math.max(0f, v);          setDirty(); }
    public void setComputersNeededForWin(int v)  { computersNeededForWin = Math.max(1, v);         setDirty(); }

    // ── Сброс ─────────────────────────────────────────────────────────────
    public void resetAll() {
        hpBoost              = DEFAULT_HP_BOOST;
        maniacCount          = DEFAULT_MANIAC_COUNT;
        gameTime             = DEFAULT_GAME_TIME;
        selectedMap          = DEFAULT_SELECTED_MAP;
        hackPointsRequired   = DEFAULT_HACK_POINTS_REQUIRED;
        pointsPerPlayer      = DEFAULT_POINTS_PER_PLAYER;
        pointsPerSpecialist  = DEFAULT_POINTS_PER_SPECIALIST;
        maxBonusPlayers      = DEFAULT_MAX_BONUS_PLAYERS;
        hackerRadius         = DEFAULT_HACKER_RADIUS;
        supportRadius        = DEFAULT_SUPPORT_RADIUS;
        qteIntervalMin       = DEFAULT_QTE_INTERVAL_MIN;
        qteIntervalMax       = DEFAULT_QTE_INTERVAL_MAX;
        qteSuccessBonus      = DEFAULT_QTE_SUCCESS_BONUS;
        qteCritBonus         = DEFAULT_QTE_CRIT_BONUS;
        computersNeededForWin = DEFAULT_COMPUTERS_NEEDED;
        setDirty();
    }

    // ── NBT ───────────────────────────────────────────────────────────────
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.putInt("hpBoost", hpBoost);
        tag.putInt("maniacCount", maniacCount);
        tag.putInt("gameTime", gameTime);
        tag.putInt("selectedMap", selectedMap);
        tag.putFloat("hackPointsRequired", hackPointsRequired);
        tag.putFloat("pointsPerPlayer", pointsPerPlayer);
        tag.putFloat("pointsPerSpecialist", pointsPerSpecialist);
        tag.putInt("maxBonusPlayers", maxBonusPlayers);
        tag.putFloat("hackerRadius", hackerRadius);
        tag.putFloat("supportRadius", supportRadius);
        tag.putInt("qteIntervalMin", qteIntervalMin);
        tag.putInt("qteIntervalMax", qteIntervalMax);
        tag.putFloat("qteSuccessBonus", qteSuccessBonus);
        tag.putFloat("qteCritBonus", qteCritBonus);
        tag.putInt("computersNeededForWin", computersNeededForWin);
        return tag;
    }

    public static GameSettings load(CompoundTag tag) {
        GameSettings s = new GameSettings();
        s.hpBoost              = tag.getInt("hpBoost");
        s.maniacCount          = tag.getInt("maniacCount");
        s.gameTime             = tag.getInt("gameTime");
        s.selectedMap          = tag.getInt("selectedMap");
        s.hackPointsRequired   = tag.contains("hackPointsRequired")  ? tag.getFloat("hackPointsRequired")  : DEFAULT_HACK_POINTS_REQUIRED;
        s.pointsPerPlayer      = tag.contains("pointsPerPlayer")     ? tag.getFloat("pointsPerPlayer")     : DEFAULT_POINTS_PER_PLAYER;
        s.pointsPerSpecialist  = tag.contains("pointsPerSpecialist") ? tag.getFloat("pointsPerSpecialist") : DEFAULT_POINTS_PER_SPECIALIST;
        s.maxBonusPlayers      = tag.contains("maxBonusPlayers")     ? tag.getInt("maxBonusPlayers")       : DEFAULT_MAX_BONUS_PLAYERS;
        s.hackerRadius         = tag.contains("hackerRadius")        ? tag.getFloat("hackerRadius")        : DEFAULT_HACKER_RADIUS;
        s.supportRadius        = tag.contains("supportRadius")       ? tag.getFloat("supportRadius")       : DEFAULT_SUPPORT_RADIUS;
        s.qteIntervalMin       = tag.contains("qteIntervalMin")      ? tag.getInt("qteIntervalMin")        : DEFAULT_QTE_INTERVAL_MIN;
        s.qteIntervalMax       = tag.contains("qteIntervalMax")      ? tag.getInt("qteIntervalMax")        : DEFAULT_QTE_INTERVAL_MAX;
        s.qteSuccessBonus      = tag.contains("qteSuccessBonus")     ? tag.getFloat("qteSuccessBonus")     : DEFAULT_QTE_SUCCESS_BONUS;
        s.qteCritBonus         = tag.contains("qteCritBonus")        ? tag.getFloat("qteCritBonus")        : DEFAULT_QTE_CRIT_BONUS;
        s.computersNeededForWin = tag.contains("computersNeededForWin") ? tag.getInt("computersNeededForWin") : DEFAULT_COMPUTERS_NEEDED;
        return s;
    }

    public static GameSettings get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(GameSettings::load, GameSettings::new, DATA_NAME);
    }
}
