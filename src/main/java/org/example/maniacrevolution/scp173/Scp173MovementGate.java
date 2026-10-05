package org.example.maniacrevolution.scp173;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Gate decoded vanilla movement before the vanilla listener. No reflection or mixin mappings. */
public final class Scp173MovementGate extends ChannelInboundHandlerAdapter {
    private static final String NAME = "maniacrev_scp173_movement";
    private static final ThreadLocal<JumpBaseline> PACKET_JUMP = new ThreadLocal<>();
    private final ServerGamePacketListenerImpl listener;
    private Scp173MovementGate(ServerGamePacketListenerImpl listener) { this.listener = listener; }

    public static void install(ServerGamePacketListenerImpl listener) {
        var channel = listener.connection.channel();
        channel.eventLoop().execute(() -> {
            if (channel.isActive() && channel.pipeline().get(NAME) == null)
                channel.pipeline().addBefore("packet_handler", NAME, new Scp173MovementGate(listener));
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        if (!(message instanceof ServerboundMovePlayerPacket || message instanceof ServerboundMoveVehiclePacket
                || message instanceof ServerboundPlayerInputPacket)) {
            super.channelRead(ctx, message);
            return;
        }
        // Match vanilla's main-thread enqueue. Never inspect players/worlds on Netty's thread.
        listener.player.getServer().execute(() -> {
            if (!listener.connection.isConnected()) return;
            // listener survives respawn; the player instance does not.
            Object accepted = filter(listener.player, message);
            if (accepted == null) return;
            // Call on the server thread directly: fireChannelRead here would bounce back to
            // Netty and enqueue again, letting later ability packets overtake this movement.
            // The vanilla listener still owns validation, accounting and teleport acknowledgements.
            if (accepted instanceof ServerboundMovePlayerPacket move) handleMove(listener, move);
            else if (accepted instanceof ServerboundPlayerInputPacket input) listener.handlePlayerInput(input);
            else if (accepted instanceof ServerboundMoveVehiclePacket vehicle) listener.handleMoveVehicle(vehicle);
        });
    }

    static void handleMove(ServerGamePacketListenerImpl listener, ServerboundMovePlayerPacket move) {
        var p = listener.player;
        boolean serverPhysics = Scp173Manager.held(p);
        boolean mayfly = p.getAbilities().mayfly;
        // Held packets report the current server position, not a client falling delta. Mark
        // this validation as server-owned so vanilla does not mistake it for illegal hovering.
        // This flag is never sent to the client and is restored before any subsequent packet.
        if (serverPhysics) p.getAbilities().mayfly = true;
        // Client positions already implement an ordinary free jump. Vanilla also deposits a
        // server-side sprint kick and upward jump velocity, which would later look like a perk
        // impulse when taking ownership of physics. Remove ONLY that deposit at the jump hook.
        boolean ordinaryJump = Scp173Manager.active(p) && p.onGround() && !move.isOnGround()
                && move.getY(p.getY()) > p.getY();
        if (ordinaryJump) PACKET_JUMP.set(new JumpBaseline(p, p.getDeltaMovement()));
        try { listener.handleMovePlayer(move); }
        finally {
            if (ordinaryJump) PACKET_JUMP.remove();
            if (serverPhysics) p.getAbilities().mayfly = mayfly;
        }
    }

    static void packetJump(net.minecraft.world.entity.LivingEntity entity) {
        var baseline = PACKET_JUMP.get();
        if (baseline != null && baseline.player == entity) entity.setDeltaMovement(baseline.velocity);
    }

    private record JumpBaseline(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.Vec3 velocity) {}

    static Object filter(net.minecraft.server.level.ServerPlayer p, Object message) {
        if (message instanceof ServerboundMovePlayerPacket move && (!Double.isFinite(move.getX(p.getX()))
                || !Double.isFinite(move.getY(p.getY())) || !Double.isFinite(move.getZ(p.getZ()))
                || !Float.isFinite(move.getYRot(p.getYRot())) || !Float.isFinite(move.getXRot(p.getXRot()))))
            return message; // Vanilla rejects invalid movement before a mod cooldown can issue a correction.
        boolean jumpBlocked = org.example.maniacrevolution.util.PlayerModeUtil.isSurvivalOrAdventure(p)
                && p.hasEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get());
        if ((Scp173Manager.active(p) || jumpBlocked) && !Scp173Manager.held(p) && message instanceof ServerboundMovePlayerPacket move
                && p.onGround() && move.getY(p.getY()) > p.getY() + 1.0e-6 && p.getDeltaMovement().y <= 0
                && !p.onClimbable() && !p.isInWaterOrBubble() && !p.isInLava()) {
            var shifted = p.getBoundingBox().move(move.getX(p.getX()) - p.getX(), move.getY(p.getY()) - p.getY(),
                    move.getZ(p.getZ()) - p.getZ());
            // Stepping onto a supported block is not a jump. A forged onGround flag must not
            // bypass the cooldown for rising into unsupported space; external upward velocity is exempt.
            boolean supported = !p.level().noCollision(p, shifted.move(0, -0.05, 0));
            if (!supported && (jumpBlocked || !Scp173GameplayManager.permitClientJump(p))) {
                p.connection.teleport(p.getX(), p.getY(), p.getZ(), move.getYRot(p.getYRot()), move.getXRot(p.getXRot()));
                return null;
            }
        }
        if (!Scp173Manager.held(p)) return message;
        if (message instanceof ServerboundMoveVehiclePacket) return null;
        if (message instanceof ServerboundMovePlayerPacket)
            return new ServerboundMovePlayerPacket.PosRot(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), p.onGround());
        return new ServerboundPlayerInputPacket(0, 0, false, false);
    }
}
