package org.example.maniacrevolution.engineer.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.engineer.*;
import org.example.maniacrevolution.gui.GuideTheme;

/** Compact non-pausing tablet; shares guide colours, typography and button treatment. */
public final class EngineerScreen extends Screen {
    public final boolean remote;
    private int left, top, panelWidth, page, scenario;
    private BlockPos selected;
    private net.minecraft.resources.ResourceLocation selectedDimension;
    private final List<Button> rows = new ArrayList<>();
    private Button launch, mode, previous, next;
    public EngineerScreen(boolean remote) { super(Component.translatable("screen.maniacrev.engineer." + (remote ? "remote" : "workshop"))); this.remote = remote; }
    @Override public boolean isPauseScreen() { return false; }
    private static Component t(String key, Object... args) { return Component.translatable("screen.maniacrev.engineer." + key, args); }
    private Button button(int x, int y, int w, Component text, Button.OnPress action) {
        var b = new Button(x, y, w, 20, text, action, supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                GuideTheme.drawButton(g, font, getX(), getY(), getWidth(), getHeight(), getMessage().getString(), active ? GuideTheme.GOLD : GuideTheme.BORDER_SOFT, isHoveredOrFocused() && active, false);
            }
        };
        return addRenderableWidget(b);
    }
    @Override protected void init() {
        panelWidth = Math.min(330, width - 16); left = (width - panelWidth) / 2; top = Math.max(4, (height - 226) / 2); rows.clear();
        if (!remote) {
            for (int i = 0; i < 3; i++) {
                final int type = i;
                rows.add(button(left + 34, top + 49 + i * 34, panelWidth - 46,
                        i == 2 ? t("buy.2", (int) EngineerRules.ARMOR_PER_SCRAP, EngineerRules.PLATE_COST) : t("buy." + i, EngineerRules.cost(i)), b -> EngineerClient.select(type)));
            }
            button(left + 12, top + 176, panelWidth - 24, t("close"), b -> onClose());
        } else {
            for (int i = 0; i < 4; i++) {
                final int index = i;
                rows.add(button(left + 10, top + 44 + i * 25, panelWidth - 20, Component.empty(), b -> {
                    var panels = panels(); int n = page * 4 + index;
                    if (n < panels.size()) { selected = panels.get(n).pos(); selectedDimension = panels.get(n).dimension(); }
                }));
            }
            previous = button(left + 10, top + 145, 24, Component.literal("‹"), b -> page = Math.max(0, page - 1));
            next = button(left + panelWidth - 34, top + 145, 24, Component.literal("›"), b -> page++);
            mode = button(left + 42, top + 145, panelWidth - 84, t("mode.0"), b -> scenario = 1 - scenario);
            launch = button(left + 10, top + 173, panelWidth - 20, t("launch"), b -> {
                if (selected != null) EngineerClient.request(EngineerActionPacket.Action.PLAY, selected, scenario);
            });
            button(left + 10, top + 199, panelWidth - 20, t("close"), b -> onClose());
        }
    }
    private List<EngineerStatePacket.Panel> panels() { return EngineerClient.state == null ? List.of() : EngineerClient.state.panels(); }
    @Override public void tick() {
        var s = EngineerClient.state;
        if (s == null) { rows.forEach(b -> b.active = false); if (launch != null) launch.active = false; return; }
        if (!remote) {
            boolean creative = minecraft.player != null && minecraft.player.isCreative();
            for (int i = 0; i < 3; i++) rows.get(i).active = s.active() && s.work() < 0 && s.selected() < 0 && (creative || s.scrap() >= EngineerRules.cost(i))
                    && (i == 0 ? s.barricades() < EngineerRules.MAX_BARRICADES && (creative || EngineerClient.time() >= s.buildReady())
                    : i == 1 ? s.panels().size() < EngineerRules.MAX_PANELS && (creative || EngineerClient.time() >= s.buildReady()) : s.armor() < EngineerRules.MAX_ARMOR);
            rows.get(2).setMessage(s.armor() >= EngineerRules.MAX_ARMOR ? t("armor_full", (int) EngineerRules.MAX_ARMOR, (int) EngineerRules.MAX_ARMOR)
                    : t("buy.2", EngineerClient.armorText(Math.min(EngineerRules.ARMOR_PER_SCRAP, EngineerRules.MAX_ARMOR - s.armor())), EngineerRules.PLATE_COST));
        } else {
            var panels = panels(); page = Math.min(page, Math.max(0, (panels.size() - 1) / 4));
            if (panels.stream().noneMatch(this::selected) && !panels.isEmpty()) {
                var first = panels.stream().filter(p -> minecraft.level != null && p.dimension().equals(minecraft.level.dimension().location())).findFirst().orElse(panels.get(0));
                selected = first.pos(); selectedDimension = first.dimension();
            }
            for (int i = 0; i < rows.size(); i++) {
                int n = page * 4 + i; var b = rows.get(i); b.visible = n < panels.size(); b.active = b.visible;
                if (b.visible) {
                    var p = panels.get(n);
                    String label = (selected(p) ? "◆ " : "  ") + "#" + (n + 1) + "  " + p.pos().getX() + ", " + p.pos().getY() + ", " + p.pos().getZ() + "  · " + p.uses() + "/3  · " + status(p);
                    b.setMessage(Component.literal(font.plainSubstrByWidth(label, panelWidth - 30)));
                }
            }
            previous.active = page > 0; next.active = (page + 1) * 4 < panels.size(); mode.setMessage(t("mode." + scenario));
            var p = panels.stream().filter(this::selected).findFirst().orElse(null);
            launch.active = p != null && p.loaded() && p.uses() > 0 && EngineerClient.time() >= p.readyAt() && p.playingUntil() <= EngineerClient.time()
                    && minecraft.level != null && p.dimension().equals(minecraft.level.dimension().location());
        }
    }
    private String status(EngineerStatePacket.Panel p) {
        if (!p.loaded() || minecraft.level == null || !p.dimension().equals(minecraft.level.dimension().location())) return t("offline").getString();
        if (p.playingUntil() > EngineerClient.time()) return t("playing").getString();
        if (p.readyAt() > EngineerClient.time()) return (int) Math.ceil((p.readyAt() - EngineerClient.time()) / 20) + " с";
        return t("ready").getString();
    }
    private boolean selected(EngineerStatePacket.Panel p) { return p.pos().equals(selected) && p.dimension().equals(selectedDimension); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        tick(); renderBackground(g);
        int panelHeight = remote ? 226 : 204;
        g.fill(left, top, left + panelWidth, top + panelHeight, GuideTheme.PANEL);
        g.renderOutline(left, top, panelWidth, panelHeight, GuideTheme.BORDER_SOFT);
        g.fill(left + 1, top + 1, left + panelWidth - 1, top + 23, GuideTheme.HEADER);
        g.drawCenteredString(font, title, width / 2, top + 8, GuideTheme.GOLD);
        var s = EngineerClient.state;
        g.drawString(font, t("scrap", s == null ? 0 : s.scrap(), EngineerRules.MAX_SCRAP), left + 12, top + 30, GuideTheme.TEXT, false);
        if (!remote) {
            ItemStack[] icons = {new ItemStack(ModItems.ENGINEER_BARRICADE.get()), new ItemStack(ModItems.ENGINEER_PANEL.get()), new ItemStack(ModItems.ENGINEER_SCRAP.get())};
            for (int i = 0; i < 3; i++) g.renderItem(icons[i], left + 12, top + 51 + i * 34);
            String counts = t("counts", s == null ? 0 : s.barricades(), s == null ? 0 : s.panels().size()).getString();
            g.drawString(font, font.plainSubstrByWidth(counts, panelWidth - 24), left + 12, top + 155, GuideTheme.TEXT_MUTED, false);
            g.drawString(font, t("armor", EngineerClient.armorText(s == null ? 0 : s.armor()), (int) EngineerRules.MAX_ARMOR), left + 12, top + 166, 0xFFA0A8B0, false);
        } else if (panels().isEmpty()) {
            g.drawCenteredString(font, t("empty"), width / 2, top + 82, GuideTheme.TEXT_MUTED);
            g.drawCenteredString(font, t("empty_hint"), width / 2, top + 97, GuideTheme.TEXT_SECONDARY);
        }
        super.render(g, mouseX, mouseY, partial);
    }
}
