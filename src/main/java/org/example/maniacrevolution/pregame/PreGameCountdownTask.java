package org.example.maniacrevolution.pregame;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 5-секундный отсчёт перед стартом игры.
 * После завершения выполняет все команды из функции maniac:game/start_game
 * (перенесённые сюда из датапака).
 */
public class PreGameCountdownTask {

    private final MinecraftServer server;
    private Timer timer;
    private int remainingSeconds;
    private boolean running;

    public PreGameCountdownTask(MinecraftServer server) {
        this.server = server;
        this.remainingSeconds = 5;
        this.running = false;
    }

    public void start() {
        if (running) return;
        running = true;
        timer = new Timer("PreGameCountdown");

        PreGameReadyManager.broadcast("§aИгра начнётся через 5 секунд...");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (remainingSeconds > 0) {
                    showTitle(remainingSeconds);
                    remainingSeconds--;
                } else {
                    finish();
                }
            }
        }, 1000, 1000);
    }

    public void cancel() {
        if (!running) return;
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        PreGameReadyManager.broadcast("§cОтсчёт отменён — не все игроки готовы");
    }

    private void showTitle(int seconds) {
        server.execute(() -> {
            String color = seconds <= 3 ? "§c" : "§e";
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 20, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal(color + "§l" + seconds)
                ));
            }
        });
    }

    private void finish() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withMaximumPermission(4),
                        "function maniac:game/start_game"
                );
                // Сбрасываем прелобби-готовность после старта
                PreGameReadyManager.resetAll(server);
                Maniacrev.LOGGER.info("[PreGame] Game started successfully!");
            } catch (Exception e) {
                Maniacrev.LOGGER.error("[PreGame] Failed to start game", e);
            }
        });
    }



    public boolean isRunning() { return running; }
    public int getRemainingSeconds() { return remainingSeconds; }
}