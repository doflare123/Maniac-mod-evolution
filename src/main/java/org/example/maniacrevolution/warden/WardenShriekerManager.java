package org.example.maniacrevolution.warden;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.*;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenShriekerManager {
    private record Active(ServerLevel level, BlockPos pos) {}
    private static final Set<Active> ACTIVE = new HashSet<>();
    private WardenShriekerManager() {}
    private static boolean gameMode(ServerPlayer player) {
        var mode = player.gameMode.getGameModeForPlayer(); return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }
    public static void step(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer source) {
        if (source == null || source.serverLevel() != level || state.getValue(WardenShriekerBlock.SHRIEKING)
                || !(level.getBlockEntity(pos) instanceof WardenShriekerBlockEntity entity) || ACTIVE.size() >= 1024) return;
        String team = source.getTeam() == null ? null : source.getTeam().getName();
        if (!WardenShriekerRules.trigger(WardenShriekerData.get(level.getServer()).enabled(), source.isAlive(),
                gameMode(source), GameManager.getPhaseValue(), team, entity.cooling(level.getGameTime()))) return;
        Vec3 origin = Vec3.atCenterOf(pos);
        var packet = new WardenShriekerEchoPacket(level.dimension().location(), origin, level.getGameTime(), WardenEchoPose.capture(source));
        if (!packet.valid()) return;
        entity.start(level.getGameTime());
        level.setBlock(pos, state.setValue(WardenShriekerBlock.SHRIEKING, true).setValue(WardenShriekerBlock.COOLING, true), 3);
        ACTIVE.add(new Active(level, pos.immutable()));
        sound(level, pos, true);
        for (var listener : level.getServer().getPlayerList().getPlayers()) {
            String listenerTeam = listener.getTeam() == null ? null : listener.getTeam().getName();
            if (listener.serverLevel() == level
                    && WardenMatchRules.active(listener.isAlive(), gameMode(listener), GameManager.getPhaseValue(),
                    PlayerDataManager.get(listener).getManiacClassId(), listenerTeam)) ModNetworking.sendToPlayer(packet, listener);
        }
    }
    private static void sound(ServerLevel level, BlockPos pos, boolean start) {
        var packet = new WardenShriekSoundPacket(level.dimension().location(), pos, start);
        for (var player : level.getServer().getPlayerList().getPlayers())
            if (player.serverLevel() == level && (!start || player.position().distanceToSqr(Vec3.atCenterOf(pos)) < 32 * 32))
                ModNetworking.sendToPlayer(packet, player);
    }
    public static void stop(ServerLevel level, BlockPos pos) {
        if (ACTIVE.remove(new Active(level, pos))) sound(level, pos, false);
    }
    private static void stopAll() {
        for (var active : Set.copyOf(ACTIVE)) {
            if (active.level.hasChunkAt(active.pos)) {
                var state = active.level.getBlockState(active.pos);
                if (state.getBlock() instanceof WardenShriekerBlock && state.getValue(WardenShriekerBlock.SHRIEKING))
                    active.level.setBlock(active.pos, state.setValue(WardenShriekerBlock.SHRIEKING, false), 3);
                if (active.level.getBlockEntity(active.pos) instanceof WardenShriekerBlockEntity entity) entity.stopShriek();
            }
            stop(active.level, active.pos);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3) { stopAll(); return; }
        for (var active : Set.copyOf(ACTIVE)) {
            if (!active.level.hasChunkAt(active.pos)) stop(active.level, active.pos);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maniacrev").then(Commands.literal("warden")
                .then(Commands.literal("shriekers").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("enable").executes(c -> toggle(c.getSource(), true)))
                        .then(Commands.literal("disable").executes(c -> toggle(c.getSource(), false)))
                        .then(Commands.literal("status").executes(c -> status(c.getSource()))))));
    }
    private static int toggle(CommandSourceStack source, boolean value) {
        WardenShriekerData.get(source.getServer()).setEnabled(value);
        if (!value) stopAll();
        return status(source);
    }
    private static int status(CommandSourceStack source) {
        boolean enabled = WardenShriekerData.get(source.getServer()).enabled();
        source.sendSuccess(() -> Component.literal("Крикуны Вардена: " + (enabled ? "включены" : "выключены")), false);
        return enabled ? 1 : 0;
    }
}
