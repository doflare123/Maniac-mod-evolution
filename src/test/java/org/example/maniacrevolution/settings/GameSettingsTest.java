package org.example.maniacrevolution.settings;

import net.minecraft.nbt.CompoundTag;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.example.maniacrevolution.network.packets.SyncSettingsPacket;
import org.example.maniacrevolution.network.packets.UpdateSettingsPacket;
import org.example.maniacrevolution.game.GameManager;

/** Run by verifyGameSettings; no world or network connection is required. */
public final class GameSettingsTest {
    public static void main(String[] args) {
        GameSettings defaults = GameSettings.load(new CompoundTag());
        check(defaults.getGameTime() == 10 && defaults.getManiacCount() == 1,
                "Missing NBT fields must keep defaults, not become zero");
        check(defaults.getGraphicsMode() == GraphicsMode.NORMAL, "Old worlds retain the normal atmosphere");

        GameSettings settings = new GameSettings();
        settings.setHpBoost(8);
        settings.setManiacCount(3);
        settings.setGameTime(5);
        settings.setSelectedMap(2);
        settings.setThreePerksEnabled(true);
        settings.setGraphicsMode(GraphicsMode.HEAVY);
        settings.setHackPointsRequired(21.5F);
        settings.setPointsPerPlayer(0.3F);
        settings.setPointsPerSpecialist(0.45F);
        settings.setMaxBonusPlayers(6);
        settings.setHackerRadius(2.4F);
        settings.setSupportRadius(4.6F);
        settings.setQteIntervalMin(7);
        settings.setQteIntervalMax(9);
        settings.setQteSuccessBonus(0.75F);
        settings.setQteCritBonus(1.5F);
        settings.setComputersNeededForWin(5);
        CompoundTag saved = settings.save(new CompoundTag());
        GameSettings loaded = GameSettings.load(saved);
        check(saved.equals(loaded.save(new CompoundTag())), "Every menu field must survive a world reload");
        check(loaded.getPerkLimit() == 3, "Experimental perk limit must survive reload");
        FriendlyByteBuf sync = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf update = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf roundTrip = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncSettingsPacket.encode(SyncSettingsPacket.from(loaded), sync);
            UpdateSettingsPacket.encode(new UpdateSettingsPacket(8, 3, 5, 2,
                    21.5F, 0.3F, 0.45F, 6, 2.4F, 4.6F, 7, 9, 0.75F, 1.5F, 5, true, GraphicsMode.HEAVY), update);
            check(sync.equals(update), "Menu upload and server snapshot must contain all the same fields");
            UpdateSettingsPacket.encode(UpdateSettingsPacket.decode(update), roundTrip);
            check(!update.isReadable() && sync.equals(roundTrip), "Upload codec must preserve every setting");
            SyncSettingsPacket decoded = SyncSettingsPacket.decode(sync);
            decoded.applyToClient();
            check(ClientGameSettings.getGraphicsMode() == GraphicsMode.HEAVY, "Snapshot changes the client's active mode");
            update.clear();
            SyncSettingsPacket.encode(decoded, update);
            check(!sync.isReadable() && update.equals(roundTrip), "Snapshot codec must preserve every setting");
        } finally {
            sync.release();
            update.release();
            roundTrip.release();
        }

        CompoundTag invalid = new CompoundTag();
        invalid.putInt("gameTime", -5);
        invalid.putInt("maniacCount", 0);
        invalid.putInt("graphicsMode", 100);
        invalid.putFloat("hackPointsRequired", Float.NaN);
        invalid.putFloat("pointsPerPlayer", Float.POSITIVE_INFINITY);
        invalid.putInt("qteIntervalMin", 12);
        invalid.putInt("qteIntervalMax", 2);
        GameSettings sanitized = GameSettings.load(invalid);
        check(sanitized.getGraphicsMode() == GraphicsMode.NORMAL, "Invalid saved mode safely defaults to normal");
        check(sanitized.getGameTime() == 1 && sanitized.getManiacCount() == 1, "Validate loaded integers");
        check(Float.isFinite(sanitized.getHackPointsRequired())
                && Float.isFinite(sanitized.getPointsPerPlayer()), "Reject non-finite hack settings");
        check(sanitized.getQteIntervalMax() >= sanitized.getQteIntervalMin(), "QTE range must remain valid");
        sanitized.setGameTime(Integer.MAX_VALUE);
        sanitized.setManiacCount(100);
        check(sanitized.getManiacCount() == 3, "Datapack supports at most three maniacs");
        check((long) sanitized.getGameTime() * 1200 <= Integer.MAX_VALUE, "Timer ticks must not overflow");
        loaded.resetAll();
        check(loaded.save(new CompoundTag()).equals(new GameSettings().save(new CompoundTag())),
                "Reset must restore every setting");

        for (GraphicsMode mode : GraphicsMode.values()) {
            settings.setGraphicsMode(mode);
            check(GameSettings.load(settings.save(new CompoundTag())).getGraphicsMode() == mode,
                    "Every mode, including OFF=0, survives reload: " + mode);
            FriendlyByteBuf modeSync = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SyncSettingsPacket.encode(SyncSettingsPacket.from(settings), modeSync);
                SyncSettingsPacket.decode(modeSync).applyToClient();
                check(!modeSync.isReadable() && ClientGameSettings.getGraphicsMode() == mode,
                        "Every mode reaches connecting clients: " + mode);
            } finally { modeSync.release(); }
        }
        ClientGameSettings.resetGraphicsMode();
        check(ClientGameSettings.getGraphicsMode() == GraphicsMode.NORMAL, "Disconnect clears the old host's OFF mode");

        GameManager.shutdown();
        GameManager.setMaxTime(2400);
        GameManager.setTime(2400);
        GameManager.applyConfiguredDuration(300);
        GameManager.startTimer();
        check(GameManager.getCurrentTimeSeconds() == 300 && GameManager.getMaxTimeSeconds() == 300,
                "Five-minute setting must replace an old forty-minute countdown");
        GameManager.setTime(123);
        GameManager.applyConfiguredDuration(300);
        check(GameManager.getCurrentTimeSeconds() == 123, "Other settings must not restart an active timer");
        GameManager.stopTimer();
        GameManager.startNewRoundTimer(300);
        check(GameManager.getCurrentTimeSeconds() == 300, "New round must discard the previous remaining time");
        GameManager.shutdown();
        System.out.println("Game settings and timer regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
