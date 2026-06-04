package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.client.screen.CharacterSelectionScreen;

import java.util.function.Supplier;

/**
 * Пакет для открытия меню выбора персонажа (Server -> Client)
 */
public class OpenCharacterMenuPacket {
    private final CharacterType type;

    public OpenCharacterMenuPacket(CharacterType type) {
        this.type = type;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(type);
    }

    public static OpenCharacterMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenCharacterMenuPacket(buf.readEnum(CharacterType.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new CharacterSelectionScreen(type));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}