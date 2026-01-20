package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.PlayerTeam;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.RequestDeadPlayersPacket;
import org.example.maniacrevolution.network.packets.ResurrectPlayerPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResurrectionScreen extends Screen {
    private static final ResourceLocation GOTHIC_FRAME = new ResourceLocation(Maniacrev.MODID, "textures/gui/gothic_frame.png");
    private static final int FRAME_WIDTH = 256;
    private static final int FRAME_HEIGHT = 200;

    private List<DeadPlayer> deadPlayers = new ArrayList<>();
    private static List<DeadPlayer> pendingDeadPlayers = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE = 5;

    private float animationTick = 0;

    public ResurrectionScreen() {
        super(Component.literal("Воскрешение"));
    }

    @Override
    protected void init() {
        super.init();

        // Очищаем старые данные
        deadPlayers.clear();
        pendingDeadPlayers.clear();

        // Запрашиваем список мёртвых игроков с сервера
        ModNetworking.sendToServer(new RequestDeadPlayersPacket());

        System.out.println("[ResurrectionScreen] Sent request for dead players");
    }

    /**
     * Статический метод для обновления списка (вызывается из пакета)
     */
    public static void updateDeadPlayers(List<RequestDeadPlayersPacket.DeadPlayerInfo> players) {
        pendingDeadPlayers.clear();

        System.out.println("[ResurrectionScreen] updateDeadPlayers called with " + players.size() + " players");

        for (var info : players) {
            pendingDeadPlayers.add(new DeadPlayer(
                    info.uuid,
                    info.name,
                    info.name
            ));
            System.out.println("[ResurrectionScreen] Added to pending: " + info.name);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Синхронизируем pending список с основным
        if (!pendingDeadPlayers.isEmpty()) {
            deadPlayers.clear();
            deadPlayers.addAll(pendingDeadPlayers);
            pendingDeadPlayers.clear();
            System.out.println("[ResurrectionScreen] Loaded " + deadPlayers.size() + " players for display");
        }

        this.renderBackground(guiGraphics);
        animationTick += partialTick;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        renderGothicFrame(guiGraphics, centerX, centerY);

        drawCenteredString(
                guiGraphics,
                this.font,
                "§5§l✦ Врата Смерти ✦",
                centerX,
                centerY - 85,
                0x8B00FF
        );

        renderPlayerList(guiGraphics, centerX, centerY, mouseX, mouseY);

        drawCenteredString(
                guiGraphics,
                this.font,
                "§7[ЛКМ] Воскресить | [ПКМ/ESC] Отмена",
                centerX,
                centerY + 90,
                0x808080
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderGothicFrame(GuiGraphics guiGraphics, int centerX, int centerY) {
        PoseStack poseStack = guiGraphics.pose();

        // Темный фон с прозрачностью
        int left = centerX - FRAME_WIDTH / 2;
        int top = centerY - FRAME_HEIGHT / 2;
        int right = centerX + FRAME_WIDTH / 2;
        int bottom = centerY + FRAME_HEIGHT / 2;

        // Основной фон
        guiGraphics.fill(left, top, right, bottom, 0xCC000000);

        // Границы с градиентом
        int borderColor1 = 0xFF4B0082; // Темно-фиолетовый
        int borderColor2 = 0xFF8B00FF; // Фиолетовый

        // Верхняя граница
        guiGraphics.fill(left - 2, top - 2, right + 2, top, borderColor1);
        // Нижняя граница
        guiGraphics.fill(left - 2, bottom, right + 2, bottom + 2, borderColor1);
        // Левая граница
        guiGraphics.fill(left - 2, top, left, bottom, borderColor1);
        // Правая граница
        guiGraphics.fill(right, top, right + 2, bottom, borderColor1);

        // Декоративные углы
        renderCornerDecoration(guiGraphics, left - 5, top - 5, true, true);
        renderCornerDecoration(guiGraphics, right + 5, top - 5, false, true);
        renderCornerDecoration(guiGraphics, left - 5, bottom + 5, true, false);
        renderCornerDecoration(guiGraphics, right + 5, bottom + 5, false, false);

        // Анимированные частицы души
        renderSoulParticles(guiGraphics, centerX, centerY);
    }

    private void renderCornerDecoration(GuiGraphics guiGraphics, int x, int y, boolean flipX, boolean flipY) {
        int size = 10;
        int color = 0xFF8B00FF;

        int x1 = flipX ? x - size : x;
        int x2 = flipX ? x : x + size;
        int y1 = flipY ? y - size : y;
        int y2 = flipY ? y : y + size;

        // Рисуем декоративную линию
        guiGraphics.fill(x1, y, x2, y + 1, color);
        guiGraphics.fill(x, y1, x + 1, y2, color);
    }

    private void renderSoulParticles(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Рисуем анимированные "души" вокруг рамки
        for (int i = 0; i < 8; i++) {
            float angle = (float) (animationTick * 0.02f + i * Math.PI / 4);
            int radius = 130;

            int px = (int) (centerX + Math.cos(angle) * radius);
            int py = (int) (centerY + Math.sin(angle) * radius);

            int alpha = (int) (128 + Math.sin(animationTick * 0.1f + i) * 127);
            int color = (alpha << 24) | 0x8B00FF;

            guiGraphics.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }

    private void renderPlayerList(GuiGraphics guiGraphics, int centerX, int centerY, int mouseX, int mouseY) {
        if (deadPlayers.isEmpty()) {
            drawCenteredString(
                    guiGraphics,
                    this.font,
                    "§7Нет душ для воскрешения",
                    centerX,
                    centerY,
                    0x808080
            );
            return;
        }

        int startY = centerY - 60;
        int entryHeight = 25;

        for (int i = 0; i < Math.min(deadPlayers.size(), MAX_VISIBLE); i++) {
            int index = i + scrollOffset;
            if (index >= deadPlayers.size()) break;

            DeadPlayer player = deadPlayers.get(index);
            int entryY = startY + i * entryHeight;

            boolean isHovered = isMouseOverEntry(mouseX, mouseY, centerX, entryY, entryHeight);
            boolean isSelected = index == selectedIndex;

            renderPlayerEntry(guiGraphics, centerX, entryY, player, isHovered, isSelected);
        }

        // Индикатор прокрутки
        if (deadPlayers.size() > MAX_VISIBLE) {
            drawCenteredString(
                    guiGraphics,
                    this.font,
                    "§7▲ " + (scrollOffset + 1) + "/" + deadPlayers.size() + " ▼",
                    centerX,
                    startY + MAX_VISIBLE * entryHeight + 5,
                    0x808080
            );
        }
    }

    private void renderPlayerEntry(GuiGraphics guiGraphics, int centerX, int y, DeadPlayer player, boolean hovered, boolean selected) {
        int left = centerX - 110;
        int right = centerX + 110;
        int top = y;
        int bottom = y + 20;

        // Фон entry
        int bgColor;
        if (selected) {
            bgColor = 0x664B0082;
        } else if (hovered) {
            bgColor = 0x442B0052;
        } else {
            bgColor = 0x221B0032;
        }

        guiGraphics.fill(left, top, right, bottom, bgColor);

        // Границы
        int borderColor = selected ? 0xFF8B00FF : (hovered ? 0xFF6A0DAD : 0xFF4B0082);
        guiGraphics.fill(left, top, right, top + 1, borderColor);
        guiGraphics.fill(left, bottom - 1, right, bottom, borderColor);

        // Skull icon (можно заменить на рендер головы игрока)
        drawString(guiGraphics, this.font, "§5☠", left + 5, top + 6, 0xFFFFFF);

        // Имя игрока
        String displayName = "§f" + player.displayName;
        drawString(guiGraphics, this.font, displayName, left + 25, top + 6, 0xFFFFFF);

        // Подпись "Душа"
        drawString(guiGraphics, this.font, "§7Душа", right - 40, top + 6, 0x808080);
    }

    private boolean isMouseOverEntry(int mouseX, int mouseY, int centerX, int entryY, int entryHeight) {
        int left = centerX - 110;
        int right = centerX + 110;
        return mouseX >= left && mouseX <= right && mouseY >= entryY && mouseY <= entryY + 20;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // ЛКМ
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int startY = centerY - 60;
            int entryHeight = 25;

            for (int i = 0; i < Math.min(deadPlayers.size(), MAX_VISIBLE); i++) {
                int index = i + scrollOffset;
                if (index >= deadPlayers.size()) break;

                int entryY = startY + i * entryHeight;

                if (isMouseOverEntry((int) mouseX, (int) mouseY, centerX, entryY, entryHeight)) {
                    selectedIndex = index;
                    resurrectPlayer(deadPlayers.get(index));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (deadPlayers.size() > MAX_VISIBLE) {
            scrollOffset = Math.max(0, Math.min(deadPlayers.size() - MAX_VISIBLE,
                    scrollOffset - (int) delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void resurrectPlayer(DeadPlayer player) {
        // Отправляем пакет на сервер
        ModNetworking.sendToServer(new ResurrectPlayerPacket(player.uuid));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class DeadPlayer {
        public final UUID uuid;
        public final String displayName;
        public final String scoreboardName;

        public DeadPlayer(UUID uuid, String displayName, String scoreboardName) {
            this.uuid = uuid;
            this.displayName = displayName;
            this.scoreboardName = scoreboardName;
        }
    }

    // Вспомогательные методы для рисования
    private void drawCenteredString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font,
                                    String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    private void drawString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font,
                            String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color);
    }
}