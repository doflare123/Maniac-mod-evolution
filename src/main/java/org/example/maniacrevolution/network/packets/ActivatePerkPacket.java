package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.ghost.GhostPossessionManager;

import java.util.function.Supplier;

public class ActivatePerkPacket {
    public ActivatePerkPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static ActivatePerkPacket decode(FriendlyByteBuf buf) {
        return new ActivatePerkPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (GhostPossessionManager.isPossessed(player)) {
                player.displayClientMessage(Component.translatable("message.maniacrev.perk.possessed"), true);
                return;
            }

            PlayerData data = PlayerDataManager.get(player);
            PerkInstance active = data.getActivePerk();

            if (active == null) {
                player.displayClientMessage(Component.translatable("message.maniacrev.perk.none_selected"), true);
                return;
            }

            PerkPhase phase = GameManager.getCurrentPhase();
            if (phase == null) {
                player.displayClientMessage(Component.translatable("message.maniacrev.game.not_started"), true);
                return;
            }

            // Проверка эффекта Тишины — для всех игроков
            if (player.hasEffect(org.example.maniacrevolution.effect.ModEffects.SILENCE.get())) {
                int remaining = player.getEffect(org.example.maniacrevolution.effect.ModEffects.SILENCE.get()).getDuration() / 20;
                player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.silenced", remaining), true);
                PlayerDataManager.syncToClient(player);
                return;
            }

            PerkInstance.ActivationResult result = active.tryActivate(player, phase);

            switch (result) {
                case SUCCESS -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.activated", active.getPerk().getName()), true);
                case ON_COOLDOWN -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.cooldown", active.getCooldownRemainingSeconds()), true);
                case WRONG_PHASE -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.wrong_phase"), true);
                case WRONG_GAMEMODE -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.wrong_gamemode"), true);
                case NOT_ACTIVE_PERK -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.not_active"), true);
                case NOT_ENOUGH_MANA -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.not_enough_mana"), true);
                case CONDITION_NOT_MET -> player.displayClientMessage(
                        Component.translatable("message.maniacrev.perk.condition_not_met"), true);
            }

            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
