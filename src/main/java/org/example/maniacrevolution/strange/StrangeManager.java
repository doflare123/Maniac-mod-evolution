package org.example.maniacrevolution.strange;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.cloak.CloakManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.skill.ToggleSkill;
import org.example.maniacrevolution.util.ManaUtil;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class StrangeManager {
    public enum Stance { DEFENSE, COMBAT }
    public static final ToggleSkill<Stance> STANCE = new ToggleSkill<>("strange_stance", List.of(Stance.values()));
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Long> TRANSIT = new HashMap<>();
    private static final Set<UUID> ACTIVE_AMULETS = new HashSet<>();
    private static final TicketType<UUID> PORTAL_TICKET = TicketType.create("maniacrev_strange_portal", Comparator.<UUID>naturalOrder(), 450);
    private static final class State {
        Stance stance = STANCE.initial();
        Anchor mark;
        StrangeEffectEntity effect;
        Portal portal;
        long nextAction, nextWhip, strikeAt;
        Vec3 whipDirection;
        Channel channel;
    }
    private record Channel(ItemStack stack, net.minecraft.world.InteractionHand hand, ResourceKey<Level> dimension, long ends, boolean closing) {}
    private record Anchor(ResourceKey<Level> dimension, Vec3 position, float yaw) {}
    private static final class Portal {
        final StrangeEffectEntity entrance, exit;
        final long expires;
        final UUID amulet;
        long closingAt;
        boolean activated;
        final Set<UUID> cut = new HashSet<>();
        Portal(StrangeEffectEntity entrance, StrangeEffectEntity exit, long expires, UUID amulet) {
            this.entrance = entrance; this.exit = exit; this.expires = expires; this.amulet = amulet;
        }
        StrangeEffectEntity entrance() { return entrance; }
        StrangeEffectEntity exit() { return exit; }
        long expires() { return expires; }
    }
    public static boolean activeAmulet(UUID id) { return ACTIVE_AMULETS.contains(id); }
    public static boolean switchStance(ServerPlayer player) {
        if (!usable(player)) return false;
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        Stance previous = state.stance;
        action(player, StrangeActionPacket.Action.STANCE);
        return state.stance != previous;
    }
    public static void useAmulet(ServerPlayer player, ItemStack stack, boolean mark) {
        if (stack.hasTag() && stack.getTag().hasUUID("PortalUse") && !activeAmulet(stack.getTag().getUUID("PortalUse"))) {
            stack.shrink(1); return;
        }
        action(player, mark ? StrangeActionPacket.Action.MARK : StrangeActionPacket.Action.PORTAL);
    }

    private static boolean usable(ServerPlayer player) {
        var downed = DownedCapability.get(player);
        return wearing(player) && !player.isSleeping() && !player.isPassenger()
                && !CloakManager.restrained(player) && !player.hasEffect(ModEffects.STUN.get())
                && (downed == null || downed.getState() != DownedState.DOWNED);
    }
    private static boolean wearing(ServerPlayer player) {
        return CloakManager.equipped(player) && player.isAlive() && !player.hasDisconnected()
                && !player.isCreative() && !player.isSpectator();
    }
    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.maniacrev.strange." + key), true);
    }
    public static void action(ServerPlayer player, StrangeActionPacket.Action action) {
        if (!usable(player)) return;
        ItemStack amulet = player.getMainHandItem().is(ModItems.SPACE_AMULET.get()) ? player.getMainHandItem() : player.getOffhandItem();
        if ((action == StrangeActionPacket.Action.MARK || action == StrangeActionPacket.Action.PORTAL)
                && !amulet.is(ModItems.SPACE_AMULET.get())) return;
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        long tick = now(player.server);
        if (state.channel != null) return;
        if (tick < state.nextAction) return;
        state.nextAction = tick + 4;
        switch (action) {
            case STANCE -> {
                if (state.effect != null && state.effect.kind() == StrangeEffectEntity.WHIP) return;
                state.stance = STANCE.next(state.stance);
                if (state.effect != null) state.effect.kind(state.stance.ordinal());
                message(player, state.stance == Stance.DEFENSE ? "defense" : "combat");
            }
            case WHIP -> {
                if (state.stance != Stance.COMBAT || tick < state.nextWhip) return;
                if (!ManaUtil.consumeMana(player, StrangeRules.WHIP_MANA)) { message(player, "no_mana"); return; }
                ensureEffect(player, state);
                state.whipDirection = player.getLookAngle();
                state.strikeAt = tick + StrangeRules.WINDUP_TICKS;
                state.nextWhip = tick + StrangeRules.WHIP_COOLDOWN;
                state.effect.setYRot(player.getYRot());
                state.effect.setXRot(player.getXRot());
                state.effect.kind(StrangeEffectEntity.WHIP);
            }
            case MARK -> {
                if (state.mark != null) { message(player, "already_marked"); return; }
                if (!safe(player.serverLevel(), player.position(), player)) { message(player, "blocked"); return; }
                state.mark = new Anchor(player.level().dimension(), player.position(), player.getYRot());
                message(player, "marked");
            }
            case PORTAL -> {
                if (state.effect != null && state.effect.kind()==StrangeEffectEntity.WHIP) return;
                if (state.portal != null) {
                    if (state.portal.closingAt != 0 || !state.portal.activated) return;
                    beginChannel(player,state,amulet,tick,true);
                    closePortal(state,StrangeRules.CLOSE_CAST_TICKS);
                    return;
                }
                if (amulet.hasTag() && amulet.getTag().hasUUID("PortalUse")) return;
                openPortal(player, state, tick);
            }
            case CAPTURE -> CloakManager.throwCloak(player);
        }
    }

    // Before damage-triggered perks; armor and vanilla absorption have already been applied.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !usable(player) || event.getAmount() <= 0
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        if (state.stance == Stance.DEFENSE && ManaUtil.consumeMana(player, StrangeRules.shieldCost(event.getAmount()))) {
            event.setAmount(0);
            event.setCanceled(true);
        }
    }
    private static StrangeEffectEntity spawn(ServerLevel level, Vec3 position, float yaw, ServerPlayer owner, int kind) {
        StrangeEffectEntity effect = ModEntities.STRANGE_EFFECT.get().create(level);
        if (effect == null) throw new IllegalStateException("Strange effect not registered");
        effect.moveTo(position.x, position.y, position.z, yaw, 0);
        effect.owner(owner); effect.kind(kind);
        level.addFreshEntity(effect);
        return effect;
    }
    private static void ensureEffect(ServerPlayer player, State state) {
        if (state.effect != null && (state.effect.isRemoved() || state.effect.level() != player.level())) {
            state.effect.discard(); state.effect = null; state.strikeAt = 0;
        }
        if (state.effect == null) state.effect = spawn(player.serverLevel(), player.position(), player.getYRot(), player, state.stance.ordinal());
    }
    private static void strike(ServerPlayer owner, State state) {
        Vec3 eye = owner.getEyePosition(), end = eye.add(state.whipDirection.scale(StrangeRules.WHIP_RANGE));
        owner.serverLevel().players().stream()
                .filter(p -> p != owner && p.isAlive() && !p.isSpectator() && !p.isCreative()
                        && PerkTeam.fromPlayer(p) == PerkTeam.MANIAC && owner.hasLineOfSight(p))
                .filter(p -> p.getBoundingBox().inflate(.35).clip(eye, end).isPresent())
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(owner)))
                .ifPresent(p -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, StrangeRules.SLOW_TICKS, 1)));
    }

    private static void openPortal(ServerPlayer owner, State state, long tick) {
        if (state.mark == null) { message(owner, "no_mark"); return; }
        ServerLevel destination = owner.server.getLevel(state.mark.dimension());
        if (destination == null) { message(owner, "blocked"); return; }
        Vec3 forward = Vec3.directionFromRotation(0, owner.getYRot());
        Vec3 entrance = owner.position().add(forward.scale(2));
        // Read the destination before charging; bounded tickets keep both rings alive for 15 seconds.
        destination.getChunkAt(BlockPos.containing(state.mark.position()));
        if (!safe(owner.serverLevel(), entrance, owner) || findExit(destination, state.mark.position(), state.mark.yaw(), owner) == null) {
            message(owner, "blocked"); return;
        }
        var entry = spawn(owner.serverLevel(), entrance, owner.getYRot(), owner, StrangeEffectEntity.PORTAL);
        var exit = spawn(destination, state.mark.position(), state.mark.yaw(), owner, StrangeEffectEntity.PORTAL);
        ItemStack amulet = owner.getMainHandItem().is(ModItems.SPACE_AMULET.get()) ? owner.getMainHandItem() : owner.getOffhandItem();
        UUID use = UUID.randomUUID();
        state.portal = new Portal(entry, exit, tick + StrangeRules.OPEN_CAST_TICKS + StrangeRules.PORTAL_TICKS, use);
        beginChannel(owner,state,amulet,tick,false);
        holdPortalChunk(entry, true); holdPortalChunk(exit, true);
        PortalViewPacket.broadcast(entry, exit);
        PortalViewPacket.broadcast(exit, entry);
    }
    private static void beginChannel(ServerPlayer owner, State state, ItemStack stack, long tick, boolean closing) {
        var hand=stack==owner.getMainHandItem()?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND;
        state.channel=new Channel(stack,hand,owner.level().dimension(),tick+(closing?StrangeRules.CLOSE_CAST_TICKS:StrangeRules.OPEN_CAST_TICKS),closing);
        ensureEffect(owner,state);
        state.effect.setYRot(owner.getYRot());
        boolean left=owner.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
        state.effect.drawingLeft(hand==net.minecraft.world.InteractionHand.MAIN_HAND?left:!left);
        state.effect.kind(closing?StrangeEffectEntity.PORTAL_CLOSE:StrangeEffectEntity.PORTAL_OPEN);
    }
    private static void updateChannel(ServerPlayer owner, State state, long tick) {
        Channel channel=state.channel;
        if(channel==null)return;
        boolean valid=owner!=null && usable(owner) && owner.level().dimension()==channel.dimension()
                && owner.getItemInHand(channel.hand())==channel.stack() && channel.stack().is(ModItems.SPACE_AMULET.get())
                && state.portal!=null && !state.portal.entrance().isRemoved() && !state.portal.exit().isRemoved();
        if(!valid) {
            state.channel=null;
            if(!channel.closing()) finishPortal(state);
        } else if(tick>=channel.ends()) {
            state.channel=null;
            if(!channel.closing() && state.portal!=null) {
                if(ManaUtil.consumeMana(owner,StrangeRules.portalCost(ManaUtil.getMana(owner)))) {
                    channel.stack().getOrCreateTag().putUUID("PortalUse",state.portal.amulet);
                    ACTIVE_AMULETS.add(state.portal.amulet); state.portal.activated=true;
                    message(owner,"opened");
                } else { finishPortal(state); message(owner,"no_mana"); }
            }
        }
        if(state.channel==null && state.effect!=null && state.effect.casting()) state.effect.kind(state.stance.ordinal());
    }
    private static boolean safe(ServerLevel level, Vec3 position, ServerPlayer player) {
        AABB box = player.getBoundingBox().move(position.subtract(player.position()));
        return position.y >= level.getMinBuildHeight() && box.maxY < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(box) && level.noCollision(player, box) && !level.containsAnyLiquid(box);
    }
    private static Vec3 findExit(ServerLevel level, Vec3 center, float yaw, ServerPlayer player) {
        Vec3 normal = Vec3.directionFromRotation(0, yaw);
        for (double side : new double[]{1.25, -1.25, 0}) {
            Vec3 point = center.add(normal.scale(side));
            level.getChunkAt(BlockPos.containing(point));
            if (safe(level, point, player)) return point;
        }
        return null;
    }
    private static void transit(StrangeEffectEntity from, StrangeEffectEntity to, long tick) {
        if (!(from.level() instanceof ServerLevel level) || !(to.level() instanceof ServerLevel destination)) return;
        Vec3 normal = Vec3.directionFromRotation(0, from.getYRot());
        Vec3 right = new Vec3(normal.z, 0, -normal.x);
        for (ServerPlayer player : new ArrayList<>(level.players())) {
            if (!player.isAlive() || player.isSpectator() || player.isPassenger() || CloakManager.restrained(player)
                    || TRANSIT.getOrDefault(player.getUUID(), 0L) > tick) continue;
            Vec3 delta = player.position().subtract(from.position());
            Vec3 previous = new Vec3(player.xo, player.yo, player.zo).subtract(from.position());
            if (!StrangeRules.entersPortal(delta.dot(right), delta.y, delta.dot(normal),
                    previous.dot(right), previous.y, previous.dot(normal))) continue;
            Vec3 exit = findExit(destination, to.position(), to.getYRot(), player);
            if (exit == null) continue;
            TRANSIT.put(player.getUUID(), tick + 30);
            CloakManager.beforePortalTravel(player);
            player.teleportTo(destination, exit.x, exit.y, exit.z, to.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long tick = now(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (usable(player)) STATES.computeIfAbsent(player.getUUID(), id -> new State());
        }
        for (var entry : STATES.entrySet()) {
            State state = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            updateChannel(owner,state,tick);
            if (owner == null || !wearing(owner)) clearVisuals(state);
            else if (usable(owner)) {
                ensureEffect(owner, state);
                state.effect.owner(owner);
                state.effect.setPos(owner.position());
                if (state.effect.kind() != StrangeEffectEntity.WHIP && !state.effect.casting()) {
                    state.effect.setYRot(owner.getYRot()); state.effect.setXRot(0);
                }
                if (state.strikeAt > 0 && tick >= state.strikeAt) { strike(owner, state); state.strikeAt = 0; }
                if (state.effect.kind() == StrangeEffectEntity.WHIP && state.effect.age() >= StrangeRules.WHIP_TICKS)
                    state.effect.kind(state.stance.ordinal());
            } else {
                if (state.effect != null) state.effect.discard();
                state.effect = null; state.strikeAt = 0;
            }
            if (state.portal != null) {
                Portal portal = state.portal;
                if (portal.closingAt > 0) {
                    cutAtClosingPortal(portal, portal.entrance()); cutAtClosingPortal(portal, portal.exit());
                    if (tick >= portal.closingAt) finishPortal(state);
                } else if (tick >= portal.expires() || portal.entrance().isRemoved() || portal.exit().isRemoved()) closePortal(state);
                else {
                    if (portal.activated) {
                        transit(portal.entrance(), portal.exit(), tick); transit(portal.exit(), portal.entrance(), tick);
                    }
                    if (tick % 20 == 0) {
                        PortalViewPacket.broadcast(portal.entrance(), portal.exit());
                        PortalViewPacket.broadcast(portal.exit(), portal.entrance());
                    }
                }
            }
        }
        TRANSIT.values().removeIf(expiry -> expiry <= tick);
    }
    public static boolean managed(StrangeEffectEntity entity) {
        return STATES.values().stream().anyMatch(s -> s.effect == entity
                || s.portal != null && (s.portal.entrance() == entity || s.portal.exit() == entity));
    }
    private static void closePortal(State state) {
        closePortal(state,StrangeRules.COLLAPSE_TICKS);
    }
    private static void closePortal(State state,int duration) {
        if (state.portal == null) return;
        if (state.portal.closingAt != 0) return;
        state.portal.closingAt = now(((ServerLevel)state.portal.entrance().level()).getServer()) + duration;
        state.portal.entrance().closing(duration); state.portal.exit().closing(duration);
    }
    private static void finishPortal(State state) {
        if (state.portal == null) return;
        holdPortalChunk(state.portal.entrance(), false); holdPortalChunk(state.portal.exit(), false);
        ACTIVE_AMULETS.remove(state.portal.amulet);
        var server = ((ServerLevel)state.portal.entrance().level()).getServer();
        for (var holder : server.getPlayerList().getPlayers()) for (var stack : java.util.stream.Stream.concat(
                holder.getInventory().items.stream(), holder.getInventory().offhand.stream()).toList()) {
            if (stack.is(ModItems.SPACE_AMULET.get()) && stack.hasTag() && stack.getTag().hasUUID("PortalUse")
                    && stack.getTag().getUUID("PortalUse").equals(state.portal.amulet))
                stack.hurtAndBreak(1, holder, p -> {
                    if (stack == p.getMainHandItem()) p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND);
                    else if (stack == p.getOffhandItem()) p.broadcastBreakEvent(net.minecraft.world.InteractionHand.OFF_HAND);
                });
        }
        state.portal.entrance().discard(); state.portal.exit().discard(); state.portal = null;
    }
    private static void cutAtClosingPortal(Portal portal, StrangeEffectEntity ring) {
        ServerLevel level = (ServerLevel) ring.level();
        Vec3 normal = Vec3.directionFromRotation(0, ring.getYRot());
        Vec3 right = new Vec3(normal.z, 0, -normal.x);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator() || portal.cut.contains(player.getUUID())) continue;
            Vec3 d = player.position().subtract(ring.position());
            Vec3 p = new Vec3(player.xo, player.yo, player.zo).subtract(ring.position());
            if (!StrangeRules.entersPortal(d.dot(right), d.y, d.dot(normal), p.dot(right), p.y, p.dot(normal))) continue;
            portal.cut.add(player.getUUID());
            var type = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            Maniacrev.loc("portal_collapse")));
            float absorption = player.getAbsorptionAmount();
            player.setAbsorptionAmount(0);
            try { player.hurt(new net.minecraft.world.damagesource.DamageSource(type), StrangeRules.collapseDamage(player.getMaxHealth())); }
            finally { player.setAbsorptionAmount(absorption); }
        }
    }
    private static void holdPortalChunk(StrangeEffectEntity ring, boolean hold) {
        if (!(ring.level() instanceof ServerLevel level)) return;
        ChunkPos chunk = new ChunkPos(ring.blockPosition());
        if (hold) level.getChunkSource().addRegionTicket(PORTAL_TICKET, chunk, 2, ring.getUUID());
        else level.getChunkSource().removeRegionTicket(PORTAL_TICKET, chunk, 2, ring.getUUID());
    }
    private static void clearVisuals(State state) {
        if(state.channel!=null && !state.channel.closing()) finishPortal(state);
        state.channel=null;
        if (state.effect != null) state.effect.discard();
        state.effect = null; state.strikeAt = 0; closePortal(state);
    }
    /** Match boundaries, never armor changes, death, or logout, reset the one-use marker. */
    public static void resetMatch() {
        STATES.values().forEach(s -> { clearVisuals(s); finishPortal(s); }); STATES.clear(); TRANSIT.clear(); ACTIVE_AMULETS.clear();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        State state = STATES.get(event.getEntity().getUUID());
        if (state != null) clearVisuals(state);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { resetMatch(); }
}
