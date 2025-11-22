package org.example.maniacrevolution.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

public enum PerkTeam {
    MANIAC("Маньяк", ChatFormatting.DARK_RED, "maniac"),
    SURVIVOR("Выживший", ChatFormatting.GREEN, "survivors"),
    ALL("Все", ChatFormatting.WHITE, null);

    private final String displayName;
    private final ChatFormatting color;
    private final String teamName; // Название команды в scoreboard

    PerkTeam(String displayName, ChatFormatting color, String teamName) {
        this.displayName = displayName;
        this.color = color;
        this.teamName = teamName;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public ChatFormatting getColor() { return color; }
    public String getTeamName() { return teamName; }

    public static PerkTeam fromPlayer(ServerPlayer player) {
        PlayerTeam team = (PlayerTeam) player.getTeam();
        if (team == null) return null;

        String name = team.getName();
        if ("maniac".equals(name)) return MANIAC;
        if ("survivors".equals(name)) return SURVIVOR;
        return null;
    }
}