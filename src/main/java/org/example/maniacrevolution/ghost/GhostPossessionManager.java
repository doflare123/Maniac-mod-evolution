package org.example.maniacrevolution.ghost;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncGhostPossessionPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class GhostPossessionManager {
    public static final int GHOST_CLASS_ID = 13;
    private static final int POSSESSION_DURATION_TICKS = 8 * 20;
    private static final int POSSESSION_COOLDOWN_TICKS = 25 * 20;

    private static final Map<UUID, PossessionState> ACTIVE_POSSESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> TARGET_TO_POSSESSOR = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private static boolean redirectingDamage = false;
    private GhostPossessionManager() {
    }

    public static boolean startPossession(ServerPlayer possessor, ServerPlayer target) {
        return startPossession(possessor, target, false);
    }

    public static boolean startPossession(ServerPlayer possessor, ServerPlayer target, boolean ignoreCooldown) {
        if (possessor == null || target == null) {
            return false;
        }

        if (!isGhostClass(possessor)) {
            possessor.displayClientMessage(Component.literal("§cЭта способность доступна только классу Призрак."), true);
            return false;
        }

        if (!isValidTarget(target)) {
            possessor.displayClientMessage(Component.literal("§cЭтого выжившего сейчас нельзя захватить."), true);
            return false;
        }

        if (isPossessing(possessor)) {
            possessor.displayClientMessage(Component.literal("§cВы уже управляете чужим телом."), true);
            return false;
        }

        if (isPossessed(target)) {
            possessor.displayClientMessage(Component.literal("§cЭта цель уже захвачена другим Призраком."), true);
            return false;
        }

        if (possessor.getServer() == null) {
            return false;
        }

        long now = possessor.getServer().getTickCount();
        long cooldownUntil = COOLDOWN_UNTIL.getOrDefault(possessor.getUUID(), 0L);
        if (!ignoreCooldown && cooldownUntil > now) {
            long remainingSeconds = (cooldownUntil - now + 19) / 20;
            possessor.displayClientMessage(Component.literal("§cВселение на перезарядке: " + remainingSeconds + "с"), true);
            return false;
        }

        ACTIVE_POSSESSIONS.put(possessor.getUUID(), new PossessionState(target.getUUID(), now + POSSESSION_DURATION_TICKS));
        TARGET_TO_POSSESSOR.put(target.getUUID(), possessor.getUUID());
        COOLDOWN_UNTIL.put(possessor.getUUID(), now + POSSESSION_COOLDOWN_TICKS);

        syncTargetToPossessor(possessor, target);
        applyPossessorEffects(possessor);
        applyTargetEffects(target);
        syncClientState(possessor, true, true, target.getId());
        syncClientState(target, true, false, -1);

        possessor.displayClientMessage(Component.literal("§dВы вселились в " + target.getName().getString()), false);
        target.displayClientMessage(Component.literal("§5Ваше тело захватил Призрак!"), false);
        return true;
    }

    public static boolean releasePossession(ServerPlayer possessor, String reason) {
        if (possessor == null) {
            return false;
        }

        PossessionState state = ACTIVE_POSSESSIONS.remove(possessor.getUUID());
        if (state == null) {
            return false;
        }

        TARGET_TO_POSSESSOR.remove(state.targetUuid());

        finishPossession(possessor, state, reason);
        return true;
    }

    public static boolean isPossessing(Player player) {
        return player != null && ACTIVE_POSSESSIONS.containsKey(player.getUUID());
    }

    public static boolean isPossessed(Player player) {
        return player != null && TARGET_TO_POSSESSOR.containsKey(player.getUUID());
    }

    public static String getStatus(Player player) {
        if (player == null) {
            return "нет игрока";
        }

        if (isPossessing(player)) {
            PossessionState state = ACTIVE_POSSESSIONS.get(player.getUUID());
            return "контролирует " + state.targetUuid();
        }

        if (isPossessed(player)) {
            return "находится под контролем";
        }

        return "свободен";
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isPossessed(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer possessor)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }
        if (isPossessing(possessor)) {
            ServerPlayer possessedTarget = getPossessedTargetInternal(possessor);
            if (possessedTarget != null && target != possessedTarget) {
                possessedTarget.swing(event.getHand(), true);
            }
            return;
        }
        if (!possessor.isShiftKeyDown()) {
            return;
        }
        if (!isGhostClass(possessor)) {
            return;
        }

        event.setCanceled(true);
        startPossession(possessor, target);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        long now = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, PossessionState>> iterator = ACTIVE_POSSESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PossessionState> entry = iterator.next();
            ServerPlayer possessor = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (possessor == null) {
                TARGET_TO_POSSESSOR.remove(entry.getValue().targetUuid());
                iterator.remove();
                continue;
            }

            ServerPlayer target = event.getServer().getPlayerList().getPlayer(entry.getValue().targetUuid());
            if (target == null || !isValidTarget(target) || !isGhostClass(possessor) || possessor.isSpectator()) {
                iterator.remove();
                TARGET_TO_POSSESSOR.remove(entry.getValue().targetUuid());
                finishPossession(possessor, entry.getValue(), "цель больше недоступна");
                continue;
            }

            if (now >= entry.getValue().endTick()) {
                iterator.remove();
                TARGET_TO_POSSESSOR.remove(entry.getValue().targetUuid());
                finishPossession(possessor, entry.getValue(), "время вышло");
                continue;
            }

            applyPossessorEffects(possessor);
            applyTargetEffects(target);
            syncTargetToPossessor(possessor, target);
        }
    }

    @SubscribeEvent
    public static void onPossessedPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!isPossessed(player)) {
            return;
        }

        UUID possessorUuid = TARGET_TO_POSSESSOR.get(player.getUUID());
        ServerPlayer possessor = possessorUuid != null && player.getServer() != null
                ? player.getServer().getPlayerList().getPlayer(possessorUuid)
                : null;

        player.setDeltaMovement(Vec3.ZERO);
        player.setShiftKeyDown(possessor != null && possessor.isShiftKeyDown());
        player.setSprinting(possessor != null && possessor.isSprinting());
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isPossessed(event.getEntity())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer possessor && isPossessing(possessor)) {
            ServerPlayer target = getPossessedTargetInternal(possessor);
            if (target != null) {
                target.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isPossessed(event.getEntity())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer possessor && isPossessing(possessor)) {
            ServerPlayer target = getPossessedTargetInternal(possessor);
            if (target != null) {
                target.swing(event.getHand(), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPossessed(event.getEntity())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer possessor && isPossessing(possessor)) {
            ServerPlayer target = getPossessedTargetInternal(possessor);
            if (target != null) {
                target.swing(event.getHand(), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isPossessed(event.getEntity())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer possessor && isPossessing(possessor)) {
            ServerPlayer target = getPossessedTargetInternal(possessor);
            if (target != null) {
                target.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (redirectingDamage) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer possessor)) {
            return;
        }
        if (!isPossessing(possessor)) {
            return;
        }

        PossessionState state = ACTIVE_POSSESSIONS.get(possessor.getUUID());
        if (state == null || possessor.getServer() == null) {
            return;
        }

        ServerPlayer target = possessor.getServer().getPlayerList().getPlayer(state.targetUuid());
        if (target == null) {
            return;
        }

        event.setCanceled(true);
        redirectingDamage = true;
        try {
            target.hurt(event.getSource(), event.getAmount());
        } finally {
            redirectingDamage = false;
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (isPossessing(player)) {
            releasePossession(player, "носитель погиб");
            return;
        }

        UUID possessorUuid = TARGET_TO_POSSESSOR.get(player.getUUID());
        if (possessorUuid == null || player.getServer() == null) {
            return;
        }

        ServerPlayer possessor = player.getServer().getPlayerList().getPlayer(possessorUuid);
        if (possessor != null) {
            releasePossession(possessor, "жертва погибла");
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (isPossessing(player)) {
            releasePossession(player, "игрок вышел");
            return;
        }

        UUID possessorUuid = TARGET_TO_POSSESSOR.get(player.getUUID());
        if (possessorUuid == null || player.getServer() == null) {
            return;
        }

        ServerPlayer possessor = player.getServer().getPlayerList().getPlayer(possessorUuid);
        if (possessor != null) {
            releasePossession(possessor, "цель вышла");
        }
    }

    private static boolean isGhostClass(ServerPlayer player) {
        Team team = player.getTeam();
        if (team == null || !"maniac".equalsIgnoreCase(team.getName())) {
            return false;
        }

        Objective objective = player.getScoreboard().getObjective("ManiacClass");
        if (objective == null) {
            return false;
        }

        int classId = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
        return classId == GHOST_CLASS_ID;
    }

    private static boolean isValidTarget(ServerPlayer target) {
        if (target.isCreative() || target.isSpectator() || target.isDeadOrDying()) {
            return false;
        }

        Team team = target.getTeam();
        if (team == null || !"survivors".equalsIgnoreCase(team.getName())) {
            return false;
        }

        DownedData downedData = DownedCapability.get(target);
        return downedData == null || downedData.getState() != DownedState.DOWNED;
    }

    private static void syncTargetToPossessor(ServerPlayer possessor, ServerPlayer target) {
        target.moveTo(
                possessor.getX(),
                possessor.getY(),
                possessor.getZ(),
                possessor.getYRot(),
                possessor.getXRot()
        );
        target.connection.teleport(
                possessor.getX(),
                possessor.getY(),
                possessor.getZ(),
                possessor.getYRot(),
                possessor.getXRot()
        );
        target.setYHeadRot(possessor.getYHeadRot());
        target.yBodyRot = possessor.yBodyRot;
        target.setDeltaMovement(possessor.getDeltaMovement());
        target.setShiftKeyDown(possessor.isShiftKeyDown());
        target.setSprinting(possessor.isSprinting());
    }

    private static void applyPossessorEffects(ServerPlayer possessor) {
        possessor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
    }

    private static void clearPossessorEffects(ServerPlayer possessor) {
        possessor.removeEffect(MobEffects.INVISIBILITY);
    }

    private static void applyTargetEffects(ServerPlayer target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 255, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 255, false, false));
    }

    private static void clearTargetEffects(ServerPlayer target) {
        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(MobEffects.JUMP);
        target.removeEffect(MobEffects.WEAKNESS);
    }

    private static void syncClientState(ServerPlayer player, boolean active, boolean controller, int targetEntityId) {
        ModNetworking.sendToPlayer(new SyncGhostPossessionPacket(active, controller, targetEntityId), player);
    }

    private static void finishPossession(ServerPlayer possessor, PossessionState state, String reason) {
        ServerPlayer target = possessor.getServer() != null
                ? possessor.getServer().getPlayerList().getPlayer(state.targetUuid())
                : null;

        syncClientState(possessor, false, false, -1);
        clearPossessorEffects(possessor);
        possessor.displayClientMessage(Component.literal("§7Вселение завершено" + (reason == null || reason.isBlank() ? "" : ": " + reason)), true);

        if (target != null) {
            syncClientState(target, false, false, -1);
            clearTargetEffects(target);
            target.displayClientMessage(Component.literal("§aВы снова контролируете себя."), true);
        }
    }

    private static ServerPlayer getPossessedTargetInternal(ServerPlayer possessor) {
        if (possessor == null || possessor.getServer() == null) {
            return null;
        }

        PossessionState state = ACTIVE_POSSESSIONS.get(possessor.getUUID());
        if (state == null) {
            return null;
        }

        return possessor.getServer().getPlayerList().getPlayer(state.targetUuid());
    }

    private record PossessionState(UUID targetUuid, long endTick) {
    }
}
