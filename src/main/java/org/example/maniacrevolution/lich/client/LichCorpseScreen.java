package org.example.maniacrevolution.lich.client;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.lich.LichRules;
import org.example.maniacrevolution.lich.LichUnit;
import org.example.maniacrevolution.mana.ClientManaData;

/** Corpse ritual only; army management keeps its existing screen. */
public final class LichCorpseScreen extends Screen {
    private static final int INK = 0xffe1efdf, JADE = 0xff79d6b1, MUTED = 0xff92a79d, GOLD = 0xffc5ad72;
    private static final int MANA = 0xff3c9fe8;
    private static final int NO_MANA = 0xffff858b;
    private static final ResourceLocation SWORD = new ResourceLocation("minecraft", "textures/item/iron_sword.png");
    private static final String[] ROLES = {"ПЕХОТА", "СТРЕЛОК", "РАЗВЕДЧИК", "ТЯЖЁЛЫЙ БОЕЦ"};
    private static final String[] TRAITS = {"Ближний бой", "Атакует издалека", "Обнаруживает цели", "Устойчив к отбрасыванию"};
    private final UUID corpse;
    private final LichUnit[] previews = new LichUnit[4];
    private final long opened = System.nanoTime();
    private double uiScale, originX, originY;
    private RitualButton[] cards;
    private RitualButton[] controls;

    public LichCorpseScreen(UUID corpse) { super(Component.literal("Ритуал некромантии")); this.corpse = corpse; }
    private double seconds() { return (System.nanoTime() - opened) / 1_000_000_000.0; }
    @Override protected void init() {
        uiScale = LichCorpseLayout.scale(width, height);
        originX = (width - LichCorpseLayout.WIDTH * uiScale) / 2;
        originY = (height - LichCorpseLayout.HEIGHT * uiScale) / 2;
        cards = new RitualButton[4];
        for (var kind : LichRules.Kind.values()) {
            int i = kind.ordinal();
            cards[i] = addRenderableWidget(new RitualButton(kind.title + ", " + kind.cost + " маны. " + TRAITS[i], i, () -> cast("raise", i)));
            if (previews[i] == null && minecraft.level != null) {
                var unit = ModEntities.LICH_UNIT.get().create(minecraft.level);
                if (unit != null) {
                    unit.configure(minecraft.player.getUUID(), kind);
                    CompoundTag saved = new CompoundTag(); unit.saveWithoutId(saved);
                    saved.putInt("LichEmerge", 0); unit.readAdditionalSaveData(saved);
                    previews[i] = unit;
                }
            }
        }
        var absorb = addRenderableWidget(new RitualButton("Поглотить труп, +20 маны. Пробел", -2, () -> cast("absorb", 0)));
        absorb.place(160, 308, 300, 34, .16);
        var army = addRenderableWidget(new RitualButton("Армия", -1, () -> LichClient.request("open")));
        army.place(26, 18, 66, 22, .04);
        var close = addRenderableWidget(new RitualButton("×", -1, this::onClose));
        close.place(570, 18, 24, 22, .04);
        controls = new RitualButton[]{absorb, army, close};
        updateCards();
    }
    private void cast(String action, int kind) {
        CompoundTag data = new CompoundTag(); data.putUUID("corpse", corpse); data.putInt("kind", kind);
        LichClient.request(action, data); onClose();
    }
    private void updateCards() {
        double elapsed = seconds();
        for (int i = 0; i < cards.length; i++) {
            var card = cards[i]; double s = LichCorpseLayout.cardScale(i, elapsed);
            card.bounds(LichCorpseLayout.centerX(i, elapsed) - 66 * s, 177 - 111 * s, 132 * s, 222 * s);
            card.active = elapsed > .3 + i * .045 && ClientManaData.getMana() >= LichRules.Kind.values()[i].cost;
        }
        for (var control : controls) control.updateReveal(elapsed);
    }
    @Override public void tick() { updateCards(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (org.example.maniacrevolution.event.ArmorAbilityKeyHandler.closeLichMenu(this, key, scan)) return true;
        if (key == GLFW.GLFW_KEY_SPACE) {
            if (minecraft.options.keyJump.matches(GLFW.GLFW_KEY_SPACE, scan)) LichRitualInput.consumeSpaceUntilRelease();
            cast("absorb", 0); return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
            int kind = key - GLFW.GLFW_KEY_1;
            // Keyboard selection is unambiguous even while the cards overlap.
            if (ClientManaData.getMana() >= LichRules.Kind.values()[kind].cost) cast("raise", kind);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        updateCards();
        return super.mouseClicked(x, y, button);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateCards();
        float background = LichCorpseLayout.reveal(seconds(), 0, 0).opacity();
        g.fillGradient(0, 0, width, height, fade(0xe5091413, background), fade(0xf0030809, background));
        g.pose().pushPose(); g.pose().translate(originX, originY, 0); g.pose().scale((float)uiScale, (float)uiScale, 1);
        float header = reveal(g, .02, 310, 39, 18);
        if (header > .02f) {
            g.drawCenteredString(font, "РИТУАЛ НЕКРОМАНТИИ", 310, 20, fade(GOLD, header));
            g.drawCenteredString(font, "Одна душа. Четыре облика.", 310, 39, fade(MUTED, header));
            g.fill(225, 56, 395, 57, fade(0xff34584c, header));
        }
        g.pose().popPose();
        String balance = Integer.toString((int)ClientManaData.getMana());
        String timing = "  ·  Воплощение / поглощение: 0,5 с";
        int start = 310 - (font.width(balance) + 13 + font.width(timing)) / 2;
        float resources = reveal(g, .12, 310, 297, -18);
        if (resources > .02f) {
            mana(g, balance, start, 293, resources);
            g.drawString(font, timing, start + font.width(balance) + 13, 293, fade(MUTED, resources), false);
        }
        g.pose().popPose();
        float hints = reveal(g, .22, 310, 358, -18);
        if (hints > .02f) g.drawCenteredString(font, "1–4  выбрать     •     ПРОБЕЛ  поглотить     •     ПКМ / ESC  закрыть", 310, 354, fade(0xff687e75, hints));
        g.pose().popPose();
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }
    private float reveal(GuiGraphics g, double delay, double x, double y, double travel) {
        var motion = LichCorpseLayout.reveal(seconds(), delay, travel);
        g.pose().pushPose(); g.pose().translate(x, y + motion.offsetY(), 0);
        g.pose().scale((float)motion.scale(), (float)motion.scale(), 1); g.pose().translate(-x, -y, 0);
        return motion.opacity();
    }
    private static int fade(int color, float opacity) {
        return (color & 0x00ffffff) | (Math.round((color >>> 24) * opacity) << 24);
    }
    /** Small faceted crystal drawn in GUI coordinates, so it scales with the cards. */
    private void mana(GuiGraphics g, String value, int x, int y) {
        mana(g, value, x, y, 1);
    }
    private void mana(GuiGraphics g, String value, int x, int y, float opacity) {
        mana(g, value, x, y, opacity, MANA);
    }
    private void mana(GuiGraphics g, String value, int x, int y, float opacity, int textColor) {
        g.drawString(font, value, x, y, fade(textColor, opacity), false);
        int cx = x + font.width(value) + 6;
        for (int row=0; row<10; row++) {
            int radius = Math.min(row, 9-row) * 3 / 4;
            g.fill(cx-radius, y-1+row, cx+radius+1, y+row, fade(0xff216ba8, opacity));
            g.fill(cx-radius, y-1+row, cx+1, y+row, fade(MANA, opacity));
        }
        g.fill(cx-1, y+1, cx+1, y+4, fade(0xffb7ebff, opacity));
    }
    private void heart(GuiGraphics g, int x, int y) {
        g.fill(x+1,y,x+4,y+2,0xffde8185); g.fill(x+5,y,x+8,y+2,0xffde8185);
        g.fill(x,y+2,x+9,y+4,0xffde8185);
        for(int row=0;row<4;row++)g.fill(x+row+1,y+4+row,x+8-row,y+5+row,0xffde8185);
    }
    private final class RitualButton extends AbstractButton {
        private final int index;
        private final Runnable action;
        private double x, y, w, h;
        private double homeX, homeY, homeW, homeH, delay;
        private float opacity = 1;
        RitualButton(String label, int index, Runnable action) {
            super(0, 0, 1, 1, Component.literal(label)); this.index = index; this.action = action;
        }
        void place(double x, double y, double w, double h, double delay) {
            homeX=x; homeY=y; homeW=w; homeH=h; this.delay=delay;
        }
        void updateReveal(double elapsed) {
            var motion = LichCorpseLayout.reveal(elapsed, delay, index == -2 ? -18 : 18);
            double scale = motion.scale(); opacity = motion.opacity(); active = opacity > .6f;
            bounds(homeX + homeW*(1-scale)/2, homeY + homeH*(1-scale)/2 + motion.offsetY(), homeW*scale, homeH*scale);
        }
        void bounds(double x, double y, double w, double h) {
            this.x=x; this.y=y; this.w=w; this.h=h;
            setX((int)Math.round(originX+x*uiScale)); setY((int)Math.round(originY+y*uiScale));
            setWidth(Math.max(1,(int)Math.round(w*uiScale))); height=Math.max(1,(int)Math.round(h*uiScale));
        }
        @Override public void onPress() { if (active) action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            if (opacity <= .02f) return;
            boolean highlight = isHoveredOrFocused() && active;
            boolean lacksMana = index >= 0 && ClientManaData.getMana() < LichRules.Kind.values()[index].cost;
            g.pose().pushPose(); g.pose().translate(originX+x*uiScale, originY+y*uiScale, 0);
            float scale = (float)(uiScale * (index < 0 ? w / homeW : w / 132)); g.pose().scale(scale, scale, 1);
            int cw = index < 0 ? (int)homeW : 132, ch = index < 0 ? (int)homeH : 222;
            int edge = lacksMana ? 0xffb75862 : highlight ? JADE : 0xff36594c;
            g.fillGradient(0, 0, cw, ch, fade(lacksMana ? 0xff302022 : highlight ? 0xff183d32 : 0xff10271f, opacity), fade(0xff080f12, opacity));
            g.fill(0, 0, cw, 1, fade(edge, opacity)); g.fill(0, ch-1, cw, ch, fade(edge, opacity));
            g.fill(0, 0, 1, ch, fade(edge, opacity)); g.fill(cw-1, 0, cw, ch, fade(edge, opacity));
            if (index == -2) {
                g.drawCenteredString(font, "Поглотить труп   [ПРОБЕЛ]", cw/2, 6, fade(highlight ? INK : JADE, opacity));
                mana(g, "+20", (cw-font.width("+20")-13)/2, 21, opacity);
            }
            else if (index < 0) g.drawCenteredString(font, getMessage(), cw/2, (ch-8)/2, fade(highlight ? INK : JADE, opacity));
            else {
                var kind = LichRules.Kind.values()[index];
                g.drawString(font, "0" + (index+1), 10, 10, GOLD, false);
                String cost = Integer.toString(kind.cost);
                mana(g, cost, 122-font.width(cost)-13, 10, 1, lacksMana ? NO_MANA : MANA);
                g.drawCenteredString(font, kind.title, 66, 28, INK);
                g.fillGradient(8, 45, 124, 132, 0xff0a1917, 0xff1d382e);
                if (previews[index] != null) {
                    var unit = previews[index]; unit.tickCount = (int)(seconds()*20);
                    int clipX = (int)Math.floor(originX+x*uiScale+8*scale), clipY = (int)Math.floor(originY+y*uiScale+44*scale);
                    g.enableScissor(clipX, clipY, (int)Math.ceil(originX+x*uiScale+124*scale), (int)Math.ceil(originY+y*uiScale+133*scale));
                    InventoryScreen.renderEntityInInventoryFollowsAngle(g, 66, 129, 35, -.16f, 0, unit);
                    g.disableScissor();
                }
                g.drawCenteredString(font, ROLES[index], 66, 139, GOLD);
                heart(g, 10, 157);
                g.drawString(font, Integer.toString((int)kind.hp), 122-font.width(Integer.toString((int)kind.hp)), 157, INK, false);
                String damage = (int)kind.damage + " / " + (kind.interval / 20.0) + " с";
                g.pose().pushPose(); g.pose().translate(10, 169, 0); g.pose().scale(.625f, .625f, 1);
                g.blit(SWORD, 0, 0, 0, 0, 16, 16, 16, 16); g.pose().popPose();
                g.drawString(font, damage, 122-font.width(damage), 170, INK, false);
                g.pose().pushPose(); g.pose().translate(66, 185, 0); g.pose().scale(.8f, .8f, 1);
                g.drawCenteredString(font, TRAITS[index], 0, 0, MUTED); g.pose().popPose();
                if (lacksMana) g.fill(1, 202, cw-1, ch-1, 0xff36191f);
                g.fill(10, 201, 122, 202, edge);
                g.drawCenteredString(font, lacksMana ? "МАЛО МАНЫ" : "ПОДНЯТЬ", 66, 208, lacksMana ? NO_MANA : active ? JADE : MUTED);
            }
            g.pose().popPose();
        }
    }
}
