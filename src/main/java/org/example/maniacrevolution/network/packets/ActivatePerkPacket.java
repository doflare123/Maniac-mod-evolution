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

            PlayerData data = PlayerDataManager.get(player);
            PerkInstance active = data.getActivePerk();

            if (active == null) {
                player.displayClientMessage(Component.literal("§cУ вас нет выбранных перков!"), true);
                return;
            }

            PerkPhase phase = GameManager.getCurrentPhase();
            if (phase == null) {
                player.displayClientMessage(Component.literal("§cИгра не запущена!"), true);
                return;
            }

            // Проверка эффекта Тишины — для всех игроков
            if (player.hasEffect(org.example.maniacrevolution.effect.ModEffects.SILENCE.get())) {
                int remaining = player.getEffect(org.example.maniacrevolution.effect.ModEffects.SILENCE.get()).getDuration() / 20;
                player.displayClientMessage(
                        Component.literal("§c🔇 Тишина! Перки заблокированы ещё " + remaining + " сек."), true);
                PlayerDataManager.syncToClient(player);
                return;
            }

            PerkInstance.ActivationResult result = active.tryActivate(player, phase);

            switch (result) {
                case SUCCESS -> player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§a")
                                .append(active.getPerk().getName())
                                .append(" активирован!"), true);
                case ON_COOLDOWN -> player.displayClientMessage(
                        Component.literal("§cПерк на перезарядке! Осталось: " + active.getCooldownRemainingSeconds() + " сек"), true);
                case WRONG_PHASE -> player.displayClientMessage(
                        Component.literal("§cПерк недоступен в этой фазе игры!"), true);
                case WRONG_GAMEMODE -> player.displayClientMessage(
                        Component.literal("§cПерки работают только в режиме Приключения!"), true);
                case NOT_ACTIVE_PERK -> player.displayClientMessage(
                        Component.literal("§eВыберите активный перк!"), true);
                case NOT_ENOUGH_MANA -> player.displayClientMessage(
                        Component.literal("§b\uD83D\uDE30 Недостаточно маны для активации перка!"), true);
                case CONDITION_NOT_MET -> player.displayClientMessage(
                        Component.literal("§cУсловие не выполнено!"), true);
            }

            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
