package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.effect.client.FearClientHandler;

import java.util.function.Supplier;

/**
 * Пакет для синхронизации направления страха с клиентом.
 * Отправляется с сервера на клиент при наложении эффекта страха.
 */
public class FearDirectionPacket {
    private final double x;
    private final double z;

    public FearDirectionPacket(Vec3 direction) {
        this.x = direction.x;
        this.z = direction.z;
    }

    // Декодер (читает из буфера)
    public FearDirectionPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.z = buf.readDouble();
    }

    // Энкодер (пишет в буфер)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(z);
    }

    // Статический метод для encode
    public static void encode(FearDirectionPacket packet, FriendlyByteBuf buf) {
        packet.toBytes(buf);
    }

    // Статический метод для decode
    public static FearDirectionPacket decode(FriendlyByteBuf buf) {
        return new FearDirectionPacket(buf);
    }

    // Обработчик (выполняется на клиенте)
    public static void handle(FearDirectionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Выполняется на клиенте
            Vec3 direction = new Vec3(packet.x, 0, packet.z);
            FearClientHandler.setFearDirection(direction);
        });
        ctx.get().setPacketHandled(true);
    }
}