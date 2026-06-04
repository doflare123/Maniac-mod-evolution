package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;

import java.util.function.Supplier;

/**
 * Пакет для синхронизации состояния генератора с клиентом
 */
public class SyncGeneratorPacket {

    private final BlockPos pos;
    private final int charge;
    private final boolean powered;

    public SyncGeneratorPacket(BlockPos pos, int charge, boolean powered) {
        this.pos = pos;
        this.charge = charge;
        this.powered = powered;
    }

    public SyncGeneratorPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.charge = buf.readInt();
        this.powered = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(charge);
        buf.writeBoolean(powered);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обработка на клиенте
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof FNAFGeneratorBlockEntity generator) {
                    // Используем методы БЕЗ синхронизации, чтобы не создать цикл пакетов
                    generator.setCharge(charge, false);
                    generator.setPowered(powered, false);
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}