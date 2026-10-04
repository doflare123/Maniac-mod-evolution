package org.example.maniacrevolution.stats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.*;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.*;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.map.MapRegistry;
import org.example.maniacrevolution.perk.PerkRegistry;
import org.example.maniacrevolution.settings.GameSettings;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Server-thread aggregates, match-local anonymous participants and asynchronous upload. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsManager {
    public static final int RESULT_ERROR = -1, RESULT_SKIPPED = -2, RESULT_QUEUED = -3;
    private static final String OPT_OUT = "maniacrev_stats_opt_out";
    private static final int MAX_PLAYERS = 128, MAX_EVENTS = 512;
    private static final List<ResourceLocation> VANILLA_STATS = List.of(Stats.JUMP, Stats.WALK_ONE_CM,
            Stats.SPRINT_ONE_CM, Stats.SWIM_ONE_CM, Stats.FALL_ONE_CM, Stats.FLY_ONE_CM,
            Stats.DAMAGE_DEALT, Stats.DAMAGE_TAKEN, Stats.DAMAGE_ABSORBED, Stats.DAMAGE_BLOCKED_BY_SHIELD,
            Stats.DEATHS, Stats.PLAYER_KILLS, Stats.MOB_KILLS);
    private static Match active;
    private static String sessionId;
    private static MinecraftServer currentServer;
    private static String lastSkipReason = "";

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server; active = null; sessionId = UUID.randomUUID().toString();
        StatsConfig.load(); StatsTransport.start();
    }
    public static void onServerStopping() {
        if (active != null && !active.submitted) submitUnfinished("server_stopping");
        StatsTransport.stop(); active = null; currentServer = null;
    }
    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (active != null && active.endedAt == null && isTrackedTeam(player)
                && !active.roster.contains(player.getUUID())) {
            if (active.roster.size() >= MAX_PLAYERS) { active.quality.add("participant_limit"); return; }
            active.quality.add("late_join");
            active.roster.add(player.getUUID());
            addParticipant(active, player, true);
        }
    }
    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (active == null || active.endedAt != null || !active.roster.contains(player.getUUID())) return;
        active.quality.add("participant_disconnected");
        Participant p = participant(player);
        if (p != null) {
            p.end = playerSnapshot(player); p.delta = vanillaDelta(player, p.baseline);
            p.disconnected = true;
            event("disconnect", player, null, Map.of());
        }
    }
    public static void onGameStarted(MinecraftServer server) {
        if (!StatsConfig.isValid()) return;
        if (active != null && !active.submitted) submitUnfinished("replaced_by_new_match");
        Match m = new Match(); active = m;
        m.settings = settings(server); m.map = map(server); m.catalog = catalog();
        m.startedAt = Instant.now().toString(); m.startNanos = System.nanoTime(); m.startTick = server.getTickCount();
        m.lastPhase = GameManager.getPhaseValue();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isTrackedTeam(player)) continue;
            if (m.roster.size() >= MAX_PLAYERS) { m.quality.add("participant_limit"); break; }
            m.roster.add(player.getUUID());
            if ("survivors".equalsIgnoreCase(player.getTeam().getName())) m.survivors++; else m.maniacs++;
            addParticipant(m, player, false);
        }
        Maniacrev.LOGGER.info("[Stats] Tracking match {}: {} participants", m.id, m.roster.size());
    }
    public static void onGameStopped(MinecraftServer server) { freeze(server, "game_stopped"); }
    public static String getLastSkipReason() { return lastSkipReason; }
    public static boolean optedOut(ServerPlayer player) { return player.getPersistentData().getBoolean(OPT_OUT); }
    public static void optOut(ServerPlayer player) {
        player.getPersistentData().putBoolean(OPT_OUT, true);
        if (active != null) {
            Participant removed = active.players.remove(player.getUUID());
            if (removed != null) active.events.removeIf(e -> removed.key.equals(e.get("actor")) || removed.key.equals(e.get("target")));
            active.quality.add("opted_out_participants");
        }
    }

    public static CompletableFuture<Integer> sendStats(MinecraftServer server, int winner) {
        if (active == null) {
            lastSkipReason = "нет начатого матча; сначала /maniacrev start";
            return CompletableFuture.completedFuture(RESULT_SKIPPED);
        }
        if (active.submitted) return active.submission;
        freeze(server, "result_reported");
        Match match = active;
        Map<String, Object> packet = packet(match, winner);
        match.submission = StatsTransport.send(packet);
        match.submitted = true;
        match.submission.thenAccept(result -> {
            if (result == RESULT_ERROR && currentServer != null) currentServer.execute(() -> match.submitted = false);
        });
        return match.submission;
    }

    private static void submitUnfinished(String reason) {
        if (currentServer == null) return;
        active.quality.add(reason);
        freeze(currentServer, reason);
        active.submission = StatsTransport.send(packet(active, -1)); active.submitted = true;
    }

    private static void addParticipant(Match match, ServerPlayer player, boolean late) {
        if (optedOut(player)) { match.quality.add("opted_out_participants"); return; }
        Participant p = new Participant(); p.key = "p" + (++match.nextPlayer);
        p.team = "survivors".equalsIgnoreCase(player.getTeam().getName()) ? "survivor" : "maniac";
        p.start = playerSnapshot(player); p.baseline = vanilla(player);
        p.position = player.position(); p.dimension = player.level().dimension().location().toString();
        p.lateJoin = late;
        match.players.put(player.getUUID(), p);
    }

    private static Participant participant(ServerPlayer player) {
        return active == null || active.endedAt != null || player == null || optedOut(player) ? null : active.players.get(player.getUUID());
    }
    public static void count(ServerPlayer player, String metric, double value) {
        Participant p = participant(player); if (p != null) p.metrics.add(metric, value);
    }
    public static void perk(ServerPlayer player, String id, String outcome) {
        Participant p = participant(player); if (p == null) return;
        if (p.perks.size() >= 64 && !p.perks.containsKey(id)) return;
        p.perks.computeIfAbsent(id, k -> new StatsMetrics()).add(outcome, 1);
    }
    public static void knockdown(ServerPlayer target, ServerPlayer attacker) {
        count(target, "knockdowns_received", 1); count(attacker, "knockdowns_dealt", 1);
        event("knockdown", attacker, target, Map.of());
    }
    public static void revive(ServerPlayer target, ServerPlayer helper) {
        count(target, "revived", 1); count(helper, "revives_given", 1);
        event("revive", helper, target, Map.of());
    }
    public static void event(String type, ServerPlayer actor, ServerPlayer target, Map<String, Object> data) {
        if (active == null || active.endedAt != null) return;
        Participant a = participant(actor), t = participant(target);
        if (actor != null && a == null || target != null && t == null) return;
        if (active.events.size() >= MAX_EVENTS) { active.droppedEvents++; return; }
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", type); e.put("seconds", elapsed(active));
        if (a != null) e.put("actor", a.key); if (t != null) e.put("target", t.key);
        e.put("data", data); active.events.add(e);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null || active.endedAt != null
                || event.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = event.getServer(); Match m = active;
        double mspt = server.getAverageTickTime();
        m.samples++; m.sumMspt += mspt; m.maxMspt = Math.max(m.maxMspt, mspt);
        if (mspt > 50) m.slowSamples++;
        int phase = GameManager.getPhaseValue();
        if (phase != m.lastPhase) {
            event("phase", null, null, Map.of("from", m.lastPhase, "to", phase));
            m.lastPhase = phase;
            if (phase == 0) { freeze(server, "phase_zero"); return; }
        }
        for (var entry : m.players.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Participant p = entry.getValue();
            if (player == null || optedOut(player)) continue;
            p.metrics.add("sample_count", 1); p.metrics.add("health_sample_sum", player.getHealth());
            p.metrics.add("ping_sample_sum_ms", Math.max(0, player.latency));
            p.metrics.add("food_sample_sum", player.getFoodData().getFoodLevel());
            DownedData d = DownedCapability.get(player);
            String state = d != null && d.getState() == DownedState.DOWNED ? "downed"
                    : player.isSpectator() || !player.isAlive() ? "eliminated" : "alive";
            p.metrics.add("sampled_" + state + "_ticks", 20);
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                p.metrics.add("mana_sample_count", 1); p.metrics.add("mana_sample_sum", mana.getMana());
            });
            Vec3 position = player.position(); String dimension = player.level().dimension().location().toString();
            double distance = position.distanceTo(p.position);
            if (dimension.equals(p.dimension) && distance < 32) p.metrics.add("sampled_distance_blocks", distance);
            else p.metrics.add("teleport_samples", 1);
            p.position = position; p.dimension = dimension;
            p.metrics.add("phase_" + Math.max(0, Math.min(phase, 3)) + "_sampled_ticks", 20);
            String team = player.getTeam() == null ? "none" : player.getTeam().getName();
            if (!("survivor".equals(p.team) ? "survivors" : "maniac").equalsIgnoreCase(team)) m.quality.add("team_changed");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        double amount = Math.min(event.getAmount(), target.getHealth());
        if (amount <= 0) return;
        count(target, "damage_taken_hp", amount); count(target, "hits_received", 1);
        ServerPlayer attacker = org.example.maniacrevolution.util.ManiacDamageAttribution.peekResponsibleManiac(target, event.getSource());
        if (attacker != null) {
            count(attacker, "damage_dealt_to_players_hp", amount); count(attacker, "hits_dealt", 1);
        }
        Participant p = participant(target);
        if (p != null) p.damageTypes.add(event.getSource().getMsgId(), amount);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        count(target, "deaths", 1);
        ServerPlayer killer = org.example.maniacrevolution.util.ManiacDamageAttribution.peekResponsibleManiac(target, event.getSource());
        count(killer, "player_kills", 1);
        event("death", killer, target, Map.of("source", event.getSource().getMsgId()));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) count(p, "healing_requested_hp", Math.min(event.getAmount(), p.getMaxHealth() - p.getHealth()));
    }
    @SubscribeEvent
    public static void usedItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Participant p = participant(player);
            if (p != null) p.items.add(BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).toString(), 1);
        }
    }
    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putBoolean(OPT_OUT, event.getOriginal().getPersistentData().getBoolean(OPT_OUT));
    }

    private static void freeze(MinecraftServer server, String reason) {
        if (active == null || active.endedAt != null) return;
        Match m = active;
        m.durationSeconds = elapsed(m); m.durationTicks = Math.max(0, server.getTickCount() - m.startTick);
        m.endSettings = settings(server); m.computers = HackManager.get().getTotalHacked();
        m.activeSurvivors = 0;
        for (var entry : m.players.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey()); Participant p = entry.getValue();
            if (player != null) {
                p.end = playerSnapshot(player); p.delta = vanillaDelta(player, p.baseline);
                DownedData d = DownedCapability.get(player);
                if ("survivor".equals(p.team) && player.isAlive() && !player.isSpectator()
                        && (d == null || d.getState() != DownedState.DOWNED)) m.activeSurvivors++;
            }
        }
        if (m.durationSeconds < 60) m.quality.add("short_match");
        if (m.survivors == 0 || m.maniacs == 0) m.quality.add("missing_team");
        if (m.players.size() != m.roster.size()) m.quality.add("partial_roster");
        m.endedAt = Instant.now().toString(); m.endReason = reason;
    }
    private static double elapsed(Match m) { return Math.max(0, (System.nanoTime() - m.startNanos) / 1_000_000_000.0); }
    private static Map<String, Object> packet(Match m, int winner) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 2); result.put("matchId", m.id); result.put("sessionId", sessionId);
        result.put("modVersion", ModList.get().getModContainerById(Maniacrev.MODID).map(c -> c.getModInfo().getVersion().toString()).orElse("unknown"));
        result.put("modStatsVersion", StatsVersion.VALUE);
        result.put("minecraftVersion", "1.20.1"); result.put("startedAt", m.startedAt); result.put("endedAt", m.endedAt);
        result.put("winner", winner); result.put("durationSeconds", m.durationSeconds); result.put("durationTicks", m.durationTicks);
        result.put("survivorsCount", m.survivors); result.put("maniacsCount", m.maniacs);
        result.put("activeSurvivorsAtEnd", m.activeSurvivors);
        result.put("endReason", m.endReason); result.put("qualityFlags", new ArrayList<>(m.quality));
        result.put("eligibleForBalance", winner >= 0 && m.quality.isEmpty());
        result.put("map", m.map); result.put("settings", m.settings); result.put("endSettings", m.endSettings);
        result.put("computersCharged", m.computers); result.put("catalog", m.catalog);
        result.put("performance", Map.of("samples", m.samples, "meanMspt", m.samples == 0 ? 0 : m.sumMspt / m.samples,
                "maxMspt", m.maxMspt, "samplesOver50Ms", m.slowSamples));
        List<Map<String, Object>> players = new ArrayList<>();
        for (Participant p : m.players.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", p.key); row.put("team", p.team); row.put("lateJoin", p.lateJoin); row.put("disconnected", p.disconnected);
            row.put("start", p.start); row.put("end", p.end); row.put("metrics", p.metrics.snapshot());
            row.put("vanillaDelta", p.delta); row.put("damageTypes", p.damageTypes.snapshot()); row.put("itemsUsed", p.items.snapshot());
            Map<String, Object> perks = new LinkedHashMap<>(); p.perks.forEach((id, counters) -> perks.put(id, counters.snapshot()));
            row.put("perkCounters", perks); players.add(row);
        }
        result.put("players", players); result.put("events", new ArrayList<>(m.events)); result.put("droppedEvents", m.droppedEvents);
        return result;
    }
    private static Map<String, Object> playerSnapshot(ServerPlayer player) {
        Map<String, Object> out = new LinkedHashMap<>();
        String team = player.getTeam() == null ? "none" : player.getTeam().getName();
        CharacterType type = "survivors".equalsIgnoreCase(team) ? CharacterType.SURVIVOR : CharacterType.MANIAC;
        var data = PlayerDataManager.get(player);
        int raw = type == CharacterType.SURVIVOR ? data.getSurvivorClassId() : data.getManiacClassId();
        var objective = player.getScoreboard().getObjective(type == CharacterType.SURVIVOR ? "SurvivorClass" : "ManiacClass");
        if (objective != null) raw = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
        final int scoreboardId = raw;
        String code = CharacterRegistry.getClassesByType(type).stream().filter(c -> c.getScoreboardId() == scoreboardId)
                .map(CharacterClass::getId).findFirst().orElse("unknown_" + type.name().toLowerCase(Locale.ROOT) + "_" + raw);
        out.put("classCode", code); out.put("classScoreboardId", raw);
        out.put("perks", data.getSelectedPerks().stream().map(p -> p.getPerk().getId()).toList());
        out.put("health", player.getHealth()); out.put("maxHealth", player.getMaxHealth()); out.put("absorption", player.getAbsorptionAmount());
        out.put("food", player.getFoodData().getFoodLevel()); out.put("gameMode", player.gameMode.getGameModeForPlayer().getName());
        out.put("alive", player.isAlive() && !player.isSpectator()); out.put("level", data.getLevel());
        out.put("coins", data.getCoins()); out.put("experience", data.getExperience());
        Map<String, Integer> scoreboard = new LinkedHashMap<>();
        player.getScoreboard().getPlayerScores(player.getScoreboardName()).entrySet().stream().limit(128)
                .forEach(entry -> scoreboard.put(entry.getKey().getName(), entry.getValue().getScore()));
        out.put("scoreboard", scoreboard);
        out.put("effects", player.getActiveEffects().stream().limit(32).map(effect -> Map.of(
                "id", BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString(),
                "amplifier", effect.getAmplifier(), "durationTicks", effect.getDuration())).toList());
        DownedData d = DownedCapability.get(player); out.put("downedState", d == null ? "unknown" : d.getState().name());
        List<Map<String, Object>> equipment = new ArrayList<>();
        for (ItemStack stack : player.getArmorSlots()) equipment.add(item(stack));
        equipment.add(item(player.getMainHandItem())); equipment.add(item(player.getOffhandItem())); out.put("equipment", equipment);
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> { out.put("mana", mana.getMana()); out.put("maxMana", mana.getMaxMana()); });
        return out;
    }
    private static Map<String, Object> item(ItemStack stack) {
        return Map.of("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), "count", stack.getCount(), "damage", stack.getDamageValue());
    }
    private static Map<String, Integer> vanilla(ServerPlayer player) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (ResourceLocation stat : VANILLA_STATS) values.put(stat.toString(), player.getStats().getValue(Stats.CUSTOM.get(stat)));
        return values;
    }
    private static Map<String, Integer> vanillaDelta(ServerPlayer player, Map<String, Integer> baseline) {
        var values = vanilla(player); values.replaceAll((key, value) -> Math.max(0, value - baseline.getOrDefault(key, value))); return values;
    }
    private static Map<String, Object> map(MinecraftServer server) {
        int id = GameSettings.get(server).getSelectedMap(); var obj = server.getScoreboard().getObjective("map");
        if (obj != null) { int score = server.getScoreboard().getOrCreatePlayerScore("Game", obj).getScore(); if (score > 0) id = score; }
        var map = MapRegistry.getMapByNumericId(id);
        return Map.of("numericId", id, "code", map == null ? "unknown_" + id : map.getId(), "name", map == null ? "unknown" : map.getName());
    }
    private static Map<String, Object> settings(MinecraftServer server) {
        var s = GameSettings.get(server); var nbt = s.save(new net.minecraft.nbt.CompoundTag());
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : nbt.getAllKeys()) {
            var value = nbt.get(key);
            if (value instanceof net.minecraft.nbt.NumericTag number) values.put(key, number.getAsNumber());
        }
        values.put("hackPointsRequired", HackConfig.HACK_POINTS_REQUIRED);
        values.put("pointsPerPlayer", HackConfig.POINTS_PER_PLAYER_PER_SECOND);
        values.put("pointsPerSpecialist", HackConfig.POINTS_PER_SPECIALIST_PER_SECOND);
        values.put("computersNeededForWin", HackConfig.COMPUTERS_NEEDED_FOR_WIN);
        return values;
    }
    private static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var perk : PerkRegistry.getAllPerks()) out.add(Map.of("kind", "perk", "code", perk.getId(), "name", perk.getName().getString(),
                "team", perk.getTeam().name().toLowerCase(Locale.ROOT), "metadata", Map.of("type", perk.getType().name(), "cooldownTicks", perk.getCooldownTicks(), "manaCost", perk.getManaCost())));
        for (var character : CharacterRegistry.getAllClasses()) out.add(Map.of("kind", "class", "code", character.getId(), "name", character.getName(),
                "team", character.getType().name().toLowerCase(Locale.ROOT), "metadata", Map.of("scoreboardId", character.getScoreboardId())));
        return out;
    }
    private static boolean isTrackedTeam(ServerPlayer player) {
        return player.getTeam() != null && ("survivors".equalsIgnoreCase(player.getTeam().getName()) || "maniac".equalsIgnoreCase(player.getTeam().getName()));
    }
    private static final class Match {
        final String id = UUID.randomUUID().toString();
        final Set<UUID> roster = new HashSet<>(); final Map<UUID, Participant> players = new LinkedHashMap<>();
        final Set<String> quality = new LinkedHashSet<>(); final List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> settings, endSettings, map; List<Map<String, Object>> catalog;
        String startedAt, endedAt, endReason; long startNanos; double durationSeconds, sumMspt, maxMspt;
        int startTick, durationTicks, survivors, maniacs, activeSurvivors, computers, samples, slowSamples, lastPhase, droppedEvents, nextPlayer;
        boolean submitted; CompletableFuture<Integer> submission;
    }
    private static final class Participant {
        String key, team, dimension; Vec3 position; boolean disconnected, lateJoin;
        Map<String, Object> start, end = Map.of(); Map<String, Integer> baseline, delta = Map.of();
        final StatsMetrics metrics = new StatsMetrics(), damageTypes = new StatsMetrics(), items = new StatsMetrics();
        final Map<String, StatsMetrics> perks = new LinkedHashMap<>();
    }
    private StatsManager() {}
}
