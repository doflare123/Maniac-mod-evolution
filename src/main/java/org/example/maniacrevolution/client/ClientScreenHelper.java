package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.maniacrevolution.character.CharacterType;

import java.util.Map;

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

    public static void closePerkScreenIfOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof org.example.maniacrevolution.gui.PerkSelectionScreen) {
            mc.setScreen(null);
        }
    }
}