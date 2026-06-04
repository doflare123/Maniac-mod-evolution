package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.PerkSelectionScreen;
import org.example.maniacrevolution.gui.ShopScreen;

import java.util.function.Supplier;

public class OpenGuiPacket {
    private final GuiType guiType;

    public OpenGuiPacket(GuiType type) {
        this.guiType = type;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(guiType);
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket(buf.readEnum(GuiType.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            switch (guiType) {
                case PERK_SELECTION -> mc.setScreen(new PerkSelectionScreen());
                case SHOP -> mc.setScreen(new ShopScreen());
                case GUIDE -> mc.setScreen(new GuideScreen());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public enum GuiType {
        PERK_SELECTION,
        SHOP,
        GUIDE
    }
}
