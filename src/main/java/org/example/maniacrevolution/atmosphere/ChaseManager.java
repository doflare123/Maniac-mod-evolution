package org.example.maniacrevolution.atmosphere;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.PlayerModeUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A chase belongs to a killer/victim pair; clients only receive their own intensity. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ChaseManager {
    private record Pair(UUID killer, UUID survivor) {}
    private static final Map<Pair, ChaseContact> CONTACTS = new HashMap<>();
    private static final Map<UUID, Float> LAST_SENT = new HashMap<>();

    private ChaseManager() {}

    private static boolean participant(ServerPlayer player, String team) {
        var downed = DownedCapability.get(player);
        return player.isAlive() && PlayerModeUtil.isSurvivalOrAdventure(player)
                && player.getTeam() != null && team.equals(player.getTeam().getName())
                && (downed == null || downed.getState() != DownedState.DOWNED);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 5 != 0) return;
        var players = event.getServer().getPlayerList().getPlayers();
        int phase = GameManager.getPhaseValue();
        long tick = event.getServer().getTickCount();
        Map<UUID, Float> intensity = new HashMap<>();
        Set<Pair> valid = new HashSet<>();
        if (phase >= 1 && phase <= 3) {
            for (ServerPlayer killer : players) {
                if (!participant(killer, "maniac")) continue;
                for (ServerPlayer survivor : players) {
                    if (!participant(survivor, "survivors") || killer.level() != survivor.level()) continue;
                    Pair pair = new Pair(killer.getUUID(), survivor.getUUID());
                    valid.add(pair);
                    double distance = killer.distanceToSqr(survivor);
                    var direction = survivor.getEyePosition().subtract(killer.getEyePosition()).normalize();
                    double facing = killer.getLookAngle().dot(direction);
                    boolean visible = distance <= 32 * 32 && facing > 0.15 && killer.hasLineOfSight(survivor);
                    boolean acquiring = visible && distance <= 24 * 24 && facing > 0.65;
                    ChaseContact contact = CONTACTS.get(pair);
                    if (contact == null && acquiring) {
                        contact = new ChaseContact();
                        CONTACTS.put(pair, contact);
                    }
                    if (contact == null) continue;
                    contact.update(tick, acquiring, visible);
                    float strength = contact.strength(tick);
                    intensity.merge(killer.getUUID(), strength, Math::max);
                    intensity.merge(survivor.getUUID(), strength, Math::max);
                    if (!acquiring && strength == 0) CONTACTS.remove(pair);
                }
            }
        }
        CONTACTS.keySet().retainAll(valid);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : players) {
            UUID id = player.getUUID();
            online.add(id);
            float value = intensity.getOrDefault(id, 0F);
            float previous = LAST_SENT.getOrDefault(id, -1F);
            // A half-second heartbeat repairs client world/respawn transitions too.
            if (value != previous || value > 0 && tick % 10 == 0) {
                ModNetworking.sendToPlayer(new ChaseStatePacket(value), player);
                LAST_SENT.put(id, value);
            }
        }
        LAST_SENT.keySet().retainAll(online);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0 || GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3
                || !(event.getEntity() instanceof ServerPlayer survivor)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)
                || killer.level() != survivor.level()
                || !participant(killer, "maniac") || !participant(survivor, "survivors")) return;
        CONTACTS.computeIfAbsent(new Pair(killer.getUUID(), survivor.getUUID()), key -> new ChaseContact())
                .hit(killer.server.getTickCount());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CONTACTS.clear();
        LAST_SENT.clear();
    }
}
