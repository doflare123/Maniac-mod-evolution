package org.example.maniacrevolution.pregame;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;

/**
 * 5-секундный отсчёт перед стартом игры.
 * После завершения выполняет все команды из функции maniac:game/start_game
 * (перенесённые сюда из датапака).
 */
public class PreGameCountdownTask {

    private final MinecraftServer server;
    private int elapsedTicks;
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
        elapsedTicks = 0;

        PreGameReadyManager.broadcast("§aИгра начнётся через 5 секунд...");
    }

    public void tick() {
        if (!running || ++elapsedTicks < 20) return;
        elapsedTicks = 0;
        if (remainingSeconds > 0) {
            showTitle(remainingSeconds--);
        } else {
            finish();
        }
    }

    public void cancel() {
        if (!running) return;
        running = false;
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
