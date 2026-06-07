package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.screen.CocoonNeedleMinigameScreen;

import java.util.function.Supplier;

public class OpenCocoonNeedleMinigamePacket {
    private final BlockPos pos;

    public OpenCocoonNeedleMinigamePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenCocoonNeedleMinigamePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static OpenCocoonNeedleMinigamePacket decode(FriendlyByteBuf buf) {
        return new OpenCocoonNeedleMinigamePacket(buf.readBlockPos());
    }

    public static void handle(OpenCocoonNeedleMinigamePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                Minecraft.getInstance().setScreen(new CocoonNeedleMinigameScreen(packet.pos)));
        ctx.get().setPacketHandled(true);
    }
}
