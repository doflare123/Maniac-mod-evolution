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

            PerkInstance.ActivationResult result = active.tryActivate(player, phase);

            switch (result) {
                case SUCCESS -> player.displayClientMessage(
                        Component.literal("§a" + active.getPerk().getName().getString() + " активирован!"), true);
                case ON_COOLDOWN -> player.displayClientMessage(
                        Component.literal("§cПерк на перезарядке! Осталось: " + active.getCooldownRemainingSeconds() + " сек"), true);
                case WRONG_PHASE -> player.displayClientMessage(
                        Component.literal("§cПерк недоступен в этой фазе игры!"), true);
                case WRONG_GAMEMODE -> player.displayClientMessage(
                        Component.literal("§cПерки работают только в режиме Приключения!"), true);
                case NOT_ACTIVE_PERK -> player.displayClientMessage(
                        Component.literal("§eВыберите активный перк!"), true);
            }

            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
