package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.perk.*;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Идеалыч (Пассивный) (Выжившие)
 * Идеальное QTE (critical) → +1% к скорости взлома, макс +5%.
 * Обычное попадание → сбрасывает дебаф если есть, иначе ничего.
 * Промах → -1% к скорости взлома, макс -10%.
 * Эффект сбрасывается когда взлом заканчивается.
 */
public class IdealychPerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    public static final float BONUS_PER_STACK  = 0.01f; // 1% за стак
    public static final int   MAX_BONUS_STACKS = 5;     // макс +5%
    public static final int   MAX_DEBUFF_STACKS = 10;   // макс -10%

    // uuid -> текущее количество стаков (положительные = бонус, отрицательные = дебаф)
    private static final Map<UUID, Integer> stacks = new ConcurrentHashMap<>();

    // uuid -> перк активен
    private static final Set<UUID> activePlayers =
            Collections.synchronizedSet(new HashSet<>());

    public IdealychPerk() {
        super(new Builder("idealych")
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Идеальное QTE: ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("+1% к скорости взлома")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(", стакается до ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("+" + MAX_BONUS_STACKS + "%.")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" Обычное попадание: сбрасывает эффект.")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" Промах: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("-1%")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(", до ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("-" + MAX_DEBUFF_STACKS + "%.")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" Сбрасывается при конце взлома.")
                        .withStyle(ChatFormatting.GRAY));
    }

    // ── Пассивный эффект ──────────────────────────────────────────────────

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        activePlayers.add(player.getUUID());
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        activePlayers.remove(player.getUUID());
        stacks.remove(player.getUUID());
    }

    // ── Обработка QTE результатов (вызывается из QTEKeyPressPacket) ───────

    /**
     * Вызывается при идеальном попадании (critical=true).
     * +1 стак бонуса, если был дебаф — сбрасывает его.
     */
    public static void onCriticalHit(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) return;

        int current = stacks.getOrDefault(player.getUUID(), 0);

        if (current < 0) {
            // Был дебаф — сбрасываем
            stacks.put(player.getUUID(), 0);
            player.displayClientMessage(
                    Component.literal("Идеалыч: дебаф сброшен!")
                            .withStyle(ChatFormatting.YELLOW), true);
        } else {
            // Добавляем стак бонуса
            int newStacks = Math.min(current + 1, MAX_BONUS_STACKS);
            stacks.put(player.getUUID(), newStacks);
            player.displayClientMessage(
                    Component.literal("Идеалыч: +" + newStacks + "% к скорости! ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal("(" + newStacks + "/" + MAX_BONUS_STACKS + ")")
                                    .withStyle(ChatFormatting.GRAY)), true);
        }
    }

    /**
     * Вызывается при обычном попадании (success=true, critical=false).
     * Сбрасывает дебаф если есть, иначе ничего.
     */
    public static void onNormalHit(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) return;

        int current = stacks.getOrDefault(player.getUUID(), 0);
        if (current < 0) {
            stacks.put(player.getUUID(), 0);
            player.displayClientMessage(
                    Component.literal("Идеалыч: дебаф сброшен!")
                            .withStyle(ChatFormatting.YELLOW), true);
        }
        // Бонусные стаки при обычном попадании НЕ добавляются и НЕ сбрасываются
    }

    /**
     * Вызывается при промахе (success=false).
     * -1 стак, если был бонус — сбрасывает его полностью перед дебафом.
     */
    public static void onMiss(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) return;

        int current = stacks.getOrDefault(player.getUUID(), 0);

        if (current > 0) {
            // Был бонус — полностью сбрасываем
            stacks.put(player.getUUID(), 0);
            player.displayClientMessage(
                    Component.literal("Идеалыч: бонус сброшен!")
                            .withStyle(ChatFormatting.RED), true);
        } else {
            // Добавляем дебаф стак
            int newStacks = Math.max(current - 1, -MAX_DEBUFF_STACKS);
            stacks.put(player.getUUID(), newStacks);
            player.displayClientMessage(
                    Component.literal("Идеалыч: " + newStacks + "% к скорости ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal("(" + Math.abs(newStacks) + "/" + MAX_DEBUFF_STACKS + ")")
                                    .withStyle(ChatFormatting.GRAY)), true);
        }
    }

    /**
     * Сбрасывает стаки когда взлом завершился/прерван.
     * Вызывается из HackSession.
     */
    public static void resetStacks(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) return;
        stacks.remove(player.getUUID());
    }

    // ── Геттер множителя для HackSession ─────────────────────────────────

    /**
     * Возвращает множитель скорости взлома для игрока.
     * 1.05 = +5%, 0.90 = -10%, 1.0 = нет эффекта.
     */
    public static float getHackMultiplier(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) return 1.0f;
        int s = stacks.getOrDefault(player.getUUID(), 0);
        return 1.0f + s * BONUS_PER_STACK;
    }

    public static boolean hasThisPerk(ServerPlayer player) {
        return activePlayers.contains(player.getUUID());
    }
}
