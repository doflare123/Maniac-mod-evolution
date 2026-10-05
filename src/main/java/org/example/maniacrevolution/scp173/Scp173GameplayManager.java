package org.example.maniacrevolution.scp173;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Scp173InputPacket.Action;
import org.example.maniacrevolution.network.packets.Scp173StatusPacket;
import org.example.maniacrevolution.util.ManaUtil;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class Scp173GameplayManager {
    private record Observer(ServerPlayer player, ResourceKey<Level> dimension, Scp173BlinkState state) {}
    private static final class Statue {
        final ServerPlayer player;
        ResourceKey<Level> dimension;
        final Scp173AbilityState state = new Scp173AbilityState();
        long lightUntil, impactUntil, clock, nextScrape;
        boolean jumpOwned;
        net.minecraft.world.phys.Vec3 lastPosition;
        Statue(ServerPlayer p) {
            player = p; dimension = p.level().dimension(); lastPosition = p.position(); clock = p.level().getGameTime();
            p.addEffect(new MobEffectInstance(ModEffects.SCP173_ARMOR.get(), -1, 0, false, false, true));
        }
    }
    private static final UUID SPEED = UUID.fromString("f3c1e4e9-e3a1-4a55-a53b-6e9e8c9dcbd1");
    private static final Map<UUID, Observer> OBSERVERS = new HashMap<>();
    private static final Map<UUID, Statue> STATUES = new HashMap<>();
    private static ServerPlayer authorized;
    private Scp173GameplayManager() {}

    private static Statue statue(ServerPlayer p) {
        var old = STATUES.get(p.getUUID());
        if (old != null && old.player != p) { clear(old); old = null; }
        if (old != null && old.dimension != p.level().dimension()) {
            // A dimension transfer clears its visual effect, not earned discounts/cooldowns or
            // a player's decision to remove armor. Remap deadlines if the new world's clock differs.
            old.state.changeClock(old.clock, p.level().getGameTime());
            old.dimension = p.level().dimension(); old.lightUntil = old.impactUntil = old.nextScrape = 0; old.lastPosition = p.position();
        }
        if (old == null) { old = new Statue(p); STATUES.put(p.getUUID(), old); }
        old.clock = p.level().getGameTime();
        return old;
    }
    private static void clear(Statue state) {
        state.player.removeEffect(ModEffects.SCP173_ARMOR.get());
        if (state.jumpOwned) state.player.removeEffect(ModEffects.JUMP_COOLDOWN.get());
        var speed = state.player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED);
    }
    public static boolean blinking(ServerPlayer p) {
        var observer = OBSERVERS.get(p.getUUID());
        return observer != null && observer.player == p && observer.dimension == p.level().dimension()
                && observer.state.remaining(p.level().getGameTime()) > 0;
    }
    public static int blackout(ServerPlayer p) {
        long now = p.level().getGameTime(), until = now;
        for (var s : STATUES.values()) if (s.dimension == p.level().dimension() && s.dimension == s.player.level().dimension()
                && Scp173Manager.active(s.player))
            until = Math.max(until, s.lightUntil);
        return (int) Math.max(0, until - now);
    }
    public static double sightRange(ServerPlayer p) {
        if (blackout(p) > 0) return 1;
        if (p.hasEffect(MobEffects.BLINDNESS)) return 3;
        var effect = p.getEffect(MobEffects.DARKNESS);
        // Minecraft 1.20.1's own FactorData drives the visual pulse; retain its timing, map 20 -> 3.
        return effect == null ? 20 : effect.getFactorData().map(f -> Scp173Rules.darknessRange(f.getFactor(p, 1), p.level().getGameTime())).orElse(20.0);
    }
    public static int observers(ServerPlayer p) {
        int count = 0;
        for (var other : p.serverLevel().players()) if (other != p && Scp173Manager.eligibleObserver(other)
                && !blinking(other) && Scp173Vision.sees(other, p)) count++;
        return count;
    }
    public static boolean permitClientJump(ServerPlayer p) {
        if (!Scp173Manager.active(p) || Scp173Manager.held(p) || p.hasEffect(ModEffects.JUMP_COOLDOWN.get())) return false;
        p.addEffect(new MobEffectInstance(ModEffects.JUMP_COOLDOWN.get(), 60, 0, false, false, true));
        statue(p).jumpOwned = true;
        return true;
    }
    public static void input(ServerPlayer p, Action action) {
        if (action == Action.BLINK) {
            var s = OBSERVERS.get(p.getUUID());
            if (s != null && s.player == p && s.dimension == p.level().dimension()
                    && Scp173Manager.eligibleObserver(p)) s.state.blink(p.level().getGameTime());
            return;
        }
        if (!Scp173Manager.active(p) || p.containerMenu != p.inventoryMenu) return;
        var s = statue(p); long now = p.level().getGameTime();
        switch (action) {
            case LIGHT -> {
                if (!s.state.lightReady()) return;
                float cost = s.state.cost();
                if (cost > 0 && !ManaUtil.consumeMana(p, cost)) return;
                s.state.usedLight(); s.lightUntil = now + 60;
                // Holding/discount/cooldown clocks are separate; held statues may activate this.
                p.serverLevel().playSound(null, p.blockPosition(), org.example.maniacrevolution.sound.ModSounds.SCP173_BLACKOUT.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1);
            }
            case MELEE -> {
                if (Scp173Manager.held(p) || !s.state.melee(now)) return;
                var target = Scp173CombatGeometry.target(p);
                boolean hit = false;
                if (target != null) {
                    authorized = p;
                    try {
                        if (!MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(p, target))) {
                            hit = target.hurt(p.damageSources().playerAttack(p), 10);
                            if (hit) p.setLastHurtMob(target);
                        }
                    } finally { authorized = null; }
                }
                if (hit) {
                    s.impactUntil = now + 6;
                    p.serverLevel().playSound(null, target.blockPosition(), org.example.maniacrevolution.sound.ModSounds.SCP173_IMPACT.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
                }
            }
            default -> { }
        }
        send(p, null, s);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void vanillaAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Scp173Manager.active(p) && p != authorized) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        update(java.util.List.copyOf(server.getPlayerList().getPlayers()));
    }

    /** One coherent match snapshot, also used by dedicated-server integration fixtures. */
    static void update(java.util.List<ServerPlayer> players) {
        STATUES.entrySet().removeIf(entry -> {
            var s = entry.getValue();
            if (players.contains(s.player) && Scp173Manager.active(s.player)) return false;
            clear(s); return true;
        });
        var active = players.stream().filter(Scp173Manager::active).toList();
        for (var p : active) statue(p);
        OBSERVERS.entrySet().removeIf(entry -> {
            var s = entry.getValue();
            return active.isEmpty() || !players.contains(s.player)
                    || !Scp173Manager.eligibleObserver(s.player) || s.dimension != s.player.level().dimension();
        });
        for (var p : players) {
            Observer observer = null;
            if (!active.isEmpty() && Scp173Manager.eligibleObserver(p)) {
                observer = OBSERVERS.computeIfAbsent(p.getUUID(), id -> new Observer(p, p.level().dimension(), new Scp173BlinkState()));
                boolean looking = active.stream().anyMatch(statue -> Scp173Vision.sees(p, statue));
                observer.state.tick(p.level().getGameTime(), looking);
            }
        }
        // All observers advance first: overlapping blink windows are independent of player-list order.
        for (var p : active) {
            var statue = STATUES.get(p.getUUID());
            if (statue != null) {
                int watching = observers(p);
                statue.state.tick(watching);
                var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    if (watching > 0) speed.removeModifier(SPEED);
                    else if (speed.getModifier(SPEED) == null) speed.addTransientModifier(new AttributeModifier(SPEED,
                            "SCP-173 movement", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                // Moving a rigid statue makes sound, including externally forced motion.
                if (p.level().getGameTime() >= statue.nextScrape && p.position().distanceToSqr(statue.lastPosition) > 0.0025 && p.onGround()) {
                    p.serverLevel().playSound(null, p.blockPosition(), org.example.maniacrevolution.sound.ModSounds.SCP173_SCRAPE.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.55F, 1);
                    statue.nextScrape = p.level().getGameTime() + 16; // The 0.65s stone drag must not stack every 0.4s.
                }
                statue.lastPosition = p.position();
            }
        }
        for (var p : players) send(p, OBSERVERS.get(p.getUUID()), STATUES.get(p.getUUID()));
    }
    private static void send(ServerPlayer p, Observer observer, Statue statue) {
        long now = p.level().getGameTime();
        boolean survivor = p.isAlive() && p.getTeam() != null && "survivors".equals(p.getTeam().getName())
                && org.example.maniacrevolution.util.PlayerModeUtil.isSurvivalOrAdventure(p);
        ModNetworking.sendToPlayer(new Scp173StatusPacket(p.level().dimension().location(), now,
                observer != null, observer == null ? 0 : observer.state.remaining(now), observer == null ? 1 : observer.state.meter(),
                observer == null ? 0 : observer.state.pressure(), survivor ? blackout(p) : 0,
                statue != null, statue != null && Scp173Manager.held(p), statue == null ? 0 : statue.state.meleeCooldown(now),
                statue == null ? 0 : statue.state.lightCooldown(),
                statue == null ? 10 : statue.state.cost(), statue == null ? 0 : (int) Math.max(0, statue.impactUntil - now),
                statue == null ? 0 : (int) Math.max(0, statue.lightUntil - now)), p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var s = STATUES.remove(event.getEntity().getUUID()); if (s != null) clear(s);
        OBSERVERS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        STATUES.values().forEach(Scp173GameplayManager::clear); STATUES.clear(); OBSERVERS.clear(); authorized = null;
    }
}
