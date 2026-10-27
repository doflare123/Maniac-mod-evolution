package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.helsing.HelsingManager;
import org.example.maniacrevolution.network.ModNetworking;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ForesterManager {
    private static final Map<UUID, Capture> CAPTURES = new HashMap<>();
    private static final Map<UUID, Work> WORK = new HashMap<>();
    private static final Set<UUID> PRESSED = new HashSet<>();
    public static final int REARM = 0, COLLECT = 1, PLACE = 2;
    private static int nextSequence;
    private static final class Capture {
        final ServerLevel level;
        final BlockPos pos;
        final Vec3 anchor;
        final long start;
        int successes, sequence, lastPress = -100;
        float target;
        Capture(ServerPlayer player, BlockPos pos) {
            level = player.serverLevel(); this.pos = pos.immutable(); start = level.getGameTime();
            anchor = new Vec3(pos.getX() + .5, pos.getY() + .125, pos.getZ() + .5);
            nextTarget(player);
        }
        void nextTarget(ServerPlayer player) {
            target = .2F + player.getRandom().nextFloat() * .6F;
            sequence = ++nextSequence;
        }
        int age() { return (int) (level.getGameTime() - start); }
    }
    private static final class Work {
        final ServerLevel level;
        final BlockPos pos;
        final int kind;
        final long start;
        final int slot;
        final Vec3 origin;
        final float yaw, pitch;
        final InteractionHand hand;
        final ItemStack stack;
        Work(ServerPlayer player, BlockPos pos, int kind, InteractionHand hand) {
            level = player.serverLevel(); this.pos = pos.immutable(); this.kind = kind; this.hand = hand;
            start = level.getGameTime(); slot = player.getInventory().selected;
            origin = player.position(); yaw = player.getYRot(); pitch = player.getXRot(); stack = player.getItemInHand(hand);
        }
        int duration() { return kind == COLLECT ? ForesterRules.COLLECT : ForesterRules.REARM; }
    }
    private ForesterManager() {}

    public static boolean vulnerable(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
    private static boolean downed(Player player) {
        var data = DownedCapability.get(player);
        return data != null && data.getState() == DownedState.DOWNED;
    }
    public static boolean canUse(Player player) {
        if (!vulnerable(player) || downed(player) || !HelsingManager.isSurvivor(player)
                || player.hasEffect(ModEffects.SILENCE.get()) || player.hasEffect(ModEffects.STUN.get()) || trapped(player)) return false;
        return player instanceof ServerPlayer server
                ? PlayerDataManager.get(server).getSurvivorClassId() == ForesterRules.CLASS_ID
                : org.example.maniacrevolution.data.ClientPlayerData.isSurvivorClass(ForesterRules.CLASS_ID);
    }
    public static boolean trapped(Player player) { return player.hasEffect(ModEffects.FORESTER_TRAPPED.get()); }
    public static boolean occupied(Level level, BlockPos pos) {
        return CAPTURES.values().stream().anyMatch(c -> c.level == level && c.pos.equals(pos));
    }
    public static boolean capture(ServerPlayer player, BlockPos pos) {
        if (!vulnerable(player) || CAPTURES.containsKey(player.getUUID()) || player.isPassenger()) return false;
        var state = player.level().getBlockState(pos);
        if (!state.is(ModBlocks.FORESTER_TRAP.get()) || state.getValue(ForesterTrapBlock.STATE) != ForesterTrapBlock.State.OPEN) return false;
        Capture capture = new Capture(player, pos);
        CAPTURES.put(player.getUUID(), capture);
        cancelWork(player);
        player.stopUsingItem();
        player.addEffect(new MobEffectInstance(ModEffects.FORESTER_TRAPPED.get(), ForesterRules.TRAP_DELAY + ForesterRules.TRAP_GAME + 2, 0, false, false, true));
        player.level().setBlockAndUpdate(pos, state.setValue(ForesterTrapBlock.STATE, ForesterTrapBlock.State.CAUGHT));
        player.connection.teleport(capture.anchor.x, capture.anchor.y, capture.anchor.z, player.getYRot(), player.getXRot());
        player.level().playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, player.getSoundSource(), .9F, .7F);
        snapshot(player);
        return true;
    }
    private static void release(ServerPlayer player, boolean bleed) {
        Capture capture = CAPTURES.remove(player.getUUID());
        player.removeEffect(ModEffects.FORESTER_TRAPPED.get());
        if (capture == null) return;
        if (capture.level.hasChunkAt(capture.pos)) {
            var state = capture.level.getBlockState(capture.pos);
            if (state.is(ModBlocks.FORESTER_TRAP.get()) && state.getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.CAUGHT)
                capture.level.setBlockAndUpdate(capture.pos, state.setValue(ForesterTrapBlock.STATE, ForesterTrapBlock.State.SPENT));
        }
        if (bleed && player.isAlive()) player.addEffect(new MobEffectInstance(ModEffects.OPEN_WOUND.get(), ForesterRules.BLEED, 0, false, false, true));
        snapshot(player);
    }
    public static void space(ServerPlayer player, int sequence, int sampledAge) {
        Capture capture = CAPTURES.get(player.getUUID());
        if (capture == null || capture.level != player.level() || capture.sequence != sequence) return;
        int age = capture.age();
        if (age < ForesterRules.TRAP_DELAY || ForesterRules.expired(age) || age - capture.lastPress < 8) return;
        capture.lastPress = age;
        // Permit bounded transit delay; never trust a client-supplied accuracy or success count.
        int testedAge = Math.max(age - 4, Math.min(age, sampledAge));
        if (ForesterRules.hit(testedAge, capture.target)) {
            capture.successes++;
            if (capture.successes == 2) { release(player, false); return; }
            capture.nextTarget(player);
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, player.getSoundSource(), .6F, 1.5F);
        } else player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), player.getSoundSource(), .4F, .6F);
        snapshot(player);
    }
    public static boolean aimedAt(ServerPlayer player, BlockPos pos) {
        if (!player.serverLevel().hasChunkAt(pos) || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 25) return false;
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(5)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }
    private static boolean workable(ServerPlayer player, BlockPos pos) {
        return workable(player, pos, true);
    }
    private static boolean workable(ServerPlayer player, BlockPos pos, boolean checkAim) {
        if (!canUse(player) || player.isUsingItem() || !player.serverLevel().hasChunkAt(pos)
                || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 36 || (checkAim && !aimedAt(player, pos))) return false;
        var state = player.level().getBlockState(pos);
        return state.is(ModBlocks.FORESTER_TRAP.get()) && state.getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.SPENT;
    }
    public static void workInput(ServerPlayer player, BlockPos pos, boolean collect) {
        startWork(player, pos, collect ? COLLECT : REARM, InteractionHand.MAIN_HAND);
    }
    public static void input(ServerPlayer player, ForesterInputPacket packet) {
        switch (packet.action()) {
            case SPACE -> space(player, packet.sequence(), packet.age());
            case RELEASE -> PRESSED.remove(player.getUUID());
            case CANCEL -> cancelWork(player);
            default -> {
                // Holding RMB after collection must never place the returned item.
                if (!PRESSED.add(player.getUUID())) return;
                if (packet.action() == ForesterInputPacket.Action.PLACE) startPlacement(player, packet.pos(), packet.hand());
                else startWork(player, packet.pos(), packet.action() == ForesterInputPacket.Action.COLLECT ? COLLECT : REARM, packet.hand());
                snapshot(player);
            }
        }
    }
    public static BlockPos placementTarget(Player player, InteractionHand hand) {
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(5)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit).getClickedPos().immutable();
    }
    private static boolean placeable(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        return placeable(player, pos, hand, true);
    }
    private static boolean placeable(ServerPlayer player, BlockPos pos, InteractionHand hand, boolean checkAim) {
        var level = player.serverLevel();
        if (!canUse(player) || !player.getItemInHand(hand).is(ModItems.FORESTER_TRAP.get()) || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos) || !level.mayInteract(player, pos)
                || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 36
                || (checkAim && !pos.equals(placementTarget(player, hand))) || !level.getBlockState(pos).isAir()) return false;
        var state = ModBlocks.FORESTER_TRAP.get().defaultBlockState();
        return state.canSurvive(level, pos) && level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.of(player));
    }
    public static void startPlacement(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        if (WORK.containsKey(player.getUUID()) || !placeable(player, pos, hand)) return;
        WORK.put(player.getUUID(), new Work(player, pos, PLACE, hand));
        workPose(player); snapshot(player);
    }
    private static void startWork(ServerPlayer player, BlockPos pos, int kind, InteractionHand hand) {
        if (WORK.containsKey(player.getUUID()) || !workable(player, pos)) return;
        WORK.put(player.getUUID(), new Work(player, pos, kind, hand));
        workPose(player); snapshot(player);
    }
    public static void cancelWork(ServerPlayer player) {
        if (WORK.remove(player.getUUID()) != null) { workPose(player); snapshot(player); }
    }
    private static void workPose(ServerPlayer player) {
        Work work = WORK.get(player.getUUID());
        ModNetworking.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ForesterWorkPacket(player.getId(), work == null ? BlockPos.ZERO : work.pos,
                        work == null ? -1 : work.kind, work == null ? 0 : (int) (work.level.getGameTime() - work.start), work == null ? 0 : work.duration()));
    }
    static ForesterStatePacket state(ServerPlayer player) {
        Capture capture = CAPTURES.get(player.getUUID()); Work work = WORK.get(player.getUUID());
        return new ForesterStatePacket(capture == null ? -1 : capture.age(),
                capture == null ? 0 : capture.successes, capture == null ? .5F : capture.target,
                capture == null ? -1 : capture.sequence, work == null ? -1 : (int) (work.level.getGameTime() - work.start),
                work != null && work.kind == COLLECT);
    }
    private static void snapshot(ServerPlayer player) {
        ModNetworking.sendToPlayer(state(player), player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Capture capture = CAPTURES.get(player.getUUID());
        if (capture != null) {
            var state = capture.level.hasChunkAt(capture.pos) ? capture.level.getBlockState(capture.pos) : null;
            if (!vulnerable(player) || capture.level != player.level() || ForesterRules.expired(capture.age())
                    || state == null || !state.is(ModBlocks.FORESTER_TRAP.get()) || state.getValue(ForesterTrapBlock.STATE) != ForesterTrapBlock.State.CAUGHT) release(player, false);
            else {
                player.stopUsingItem(); player.setDeltaMovement(Vec3.ZERO); player.hurtMarked = true; player.fallDistance = 0;
                if (player.position().distanceToSqr(capture.anchor) > .001)
                    player.connection.teleport(capture.anchor.x, capture.anchor.y, capture.anchor.z, player.getYRot(), player.getXRot());
                if (!trapped(player)) player.addEffect(new MobEffectInstance(ModEffects.FORESTER_TRAPPED.get(), ForesterRules.TRAP_DELAY + ForesterRules.TRAP_GAME - capture.age() + 2, 0, false, false, true));
                if (now % 2 == 0) snapshot(player);
            }
        } else if (trapped(player)) player.removeEffect(ModEffects.FORESTER_TRAPPED.get());
        Work work = WORK.get(player.getUUID());
        if (work != null) {
            boolean moved = ForesterRules.workInterrupted(work.origin.distanceToSqr(player.position()),
                    player.getYRot() - work.yaw, player.getXRot() - work.pitch);
            if (work.level != player.level() || moved || work.slot != player.getInventory().selected || work.stack != player.getItemInHand(work.hand)
                    || !(work.kind == PLACE ? placeable(player, work.pos, work.hand, false) : workable(player, work.pos, false))) {
                cancelWork(player);
            } else if (now - work.start >= work.duration()) {
                WORK.remove(player.getUUID());
                if (work.kind == COLLECT) {
                    player.level().removeBlock(work.pos, false);
                    var stack = new net.minecraft.world.item.ItemStack(ModItems.FORESTER_TRAP.get());
                    if (!player.addItem(stack)) player.drop(stack, false);
                } else if (work.kind == PLACE) {
                    var previous = net.minecraftforge.common.util.BlockSnapshot.create(player.level().dimension(), player.level(), work.pos);
                    player.level().setBlockAndUpdate(work.pos, ModBlocks.FORESTER_TRAP.get().defaultBlockState());
                    if (net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player, previous, net.minecraft.core.Direction.UP)) previous.restore(true, false);
                    else work.stack.shrink(1);
                } else player.level().setBlockAndUpdate(work.pos, ModBlocks.FORESTER_TRAP.get().defaultBlockState());
                player.level().playSound(null, work.pos, SoundEvents.IRON_TRAPDOOR_OPEN, player.getSoundSource(), .7F, 1.1F);
                workPose(player); snapshot(player);
            } else if (now % 2 == 0) { snapshot(player); workPose(player); }
        }
        if (vulnerable(player) && player.hasEffect(ModEffects.PEPPER.get()) && now % ForesterRules.SNEEZE_INTERVAL == 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PANDA_SNEEZE, player.getSoundSource(), 1.5F, 1.1F);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SNEEZE,
                    player.getX(), player.getEyeY() - .1, player.getZ(), 4, .12, .08, .12, .02);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damage(LivingHurtEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player) {
            cancelWork(player);
            if (event.getSource().getEntity() instanceof Player attacker && attacker != player) release(player, true);
        }
    }
    @SubscribeEvent public static void attack(AttackEntityEvent event) { if (trapped(event.getEntity())) event.setCanceled(true); }
    @SubscribeEvent public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (trapped(event.getEntity()) || (!event.getEntity().isCreative() && (event.getLevel().getBlockState(event.getPos()).is(ModBlocks.FORESTER_TRAP.get())
                || event.getEntity().getMainHandItem().is(ModItems.FORESTER_TRAP.get()) || event.getEntity().getOffhandItem().is(ModItems.FORESTER_TRAP.get())
                || WORK.containsKey(event.getEntity().getUUID())))) {
            event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent public static void rightItem(PlayerInteractEvent.RightClickItem event) { if (trapped(event.getEntity())) event.setCanceled(true); }
    @SubscribeEvent public static void itemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && trapped(player)) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) { if (trapped(event.getEntity())) event.setCanceled(true); }
    @SubscribeEvent public static void breakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent event) { if (trapped(event.getPlayer())) event.setCanceled(true); }
    @SubscribeEvent public static void death(LivingDeathEvent event) { if (event.getEntity() instanceof ServerPlayer player) cleanup(player); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) cleanup(player); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) cleanup(player); }
    private static void cleanup(ServerPlayer player) { cancelWork(player); PRESSED.remove(player.getUUID()); release(player, false); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CAPTURES.clear(); WORK.clear(); PRESSED.clear(); ForesterLightBlock.clear(); nextSequence = 0; }
}
