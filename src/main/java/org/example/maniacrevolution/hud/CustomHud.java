package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.maniacrevolution.client.ClientAddictionData;
import org.example.maniacrevolution.client.ClientFurySwipesData;
import org.example.maniacrevolution.client.ClientPlagueData;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.fleshheap.ClientFleshHeapData;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.item.armor.MedicalMaskItem;
import org.example.maniacrevolution.item.armor.NecromancerArmorItem;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.mana.ClientManaData;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.List;

public class CustomHud implements IGuiOverlay {
    public static final CustomHud INSTANCE = new CustomHud();

    private static final int HUD_HEIGHT = 60;
    private static final int PANEL_PADDING = 5;
    private static final int COMPONENT_GAP = 5;
    private static final int PERK_ICON_SIZE = 24;
    private static final int ABILITY_ICON_SIZE = 24;
    private static final int HOTBAR_SLOT_SIZE = 24;
    private static final int PENALTY_SLOT_SIZE = 16;
    private static final int RESOURCE_BAR_WIDTH = 140;
    private static final int RESOURCE_BAR_HEIGHT = 14;

    private static final int PANEL_BG = 0xB5101216;
    private static final int PANEL_BORDER = 0xCC59616C;
    private static final int SLOT_BG = 0xD0181B20;
    private static final int SLOT_BORDER = 0xCC555D67;
    private static final int SELECTED_SLOT_BORDER = 0xFFFFFFFF;
    private static final int SELECTED_PERK_BORDER = 0xFFFFC857;
    private static final int HP_COLOR = 0xFFE04444;
    private static final int HP_TRAIL_COLOR = 0xFFC98A8A;
    private static final int MANA_COLOR = 0xFF3C9FE8;
    private static final int ABSORPTION_COLOR = 0xFFFFC94A;
    private static final int PENALTY_BG = 0xD02A1115;
    private static final int PENALTY_BORDER = 0xFFD54A54;

    private static final ResourceLocation FLESH_HEAP_TEXTURE =
            new ResourceLocation("maniacrev", "textures/gui/flesh_heap.png");
    private static final ResourceLocation FURY_SWIPES_TEXTURE =
            new ResourceLocation("maniacrev", "textures/gui/fury_swipes.png");

    private static final long ITEM_NAME_DURATION_MS = 1500L;
    private static final long ITEM_NAME_FADE_MS = 400L;

    private final HudAnimationState animation = new HudAnimationState();
    private ItemStack lastSelectedItem = ItemStack.EMPTY;
    private long itemNameShowTime;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui forgeGui, GuiGraphics gui,
                       float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().screen instanceof ChatScreen) return;
        renderHud(gui, partialTick, screenWidth, screenHeight, false);
    }

    public void renderAboveChat(GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        gui.pose().pushPose();
        gui.pose().translate(0.0f, 0.0f, 400.0f);
        RenderSystem.disableDepthTest();
        renderHud(gui, partialTick, mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(), true);
        RenderSystem.enableDepthTest();
        gui.pose().popPose();
    }

    private void renderHud(GuiGraphics gui, float partialTick, int screenWidth,
                           int screenHeight, boolean chatOpen) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.options.hideGui || player == null || !PlayerModeUtil.isSurvivalOrAdventure(player)
                || !HudConfig.isCustomHudEnabled()) {
            return;
        }

        float healthPercent = safeRatio(player.getHealth(), player.getMaxHealth());
        float manaPercent = ClientManaData.getManaPercentage();
        boolean hasStatus = hasContextStatus(player);
        animation.update(healthPercent, manaPercent, player.getInventory().selected, hasStatus, mc.isPaused());

        int hudY = screenHeight - HUD_HEIGHT - 4 - (chatOpen ? 14 : 0);

        renderDock(gui, screenWidth / 2, hudY, player);
        renderContextStatus(gui, screenWidth / 2, hudY - 16, player);
        renderItemName(gui, player, screenWidth, hudY - 27);

        LevelHud.render(gui, 5, 5);
        TimerHud.render(gui, screenWidth / 2, 5);

        int trackerX = screenWidth - ComputerHackHud.WIDTH - 5;
        int trackerY = 5;
        if (ClientGameState.isGameRunning()) {
            ComputerHackHud.render(gui, trackerX, trackerY);
            trackerY += ComputerHackHud.HEIGHT + 4;
        }
        GeneratorChargeHud.render(gui, screenWidth - GeneratorChargeHud.WIDTH - 5, trackerY);
    }

    private void renderDock(GuiGraphics gui, int centerX, int y, Player player) {
        List<ClientPlayerData.ClientPerkData> perks = ClientPlayerData.getSelectedPerks();
        int activeIndex = ClientPlayerData.getActivePerkIndex();
        String activateKey = ModKeybinds.ACTIVATE_PERK.getTranslatedKeyMessage().getString();
        String switchKey = ModKeybinds.SWITCH_PERK.getTranslatedKeyMessage().getString();
        IItemWithAbility ability = findItemWithAbility(player);
        int effectWidth = PERK_ICON_SIZE * 2 + 2;
        if (ability != null) effectWidth += ABILITY_ICON_SIZE + 2;
        int panelWidth = PANEL_PADDING * 2 + RESOURCE_BAR_WIDTH * 2
                + COMPONENT_GAP * 2 + effectWidth;
        int panelX = centerX - panelWidth / 2;

        gui.fill(panelX, y, panelX + panelWidth, y + HUD_HEIGHT, PANEL_BG);
        gui.renderOutline(panelX, y, panelWidth, HUD_HEIGHT, PANEL_BORDER);
        gui.fill(panelX + 1, y + 1, panelX + panelWidth - 1, y + 2, 0x88777F89);

        int healthX = panelX + PANEL_PADDING;
        int effectX = healthX + RESOURCE_BAR_WIDTH + COMPONENT_GAP;
        int manaX = effectX + effectWidth + COMPONENT_GAP;
        int resourcesY = y + 9;
        int effectsY = y + 4;
        renderHealthBar(gui, player, healthX, resourcesY);
        renderManaBar(gui, manaX, resourcesY);

        int currentX = effectX;

        for (int i = 0; i < 2; i++) {
            String keyName = i == activeIndex ? activateKey : switchKey;
            if (i < perks.size()) {
                renderPerkSlot(gui, perks.get(i), currentX, effectsY, i == activeIndex,
                        keyName);
            } else {
                renderEmptyEffectSlot(gui, currentX, effectsY, PERK_ICON_SIZE, keyName,
                        i == activeIndex ? 0xFF9D7F3B : 0xFF59616C);
            }
            currentX += PERK_ICON_SIZE + 2;
        }

        if (ability != null) {
            renderAbilitySlot(gui, currentX, effectsY, ability, player);
        }

        int hotbarWidth = HOTBAR_SLOT_SIZE * 6 + 2 * 5;
        int penaltyWidth = PENALTY_SLOT_SIZE * 3 + 2 * 2;
        int itemRowWidth = hotbarWidth + 6 + penaltyWidth;
        int itemX = centerX - itemRowWidth / 2;
        renderHotbar(gui, itemX, y + 32, player);
        renderPenaltySlots(gui, itemX + hotbarWidth + 6,
                y + 32 + (HOTBAR_SLOT_SIZE - PENALTY_SLOT_SIZE) / 2, player);
    }

    private void renderHealthBar(GuiGraphics gui, Player player, int x, int y) {
        int innerWidth = RESOURCE_BAR_WIDTH - 2;
        gui.fill(x, y, x + RESOURCE_BAR_WIDTH, y + RESOURCE_BAR_HEIGHT, 0xE02B1114);

        int trailWidth = Math.round(innerWidth * Mth.clamp(animation.healthTrail(), 0.0f, 1.0f));
        int healthWidth = Math.round(innerWidth * Mth.clamp(animation.health(), 0.0f, 1.0f));
        if (trailWidth > 0) gui.fill(x + 1, y + 1, x + 1 + trailWidth, y + RESOURCE_BAR_HEIGHT - 1, HP_TRAIL_COLOR);
        if (healthWidth > 0) gui.fill(x + 1, y + 1, x + 1 + healthWidth, y + RESOURCE_BAR_HEIGHT - 1, HP_COLOR);

        float plague = Math.min(ClientPlagueData.getProgress(), animation.health());
        int plagueWidth = Math.round(innerWidth * Mth.clamp(plague, 0.0f, 1.0f));
        if (plagueWidth > 0) {
            int plagueColor = lerpColor(0xFF236417, 0xFF6DFF3A, ClientPlagueData.getProgress());
            gui.fill(x + 1, y + 1, x + 1 + plagueWidth, y + RESOURCE_BAR_HEIGHT - 1, plagueColor);
        }

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0f) {
            int totalWidth = Math.round(innerWidth * Mth.clamp(
                    (player.getHealth() + absorption) / player.getMaxHealth(), 0.0f, 1.0f));
            if (totalWidth > healthWidth) {
                gui.fill(x + 1 + healthWidth, y + 1, x + 1 + totalWidth,
                        y + RESOURCE_BAR_HEIGHT - 1, ABSORPTION_COLOR);
            }
        }

        gui.renderOutline(x, y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT, PANEL_BORDER);
        String text = Math.round(player.getHealth()) + " / " + Math.round(player.getMaxHealth());
        if (absorption > 0.0f) text += " +" + Math.round(absorption);
        drawCentered(gui, text, x, y + 3, RESOURCE_BAR_WIDTH, 0xFFFFFFFF);

        float regen = ClientHealthData.getHealthRegen();
        if (Math.abs(regen) > 0.01f) {
            String regenText = String.format("%+.1f", regen);
            int color = regen > 0 ? 0xFF8CFF8C : 0xFFFF9A9A;
            gui.drawString(Minecraft.getInstance().font, regenText,
                    x + RESOURCE_BAR_WIDTH - Minecraft.getInstance().font.width(regenText) - 2, y - 8, color, true);
        }
    }

    private void renderManaBar(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + RESOURCE_BAR_WIDTH, y + RESOURCE_BAR_HEIGHT, 0xE0102535);
        int fillWidth = Math.round((RESOURCE_BAR_WIDTH - 2) * Mth.clamp(animation.mana(), 0.0f, 1.0f));
        if (fillWidth > 0) {
            gui.fill(x + 1, y + 1, x + 1 + fillWidth, y + RESOURCE_BAR_HEIGHT - 1, MANA_COLOR);
        }
        gui.renderOutline(x, y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT, PANEL_BORDER);

        String text = Math.round(ClientManaData.getMana()) + " / " + Math.round(ClientManaData.getMaxMana());
        drawCentered(gui, text, x, y + 3, RESOURCE_BAR_WIDTH, 0xFFFFFFFF);

        float regen = ClientManaData.getRegenRate();
        if (regen > 0.01f) {
            String regenText = String.format("+%.1f", regen);
            gui.drawString(Minecraft.getInstance().font, regenText,
                    x + RESOURCE_BAR_WIDTH - Minecraft.getInstance().font.width(regenText) - 2,
                    y - 8, 0xFF8ED1FF, true);
        }
    }

    private void renderPerkSlot(GuiGraphics gui, ClientPlayerData.ClientPerkData perk,
                                int x, int y, boolean selected, String keyName) {
        Minecraft mc = Minecraft.getInstance();
        gui.fill(x, y, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE, SLOT_BG);
        renderPerkIcon(gui, perk, x, y, PERK_ICON_SIZE);
        gui.fill(x, y + PERK_ICON_SIZE - 2, x + PERK_ICON_SIZE, y + PERK_ICON_SIZE,
                getTypeColor(perk.type()));
        gui.renderOutline(x, y, PERK_ICON_SIZE, PERK_ICON_SIZE,
                selected ? SELECTED_PERK_BORDER : SLOT_BORDER);

        if (perk.isOnCooldown()) {
            int cooldownHeight = Math.round(PERK_ICON_SIZE * perk.getCooldownProgress());
            gui.fill(x, y + PERK_ICON_SIZE - cooldownHeight, x + PERK_ICON_SIZE,
                    y + PERK_ICON_SIZE, 0xC0000000);
            String cooldown = perk.getCooldownSeconds() + "с";
            drawCentered(gui, cooldown, x, y + 8, PERK_ICON_SIZE, 0xFFFFFFFF);
        } else if (perk.hasManaCost()) {
            String cost = Integer.toString((int) perk.manaCost());
            int color = ClientManaData.getMana() >= perk.manaCost() ? 0xFF9ED7FF : 0xFFFF7777;
            gui.drawString(mc.font, cost, x + PERK_ICON_SIZE - mc.font.width(cost) - 1,
                    y + PERK_ICON_SIZE - 10, color, true);
        }
        renderKeyHint(gui, keyName, x + 1, y + 1, selected ? 0xFFE5B94F : 0xFFB8C0C9);
    }

    private void renderAbilitySlot(GuiGraphics gui, int x, int y,
                                   IItemWithAbility ability, Player player) {
        Minecraft mc = Minecraft.getInstance();
        gui.fill(x, y, x + ABILITY_ICON_SIZE, y + ABILITY_ICON_SIZE, SLOT_BG);
        ResourceLocation icon = ability.getAbilityIcon();
        RenderSystem.enableBlend();
        gui.blit(icon, x, y, 0, 0, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE,
                ABILITY_ICON_SIZE, ABILITY_ICON_SIZE);
        RenderSystem.disableBlend();
        gui.renderOutline(x, y, ABILITY_ICON_SIZE, ABILITY_ICON_SIZE, SLOT_BORDER);

        if (ability instanceof ITimedAbility timed && timed.isAbilityActive(player)) {
            renderRectangularProgress(gui, x, y, ABILITY_ICON_SIZE,
                    timed.getDurationProgress(player), 0xFF70E28A);
            int remaining = timed.getRemainingDurationSeconds(player);
            if (remaining > 0) drawCentered(gui, remaining + "с", x, y + 8, ABILITY_ICON_SIZE, 0xFFFFFFFF);
        } else if (ability.isOnCooldown(player)) {
            int cooldownHeight = Math.round(ABILITY_ICON_SIZE * ability.getCooldownProgress(player));
            gui.fill(x, y + ABILITY_ICON_SIZE - cooldownHeight, x + ABILITY_ICON_SIZE,
                    y + ABILITY_ICON_SIZE, 0xC0000000);
            drawCentered(gui, ability.getCooldownSeconds(player) + "с", x, y + 8,
                    ABILITY_ICON_SIZE, 0xFFFFFFFF);
        } else if (ability.getManaCost() > 0.0f) {
            String cost = Integer.toString(Math.round(ability.getManaCost()));
            int color = ClientManaData.getMana() >= ability.getManaCost() ? 0xFF9ED7FF : 0xFFFF7777;
            gui.drawString(mc.font, cost, x + ABILITY_ICON_SIZE - mc.font.width(cost) - 1,
                    y + ABILITY_ICON_SIZE - 10, color, true);
        }
        renderKeyHint(gui, ModKeybinds.ACTIVATE_ARMOR_ABILITY.getTranslatedKeyMessage().getString(),
                x + 1, y + 1, 0xFF79BDEB);
    }

    private void renderHotbar(GuiGraphics gui, int x, int y, Player player) {
        for (int slot = 0; slot < 6; slot++) {
            int slotX = x + slot * (HOTBAR_SLOT_SIZE + 2);
            renderItemSlot(gui, player, slot, slotX, y, HOTBAR_SLOT_SIZE, false);
        }
    }

    private void renderPenaltySlots(GuiGraphics gui, int x, int y, Player player) {
        for (int i = 0; i < 3; i++) {
            int slot = i + 6;
            int slotX = x + i * (PENALTY_SLOT_SIZE + 2);
            renderItemSlot(gui, player, slot, slotX, y, PENALTY_SLOT_SIZE, true);
        }
    }

    private void renderItemSlot(GuiGraphics gui, Player player, int slot, int x, int y,
                                int size, boolean penalty) {
        Minecraft mc = Minecraft.getInstance();
        boolean selected = player.getInventory().selected == slot;
        float scale = animation.selectedSlotScale(slot);

        gui.pose().pushPose();
        gui.pose().translate(x + size / 2.0f, y + size / 2.0f, selected ? 20.0f : 0.0f);
        gui.pose().scale(scale, scale, 1.0f);
        int localX = -size / 2;
        int localY = -size / 2;

        gui.fill(localX, localY, localX + size, localY + size, penalty ? PENALTY_BG : SLOT_BG);
        gui.renderOutline(localX, localY, size, size,
                selected ? SELECTED_SLOT_BORDER : penalty ? PENALTY_BORDER : SLOT_BORDER);

        ItemStack stack = player.getInventory().getItem(slot);
        if (!stack.isEmpty()) {
            int itemOffset = (size - 16) / 2;
            gui.renderItem(stack, localX + itemOffset, localY + itemOffset);
            gui.renderItemDecorations(mc.font, stack, localX + itemOffset, localY + itemOffset);
        }

        String number = Integer.toString(slot + 1);
        gui.drawString(mc.font, number, localX + 1, localY + 1,
                penalty ? 0xFFFF8B92 : 0xFFB9C0C8, true);
        gui.pose().popPose();
    }

    private void renderContextStatus(GuiGraphics gui, int centerX, int topY, Player player) {
        float visibility = animation.statusVisibility();
        if (visibility < 0.02f) return;

        int flesh = ClientFleshHeapData.getStacks();
        int fury = ClientFurySwipesData.getSelfStackCount();
        boolean addiction = ClientAddictionData.isVisible();
        boolean air = player.getAirSupply() < player.getMaxAirSupply();

        int width = 0;
        if (flesh > 0) width += 20;
        if (fury > 0) width += (width > 0 ? 3 : 0) + 20;
        if (addiction) width += (width > 0 ? 3 : 0) + 70;
        if (air) width += (width > 0 ? 3 : 0) + 43;
        if (width == 0) return;

        int x = centerX - width / 2;
        int y = topY - Math.round((1.0f - visibility) * 3.0f);
        int alpha = Math.round(255.0f * visibility);

        if (flesh > 0) {
            renderStackStatus(gui, FLESH_HEAP_TEXTURE, flesh, x, y, alpha, 0xFFFFFFFF);
            x += 23;
        }
        if (fury > 0) {
            renderStackStatus(gui, FURY_SWIPES_TEXTURE, fury, x, y, alpha, 0xFFFF7A3D);
            x += 23;
        }
        if (addiction) {
            renderAddictionStatus(gui, x, y, alpha);
            x += 73;
        }
        if (air) {
            int seconds = Math.max(0, (int) Math.ceil(player.getAirSupply() / 20.0f));
            renderTextStatus(gui, "O2 " + seconds, x, y, 43, alpha, 0xFF6EC8FF);
        }
    }

    private void renderStackStatus(GuiGraphics gui, ResourceLocation texture, int value,
                                   int x, int y, int alpha, int textColor) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        RenderSystem.enableBlend();
        gui.blit(texture, x + 1, y - 1, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderCircularProgress(gui, x + 1, y - 1, 16, 1.0f,
                withAlpha(0xFF9099A4, alpha));
        String text = Integer.toString(value);
        int textX = x + 1 + (16 - Minecraft.getInstance().font.width(text)) / 2;
        gui.drawString(Minecraft.getInstance().font, text, textX, y + 3,
                withAlpha(textColor, alpha), true);
    }

    private void renderAddictionStatus(GuiGraphics gui, int x, int y, int alpha) {
        float progress = Mth.clamp(ClientAddictionData.getProgress(), 0.0f, 1.0f);
        int stage = Mth.clamp(ClientAddictionData.getStage(), 0, 3);
        int[] colors = {0xFF4FAE57, 0xFFD0B83F, 0xFFE18435, 0xFFD44747};
        gui.fill(x, y, x + 70, y + 14, withAlpha(0xC0101216, alpha));
        gui.renderOutline(x, y, 70, 14, withAlpha(PANEL_BORDER, alpha));
        gui.drawString(Minecraft.getInstance().font, "ЗАВ", x + 3, y + 3,
                withAlpha(0xFFBFC5CC, alpha), false);
        int barX = x + 24;
        int barWidth = 34;
        gui.fill(barX, y + 5, barX + barWidth, y + 10, withAlpha(0xFF282C31, alpha));
        gui.fill(barX, y + 5, barX + Math.round(barWidth * progress), y + 10,
                withAlpha(colors[stage], alpha));
        gui.drawString(Minecraft.getInstance().font, Integer.toString(stage), x + 62, y + 3,
                withAlpha(colors[stage], alpha), true);
    }

    private void renderTextStatus(GuiGraphics gui, String text, int x, int y,
                                  int width, int alpha, int color) {
        gui.fill(x, y, x + width, y + 14, withAlpha(0xC0101216, alpha));
        gui.renderOutline(x, y, width, 14, withAlpha(PANEL_BORDER, alpha));
        drawCentered(gui, text, x, y + 3, width, withAlpha(color, alpha));
    }

    private boolean hasContextStatus(Player player) {
        return ClientFleshHeapData.getStacks() > 0
                || ClientFurySwipesData.getSelfStackCount() > 0
                || ClientAddictionData.isVisible()
                || player.getAirSupply() < player.getMaxAirSupply();
    }

    private void renderItemName(GuiGraphics gui, Player player, int screenWidth, int y) {
        ItemStack current = player.getInventory().getSelected();
        long now = System.currentTimeMillis();
        if (!ItemStack.matches(current, lastSelectedItem)) {
            lastSelectedItem = current.copy();
            itemNameShowTime = current.isEmpty() ? 0L : now;
        }
        long age = now - itemNameShowTime;
        if (current.isEmpty() || age < 0L || age >= ITEM_NAME_DURATION_MS) return;

        float alpha = age <= ITEM_NAME_DURATION_MS - ITEM_NAME_FADE_MS
                ? 1.0f
                : (ITEM_NAME_DURATION_MS - age) / (float) ITEM_NAME_FADE_MS;
        String name = current.getHoverName().getString();
        int width = Minecraft.getInstance().font.width(name);
        gui.drawString(Minecraft.getInstance().font, name, (screenWidth - width) / 2,
                y, withAlpha(0xFFFFFFFF, Math.round(alpha * 255.0f)), true);
    }

    private IItemWithAbility findItemWithAbility(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof IItemWithAbility ability) return ability;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof IItemWithAbility ability) return ability;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IItemWithAbility ability) return ability;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (!(armor.getItem() instanceof IItemWithAbility ability)) continue;
            if (armor.getItem() instanceof NecromancerArmorItem && !hasFullNecromancerSet(player)) continue;
            if (armor.getItem() instanceof MedicalMaskItem && slot != EquipmentSlot.HEAD) continue;
            return ability;
        }
        return null;
    }

    private boolean hasFullNecromancerSet(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (!(armor.getItem() instanceof NecromancerArmorItem)) return false;
        }
        return true;
    }

    private void renderCircularProgress(GuiGraphics gui, int x, int y, int size,
                                        float progress, int color) {
        int segments = 64;
        int visibleSegments = Math.round(segments * Mth.clamp(progress, 0.0f, 1.0f));
        double radius = size / 2.0 - 0.75;
        double center = (size - 1) / 2.0;
        for (int i = 0; i < visibleSegments; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / segments;
            int pointX = x + (int) Math.round(center + Math.cos(angle) * radius);
            int pointY = y + (int) Math.round(center + Math.sin(angle) * radius);
            gui.fill(pointX, pointY, pointX + 1, pointY + 1, color);
        }
    }

    private void renderRectangularProgress(GuiGraphics gui, int x, int y, int size,
                                           float progress, int color) {
        int length = Math.round((size * 4.0f) * Mth.clamp(progress, 0.0f, 1.0f));
        int top = Math.min(length, size);
        if (top > 0) gui.fill(x, y, x + top, y + 1, color);
        length -= top;
        int right = Math.min(Math.max(length, 0), size);
        if (right > 0) gui.fill(x + size - 1, y, x + size, y + right, color);
        length -= right;
        int bottom = Math.min(Math.max(length, 0), size);
        if (bottom > 0) gui.fill(x + size - bottom, y + size - 1, x + size, y + size, color);
        length -= bottom;
        int left = Math.min(Math.max(length, 0), size);
        if (left > 0) gui.fill(x, y + size - left, x + 1, y + size, color);
    }

    private void renderPerkIcon(GuiGraphics gui, ClientPlayerData.ClientPerkData perk,
                                int x, int y, int size) {
        RenderSystem.enableBlend();
        gui.blit(perk.getIcon(), x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
    }

    private void renderEmptyEffectSlot(GuiGraphics gui, int x, int y, int size,
                                       String keyName, int borderColor) {
        gui.fill(x, y, x + size, y + size, 0x70181B20);
        gui.renderOutline(x, y, size, size, borderColor);
        renderKeyHint(gui, keyName, x + 1, y + 1, borderColor);
    }

    private void renderKeyHint(GuiGraphics gui, String keyName, int x, int y, int color) {
        Minecraft mc = Minecraft.getInstance();
        String key = keyName.length() > 3 ? keyName.substring(0, 3) : keyName;
        gui.drawString(mc.font, key, x, y, color, true);
    }

    private int getTypeColor(PerkType type) {
        return switch (type) {
            case PASSIVE, PASSIVE_COOLDOWN -> 0xFF4E78D4;
            case ACTIVE -> 0xFFD95B50;
            case HYBRID -> 0xFFB06CD4;
        };
    }

    private static void drawCentered(GuiGraphics gui, String text, int x, int y,
                                     int width, int color) {
        Minecraft mc = Minecraft.getInstance();
        gui.drawString(mc.font, text, x + (width - mc.font.width(text)) / 2, y, color, true);
    }

    private static float safeRatio(float value, float max) {
        return max > 0.0f ? Mth.clamp(value / max, 0.0f, 1.0f) : 0.0f;
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int first, int second, float progress) {
        float t = Mth.clamp(progress, 0.0f, 1.0f);
        int r = Math.round(Mth.lerp(t, (first >> 16) & 0xFF, (second >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(t, (first >> 8) & 0xFF, (second >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, first & 0xFF, second & 0xFF));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
