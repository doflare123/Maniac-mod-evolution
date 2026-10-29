package org.example.maniacrevolution.engineer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

public record EngineerStatePacket(long tick, boolean active, int scrap, int earned, int barricades, float armor,
        int selected, int work, BlockPos workPos, long started, long buildReady, List<Panel> panels) {
    public record Panel(ResourceLocation dimension, BlockPos pos, int uses, long playingUntil, long readyAt, boolean loaded) {}
    public void encode(FriendlyByteBuf b) {
        b.writeLong(tick); b.writeBoolean(active); b.writeVarInt(scrap); b.writeVarInt(earned); b.writeVarInt(barricades); b.writeFloat(armor);
        b.writeInt(selected); b.writeInt(work); b.writeBlockPos(workPos); b.writeLong(started); b.writeLong(buildReady);
        b.writeVarInt(panels.size());
        for (var p : panels) { b.writeResourceLocation(p.dimension); b.writeBlockPos(p.pos); b.writeVarInt(p.uses); b.writeLong(p.playingUntil); b.writeLong(p.readyAt); b.writeBoolean(p.loaded); }
    }
    public static EngineerStatePacket decode(FriendlyByteBuf b) {
        long tick = b.readLong(); boolean active = b.readBoolean(); int scrap = b.readVarInt(), earned = b.readVarInt(), barriers = b.readVarInt(); float armor = EngineerRules.clampArmor(b.readFloat());
        int selected = b.readInt(), work = b.readInt(); BlockPos pos = b.readBlockPos(); long started = b.readLong(), build = b.readLong();
        int size = b.readVarInt(); if (size < 0 || size > EngineerRules.MAX_PANELS) throw new IllegalArgumentException("Invalid engineer panel count");
        var panels = new java.util.ArrayList<Panel>();
        for (int i = 0; i < size; i++) panels.add(new Panel(b.readResourceLocation(), b.readBlockPos(), b.readVarInt(), b.readLong(), b.readLong(), b.readBoolean()));
        return new EngineerStatePacket(tick, active, scrap, earned, barriers, armor, selected, work, pos, started, build, List.copyOf(panels));
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.example.maniacrevolution.engineer.client.EngineerClient.accept(this)));
        ctx.get().setPacketHandled(true);
    }
}
