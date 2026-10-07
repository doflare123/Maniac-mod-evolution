package org.example.maniacrevolution.strange;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.example.maniacrevolution.network.ModNetworking;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.*;
import java.util.function.Supplier;

/** Small bounded block scene at the destination, including destinations in another dimension. */
public record PortalViewPacket(UUID portal, float yaw, List<Cell> cells) {
    public record Cell(BlockPos offset, int state) {}
    public static void broadcast(StrangeEffectEntity from, StrangeEffectEntity to) {
        ServerLevel destination=(ServerLevel)to.level(), source=(ServerLevel)from.level();
        var viewers=source.players().stream().filter(p->p.distanceToSqr(from)<48*48).toList();
        if(viewers.isEmpty())return;
        List<Cell> cells=new ArrayList<>();
        BlockPos center=to.blockPosition();
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-2;y<=6;y++) {
            BlockPos pos=center.offset(x,y,z);
            if(!destination.hasChunkAt(pos))continue;
            var state=destination.getBlockState(pos);
            if(!state.isAir())cells.add(new Cell(new BlockPos(x,y,z),Block.getId(state)));
        }
        var packet=new PortalViewPacket(from.getUUID(),to.getYRot(),cells);
        viewers.forEach(player->ModNetworking.sendToPlayer(packet,player));
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(portal);buffer.writeFloat(yaw);buffer.writeVarInt(cells.size());
        for(var cell:cells){buffer.writeBlockPos(cell.offset());buffer.writeVarInt(cell.state());}
    }
    public static PortalViewPacket decode(FriendlyByteBuf buffer) {
        UUID id=buffer.readUUID();float yaw=buffer.readFloat();int size=buffer.readVarInt();
        if(size<0||size>2601)throw new IllegalArgumentException("Portal scene too large");
        List<Cell> cells=new ArrayList<>(size);
        for(int i=0;i<size;i++)cells.add(new Cell(buffer.readBlockPos(),buffer.readVarInt()));
        return new PortalViewPacket(id,yaw,List.copyOf(cells));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->
                org.example.maniacrevolution.strange.client.PortalVisual.accept(this)));
        context.setPacketHandled(true);
    }
}
