package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.packets.RequestDeadPlayersPacket;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientScreenHelper {

    public static void openShopScreen() {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.gui.ShopScreen());
    }

    public static void openPerkScreen() {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.gui.PerkSelectionScreen());
    }

    public static void openRecipeBookScreen() {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.client.screen.RecipeBookScreen());
    }

    public static void openCharacterSelectionScreen(CharacterType type) {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.client.screen.CharacterSelectionScreen(type));
    }

    public static void openGuiByType(String typeName) {
        switch (typeName) {
            case "PERK_SELECTION" -> Minecraft.getInstance()
                    .setScreen(new org.example.maniacrevolution.gui.PerkSelectionScreen());
            case "SHOP" -> Minecraft.getInstance()
                    .setScreen(new org.example.maniacrevolution.gui.ShopScreen());
            case "GUIDE" -> Minecraft.getInstance()
                    .setScreen(new org.example.maniacrevolution.gui.GuideScreen());
        }
    }

    public static void openResurrectionScreen() {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.gui.ResurrectionScreen());
    }

    public static void openMedicTabletScreen() {
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.gui.MedicTabletScreen());
    }

    public static void openSettingsScreen() {
        org.example.maniacrevolution.client.screen.SettingsScreen.open();
    }

    public static void openGuidePage(int pageTypeId) {
        org.example.maniacrevolution.gui.pages.GuidePage.PageType[] values =
                org.example.maniacrevolution.gui.pages.GuidePage.PageType.values();
        org.example.maniacrevolution.gui.pages.GuidePage.PageType pageType =
                pageTypeId >= 0 && pageTypeId < values.length ? values[pageTypeId] : values[0];
        Minecraft.getInstance().setScreen(new org.example.maniacrevolution.gui.GuideScreen(pageType));
    }

    public static void handleMapVotingPacket(boolean open, int timeRemaining,
                                             Map<String, Integer> voteCount, String playerVotedMapId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof org.example.maniacrevolution.client.screen.MapVotingScreen screen) {
            screen.updateVoting(timeRemaining, voteCount);
        } else if (open) {
            mc.setScreen(new org.example.maniacrevolution.client.screen.MapVotingScreen(
                    timeRemaining, voteCount, playerVotedMapId));
        }
    }

    public static void showMapVotingResult(String winnerMapId, Map<String, Integer> finalVoteCount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof org.example.maniacrevolution.client.screen.MapVotingScreen screen) {
            screen.showResult(winnerMapId, finalVoteCount);
        }
    }

    public static void updateAgent47Money(int money) {
        org.example.maniacrevolution.gui.Agent47TabletScreen.updateMoney(money);
    }

    public static void updateAgent47Data(String targetName, int money, List<Agent47ShopConfig.ShopItem> shopItems) {
        org.example.maniacrevolution.gui.Agent47TabletScreen.updateData(targetName, money, shopItems);
    }

    public static void updateDeadPlayers(List<RequestDeadPlayersPacket.DeadPlayerInfo> deadPlayers) {
        org.example.maniacrevolution.gui.ResurrectionScreen.updateDeadPlayers(deadPlayers);
    }

    public static void syncGenerator(BlockPos pos, int charge, boolean powered) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be instanceof FNAFGeneratorBlockEntity generator) {
            generator.setCharge(charge, false);
            generator.setPowered(powered, false);
        }
    }

    public static void showTabletCooldown(int cooldownSeconds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && cooldownSeconds > 0) {
            mc.player.displayClientMessage(
                    Component.literal(String.format("В§eРљСѓР»РґР°СѓРЅ РѕС‚СЃР»РµР¶РёРІР°РЅРёСЏ: %d СЃРµРє",
                            cooldownSeconds)),
                    true
            );
        }
    }

    public static void startQTE(int generatorNumber, boolean hasQuickReflexes) {
        org.example.maniacrevolution.client.QTEClientHandler.setQuickReflexes(hasQuickReflexes);
        org.example.maniacrevolution.client.QTEClientHandler.startQTE(generatorNumber);
    }

    public static void stopQTE() {
        org.example.maniacrevolution.client.QTEClientHandler.stopQTE();
    }

    public static void setQTEQuickReflexes(boolean hasQuickReflexes) {
        org.example.maniacrevolution.client.QTEClientHandler.setQuickReflexes(hasQuickReflexes);
    }

    public static void setWallhackHighlightedPlayers(Set<UUID> highlightedPlayers, int durationTicks) {
        org.example.maniacrevolution.client.renderer.WallhackRenderer.setHighlightedPlayers(
                highlightedPlayers,
                durationTicks
        );
    }

    public static void closePerkScreenIfOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof org.example.maniacrevolution.gui.PerkSelectionScreen) {
            mc.setScreen(null);
        }
    }
}
