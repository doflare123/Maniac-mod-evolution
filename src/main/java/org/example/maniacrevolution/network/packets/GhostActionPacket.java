package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.ghost.GhostPossessionManager;

import java.util.function.Supplier;

public class GhostActionPacket {
    private final ActionType actionType;
    private final InteractionHand hand;
    private final int entityId;
    private final BlockPos blockPos;
    private final Direction direction;
    private final Vec3 hitVec;

    public GhostActionPacket(ActionType actionType, InteractionHand hand, int entityId, BlockPos blockPos, Direction direction, Vec3 hitVec) {
        this.actionType = actionType;
        this.hand = hand;
        this.entityId = entityId;
        this.blockPos = blockPos;
        this.direction = direction;
        this.hitVec = hitVec;
    }

    public static GhostActionPacket attackEntity(int entityId) {
        return new GhostActionPacket(ActionType.ATTACK_ENTITY, InteractionHand.MAIN_HAND, entityId, BlockPos.ZERO, Direction.UP, Vec3.ZERO);
    }

    public static GhostActionPacket useEntity(InteractionHand hand, int entityId, Vec3 relativeHitVec) {
        return new GhostActionPacket(ActionType.USE_ENTITY, hand, entityId, BlockPos.ZERO, Direction.UP, relativeHitVec);
    }

    public static GhostActionPacket useBlock(InteractionHand hand, BlockPos blockPos, Direction direction, Vec3 hitVec) {
        return new GhostActionPacket(ActionType.USE_BLOCK, hand, -1, blockPos, direction, hitVec);
    }

    public static GhostActionPacket useItem(InteractionHand hand) {
        return new GhostActionPacket(ActionType.USE_ITEM, hand, -1, BlockPos.ZERO, Direction.UP, Vec3.ZERO);
    }

    public static void encode(GhostActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.actionType);
        buf.writeEnum(packet.hand);
        buf.writeInt(packet.entityId);
        buf.writeBlockPos(packet.blockPos);
        buf.writeEnum(packet.direction);
        buf.writeDouble(packet.hitVec.x);
        buf.writeDouble(packet.hitVec.y);
        buf.writeDouble(packet.hitVec.z);
    }

    public static GhostActionPacket decode(FriendlyByteBuf buf) {
        ActionType actionType = buf.readEnum(ActionType.class);
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        int entityId = buf.readInt();
        BlockPos blockPos = buf.readBlockPos();
        Direction direction = buf.readEnum(Direction.class);
        Vec3 hitVec = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new GhostActionPacket(actionType, hand, entityId, blockPos, direction, hitVec);
    }

    public static void handle(GhostActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer possessor = ctx.get().getSender();
            if (possessor == null) {
                return;
            }

            switch (packet.actionType) {
                case ATTACK_ENTITY -> GhostPossessionManager.performAttackAsPossessed(possessor, packet.entityId);
                case USE_ENTITY -> GhostPossessionManager.performEntityUseAsPossessed(possessor, packet.hand, packet.entityId, packet.hitVec);
                case USE_BLOCK -> GhostPossessionManager.performBlockUseAsPossessed(
                        possessor,
                        packet.hand,
                        new BlockHitResult(packet.hitVec, packet.direction, packet.blockPos, false)
                );
                case USE_ITEM -> GhostPossessionManager.performItemUseAsPossessed(possessor, packet.hand);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public enum ActionType {
        ATTACK_ENTITY,
        USE_ENTITY,
        USE_BLOCK,
        USE_ITEM
    }
}
