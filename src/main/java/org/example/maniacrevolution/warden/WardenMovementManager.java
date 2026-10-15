package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.event.PenaltySlotManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WardenStaminaPacket;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenMovementManager {
    public static final int JUMP_COOLDOWN = 60;
    private static final UUID SPRINT_CORRECTION = UUID.fromString("3d32c03f-2050-45ae-bafd-b7e5eb707049");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        final ServerPlayer player;
        final Object dimension;
        final WardenStamina stamina = new WardenStamina();
        Vec3 position;
        MobEffectInstance jump;
        Session(ServerPlayer player) { this.player = player; dimension = player.level().dimension(); position = player.position(); }
    }
    private WardenMovementManager() {}
    /** Preserve the native sprint flag/controls, but cancel its additional 30% multiplier. */
    public static void compensateSprint(Player player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean needed = player.isSprinting()
                && (player.hasEffect(ModEffects.WARDEN_WEIGHT.get()) || player.hasEffect(ModEffects.WARDEN_BURST.get()));
        if (needed && speed.getModifier(SPRINT_CORRECTION) == null)
            speed.addTransientModifier(new AttributeModifier(SPRINT_CORRECTION, "Warden native sprint compensation",
                    1.0 / 1.3 - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        else if (!needed) speed.removeModifier(SPRINT_CORRECTION);
    }
    @SubscribeEvent public static void beforeMove(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) compensateSprint(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var session = SESSIONS.get(player.getUUID());
        if (session != null && (session.player != player || !session.dimension.equals(player.level().dimension()))) {
            clear(session); SESSIONS.remove(player.getUUID()); session = null;
        }
        if (!WardenCombatManager.active(player)) {
            if (session != null) { clear(session); SESSIONS.remove(player.getUUID()); }
            // Also clean saved class-specific effects after server restarts.
            player.removeEffect(ModEffects.WARDEN_WEIGHT.get()); player.removeEffect(ModEffects.WARDEN_BURST.get());
            compensateSprint(player); return;
        }
        if (session == null) { session = new Session(player); SESSIONS.put(player.getUUID(), session); }
        var downed = DownedCapability.get(player);
        boolean permitted = (downed == null || downed.getState() != DownedState.DOWNED)
                && !PenaltySlotManager.isInPenaltySlot(player);
        boolean sprint = permitted && player.isSprinting() && !player.isCrouching() && !player.isPassenger()
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable() && !player.isFallFlying();
        double moved = player.position().subtract(session.position).horizontalDistanceSqr();
        session.position = player.position();
        session.stamina.tick(sprint, moved > 1.0E-6 && moved < 4);
        setPace(player, session.stamina.boosting());
        compensateSprint(player);
        ModNetworking.sendToPlayer(new WardenStaminaPacket(player.level().dimension().location(), player.level().getGameTime(),
                session.stamina.amount(), session.stamina.delay(), session.stamina.boosting(), session.stamina.exhausted()), player);
    }
    private static void setPace(ServerPlayer player, boolean boost) {
        var wanted = boost ? ModEffects.WARDEN_BURST.get() : ModEffects.WARDEN_WEIGHT.get();
        var other = boost ? ModEffects.WARDEN_WEIGHT.get() : ModEffects.WARDEN_BURST.get();
        player.removeEffect(other);
        int amplifier = boost ? 59 : 9;
        var current = player.getEffect(wanted);
        if (current != null && current.isInfiniteDuration() && current.getAmplifier() == amplifier && !current.isVisible() && current.showIcon()) return;
        player.removeEffect(wanted);
        player.addEffect(new MobEffectInstance(wanted, -1, amplifier, false, false, true));
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !WardenCombatManager.active(player)
                || player.isInWaterOrBubble() || player.isInLava() || player.onClimbable() || player.hasEffect(ModEffects.JUMP_COOLDOWN.get())) return;
        var session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session(player));
        session.jump = new MobEffectInstance(ModEffects.JUMP_COOLDOWN.get(), JUMP_COOLDOWN, 0, false, false, true);
        player.addEffect(session.jump);
    }
    private static void clear(Session session) {
        var player = session.player;
        player.removeEffect(ModEffects.WARDEN_WEIGHT.get()); player.removeEffect(ModEffects.WARDEN_BURST.get());
        if (session.jump != null && player.getEffect(ModEffects.JUMP_COOLDOWN.get()) == session.jump)
            player.removeEffect(ModEffects.JUMP_COOLDOWN.get());
        compensateSprint(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var session = SESSIONS.remove(event.getEntity().getUUID()); if (session != null) clear(session);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        SESSIONS.values().forEach(WardenMovementManager::clear); SESSIONS.clear();
    }
}
