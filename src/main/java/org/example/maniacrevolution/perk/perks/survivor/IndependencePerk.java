package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedEventHandler;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.perk.*;

/**
 * Независимость (Активный) (Выжившие) — одноразовый перк.
 * Когда до конца нокдауна остаётся ≤ TRIGGER_SECONDS секунд,
 * можно активировать перк и встать с REVIVE_HP хп.
 */
public class IndependencePerk extends Perk {

    // ── Настройки ─────────────────────────────────────────────────────────
    private static final int   TRIGGER_SECONDS = 10;   // доступен в последние N секунд
    private static final float REVIVE_HP        = 5f;  // хп после самоподъёма
    private static final int   COOLDOWN_SEC     = 9999; // одноразовый
    private static final float MANA_COST        = 10f;

    public IndependencePerk() {
        super(new Builder("independence")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
                .manaCost(MANA_COST)
        );
    }

    // ── Описание ──────────────────────────────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Когда до конца нокдауна остаётся ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(TRIGGER_SECONDS + " сек.")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" — активируй перк чтобы встать с ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) REVIVE_HP + " HP")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(". Одноразовый. Стоимость: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal((int) MANA_COST + " маны.")
                        .withStyle(ChatFormatting.AQUA));
    }

    // ── Активация ─────────────────────────────────────────────────────────

    @Override
    public boolean meetsActivationCondition(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);
        if (data == null || data.getState() != DownedState.DOWNED) return false;
        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();
        return remaining <= TRIGGER_SECONDS * 20;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        DownedData data = DownedCapability.get(player);

        // Игрок должен лежать
        if (data == null || data.getState() != DownedState.DOWNED) {
            player.displayClientMessage(
                    Component.literal("Перк работает только в нокдауне!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Проверяем что осталось ≤ TRIGGER_SECONDS секунд
        int remaining = DownedData.DOWNED_TIMEOUT_TICKS - data.getDownedTicksElapsed();
        int triggerTicks = TRIGGER_SECONDS * 20;

        if (remaining > triggerTicks) {
            int remainingSec = remaining / 20;
            int availableAt = remainingSec - TRIGGER_SECONDS;
            player.displayClientMessage(
                    Component.literal("Ещё рано! Доступно через ~" + availableAt + " сек.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Всё ок — поднимаем игрока
        selfRevive(player, data);
    }

    // ── Логика самоподъёма ────────────────────────────────────────────────

    private void selfRevive(ServerPlayer player, DownedData data) {
        // Сбрасываем состояние нокдауна
        data.cancelRevive();
        data.setState(DownedState.WEAKENED);
        data.setDownedTicksElapsed(0);

        // Убираем эффекты нокдауна и восстанавливаем позу
        DownedEventHandler.clearHudForNearby(player);

        // Урезаем макс HP вдвое (как при обычном подъёме)
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            data.setOriginalMaxHp(maxHp.getBaseValue());
            double newMax = Math.max(2.0, maxHp.getBaseValue() / 2.0);
            maxHp.setBaseValue(newMax);
        }

        // Ставим 5 хп
        player.setHealth(REVIVE_HP);

        player.displayClientMessage(
                Component.literal("Вы встали самостоятельно! ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("HP: " + (int) REVIVE_HP)
                                .withStyle(ChatFormatting.RED)),
                false
        );

        // Оповещаем команду
        if (player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§e⚡ " + player.getName().getString()
                            + " §eвстал самостоятельно!"),
                    false
            );
        }
    }
}
