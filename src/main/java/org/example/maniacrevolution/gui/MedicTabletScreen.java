package org.example.maniacrevolution.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.network.ModNetworking;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI планшета медика для отображения состояния всех союзников
 */
@OnlyIn(Dist.CLIENT)
public class MedicTabletScreen extends Screen {

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    private static final int ECG_WIDTH = 120;
    private static final int ECG_HEIGHT = 30;

    private List<PlayerHealthData> survivors = new ArrayList<>();
    private int guiLeft;
    private int guiTop;

    public MedicTabletScreen() {
        super(Component.literal("Планшет медика"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        updateSurvivors();
    }

    /**
     * Обновляет список выживших из команды survivors
     */
    private void updateSurvivors() {
        survivors.clear();
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) return;

        for (Player player : mc.level.players()) {
            // Пропускаем самого игрока, который открыл планшет
            if (player == mc.player) continue;

            Team team = player.getTeam();
            if (team != null && "survivors".equalsIgnoreCase(team.getName())) {
                survivors.add(new PlayerHealthData(player));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Обновляем данные каждый кадр
        updateSurvivors();

        // Фон - правильный вызов для 1.20.1
        renderBackground(graphics);

        // Основной фон GUI
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xE0000000);

        // Рамка
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 2, 0xFF00FF00);
        graphics.fill(guiLeft, guiTop + GUI_HEIGHT - 2, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF00FF00);
        graphics.fill(guiLeft, guiTop, guiLeft + 2, guiTop + GUI_HEIGHT, 0xFF00FF00);
        graphics.fill(guiLeft + GUI_WIDTH - 2, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF00FF00);

        // Заголовок
        graphics.drawString(this.font, "§a§lМЕДИЦИНСКИЙ ПЛАНШЕТ",
                guiLeft + 10, guiTop + 10, 0xFFFFFF, false);

        // Отображаем выживших
        int yOffset = 30;
        for (int i = 0; i < survivors.size() && i < 5; i++) {
            PlayerHealthData data = survivors.get(i);
            renderPlayerInfo(graphics, data, guiLeft + 10, guiTop + yOffset, mouseX, mouseY);
            yOffset += 35;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Отображает информацию об одном игроке
     */
    /**
     * Отображает информацию об одном игроке
     */
    private void renderPlayerInfo(GuiGraphics graphics, PlayerHealthData data,
                                  int x, int y, int mouseX, int mouseY) {
        // Имя игрока
        String nameColor = data.isSpectator ? "§7" : "§f";
        graphics.drawString(this.font, nameColor + data.playerName, x, y + 7, 0xFFFFFF, false);

        // Электрокардиограмма (справа)
        drawECG(graphics, data, x + 120, y - 5);

        // Кнопка "Отследить" между именем и кардиограммой
        boolean canTrack = !data.isSpectator && data.healthPercent < 0.5F;
        boolean hovered = mouseX >= x + 45 && mouseX <= x + 115 &&
                mouseY >= y && mouseY <= y + 20;

        // Цвет кнопки: зеленый если можно отследить, серый если нельзя
        int buttonColor;
        if (!canTrack) {
            buttonColor = 0xFF333333; // Серая (неактивная)
        } else if (hovered) {
            buttonColor = 0xFF00AA00; // Светло-зеленая (наведение)
        } else {
            buttonColor = 0xFF008800; // Зеленая (активная)
        }

        // Рисуем кнопку
        graphics.fill(x + 45, y, x + 115, y + 20, buttonColor);

        // Рамка кнопки
        graphics.renderOutline(x + 45, y, 70, 20, canTrack ? 0xFF00FF00 : 0xFF666666);

        // Текст кнопки
        String buttonText = canTrack ? "§fОтследить" : "§7Отследить";
        int textWidth = this.font.width(buttonText);
        graphics.drawString(this.font, buttonText,
                x + 80 - textWidth / 2, // Центрируем текст
                y + 6,
                0xFFFFFF, false);
    }

    /**
     * Рисует электрокардиограмму в зависимости от HP игрока
     */
    private void drawECG(GuiGraphics graphics, PlayerHealthData data, int x, int y) {
        int width = ECG_WIDTH;
        int height = ECG_HEIGHT;

        // Фон ЭКГ
        graphics.fill(x, y, x + width, y + height, 0xFF001100);

        // Рамка чтобы ЭКГ не вылазила за пределы
        graphics.renderOutline(x, y, width, height, 0xFF003300);

        if (data.isSpectator) {
            // Плоская линия (смерть)
            graphics.fill(x + 2, y + height / 2, x + width - 2, y + height / 2 + 1, 0xFF00FF00);
            return;
        }

        // Параметры пульса в зависимости от HP
        float healthPercent = data.healthPercent;
        float frequency = 1.0F + (1.0F - healthPercent) * 2.0F; // Уменьшена частота
        float amplitude = 8.0F + (1.0F - healthPercent) * 10.0F; // Уменьшена амплитуда

        long time = System.currentTimeMillis();
        float offset = (time % 2000) / 2000.0F * width;

        // Рисуем ЭКГ линию с ограничением по границам
        for (int i = 0; i < width - 1; i++) {
            float x1 = i;
            float x2 = i + 1;

            // Создаем форму ЭКГ (характерный зубец)
            float progress1 = ((x1 + offset) % width) / width;
            float progress2 = ((x2 + offset) % width) / width;

            float y1 = calculateECGValue(progress1, frequency, amplitude);
            float y2 = calculateECGValue(progress2, frequency, amplitude);

            int screenY1 = y + height / 2 - (int) y1;
            int screenY2 = y + height / 2 - (int) y2;

            // ВАЖНО: Ограничиваем координаты, чтобы не выходили за границы
            screenY1 = Math.max(y + 2, Math.min(y + height - 2, screenY1));
            screenY2 = Math.max(y + 2, Math.min(y + height - 2, screenY2));

            // Рисуем линию сегмента
            drawLine(graphics, x + i, screenY1, x + i + 1, screenY2, 0xFF00FF00);
        }

        // Сетка ЭКГ (более редкая, чтобы не мерцало)
        for (int i = 0; i < width; i += 20) {
            graphics.fill(x + i, y, x + i + 1, y + height, 0x30004400);
        }
        for (int i = 0; i < height; i += 15) {
            graphics.fill(x, y + i, x + width, y + i + 1, 0x30004400);
        }
    }

    /**
     * Вычисляет значение ЭКГ для заданной позиции
     */
    private float calculateECGValue(float progress, float frequency, float amplitude) {
        float phase = progress * frequency * (float) Math.PI * 2;
        float normalizedPhase = phase % ((float) Math.PI * 2);

        // Характерная форма ЭКГ (P-QRS-T комплекс)
        float value = 0;

        // P волна
        if (normalizedPhase < 0.3) {
            value = (float) Math.sin(normalizedPhase / 0.3 * Math.PI) * amplitude * 0.15F;
        }
        // QRS комплекс
        else if (normalizedPhase < 0.5) {
            float qrsPhase = (normalizedPhase - 0.3F) / 0.2F;
            if (qrsPhase < 0.3) {
                value = -(float) Math.sin(qrsPhase / 0.3 * Math.PI) * amplitude * 0.25F; // Q
            } else if (qrsPhase < 0.6) {
                value = (float) Math.sin((qrsPhase - 0.3) / 0.3 * Math.PI) * amplitude * 0.8F; // R (уменьшен пик)
            } else {
                value = -(float) Math.sin((qrsPhase - 0.6) / 0.4 * Math.PI) * amplitude * 0.3F; // S
            }
        }
        // T волна
        else if (normalizedPhase < 0.8) {
            value = (float) Math.sin((normalizedPhase - 0.5) / 0.3 * Math.PI) * amplitude * 0.2F;
        }

        return value;
    }

    /**
     * Рисует линию между двумя точками
     */
    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, Math.min(y1, y2), x2, Math.max(y1, y2) + 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int yOffset = 30;
        for (int i = 0; i < survivors.size() && i < 5; i++) {
            PlayerHealthData data = survivors.get(i);
            int x = guiLeft + 10;
            int y = guiTop + yOffset;

            // Проверяем клик по кнопке "Отследить"
            // Кнопка активна только если HP < 50% и не spectator
            boolean canTrack = !data.isSpectator && data.healthPercent < 0.5F;

            if (mouseX >= x + 45 && mouseX <= x + 115 &&
                    mouseY >= y && mouseY <= y + 20) {

                // Отправляем пакет только если можно отследить
                if (canTrack) {
                    ModNetworking.CHANNEL.sendToServer(
                            new org.example.maniacrevolution.network.packets.StartTrackingPacket(data.playerUUID)
                    );
                    this.onClose();
                    return true;
                }
            }

            yOffset += 35;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Класс для хранения данных о здоровье игрока
     */
    private static class PlayerHealthData {
        java.util.UUID playerUUID;
        String playerName;
        float health;
        float maxHealth;
        float healthPercent;
        boolean isSpectator;

        PlayerHealthData(Player player) {
            this.playerUUID = player.getUUID();
            this.playerName = player.getName().getString();
            this.health = player.getHealth();
            this.maxHealth = player.getMaxHealth();
            this.healthPercent = health / maxHealth;
            this.isSpectator = player.isSpectator();
        }
    }
}