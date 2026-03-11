package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.perk.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mana Break (Активный) (Все)
 * При активации — следующий удар снимет ману с цели.
 */
@Mod.EventBusSubscriber
public class ManaBreakPerk extends Perk {

    // ── Настройки ────────────────────────────────────────────────────────────
    private static final float MANA_DRAIN   = 10f;   // сколько маны снимает удар
    private static final int   COOLDOWN_SEC = 90;    // кулдаун в секундах

    // Игроки, у которых перк сейчас "заряжен" (ждут удара)
    private static final Set<UUID> charged = new HashSet<>();

    public ManaBreakPerk() {
        super(new Builder("mana_break")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    // ── Описание с подставленными значениями ─────────────────────────────────

    @Override
    public Component getDescription() {
        return Component.literal("Каждый твой удар снимает ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal((int) MANA_DRAIN + " ед. маны")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" у цели. Кулдаун: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(COOLDOWN_SEC + " сек.")
                        .withStyle(ChatFormatting.RED));
    }

    // ── Обработчик удара ─────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!charged.contains(attacker.getUUID())) return;

        // Снимаем заряд независимо от того, игрок ли жертва
        charged.remove(attacker.getUUID());

        // Снимаем ману только если жертва — игрок
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        victim.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            float before = mana.getMana();
            mana.consumeMana(MANA_DRAIN);
            float drained = before - mana.getMana();

            victim.displayClientMessage(
                    Component.literal("Враг истощил твою ману на " + (int) drained + " ед.!")
                            .withStyle(ChatFormatting.DARK_AQUA),
                    true
            );

            attacker.displayClientMessage(
                    Component.literal("Mana Break: истощено " + (int) drained + " маны!")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        });
    }

    // ── Сброс при снятии перка ───────────────────────────────────────────────

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        charged.remove(player.getUUID());
    }
}
