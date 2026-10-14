package org.example.maniacrevolution.helsing;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.util.ManaUtil;
import org.example.maniacrevolution.util.ManiacDamageAttribution;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class HelsingManager {
    public static final int GESTURE_TICKS = 24;
    private static final Map<UUID, Protection> SHIELDS = new HashMap<>();
    private static final Map<UUID, Long> GESTURES = new HashMap<>();
    private static final Map<UUID, HelsingStudy<HelsingStatePacket>> STUDIES = new HashMap<>();
    private static final Set<UUID> ACTIVE_VIEWERS = new HashSet<>();
    private static final Map<UUID, Long> LAST_TICK_MILLIS = new HashMap<>();
    private static final Map<UUID, Integer> FROZEN_TICKS = new HashMap<>();
    private record Protection(UUID caster, long until) {}
    public static boolean isSurvivor(Entity entity) {
        return entity instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.getTeam() != null && "survivors".equalsIgnoreCase(p.getTeam().getName());
    }
    public static boolean isManiac(Entity entity) {
        return entity instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.getTeam() != null && "maniac".equalsIgnoreCase(p.getTeam().getName());
    }
    public static boolean isHelsing(ServerPlayer player) {
        if (!isSurvivor(player)) return false;
        return PlayerDataManager.get(player).getSurvivorClassId() == HelsingConfig.CLASS_ID;
    }
    public static boolean isFrozen(Player player) {
        return isManiac(player) && (player.hasEffect(ModEffects.HELSING_FREEZE.get())
                || player instanceof ServerPlayer server && FROZEN_TICKS.getOrDefault(player.getUUID(), -1) == server.server.getTickCount());
    }
    public static float costMultiplier(Player player) {
        return isManiac(player) && player.hasEffect(ModEffects.HELSING_RUNE.get()) ? HelsingConfig.RUNE_MULTIPLIER.get().floatValue() : 1.0F;
    }
    private static boolean downed(Player player) {
        var data = DownedCapability.get(player);
        return data != null && data.getState() == DownedState.DOWNED;
    }
    /** A small cone around the reticle, with server-side range, wall and team checks. */
    private static ServerPlayer aimed(ServerPlayer viewer, double range, boolean ally) {
        ServerPlayer best = null; double bestDot = 0.985; double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer p : viewer.serverLevel().players()) {
            if (p == viewer || (ally ? !isSurvivor(p) : !isManiac(p)) || downed(p) || p.isInvisible()) continue;
            double distance = viewer.distanceToSqr(p);
            if (distance > range * range || !viewer.hasLineOfSight(p)) continue;
            Vec3 to = p.getBoundingBox().getCenter().subtract(viewer.getEyePosition());
            double dot = viewer.getLookAngle().dot(to.normalize());
            // Aim at the actual body, allowing a small margin around its silhouette.
            boolean bodyHit = p.getBoundingBox().inflate(0.15).clip(viewer.getEyePosition(), viewer.getEyePosition().add(viewer.getLookAngle().scale(range + 1))).isPresent();
            if ((bodyHit || dot > bestDot) && distance < bestDistance) { best = p; bestDot = dot; bestDistance = distance; }
        }
        return best;
    }
    public static boolean protect(ServerPlayer caster) {
        if (!isHelsing(caster) || downed(caster) || !caster.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HELSING_HAT.get())
                || caster.hasEffect(ModEffects.SILENCE.get()) || caster.hasEffect(ModEffects.STUN.get())) return false;
        if (ArmorAbilityCooldownManager.isOnCooldown(caster, ModItems.HELSING_HAT.get())) return false;
        ServerPlayer ally = aimed(caster, HelsingConfig.ALLY_RANGE.get(), true);
        ServerPlayer target = ally == null ? caster : ally;
        if (!ManaUtil.consumeMana(caster, HelsingConfig.COST.get().floatValue())) {
            caster.displayClientMessage(Component.translatable("message.maniacrev.not_enough_mana", HelsingConfig.COST.get().intValue()), true);
            return false;
        }
        long now = caster.server.getTickCount();
        SHIELDS.put(target.getUUID(), new Protection(caster.getUUID(), now + HelsingConfig.PROTECTION.get()));
        GESTURES.put(caster.getUUID(), now + GESTURE_TICKS);
        ArmorAbilityCooldownManager.setCooldown(caster, ModItems.HELSING_HAT.get(), HelsingConfig.COOLDOWN.get());
        ArmorAbilityCooldownManager.syncToClient(caster, ModItems.HELSING_HAT.get(), 0);
        caster.displayClientMessage(Component.translatable("message.maniacrev.helsing.protected", target.getDisplayName()), true);
        sendSnapshots(caster.server);
        return true;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void weakenDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || event.getAmount() <= 0) return;
        ServerPlayer attacker = ManiacDamageAttribution.peekResponsibleManiac(victim, event.getSource());
        if (isManiac(attacker) && attacker.hasEffect(ModEffects.HELSING_WEAKNESS.get())) {
            event.setAmount(event.getAmount() * (1.0F - HelsingConfig.DAMAGE_REDUCTION.get().floatValue()));
        }
    }
    // At the final damage stage blocked/absorbed hits do not spend the protection.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void transferCurse(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || !isSurvivor(victim) || event.getAmount() <= 0) return;
        Protection shield = SHIELDS.get(victim.getUUID());
        if (shield == null || shield.until <= victim.server.getTickCount()) return;
        ServerPlayer attacker = ManiacDamageAttribution.peekResponsibleManiac(victim, event.getSource());
        if (!isManiac(attacker)) return;
        SHIELDS.remove(victim.getUUID());
        if (!isFrozen(attacker)) LAST_TICK_MILLIS.put(attacker.getUUID(), System.currentTimeMillis());
        attacker.addEffect(new MobEffectInstance(ModEffects.HELSING_WEAKNESS.get(), HelsingConfig.WEAKNESS.get(), 0, false, false, true));
        attacker.addEffect(new MobEffectInstance(ModEffects.HELSING_FREEZE.get(), HelsingConfig.FREEZE.get(), 0, false, false, true));
        var dracula = DraculaIntegration.get(attacker);
        if (dracula != null) dracula.reduceBloodFromNextHits(attacker, HelsingConfig.BLOOD_HITS.get(), 0.30F);
        sendSnapshots(victim.server);
    }
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)) return;
        long now = System.currentTimeMillis();
        long previous = LAST_TICK_MILLIS.getOrDefault(player.getUUID(), now - 50);
        LAST_TICK_MILLIS.put(player.getUUID(), now);
        if (isFrozen(player)) {
            FROZEN_TICKS.put(player.getUUID(), player.server.getTickCount());
            HelsingCooldowns.pause(player, Math.max(0, now - previous));
        }
    }
    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer(); long now = server.getTickCount();
        SHIELDS.entrySet().removeIf(e -> {
            ServerPlayer target = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer caster = server.getPlayerList().getPlayer(e.getValue().caster);
            return e.getValue().until <= now || !isSurvivor(target) || caster == null || !isHelsing(caster) || downed(target);
        });
        GESTURES.entrySet().removeIf(e -> e.getValue() <= now || server.getPlayerList().getPlayer(e.getKey()) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isHelsing(player) || downed(player)) { STUDIES.remove(player.getUUID()); continue; }
            HelsingStudy<HelsingStatePacket> state = STUDIES.computeIfAbsent(player.getUUID(), id -> new HelsingStudy<>());
            ServerPlayer prey = aimed(player, HelsingConfig.STUDY_RANGE.get(), false);
            if (prey != null) {
                state.observe(prey.getUUID(), now);
                if (state.held >= HelsingConfig.STUDY.get() && now % 5 == 0) state.intel = intel(prey);
            } else {
                state.lose(now, HelsingConfig.MEMORY.get());
            }
            if (state.target != null) {
                ServerPlayer target = server.getPlayerList().getPlayer(state.target);
                if (!isManiac(target) || target.level() != player.level() || target.isInvisible()) state.clear();
            }
        }
        if (now % 5 == 0) sendSnapshots(server);
    }
    private static HelsingStatePacket intel(ServerPlayer prey) {
        List<HelsingStatePacket.Readiness> rows = new ArrayList<>(); Set<Item> seen = new HashSet<>();
        List<ItemStack> stacks = new ArrayList<>();
        if (org.example.maniacrevolution.nightmare.NightmareManager.getInstance().isKeeper(prey))
            stacks.add(new ItemStack(ModItems.GUARDIAN_HEAD.get()));
        prey.getArmorSlots().forEach(stacks::add); prey.getInventory().items.forEach(stacks::add);
        stacks.add(prey.getOffhandItem());
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof IItemWithAbility ability && seen.add(stack.getItem())) {
                rows.add(new HelsingStatePacket.Readiness(Component.literal(ability.getAbilityName()), HelsingCooldowns.remainingSeconds(prey, stack, ability)));
            }
        }
        for (var perk : PlayerDataManager.get(prey).getSelectedPerks()) {
            rows.add(new HelsingStatePacket.Readiness(perk.getPerk().getName(), perk.getCooldownRemainingSeconds()));
        }
        var dracula = DraculaIntegration.get(prey);
        float resource = dracula != null ? dracula.blood(prey) : prey.getCapability(ManaProvider.MANA).map(m -> m.getMana()).orElse(0F);
        float maximum = dracula != null ? dracula.maxBlood(prey) : prey.getCapability(ManaProvider.MANA).map(m -> m.getMaxMana()).orElse(0F);
        return new HelsingStatePacket(-1, prey.getId(), prey.getDisplayName(), 1, resource, maximum, dracula != null, rows, Map.of(), Map.of());
    }
    private static void sendSnapshots(net.minecraft.server.MinecraftServer server) {
        long now = server.getTickCount();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            Map<Integer, Integer> shields = new HashMap<>(), gestures = new HashMap<>();
            if (isSurvivor(viewer)) SHIELDS.forEach((id, value) -> {
                ServerPlayer target = server.getPlayerList().getPlayer(id);
                if (target != null && target.level() == viewer.level() && viewer.distanceToSqr(target) <= 4096)
                    shields.put(target.getId(), (int) Math.max(0, value.until - now));
            });
            GESTURES.forEach((id, until) -> {
                ServerPlayer caster = server.getPlayerList().getPlayer(id);
                if (caster != null && caster.level() == viewer.level() && viewer.distanceToSqr(caster) <= 4096)
                    gestures.put(caster.getId(), (int) Math.max(0, until - now));
            });
            int allyId = -1; HelsingStudy<HelsingStatePacket> study = null;
            if (isHelsing(viewer) && !downed(viewer)) {
                ServerPlayer ally = aimed(viewer, HelsingConfig.ALLY_RANGE.get(), true);
                if (ally != null) allyId = ally.getId();
                study = STUDIES.get(viewer.getUUID());
            }
            HelsingStatePacket info = study == null ? null : study.intel;
            int preyId = -1; Component preyName = Component.empty(); float progress = 0;
            if (study != null && study.target != null) {
                ServerPlayer prey = server.getPlayerList().getPlayer(study.target);
                if (prey != null) { preyId = prey.getId(); preyName = prey.getDisplayName(); progress = Math.min(1F, (float) study.held / HelsingConfig.STUDY.get()); }
            }
            boolean active = allyId >= 0 || preyId >= 0 || !shields.isEmpty() || !gestures.isEmpty();
            if (!active && !ACTIVE_VIEWERS.remove(viewer.getUUID())) continue;
            if (active) ACTIVE_VIEWERS.add(viewer.getUUID());
            ModNetworking.sendToPlayer(new HelsingStatePacket(allyId, preyId, preyName, info != null ? 1 : progress,
                    info == null ? -1 : info.resource(), info == null ? 0 : info.maximum(), info != null && info.blood(),
                    info == null ? List.of() : info.abilities(), shields, gestures), viewer);
        }
    }
    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID(); SHIELDS.remove(id); SHIELDS.entrySet().removeIf(e -> e.getValue().caster.equals(id));
        STUDIES.remove(id); GESTURES.remove(id); LAST_TICK_MILLIS.remove(id); FROZEN_TICKS.remove(id);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) { clear(p); ACTIVE_VIEWERS.remove(p.getUUID()); } }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { SHIELDS.clear(); STUDIES.clear(); GESTURES.clear(); LAST_TICK_MILLIS.clear(); FROZEN_TICKS.clear(); ACTIVE_VIEWERS.clear(); }
    private HelsingManager() {}
}
