package org.example.maniacrevolution.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientGameState;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CustomTabListRenderer {

    private static final ResourceLocation LOBBY_TEXTURE =
            new ResourceLocation(Maniacrev.MODID, "textures/gui/tab/lobby.png");
    private static final ResourceLocation MANIACS_TEXTURE =
            new ResourceLocation(Maniacrev.MODID, "textures/gui/tab/maniacs.png");
    private static final ResourceLocation SURVIVORS_TEXTURE =
            new ResourceLocation(Maniacrev.MODID, "textures/gui/tab/survivors.png");

    // Реальный размер текстуры
    private static final int TEX_SIZE = 256;

    // Отображаемый размер баннера на экране (меньше текстуры — выглядит чётко)
    private static final int BANNER_W = 192;
    private static final int BANNER_H = 96;

    private static final int PLAYER_ENTRY_HEIGHT = 10;
    private static final int PLAYER_ENTRY_WIDTH = BANNER_W;
    private static final int PADDING = 2;

    // Отступ вокруг подложки
    private static final int BG_PADDING = 4;
    // Цвет подложки — тёмный полупрозрачный
    private static final int BG_COLOR = 0xAA000000;

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null) return;

        boolean gameRunning = ClientGameState.isGameRunning();

        int centerX = screenWidth / 2;
        int startY = 10; // небольшой отступ сверху

        if (!gameRunning) {
            renderLobbyTab(graphics, mc, centerX, startY);
        } else {
            renderGameTab(graphics, mc, centerX, startY);
        }
    }

    // ──────────────────────────────────────────────
    // LOBBY
    // ──────────────────────────────────────────────

    private static void renderLobbyTab(GuiGraphics graphics, Minecraft mc, int centerX, int startY) {
        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
        players.sort((a, b) -> a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName()));

        int bannerX = centerX - BANNER_W / 2;

        // Считаем полную высоту контента
        int totalHeight = BANNER_H + PADDING + players.size() * (PLAYER_ENTRY_HEIGHT + 1);
        if (players.isEmpty()) totalHeight += PLAYER_ENTRY_HEIGHT + 1;

        // Подложка под весь блок
        renderBackground(graphics,
                bannerX - BG_PADDING,
                startY - BG_PADDING,
                bannerX + BANNER_W + BG_PADDING,
                startY + totalHeight + BG_PADDING);

        // Баннер
        renderBanner(graphics, LOBBY_TEXTURE, bannerX, startY);

        int y = startY + BANNER_H + PADDING;

        if (players.isEmpty()) {
            graphics.drawString(mc.font, "—", bannerX + PADDING, y, 0xAAAAAA, true);
        } else {
            for (PlayerInfo player : players) {
                renderPlayerEntry(graphics, mc, player, bannerX, y, PLAYER_ENTRY_WIDTH);
                y += PLAYER_ENTRY_HEIGHT + 1;
            }
        }
    }

    // ──────────────────────────────────────────────
    // GAME
    // ──────────────────────────────────────────────

    private static void renderGameTab(GuiGraphics graphics, Minecraft mc, int centerX, int startY) {
        List<PlayerInfo> maniacs = new ArrayList<>();
        List<PlayerInfo> survivors = new ArrayList<>();

        for (PlayerInfo player : mc.getConnection().getListedOnlinePlayers()) {
            PlayerTeam team = player.getTeam();
            if (team == null) continue;
            String teamName = team.getName().toLowerCase();
            if (teamName.equals("maniac")) maniacs.add(player);
            else if (teamName.equals("survivors")) survivors.add(player);
        }

        maniacs.sort((a, b) -> a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName()));
        survivors.sort((a, b) -> a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName()));

        int bannerX = centerX - BANNER_W / 2;

        // Высота блока маньяков
        int maniacsRows = Math.max(maniacs.size(), 1);
        // Высота блока выживших
        int survivorsRows = Math.max(survivors.size(), 1);

        int totalHeight = BANNER_H + PADDING + maniacsRows * (PLAYER_ENTRY_HEIGHT + 1)
                + PADDING * 3
                + BANNER_H + PADDING + survivorsRows * (PLAYER_ENTRY_HEIGHT + 1);

        // Общая подложка
        renderBackground(graphics,
                bannerX - BG_PADDING,
                startY - BG_PADDING,
                bannerX + BANNER_W + BG_PADDING,
                startY + totalHeight + BG_PADDING);

        int y = startY;

        // ── Маньяки ──
        renderBanner(graphics, MANIACS_TEXTURE, bannerX, y);
        y += BANNER_H + PADDING;

        if (maniacs.isEmpty()) {
            graphics.drawString(mc.font, "—", bannerX + PADDING, y, 0xAAAAAA, true);
            y += PLAYER_ENTRY_HEIGHT + 1;
        } else {
            for (PlayerInfo player : maniacs) {
                renderPlayerEntry(graphics, mc, player, bannerX, y, PLAYER_ENTRY_WIDTH);
                y += PLAYER_ENTRY_HEIGHT + 1;
            }
        }

        y += PADDING * 3;

        // ── Выжившие ──
        renderBanner(graphics, SURVIVORS_TEXTURE, bannerX, y);
        y += BANNER_H + PADDING;

        if (survivors.isEmpty()) {
            graphics.drawString(mc.font, "—", bannerX + PADDING, y, 0xAAAAAA, true);
        } else {
            for (PlayerInfo player : survivors) {
                renderPlayerEntry(graphics, mc, player, bannerX, y, PLAYER_ENTRY_WIDTH);
                y += PLAYER_ENTRY_HEIGHT + 1;
            }
        }
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    /**
     * Полупрозрачный фон под весь блок.
     */
    private static void renderBackground(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, BG_COLOR);
    }

    /**
     * Рисует баннер: текстура 256x256 → на экране BANNER_W x BANNER_H.
     * blit(texture, screenX, screenY, destW, destH, u, v, regionW, regionH, texW, texH)
     */
    private static void renderBanner(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // Берём всю текстуру (0,0,256,256) и рисуем её в BANNER_W x BANNER_H
        graphics.blit(texture, x, y, BANNER_W, BANNER_H, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
    }

    private static void renderPlayerEntry(GuiGraphics graphics, Minecraft mc,
                                          PlayerInfo player, int x, int y, int width) {
        // Лёгкий фон строки (чуть светлее основного)
        graphics.fill(x, y, x + width, y + PLAYER_ENTRY_HEIGHT, 0x33FFFFFF);

        // Голова
        renderPlayerHead(graphics, player, x + 1, y + 1, 8);

        // Имя
        Component name = player.getTabListDisplayName() != null
                ? player.getTabListDisplayName()
                : Component.literal(player.getProfile().getName());
        graphics.drawString(mc.font, name, x + 11, y + 1, 0xFFFFFF, true);

        // Пинг
        int ping = player.getLatency();
        String pingStr = ping + "ms";
        int pingColor = ping < 100 ? 0x55FF55 : ping < 200 ? 0xFFFF55 : 0xFF5555;
        int pingX = x + width - mc.font.width(pingStr) - 2;
        graphics.drawString(mc.font, pingStr, pingX, y + 1, pingColor, true);
    }

    private static void renderPlayerHead(GuiGraphics graphics, PlayerInfo player, int x, int y, int size) {
        ResourceLocation skin = player.getSkinLocation();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // Основной слой головы (u=8, v=8, 8x8 из 64x64)
        graphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
        // Слой шапки (u=40, v=8)
        graphics.blit(skin, x, y, size, size, 40, 8, 8, 8, 64, 64);
    }
}