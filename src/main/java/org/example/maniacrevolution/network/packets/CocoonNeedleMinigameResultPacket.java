package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.block.NightmareCocoonBlock;
import org.example.maniacrevolution.block.entity.NightmareCocoonBlockEntity;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.nightmare.NightmareManager;

import java.util.function.Supplier;

public class CocoonNeedleMinigameResultPacket {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final BlockPos pos;
    private final float accuracy;

    public CocoonNeedleMinigameResultPacket(BlockPos pos, float accuracy) {
        this.pos = pos;
        this.accuracy = accuracy;
    }

    public static void encode(CocoonNeedleMinigameResultPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeFloat(packet.accuracy);
    }

    public static CocoonNeedleMinigameResultPacket decode(FriendlyByteBuf buf) {
        return new CocoonNeedleMinigameResultPacket(buf.readBlockPos(), buf.readFloat());
    }

    public static void handle(CocoonNeedleMinigameResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            float accuracy = Mth.clamp(packet.accuracy, 0.0F, 1.0F);
            if (accuracy < NightmareConfig.COCOON_NEEDLE_MINIGAME_REQUIRED_ACCURACY) return;
            if (!player.getMainHandItem().is(ModItems.AWAKENING_NEEDLE.get())) return;
            if (player.distanceToSqr(packet.pos.getX() + 0.5D, packet.pos.getY() + 0.5D,
                    packet.pos.getZ() + 0.5D) > MAX_DISTANCE_SQR) return;

            Level level = player.level();
            BlockState state = level.getBlockState(packet.pos);
            if (!(state.getBlock() instanceof NightmareCocoonBlock)) return;
            if (!(level.getBlockEntity(packet.pos) instanceof NightmareCocoonBlockEntity cocoon)) return;

            int hits = cocoon.damage(NightmareConfig.COCOON_NEEDLE_MINIGAME_DAMAGE);
            if (hits >= NightmareConfig.COCOON_REQUIRED_HITS) {
                boolean rescued = NightmareManager.getInstance().onCocoonHit(player, packet.pos);
                if (!rescued) {
                    level.destroyBlock(packet.pos, false);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
