package org.example.maniacrevolution.perk;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

public abstract class Perk {
    private final String id;
    private final String nameKey;
    private final String descriptionKey;
    private final PerkType type;
    private final PerkTeam team;
    private final Set<PerkPhase> activePhases;
    private final int cooldownTicks;
    private final ResourceLocation icon;

    protected Perk(Builder builder) {
        this.id = builder.id;
        this.nameKey = "perk.maniacrev." + id + ".name";
        this.descriptionKey = "perk.maniacrev." + id + ".desc";
        this.type = builder.type;
        this.team = builder.team;
        this.activePhases = builder.activePhases;
        this.cooldownTicks = builder.cooldownTicks;
        this.icon = builder.icon != null ? builder.icon :
                new ResourceLocation("maniacrev", "textures/perks/" + id + ".png");
    }

    // === Основные методы ===

    /** Вызывается при активации перка кнопкой (для ACTIVE и HYBRID) */
    public void onActivate(ServerPlayer player) {}

    /** Вызывается при применении пассивного эффекта (для PASSIVE и HYBRID) */
    public void applyPassiveEffect(ServerPlayer player) {}

    /** Вызывается при снятии пассивного эффекта */
    public void removePassiveEffect(ServerPlayer player) {}

    /** Вызывается каждый тик пока перк активен */
    public void onTick(ServerPlayer player) {}

    /**
     * Вызывается каждый тик для проверки условия срабатывания.
     * Возвращает true если перк должен сработать и уйти в КД.
     * Используется для PASSIVE_COOLDOWN перков.
     */
    public boolean shouldTrigger(ServerPlayer player) {
        return false;
    }

    /**
     * Вызывается при срабатывании PASSIVE_COOLDOWN перка.
     * Здесь выполняется основной эффект перка.
     */
    public void onTrigger(ServerPlayer player) {}

    /** Вызывается при старте игры (для перков с фазой START) */
    public void onGameStart(ServerPlayer player) {}

    /** Вызывается при смене фазы игры */
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {}

    /** Проверка доступности перка в текущей фазе */
    public boolean isActiveInPhase(PerkPhase phase) {
        return activePhases.contains(PerkPhase.ANY) || activePhases.contains(phase);
    }

    /** Проверка доступности перка для команды игрока */
    public boolean isAvailableForTeam(PerkTeam playerTeam) {
        return this.team == PerkTeam.ALL || this.team == playerTeam;
    }

    /** Проверка, может ли игрок использовать активную способность */
    public boolean canActivate(ServerPlayer player, PerkPhase currentPhase) {
        if (type == PerkType.PASSIVE || type == PerkType.PASSIVE_COOLDOWN) return false;
        if (!isActiveInPhase(currentPhase)) return false;
        if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.ADVENTURE) {
            return false;
        }
        return true;
    }

    // === Геттеры ===

    public String getId() { return id; }
    public PerkType getType() { return type; }
    public PerkTeam getTeam() { return team; }
    public Set<PerkPhase> getActivePhases() { return activePhases; }
    public int getCooldownTicks() { return cooldownTicks; }
    public ResourceLocation getIcon() { return icon; }

    public Component getName() {
        return Component.translatable(nameKey);
    }

    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }

    public List<Component> getTooltip() {
        return List.of(
                getName().copy().withStyle(net.minecraft.ChatFormatting.GOLD),
                Component.literal("Тип: ").withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(type.getDisplayName()),
                Component.literal("Команда: ").withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(team.getDisplayName()),
                Component.literal("Фазы: ").withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(getPhasesString()),
                cooldownTicks > 0 ?
                        Component.literal("КД: " + (cooldownTicks / 20) + " сек")
                                .withStyle(net.minecraft.ChatFormatting.RED) :
                        Component.empty(),
                Component.empty(),
                getDescription().copy().withStyle(net.minecraft.ChatFormatting.WHITE)
        );
    }

    private Component getPhasesString() {
        if (activePhases.contains(PerkPhase.ANY)) {
            return Component.literal("Любая").withStyle(net.minecraft.ChatFormatting.GREEN);
        }
        StringBuilder sb = new StringBuilder();
        for (PerkPhase phase : activePhases) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(phase.getDisplayName().getString());
        }
        return Component.literal(sb.toString()).withStyle(net.minecraft.ChatFormatting.YELLOW);
    }

    // === Builder ===

    public static class Builder {
        private final String id;
        private PerkType type = PerkType.PASSIVE;
        private PerkTeam team = PerkTeam.ALL;
        private Set<PerkPhase> activePhases = Set.of(PerkPhase.ANY);
        private int cooldownTicks = 0;
        private ResourceLocation icon;

        public Builder(String id) {
            this.id = id;
        }

        public Builder type(PerkType type) {
            this.type = type;
            return this;
        }

        public Builder team(PerkTeam team) {
            this.team = team;
            return this;
        }

        public Builder phases(PerkPhase... phases) {
            this.activePhases = Set.of(phases);
            return this;
        }

        public Builder cooldown(int seconds) {
            this.cooldownTicks = seconds * 20;
            return this;
        }

        public Builder icon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }
    }
}