package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncSettingsPacket;
import org.example.maniacrevolution.settings.GameSettings;

import java.util.function.Supplier;

public class GiveSettingsToAllPacket {

    public GiveSettingsToAllPacket() {
    }

    public static void encode(GiveSettingsToAllPacket msg, FriendlyByteBuf buf) {
        // Пустой пакет
    }

    public static GiveSettingsToAllPacket decode(FriendlyByteBuf buf) {
        return new GiveSettingsToAllPacket();
    }

    public static void handle(GiveSettingsToAllPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.hasPermissions(2)) return;

            GameSettings settings = GameSettings.get(sender.server);

            for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                // Выдаём предмет
                ItemStack item = new ItemStack(ModItems.SETTINGS_ITEM.get());
                item.setHoverName(Component.literal("§6§lНастройки"));
                if (!player.getInventory().add(item)) {
                    player.drop(item, false);
                }

                // Синхронизируем настройки каждому клиенту
                ModNetworking.sendToPlayer(new SyncSettingsPacket(
                        settings.getComputerCount(),
                        settings.getHackPoints(),
                        settings.getHpBoost(),
                        settings.getManiacCount(),
                        settings.getGameTime(),
                        settings.getSelectedMap()
                ), player);
            }

            sender.sendSystemMessage(Component.literal("§aПредмет настроек выдан всем игрокам!"));
        });
        ctx.get().setPacketHandled(true);
    }
}