package org.example.maniacrevolution.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WardenNoisePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-only producers. Client volume settings do not decide gameplay visibility. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenNoiseManager {
    private static final WardenNoiseBatch BATCH = new WardenNoiseBatch();
    private static final WardenNoiseThrottle THROTTLE = new WardenNoiseThrottle();
    private static final WardenNoiseEchoThrottle ECHO_THROTTLE = new WardenNoiseEchoThrottle();
    private static final Map<UUID, Motion> MOVEMENT = new HashMap<>();
    private static final Map<UUID, Interaction> INTERACTIONS = new HashMap<>();
    private record Motion(ServerPlayer player, Object dimension, String team, int survivorClass, int maniacClass,
                          WardenMovementNoise tracker) {}
    private record Interaction(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState before,
                               AbstractContainerMenu menu, UUID session) {}

    private WardenNoiseManager() {}

    private static boolean participant(ServerPlayer player) {
        return participant(player, player.isAlive());
    }
    private static boolean participant(ServerPlayer player, boolean alive) {
        String team = player.getTeam() == null ? null : player.getTeam().getName();
        GameType mode = player.gameMode.getGameModeForPlayer();
        int phase = GameManager.getPhaseValue();
        return WardenMatchRules.participant(alive, mode == GameType.SURVIVAL || mode == GameType.ADVENTURE, phase, team);
    }
    private static boolean warden(ServerPlayer player) {
        return participant(player) && WardenMatchRules.active(true, true, GameManager.getPhaseValue(),
                PlayerDataManager.get(player).getManiacClassId(), player.getTeam().getName());
    }
    private static void emit(ServerLevel level, UUID source, Vec3 position, WardenNoise.Kind kind, float strength, double hearing) {
        if (GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3) return;
        var noise = new WardenNoise(level.dimension().location(), source, position, kind, strength, hearing, level.getGameTime());
        if (noise.valid() && THROTTLE.allow(source, kind, noise.tick())) BATCH.offer(noise);
    }
    private static void action(ServerPlayer player, Vec3 position, WardenNoise.Kind kind, float strength) {
        if (participant(player)) emit(player.serverLevel(), player.getUUID(), position, kind, strength, WardenMatchRules.VISION_RANGE);
    }
    public static void combatNoise(ServerPlayer player, Vec3 position, float strength, double hearing) {
        if (participant(player)) emit(player.serverLevel(), player.getUUID(), position, WardenNoise.Kind.SOUND, strength, hearing);
    }
    public static void waveNoise(ServerPlayer player, Vec3 position, float strength, double hearing) {
        if (participant(player)) emit(player.serverLevel(), player.getUUID(), position, WardenNoise.Kind.WAVE, strength, hearing);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var wardens = server.getPlayerList().getPlayers().stream().filter(WardenNoiseManager::warden).toList();
        if (wardens.isEmpty()) { clear(); return; }
        MOVEMENT.entrySet().removeIf(e -> {
            var player = server.getPlayerList().getPlayer(e.getKey());
            if (player != null && participant(player)) return false;
            THROTTLE.remove(e.getKey()); return true;
        });
        for (var player : server.getPlayerList().getPlayers()) {
            if (!participant(player)) { THROTTLE.remove(player.getUUID()); continue; }
            var data = PlayerDataManager.get(player);
            String team = player.getTeam().getName();
            var motion = MOVEMENT.get(player.getUUID());
            if (motion == null || motion.player != player || !motion.dimension.equals(player.level().dimension())
                    || !motion.team.equals(team) || motion.survivorClass != data.getSurvivorClassId()
                    || motion.maniacClass != data.getManiacClassId()) {
                THROTTLE.remove(player.getUUID());
                motion = new Motion(player, player.level().dimension(), team, data.getSurvivorClassId(),
                        data.getManiacClassId(), new WardenMovementNoise());
                MOVEMENT.put(player.getUUID(), motion);
            }
            var kind = motion.tracker.sample(player.position(), player.level().getGameTime(),
                    player.onGround() && !player.isInWaterOrBubble() && !player.onClimbable(),
                    player.isSprinting(), player.isShiftKeyDown());
            if (kind != null) action(player, player.position(), kind,
                    kind == WardenNoise.Kind.STEP ? 0.25F : kind == WardenNoise.Kind.SPRINT ? 0.6F
                            : kind == WardenNoise.Kind.JUMP ? 0.5F : 0.75F);
        }
        for (var interaction : INTERACTIONS.values()) {
            var player = interaction.player;
            if (!participant(player) || player.serverLevel() != interaction.level) continue;
            var session = HackManager.get().getSessionForParticipant(player);
            UUID sessionId = session == null ? null : session.getSessionId();
            if (!interaction.before.equals(interaction.level.getBlockState(interaction.pos))
                    || player.containerMenu != interaction.menu
                    || sessionId != null && !sessionId.equals(interaction.session))
                action(player, Vec3.atCenterOf(interaction.pos), WardenNoise.Kind.INTERACTION, 0.35F);
        }
        INTERACTIONS.clear();
        for (var noise : BATCH.drain()) {
            ServerPlayer source = null;
            if (noise.source() != null) {
                source = server.getPlayerList().getPlayer(noise.source());
                if (source == null || !participant(source, source.isAlive() || noise.kind() == WardenNoise.Kind.DAMAGE)
                        || !source.level().dimension().location().equals(noise.dimension())) continue;
            }
            for (var listener : wardens) {
                if (!noise.audible(listener.level().dimension().location(), listener.position())) continue;
                var sightTarget = source != null && noise.kind() != WardenNoise.Kind.WAVE ? source.getEyePosition() : noise.position();
                var reveal = WardenNoiseVisibility.reveal(WardenNoiseVisibility.visible(listener.level(), listener,
                        listener.getEyePosition(), sightTarget), source != null, noise.kind());
                if (reveal == WardenNoiseVisibility.Reveal.PULSE) ModNetworking.sendToPlayer(new WardenNoisePacket(noise), listener);
                else if (reveal == WardenNoiseVisibility.Reveal.ECHO
                        && WardenNoiseVisibility.echoInRange(listener.position(), source.position())
                        && ECHO_THROTTLE.allow(listener.getUUID(), source.getUUID(), noise.tick())) {
                    var echo = new org.example.maniacrevolution.network.packets.WardenShriekerEchoPacket(
                            noise.dimension(), noise.position(), noise.tick(), WardenEchoPose.capture(source));
                    if (echo.valid()) ModNetworking.sendToPlayer(new WardenNoisePacket(noise, echo), listener);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            action(player, player.position(), WardenNoise.Kind.ATTACK, 0.6F);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Float.isFinite(event.getAmount()) && event.getAmount() > 0)
            action(player, player.position(), WardenNoise.Kind.DAMAGE, Math.min(1, Math.max(0.25F, event.getAmount() / 6)));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                && event.getUseBlock() != Event.Result.DENY && player.position().distanceToSqr(Vec3.atCenterOf(event.getPos())) <= 36)
            action(player, Vec3.atCenterOf(event.getPos()), WardenNoise.Kind.ATTACK, 0.4F);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !participant(player)
                || event.getUseBlock() == Event.Result.DENY || INTERACTIONS.size() >= 64
                || player.position().distanceToSqr(Vec3.atCenterOf(event.getPos())) > 36) return;
        queueInteraction(player, event.getPos());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && participant(player)) queueInteraction(player, event.getPos());
    }
    private static void queueInteraction(ServerPlayer player, BlockPos pos) {
        if (INTERACTIONS.size() >= 64) return;
        var session = HackManager.get().getSessionForParticipant(player);
        INTERACTIONS.putIfAbsent(player.getUUID(), new Interaction(player, player.serverLevel(), pos.immutable(),
                player.level().getBlockState(pos), player.containerMenu, session == null ? null : session.getSessionId()));
    }
    public static void qteFailed(ServerPlayer player) {
        if (!participant(player) || !"survivors".equalsIgnoreCase(player.getTeam().getName())
                || !HackManager.get().isParticipatingInActiveHack(player)) return;
        var session = HackManager.get().getSessionForParticipant(player);
        if (session == null || session.isFinished() || player.serverLevel() != session.getHacker().serverLevel()
                || !(player.level().getBlockEntity(session.getPos()) instanceof ComputerBlockEntity computer)
                || computer.getComputerId() != session.getComputerId()) return;
        boolean hacker = session.getHacker().getUUID().equals(player.getUUID());
        Vec3 center = new Vec3(session.getPos().getX() + 0.5, session.getPos().getY() + (hacker ? 0 : 1), session.getPos().getZ() + 0.5);
        double radius = hacker ? HackConfig.HACKER_RADIUS : HackConfig.SUPPORT_RADIUS;
        if (player.position().distanceToSqr(center) > radius * radius) return;
        action(player, Vec3.atCenterOf(session.getPos()), WardenNoise.Kind.QTE, 1);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sound(PlayLevelSoundEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getSound() == null) return;
        String id = event.getSound().value().getLocation().toString();
        float volume = event.getNewVolume();
        if (!WardenSoundPolicy.eligible(id, event.getSource()) || WardenSoundPolicy.coveredPlayerAction(id, event.getSource())
                || !Float.isFinite(volume) || volume <= 0) return;
        UUID source = null;
        Vec3 position;
        if (event instanceof PlayLevelSoundEvent.AtEntity atEntity) {
            position = atEntity.getEntity().position();
            if (atEntity.getEntity() instanceof ServerPlayer player) {
                if (!participant(player)) return;
                source = player.getUUID();
            }
        } else if (event instanceof PlayLevelSoundEvent.AtPosition atPosition) position = atPosition.getPosition();
        else return;
        emit(level, source, position, WardenNoise.Kind.SOUND, Math.min(1, volume),
                Math.min(64, Math.max(WardenMatchRules.VISION_RANGE, event.getSound().value().getRange(volume))));
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        MOVEMENT.remove(event.getEntity().getUUID()); INTERACTIONS.remove(event.getEntity().getUUID());
        THROTTLE.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { clear(); }
    private static void clear() { MOVEMENT.clear(); INTERACTIONS.clear(); THROTTLE.clear(); ECHO_THROTTLE.clear(); BATCH.clear(); }
}
