package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.character.CharacterType;

import java.util.function.Supplier;

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
        CharacterType capturedType = this.type;
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                        () -> org.example.maniacrevolution.client.ClientScreenHelper
                                .openCharacterSelectionScreen(capturedType))
        );
        ctx.get().setPacketHandled(true);
    }
}