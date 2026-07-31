package org.example.maniacrevolution.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.StartTrackingPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central bedside monitor for survivor telemetry.
 */
@OnlyIn(Dist.CLIENT)
public class MedicTabletScreen extends Screen {
    private static final int GAP = 5;
    private static final int TOP_BAR_HEIGHT = 27;
    private static final int PATIENT_HEADER_HEIGHT = 20;
    private static final int PATIENT_ROW_HEIGHT = 34;
    private static final int PATIENT_ROW_GAP = 3;

    private static final int DEVICE_SHELL = 0xFF394249;
    private static final int DEVICE_LIGHT = 0xFF78848A;
    private static final int DEVICE_DARK = 0xFF171D21;
    private static final int DISPLAY_BLACK = 0xFF050A09;
    private static final int DISPLAY_PANEL = 0xF008100F;
    private static final int DISPLAY_HEADER = 0xFF111A18;
    private static final int ECG_GREEN = 0xFF68F58A;
    private static final int ECG_GRID_MINOR = 0x1835A65F;
    private static final int ECG_GRID_MAJOR = 0x3049D477;

    private final List<PlayerHealthData> survivors = new ArrayList<>();
    private final List<PatientHit> patientHits = new ArrayList<>();

    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private int patientScroll;
    private int patientScrollMax;

    private UUID selectedPlayerUUID;
    private Component hoveredHint;

    private Rect monitorBody = Rect.EMPTY;
    private Rect display = Rect.EMPTY;
    private Rect controlBar = Rect.EMPTY;
    private Rect patientPanel = Rect.EMPTY;
    private Rect patientViewport = Rect.EMPTY;
    private Rect mainPanel = Rect.EMPTY;
    private Rect waveformPanel = Rect.EMPTY;
    private Rect vitalsPanel = Rect.EMPTY;
    private Rect trackButton = Rect.EMPTY;
    private Rect closeButton = Rect.EMPTY;

    public MedicTabletScreen() {
        super(Component.literal("Прикроватный кардиомонитор"));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        updateSurvivors();
    }

    private void calculateLayout() {
        int availableWidth = Math.max(1, width - 12);
        int availableHeight = Math.max(1, height - 28);
        int maxWidth = Math.min(680, availableWidth);
        int maxHeight = Math.min(390, availableHeight);
        int minWidth = Math.min(310, maxWidth);
        int minHeight = Math.min(210, maxHeight);

        pageWidth = Mth.clamp(Math.round(width * 0.82F), minWidth, maxWidth);
        pageHeight = Mth.clamp(Math.round(height * 0.82F), minHeight, maxHeight);
        pageX = (width - pageWidth) / 2;
        pageY = Math.max(3, (height - pageHeight - 10) / 2);

        monitorBody = new Rect(pageX, pageY, pageWidth, pageHeight);
        display = new Rect(pageX + 8, pageY + 8,
                Math.max(1, pageWidth - 16), Math.max(1, pageHeight - 42));
        controlBar = new Rect(pageX + 8, display.bottom() + 5,
                Math.max(1, pageWidth - 16), 23);

        int contentY = display.y() + TOP_BAR_HEIGHT;
        int contentHeight = Math.max(1, display.bottom() - contentY);
        int sidebarMaximum = Math.min(150, display.width());
        int sidebarMinimum = Math.min(104, sidebarMaximum);
        int sidebarWidth = Mth.clamp(display.width() * 24 / 100,
                sidebarMinimum, sidebarMaximum);
        patientPanel = new Rect(display.x(), contentY, sidebarWidth, contentHeight);
        mainPanel = new Rect(patientPanel.right() + 1, contentY,
                Math.max(1, display.right() - patientPanel.right() - 1), contentHeight);
        patientViewport = new Rect(patientPanel.x() + 4,
                patientPanel.y() + PATIENT_HEADER_HEIGHT + 4,
                Math.max(1, patientPanel.width() - 8),
                Math.max(1, patientPanel.height() - PATIENT_HEADER_HEIGHT - 8));

        int innerTop = mainPanel.y() + PATIENT_HEADER_HEIGHT;
        int innerHeight = Math.max(1, mainPanel.bottom() - innerTop);
        int vitalsMaximum = Math.min(112, mainPanel.width());
        int vitalsMinimum = Math.min(70, vitalsMaximum);
        int vitalsWidth = Mth.clamp(mainPanel.width() * 28 / 100,
                vitalsMinimum, vitalsMaximum);
        vitalsPanel = new Rect(mainPanel.right() - vitalsWidth - 4,
                innerTop + 4, vitalsWidth, Math.max(1, innerHeight - 8));
        waveformPanel = new Rect(mainPanel.x() + 4, innerTop + 4,
                Math.max(1, vitalsPanel.x() - mainPanel.x() - 8),
                Math.max(1, innerHeight - 8));

        closeButton = new Rect(controlBar.right() - 20, controlBar.y() + 2, 18, 19);
        int trackWidth = Mth.clamp(controlBar.width() * 25 / 100, 76, 104);
        trackButton = new Rect(closeButton.x() - trackWidth - GAP,
                controlBar.y() + 2, trackWidth, 19);
    }

    private void updateSurvivors() {
        survivors.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            selectedPlayerUUID = null;
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (player == minecraft.player) {
                continue;
            }
            Team team = player.getTeam();
            if (team != null && "survivors".equalsIgnoreCase(team.getName())) {
                survivors.add(new PlayerHealthData(player));
            }
        }

        if (survivors.isEmpty()) {
            selectedPlayerUUID = null;
        } else if (selectedPatient() == null) {
            selectedPlayerUUID = survivors.get(0).playerUUID;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateSurvivors();
        hoveredHint = null;
        patientHits.clear();

        renderBackground(graphics);
        drawBackdrop(graphics);
        drawMonitorHousing(graphics);
        drawDisplay(graphics, mouseX, mouseY);
        drawControls(graphics, mouseX, mouseY);

        if (hoveredHint != null) {
            graphics.renderTooltip(font, hoveredHint, mouseX, mouseY);
        }
    }

    private void drawBackdrop(GuiGraphics graphics) {
        graphics.fillGradient(0, 0, width, height, 0xE50A1212, 0xF0030708);
        int glowWidth = Math.max(1, Math.min(Math.max(1, width - 12), pageWidth + 46));
        int glowX = (width - glowWidth) / 2;
        graphics.fillGradient(glowX, Math.max(0, pageY - 10), glowX + glowWidth,
                Math.min(height, monitorBody.bottom() + 16), 0x182D6258, 0x08000000);
    }

    private void drawMonitorHousing(GuiGraphics graphics) {
        drawStand(graphics);

        graphics.fill(monitorBody.x() + 4, monitorBody.y() + 5,
                monitorBody.right() + 4, monitorBody.bottom() + 5, 0xA0000000);
        graphics.fill(monitorBody.x(), monitorBody.y(), monitorBody.right(), monitorBody.bottom(),
                DEVICE_SHELL);
        graphics.renderOutline(monitorBody.x(), monitorBody.y(), monitorBody.width(),
                monitorBody.height(), DEVICE_LIGHT);
        graphics.fill(monitorBody.x() + 1, monitorBody.y() + 1,
                monitorBody.right() - 1, monitorBody.y() + 3, 0xFF566169);
        graphics.fill(monitorBody.x() + 1, monitorBody.bottom() - 3,
                monitorBody.right() - 1, monitorBody.bottom() - 1, DEVICE_DARK);

        drawScrew(graphics, monitorBody.x() + 4, monitorBody.y() + 4);
        drawScrew(graphics, monitorBody.right() - 7, monitorBody.y() + 4);
        drawScrew(graphics, monitorBody.x() + 4, monitorBody.bottom() - 7);
        drawScrew(graphics, monitorBody.right() - 7, monitorBody.bottom() - 7);

        graphics.fill(display.x() - 2, display.y() - 2,
                display.right() + 2, display.bottom() + 2, DEVICE_DARK);
        graphics.renderOutline(display.x() - 2, display.y() - 2,
                display.width() + 4, display.height() + 4, 0xFF111619);
        graphics.fill(display.x(), display.y(), display.right(), display.bottom(), DISPLAY_BLACK);

        graphics.fill(controlBar.x(), controlBar.y(), controlBar.right(), controlBar.bottom(),
                0xFF252D32);
        graphics.renderOutline(controlBar.x(), controlBar.y(), controlBar.width(),
                controlBar.height(), 0xFF59636A);
    }

    private void drawStand(GuiGraphics graphics) {
        int centerX = monitorBody.x() + monitorBody.width() / 2;
        int standBottom = Math.min(height - 2, monitorBody.bottom() + 10);
        graphics.fill(centerX - 6, monitorBody.bottom(), centerX + 6, standBottom - 3,
                DEVICE_DARK);
        graphics.fill(centerX - 23, standBottom - 4, centerX + 23, standBottom,
                0xFF30383D);
        graphics.fill(centerX - 17, standBottom - 3, centerX + 17, standBottom - 2,
                DEVICE_LIGHT);
    }

    private void drawScrew(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 3, y + 3, 0xFF161C20);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, 0xFF8B969B);
    }

    private void drawDisplay(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTopBar(graphics);
        drawPatientPanel(graphics, mouseX, mouseY);

        PlayerHealthData selected = selectedPatient();
        if (selected == null) {
            drawEmptyMonitor(graphics);
        } else {
            drawMainMonitor(graphics, selected);
        }
    }

    private void drawTopBar(GuiGraphics graphics) {
        graphics.fill(display.x(), display.y(), display.right(),
                display.y() + TOP_BAR_HEIGHT, DISPLAY_HEADER);
        graphics.fill(display.x(), display.y() + TOP_BAR_HEIGHT - 1,
                display.right(), display.y() + TOP_BAR_HEIGHT, 0xFF315148);

        int badgeX = display.x() + 6;
        int badgeY = display.y() + 5;
        drawMedicalCross(graphics, badgeX, badgeY, 17, ECG_GREEN, 0xFF13251E);

        String channel = survivors.isEmpty() ? "НЕТ КАНАЛОВ" : "ПОСТОВ: " + survivors.size();
        int channelWidth = font.width(channel) + 17;
        int channelX = display.right() - channelWidth - 6;
        graphics.fill(channelX, display.y() + 6,
                channelX + channelWidth, display.y() + 21, 0xFF0B1412);
        graphics.renderOutline(channelX, display.y() + 6, channelWidth, 15,
                survivors.isEmpty() ? GuideTheme.TEXT_MUTED : ECG_GREEN);
        graphics.fill(channelX + 5, display.y() + 11,
                channelX + 8, display.y() + 14,
                survivors.isEmpty() ? GuideTheme.TEXT_MUTED : pulseAlpha(ECG_GREEN));
        graphics.drawString(font, channel, channelX + 11, display.y() + 10,
                survivors.isEmpty() ? GuideTheme.TEXT_MUTED : ECG_GREEN, false);

        int titleX = badgeX + 23;
        int titleWidth = Math.max(1, channelX - titleX - GAP);
        graphics.drawString(font, trim("ПРИКРОВАТНЫЙ МОНИТОР  MED-01", titleWidth),
                titleX, display.y() + 6, GuideTheme.TEXT, false);
        if (display.width() >= 410) {
            graphics.drawString(font, trim("ЦЕНТРАЛЬНАЯ ТЕЛЕМЕТРИЯ ОТРЯДА", titleWidth),
                    titleX, display.y() + 16, GuideTheme.TEXT_MUTED, false);
        }
    }

    private void drawPatientPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(patientPanel.x(), patientPanel.y(), patientPanel.right(), patientPanel.bottom(),
                DISPLAY_PANEL);
        graphics.fill(patientPanel.right() - 1, patientPanel.y(),
                patientPanel.right(), patientPanel.bottom(), 0xFF315148);
        graphics.fill(patientPanel.x(), patientPanel.y(), patientPanel.right(),
                patientPanel.y() + PATIENT_HEADER_HEIGHT, 0xFF0E1715);

        String header = "ПАЦИЕНТЫ  " + survivors.size();
        graphics.drawString(font, trim(header, patientPanel.width() - 12),
                patientPanel.x() + 6, patientPanel.y() + 6,
                GuideTheme.TEXT_SECONDARY, false);

        if (survivors.isEmpty()) {
            patientScroll = 0;
            patientScrollMax = 0;
            graphics.drawCenteredString(font, "НЕТ СИГНАЛА",
                    patientViewport.x() + patientViewport.width() / 2,
                    patientViewport.y() + 12, GuideTheme.TEXT_MUTED);
            return;
        }

        int contentHeight = survivors.size() * PATIENT_ROW_HEIGHT
                + Math.max(0, survivors.size() - 1) * PATIENT_ROW_GAP;
        patientScrollMax = Math.max(0, contentHeight - patientViewport.height());
        patientScroll = Mth.clamp(patientScroll, 0, patientScrollMax);

        graphics.enableScissor(patientViewport.x(), patientViewport.y(),
                patientViewport.right(), patientViewport.bottom());
        for (int index = 0; index < survivors.size(); index++) {
            Rect row = new Rect(patientViewport.x(),
                    patientViewport.y() + index * (PATIENT_ROW_HEIGHT + PATIENT_ROW_GAP)
                            - patientScroll,
                    patientViewport.width() - (patientScrollMax > 0 ? 3 : 0),
                    PATIENT_ROW_HEIGHT);
            if (row.bottom() <= patientViewport.y() || row.y() >= patientViewport.bottom()) {
                continue;
            }

            PlayerHealthData data = survivors.get(index);
            boolean hovered = patientViewport.contains(mouseX, mouseY)
                    && row.contains(mouseX, mouseY);
            boolean selected = data.playerUUID.equals(selectedPlayerUUID);
            drawPatientRow(graphics, data, index, row, hovered, selected);
            patientHits.add(new PatientHit(data.playerUUID, row));
        }
        graphics.disableScissor();

        drawPatientScrollBar(graphics, contentHeight);
    }

    private void drawPatientRow(GuiGraphics graphics, PlayerHealthData data, int index,
                                Rect row, boolean hovered, boolean selected) {
        int stateColor = stateColor(data);
        int background = selected ? 0xFF14271F : hovered ? 0xFF111D1A : 0xE5091110;
        graphics.fill(row.x(), row.y(), row.right(), row.bottom(), background);
        graphics.renderOutline(row.x(), row.y(), row.width(), row.height(),
                selected ? stateColor : hovered ? 0xFF456058 : 0xFF202D29);
        if (selected) {
            graphics.fill(row.x(), row.y(), row.x() + 3, row.bottom(), stateColor);
        }

        String bed = index < 9 ? "0" + (index + 1) : Integer.toString(index + 1);
        int bedX = row.x() + 5;
        int bedY = row.y() + 7;
        graphics.fill(bedX, bedY, bedX + 19, bedY + 20, 0xFF050A09);
        graphics.renderOutline(bedX, bedY, 19, 20, stateColor);
        graphics.drawCenteredString(font, bed, bedX + 9, bedY + 6, stateColor);

        int textX = bedX + 24;
        int textWidth = Math.max(1, row.right() - textX - 9);
        graphics.drawString(font, trim(data.playerName, textWidth), textX, row.y() + 6,
                selected ? GuideTheme.TEXT : GuideTheme.TEXT_SECONDARY, false);
        String status = data.isSpectator ? "--" : Math.round(data.healthPercent * 100.0F) + "% HP";
        graphics.drawString(font, trim(status, textWidth), textX, row.y() + 19,
                stateColor, false);
        graphics.fill(row.right() - 6, row.y() + 6, row.right() - 3, row.y() + 9,
                data.isSpectator ? GuideTheme.TEXT_MUTED : pulseAlpha(stateColor));
    }

    private void drawMainMonitor(GuiGraphics graphics, PlayerHealthData data) {
        graphics.fill(mainPanel.x(), mainPanel.y(), mainPanel.right(), mainPanel.bottom(),
                DISPLAY_BLACK);
        graphics.fill(mainPanel.x(), mainPanel.y(), mainPanel.right(),
                mainPanel.y() + PATIENT_HEADER_HEIGHT, 0xFF0A1210);

        int bedIndex = selectedPatientIndex();
        String bed = bedIndex < 9 ? "0" + (bedIndex + 1) : Integer.toString(bedIndex + 1);
        String patient = "КОЙКА " + bed + "  •  " + data.playerName;
        graphics.drawString(font, trim(patient, mainPanel.width() - 94),
                mainPanel.x() + 6, mainPanel.y() + 6, GuideTheme.TEXT, false);
        String state = stateLabel(data);
        graphics.drawString(font, state,
                mainPanel.right() - font.width(state) - 6,
                mainPanel.y() + 6, stateColor(data), false);

        drawWaveform(graphics, data);
        drawVitals(graphics, data);
    }

    private void drawWaveform(GuiGraphics graphics, PlayerHealthData data) {
        graphics.fill(waveformPanel.x(), waveformPanel.y(),
                waveformPanel.right(), waveformPanel.bottom(), DISPLAY_BLACK);
        graphics.renderOutline(waveformPanel.x(), waveformPanel.y(),
                waveformPanel.width(), waveformPanel.height(), 0xFF24332E);

        int labelHeight = Math.min(17, Math.max(10, waveformPanel.height() / 5));
        graphics.fill(waveformPanel.x() + 1, waveformPanel.y() + 1,
                waveformPanel.right() - 1, waveformPanel.y() + labelHeight, 0xFF08110E);
        graphics.drawString(font, "ECG  I", waveformPanel.x() + 5,
                waveformPanel.y() + 5, ECG_GREEN, false);
        if (waveformPanel.width() >= 150) {
            String speed = "25 mm/s";
            graphics.drawString(font, speed,
                    waveformPanel.right() - font.width(speed) - 5,
                    waveformPanel.y() + 5, GuideTheme.TEXT_MUTED, false);
        }

        Rect waveArea = new Rect(waveformPanel.x() + 2,
                waveformPanel.y() + labelHeight,
                Math.max(1, waveformPanel.width() - 4),
                Math.max(1, waveformPanel.height() - labelHeight - 2));
        drawECG(graphics, data, waveArea);
    }

    private void drawVitals(GuiGraphics graphics, PlayerHealthData data) {
        int color = stateColor(data);
        graphics.fill(vitalsPanel.x(), vitalsPanel.y(), vitalsPanel.right(), vitalsPanel.bottom(),
                0xFF07100D);
        graphics.renderOutline(vitalsPanel.x(), vitalsPanel.y(), vitalsPanel.width(),
                vitalsPanel.height(), 0xFF294036);

        graphics.drawString(font, "HP", vitalsPanel.x() + 5, vitalsPanel.y() + 5,
                ECG_GREEN, false);
        graphics.drawString(font, "%", vitalsPanel.right() - font.width("%") - 5,
                vitalsPanel.y() + 5, GuideTheme.TEXT_MUTED, false);

        String percent = data.isSpectator ? "--"
                : Integer.toString(Math.round(data.healthPercent * 100.0F));
        float numberScale = vitalsPanel.width() >= 94 && vitalsPanel.height() >= 105 ? 1.8F : 1.35F;
        drawScaledCenteredString(graphics, percent,
                vitalsPanel.x() + vitalsPanel.width() / 2,
                vitalsPanel.y() + 19, color, numberScale);

        int dividerY = vitalsPanel.y() + Math.min(50, Math.max(39, vitalsPanel.height() / 3));
        graphics.fill(vitalsPanel.x() + 5, dividerY,
                vitalsPanel.right() - 5, dividerY + 1, 0xFF294036);

        int lineY = dividerY + 6;
        graphics.drawString(font, "ЗДОРОВЬЕ", vitalsPanel.x() + 5, lineY,
                GuideTheme.TEXT_MUTED, false);
        lineY += 11;
        String rawHealth = Math.round(data.health) + " / " + Math.round(data.maxHealth);
        graphics.drawString(font, trim(rawHealth, vitalsPanel.width() - 10),
                vitalsPanel.x() + 5, lineY, color, false);

        if (lineY + 27 < vitalsPanel.bottom()) {
            lineY += 17;
            graphics.drawString(font, "СОСТОЯНИЕ", vitalsPanel.x() + 5, lineY,
                    GuideTheme.TEXT_MUTED, false);
            lineY += 11;
            graphics.drawString(font, trim(stateLabel(data), vitalsPanel.width() - 10),
                    vitalsPanel.x() + 5, lineY, color, false);
        }

        if (canTrack(data) && vitalsPanel.height() >= 112) {
            graphics.fill(vitalsPanel.x() + 1, vitalsPanel.bottom() - 17,
                    vitalsPanel.right() - 1, vitalsPanel.bottom() - 1, 0xFF351416);
            graphics.fill(vitalsPanel.x() + 5, vitalsPanel.bottom() - 11,
                    vitalsPanel.x() + 9, vitalsPanel.bottom() - 7,
                    pulseAlpha(GuideTheme.RED));
            graphics.drawString(font, trim("ТРЕВОГА", vitalsPanel.width() - 20),
                    vitalsPanel.x() + 13, vitalsPanel.bottom() - 13,
                    GuideTheme.RED, false);
        }
    }

    private void drawECG(GuiGraphics graphics, PlayerHealthData data, Rect bounds) {
        for (int x = bounds.x() + 8; x < bounds.right(); x += 8) {
            int color = (x - bounds.x()) % 32 == 0 ? ECG_GRID_MAJOR : ECG_GRID_MINOR;
            graphics.fill(x, bounds.y(), x + 1, bounds.bottom(), color);
        }
        for (int y = bounds.y() + 8; y < bounds.bottom(); y += 8) {
            int color = (y - bounds.y()) % 32 == 0 ? ECG_GRID_MAJOR : ECG_GRID_MINOR;
            graphics.fill(bounds.x(), y, bounds.right(), y + 1, color);
        }

        int lineColor = data.isSpectator ? GuideTheme.TEXT_MUTED : ECG_GREEN;
        if (data.isSpectator || bounds.width() < 4 || bounds.height() < 4) {
            int flatY = bounds.y() + bounds.height() / 2;
            graphics.fill(bounds.x() + 1, flatY, bounds.right() - 1, flatY + 1, lineColor);
            return;
        }

        int waveformWidth = Math.max(1, bounds.width() - 3);
        float frequency = 1.15F + (1.0F - data.healthPercent) * 1.65F;
        float amplitude = Math.min(bounds.height() * 0.38F,
                8.0F + (1.0F - data.healthPercent) * 9.0F);
        float offset = (System.currentTimeMillis() % 1900L) / 1900.0F * waveformWidth;

        for (int i = 0; i < waveformWidth - 1; i++) {
            float progress1 = ((i + offset) % waveformWidth) / waveformWidth;
            float progress2 = ((i + 1 + offset) % waveformWidth) / waveformWidth;
            int y1 = bounds.y() + bounds.height() / 2
                    - Math.round(calculateECGValue(progress1, frequency, amplitude));
            int y2 = bounds.y() + bounds.height() / 2
                    - Math.round(calculateECGValue(progress2, frequency, amplitude));
            y1 = Mth.clamp(y1, bounds.y() + 1, bounds.bottom() - 2);
            y2 = Mth.clamp(y2, bounds.y() + 1, bounds.bottom() - 2);
            drawLine(graphics, bounds.x() + 1 + i, y1,
                    bounds.x() + 2 + i, y2, lineColor);
        }

        int scanX = bounds.x() + 1
                + (int) ((System.currentTimeMillis() % 1500L) / 1500.0F * waveformWidth);
        graphics.fill(scanX, bounds.y() + 1, scanX + 1, bounds.bottom() - 1, 0x8068F58A);
    }

    private float calculateECGValue(float progress, float frequency, float amplitude) {
        float cycle = (progress * frequency) % 1.0F;
        if (cycle < 0.12F) {
            return (float) Math.sin(cycle / 0.12F * Math.PI) * amplitude * 0.13F;
        }
        if (cycle >= 0.18F && cycle < 0.22F) {
            return -(cycle - 0.18F) / 0.04F * amplitude * 0.25F;
        }
        if (cycle >= 0.22F && cycle < 0.255F) {
            return (-0.25F + (cycle - 0.22F) / 0.035F * 1.25F) * amplitude;
        }
        if (cycle >= 0.255F && cycle < 0.30F) {
            return (1.0F - (cycle - 0.255F) / 0.045F * 1.35F) * amplitude;
        }
        if (cycle >= 0.30F && cycle < 0.34F) {
            return (-0.35F + (cycle - 0.30F) / 0.04F * 0.35F) * amplitude;
        }
        if (cycle >= 0.48F && cycle < 0.68F) {
            return (float) Math.sin((cycle - 0.48F) / 0.20F * Math.PI) * amplitude * 0.22F;
        }
        return 0.0F;
    }

    private void drawEmptyMonitor(GuiGraphics graphics) {
        graphics.fill(mainPanel.x(), mainPanel.y(), mainPanel.right(), mainPanel.bottom(),
                DISPLAY_BLACK);
        int centerX = mainPanel.x() + mainPanel.width() / 2;
        int centerY = mainPanel.y() + mainPanel.height() / 2;
        int iconSize = Math.min(40, Math.max(22, mainPanel.height() / 4));
        drawMedicalCross(graphics, centerX - iconSize / 2,
                centerY - iconSize / 2 - 15, iconSize,
                GuideTheme.TEXT_MUTED, 0xFF0B1210);
        graphics.drawCenteredString(font, "ПАЦИЕНТ НЕ ПОДКЛЮЧЁН", centerX,
                centerY + 12, GuideTheme.TEXT_SECONDARY);
        if (mainPanel.height() >= 90) {
            graphics.drawCenteredString(font, "Ожидание сигнала команды survivors", centerX,
                    centerY + 25, GuideTheme.TEXT_MUTED);
        }
    }

    private void drawControls(GuiGraphics graphics, int mouseX, int mouseY) {
        PlayerHealthData selected = selectedPatient();
        boolean trackable = selected != null && canTrack(selected);
        boolean trackHovered = trackButton.contains(mouseX, mouseY);

        int ledX = controlBar.x() + 7;
        int ledY = controlBar.y() + 9;
        graphics.fill(ledX, ledY, ledX + 5, ledY + 5,
                survivors.isEmpty() ? GuideTheme.TEXT_MUTED : pulseAlpha(ECG_GREEN));
        graphics.drawString(font, survivors.isEmpty() ? "STANDBY" : "MONITORING",
                ledX + 10, controlBar.y() + 8,
                survivors.isEmpty() ? GuideTheme.TEXT_MUTED : ECG_GREEN, false);

        int hintX = ledX + 78;
        int hintWidth = Math.max(1, trackButton.x() - hintX - GAP);
        String hint = selected == null ? "НЕТ ПАЦИЕНТА"
                : trackable ? "ТРЕВОГА: ДОСТУПНО ОТСЛЕЖИВАНИЕ"
                : "ОТСЛЕЖИВАНИЕ ПРИ HP < 50%";
        graphics.drawString(font, trim(hint, hintWidth), hintX, controlBar.y() + 8,
                trackable ? GuideTheme.RED : GuideTheme.TEXT_MUTED, false);

        drawHardwareButton(graphics, trackButton, "ОТСЛЕДИТЬ", ECG_GREEN,
                trackHovered, trackable);
        drawHardwareButton(graphics, closeButton, "×", GuideTheme.RED,
                closeButton.contains(mouseX, mouseY), true);

        if (trackHovered) {
            hoveredHint = trackable
                    ? Component.literal("Начать отслеживание выбранного союзника")
                    : Component.literal(selected == null
                    ? "Сначала выберите пациента"
                    : selected.isSpectator
                    ? "Сигнал игрока потерян"
                    : "Отслеживание доступно при здоровье ниже 50%");
        }
    }

    private void drawHardwareButton(GuiGraphics graphics, Rect bounds, String label,
                                    int accent, boolean hovered, boolean active) {
        int background = !active ? 0xFF20272B
                : hovered ? GuideTheme.lerpColor(0xFF273238, accent, 0.20F)
                : 0xFF273238;
        int border = !active ? 0xFF4D575C : hovered ? GuideTheme.TEXT : accent;
        int textColor = !active ? GuideTheme.TEXT_MUTED : hovered ? GuideTheme.TEXT : accent;

        graphics.fill(bounds.x() + 1, bounds.y() + 2,
                bounds.right() + 1, bounds.bottom() + 2, 0x80000000);
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);
        graphics.fill(bounds.x() + 2, bounds.y() + 2,
                bounds.right() - 2, bounds.y() + 3, 0xFF596269);
        graphics.drawCenteredString(font, trim(label, bounds.width() - 6),
                bounds.x() + bounds.width() / 2, bounds.y() + 6, textColor);
    }

    private void drawPatientScrollBar(GuiGraphics graphics, int contentHeight) {
        if (patientScrollMax <= 0 || contentHeight <= 0) {
            return;
        }
        int trackX = patientViewport.right() - 2;
        int thumbHeight = Math.max(11,
                patientViewport.height() * patientViewport.height() / contentHeight);
        int travel = Math.max(1, patientViewport.height() - thumbHeight);
        int thumbY = patientViewport.y()
                + Math.round(travel * (patientScroll / (float) patientScrollMax));
        graphics.fill(trackX, patientViewport.y(), trackX + 2, patientViewport.bottom(),
                0xFF101815);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, ECG_GREEN);
    }

    private void drawMedicalCross(GuiGraphics graphics, int x, int y, int size,
                                  int color, int background) {
        graphics.fill(x, y, x + size, y + size, background);
        graphics.renderOutline(x, y, size, size, color);
        int thickness = Math.max(2, size / 5);
        int arm = Math.max(thickness + 2, size / 2);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        graphics.fill(centerX - thickness / 2, centerY - arm / 2,
                centerX + (thickness + 1) / 2, centerY + (arm + 1) / 2, color);
        graphics.fill(centerX - arm / 2, centerY - thickness / 2,
                centerX + (arm + 1) / 2, centerY + (thickness + 1) / 2, color);
    }

    private void drawScaledCenteredString(GuiGraphics graphics, String text,
                                          int centerX, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round(centerX / scale - font.width(text) / 2.0F);
        int scaledY = Math.round(y / scale);
        graphics.drawString(font, text, scaledX, scaledY, color, false);
        graphics.pose().popPose();
    }

    private PlayerHealthData selectedPatient() {
        if (selectedPlayerUUID == null) {
            return null;
        }
        for (PlayerHealthData data : survivors) {
            if (data.playerUUID.equals(selectedPlayerUUID)) {
                return data;
            }
        }
        return null;
    }

    private int selectedPatientIndex() {
        for (int index = 0; index < survivors.size(); index++) {
            if (survivors.get(index).playerUUID.equals(selectedPlayerUUID)) {
                return index;
            }
        }
        return 0;
    }

    private int stateColor(PlayerHealthData data) {
        if (data.isSpectator) {
            return GuideTheme.TEXT_MUTED;
        }
        if (data.healthPercent < 0.5F) {
            return GuideTheme.RED;
        }
        if (data.healthPercent < 0.75F) {
            return GuideTheme.GOLD;
        }
        return ECG_GREEN;
    }

    private String stateLabel(PlayerHealthData data) {
        if (data.isSpectator) {
            return "НЕТ СИГНАЛА";
        }
        if (data.healthPercent < 0.25F) {
            return "КРИТИЧНО";
        }
        if (data.healthPercent < 0.5F) {
            return "ТРЕВОГА";
        }
        if (data.healthPercent < 0.75F) {
            return "РАНЕН";
        }
        return "СТАБИЛЬНО";
    }

    private boolean canTrack(PlayerHealthData data) {
        return !data.isSpectator && data.healthPercent < 0.5F;
    }

    private int pulseAlpha(int color) {
        float wave = (float) (Math.sin(System.currentTimeMillis() / 220.0) * 0.5 + 0.5);
        int alpha = 130 + Math.round(wave * 125.0F);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, Math.min(y1, y2), x2, Math.max(y1, y2) + 1, color);
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int bodyWidth = Math.max(0, maxWidth - font.width(suffix));
        return font.plainSubstrByWidth(text, bodyWidth) + suffix;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (closeButton.contains(mouseX, mouseY)) {
            onClose();
            return true;
        }
        if (patientViewport.contains(mouseX, mouseY)) {
            for (PatientHit hit : patientHits) {
                if (hit.bounds().contains(mouseX, mouseY)) {
                    selectedPlayerUUID = hit.playerUUID();
                    return true;
                }
            }
        }
        PlayerHealthData selected = selectedPatient();
        if (trackButton.contains(mouseX, mouseY) && selected != null && canTrack(selected)) {
            ModNetworking.CHANNEL.sendToServer(new StartTrackingPacket(selected.playerUUID));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (patientViewport.contains(mouseX, mouseY) && patientScrollMax > 0) {
            patientScroll = Mth.clamp(patientScroll - (int) Math.round(delta * 28.0),
                    0, patientScrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PlayerHealthData {
        private final UUID playerUUID;
        private final String playerName;
        private final float health;
        private final float maxHealth;
        private final float healthPercent;
        private final boolean isSpectator;

        private PlayerHealthData(Player player) {
            this.playerUUID = player.getUUID();
            this.playerName = player.getName().getString();
            this.health = player.getHealth();
            this.maxHealth = player.getMaxHealth();
            this.healthPercent = maxHealth <= 0.0F ? 0.0F : health / maxHealth;
            this.isSpectator = player.isSpectator();
        }
    }

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    private record PatientHit(UUID playerUUID, Rect bounds) {
    }
}
