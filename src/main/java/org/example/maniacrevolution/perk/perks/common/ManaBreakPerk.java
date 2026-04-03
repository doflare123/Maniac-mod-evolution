package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.perk.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mana Break (Пассивный с КД) (Все)
 * При ударе по противнику из другой команды (если перк не в КД) — снимает ману.
 */
@Mod.EventBusSubscriber
public class ManaBreakPerk extends Perk {

    private static final float MANA_DRAIN   = 10f;
    private static final int   COOLDOWN_SEC = 90;

    // Жертва текущего удара — передаётся из события в onTrigger через тик
    private static final Map<UUID, ServerPlayer> pendingVictim = new HashMap<>();

    public ManaBreakPerk() {
        super(new Builder("mana_break")
                .type(PerkType.PASSIVE_COOLDOWN)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.ANY)
                .cooldown(COOLDOWN_SEC)
        );
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev.mana_break.desc",
                (int) MANA_DRAIN,
                COOLDOWN_SEC
        );
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        return pendingVictim.containsKey(player.getUUID());
    }

    @Override
    public void onTrigger(ServerPlayer player) {
        ServerPlayer victim = pendingVictim.remove(player.getUUID());
        if (victim == null || !victim.isAlive()) return;

        victim.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            float before = mana.getMana();
            mana.consumeMana(MANA_DRAIN);
            float drained = before - mana.getMana();

            victim.displayClientMessage(
                    Component.literal("Враг истощил твою ману на " + (int) drained + " ед.!")
                            .withStyle(ChatFormatting.DARK_AQUA), true);

            player.displayClientMessage(
                    Component.literal("Mana Break: истощено " + (int) drained + " маны!")
                            .withStyle(ChatFormatting.AQUA), true);
        });
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        pendingVictim.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Разные команды
        if (attacker.getTeam() == null || victim.getTeam() == null) return;
        if (attacker.getTeam().equals(victim.getTeam())) return;

        // Проверяем есть ли у атакующего этот перк
        PlayerData data = PlayerDataManager.get(attacker);
        boolean hasPerk = data.getSelectedPerks().stream()
                .anyMatch(inst -> inst.getPerk().getId().equals("mana_break"));
        if (!hasPerk) return;

        pendingVictim.put(attacker.getUUID(), victim);
    }
}