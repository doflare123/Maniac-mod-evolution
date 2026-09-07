package org.example.maniacrevolution.roseglasses;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.item.RoseColoredGlassesItem;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.survivor.RoseColoredGlassesPerk;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class RoseColoredGlassesManager {
    private static final int DISPLAY_EFFECT_DURATION_TICKS = 10;
    private static final int DISPLAY_EFFECT_REFRESH_THRESHOLD_TICKS = 6;
    private static final float MINIMUM_DAMAGE = 0.0F;
    private static final float ACTIVATION_SOUND_VOLUME = 0.65F;
    private static final float ACTIVATION_SOUND_PITCH = 1.65F;
    private static final float BREAK_SOUND_VOLUME = 0.9F;
    private static final float BREAK_SOUND_PITCH = 1.2F;

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    private static long lastBeginTick = Long.MIN_VALUE;
    private static boolean matchInitialized;

    private RoseColoredGlassesManager() {
    }

    public static void beginForOwner(ServerPlayer owner) {
        long gameTick = owner.server.overworld().getGameTime();
        if (!matchInitialized || lastBeginTick != gameTick) {
            clearMatch(owner.server);
            matchInitialized = true;
            lastBeginTick = gameTick;
        }

        removeGlasses(owner);
        ItemStack glasses = RoseColoredGlassesItem.createFor(owner);
        if (!owner.getInventory().add(glasses)) {
            owner.containerMenu.setCarried(glasses);
        }
        owner.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        MinecraftServer server = event.getServer();
        int phase = GameManager.getPhaseValue();
        if (phase == 0) {
            if (matchInitialized) {
                clearMatch(server);
            }
            return;
        }

        enforceBoundItems(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, phase);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= MINIMUM_DAMAGE
                || !player.hasEffect(ModEffects.ROSE_COLORED_GLASSES.get())
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                || event.getSource().is(DamageTypes.GENERIC_KILL)) {
            return;
        }

        int phase = GameManager.getPhaseValue();
        ItemStack glasses = player.getOffhandItem();
        if (!isActivePhase(phase)
                || !glasses.is(ModItems.ROSE_COLORED_GLASSES.get())
                || !RoseColoredGlassesItem.belongsTo(glasses, player.getUUID())) {
            return;
        }

        float incomingDamage = event.getAmount();
        float fullyReducedDamage = Math.max(RoseColoredGlassesPerk.MINIMUM_FINAL_DAMAGE,
                (float) Math.floor(incomingDamage * RoseColoredGlassesPerk.DAMAGE_MULTIPLIER));
        float requestedReduction = incomingDamage - fullyReducedDamage;
        if (requestedReduction <= MINIMUM_DAMAGE) {
            return;
        }

        int remainingDurability = glasses.getMaxDamage() - glasses.getDamageValue();
        if (remainingDurability <= 0) {
            return;
        }

        int fullDurabilityCost = Math.max(RoseColoredGlassesPerk.MINIMUM_DURABILITY_COST,
                (int) Math.floor(requestedReduction));
        float appliedReduction;
        int durabilityCost;
        if (remainingDurability >= fullDurabilityCost) {
            appliedReduction = requestedReduction;
            durabilityCost = fullDurabilityCost;
        } else {
            appliedReduction = Math.min(requestedReduction, remainingDurability);
            durabilityCost = remainingDurability;
        }

        event.setAmount(Math.max(RoseColoredGlassesPerk.MINIMUM_FINAL_DAMAGE,
                incomingDamage - appliedReduction));
        int newDamage = glasses.getDamageValue() + durabilityCost;
        if (newDamage >= glasses.getMaxDamage()) {
            glasses.shrink(1);
            player.broadcastBreakEvent(net.minecraft.world.InteractionHand.OFF_HAND);
        } else {
            glasses.setDamageValue(newDamage);
        }

        if (glasses.isEmpty()) {
            player.removeEffect(ModEffects.ROSE_COLORED_GLASSES.get());
            ACTIVE_PLAYERS.remove(player.getUUID());
            player.playNotifySound(SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                    BREAK_SOUND_VOLUME, BREAK_SOUND_PITCH);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGlassesToss(ItemTossEvent event) {
        if (!event.getEntity().getItem().is(ModItems.ROSE_COLORED_GLASSES.get())) {
            return;
        }
        event.setCanceled(true);
        ItemStack stack = event.getEntity().getItem();
        if (!event.getPlayer().getInventory().add(stack)) {
            event.getPlayer().containerMenu.setCarried(stack);
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recoverGlassesFromOpenMenu(player);
            recoverCarriedGlasses(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ACTIVE_PLAYERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE_PLAYERS.clear();
        matchInitialized = false;
        lastBeginTick = Long.MIN_VALUE;
    }

    private static void tickPlayer(ServerPlayer player, int phase) {
        UUID playerId = player.getUUID();
        ItemStack offhand = player.getOffhandItem();
        boolean canActivate = isActivePhase(phase)
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                && PerkTeam.fromPlayer(player) == PerkTeam.SURVIVOR
                && PlayerDataManager.get(player).getPerkInstance(RoseColoredGlassesPerk.ID) != null
                && offhand.is(ModItems.ROSE_COLORED_GLASSES.get())
                && RoseColoredGlassesItem.belongsTo(offhand, playerId);

        if (!canActivate) {
            deactivate(player);
            return;
        }

        MobEffectInstance current = player.getEffect(ModEffects.ROSE_COLORED_GLASSES.get());
        if (current == null || current.getDuration() <= DISPLAY_EFFECT_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.ROSE_COLORED_GLASSES.get(),
                    DISPLAY_EFFECT_DURATION_TICKS,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (ACTIVE_PLAYERS.add(playerId)) {
            player.playNotifySound(SoundEvents.GLASS_PLACE, SoundSource.PLAYERS,
                    ACTIVATION_SOUND_VOLUME, ACTIVATION_SOUND_PITCH);
        }
    }

    private static void deactivate(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ACTIVE_PLAYERS.remove(playerId);
        player.removeEffect(ModEffects.ROSE_COLORED_GLASSES.get());
    }

    private static boolean isActivePhase(int phase) {
        return phase == RoseColoredGlassesPerk.FIRST_ACTIVE_PHASE.getScoreboardValue()
                || phase == RoseColoredGlassesPerk.LAST_ACTIVE_PHASE.getScoreboardValue();
    }

    private static void enforceBoundItems(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ModItems.ROSE_COLORED_GLASSES.get())) continue;
                UUID ownerId = RoseColoredGlassesItem.getOwner(stack);
                if (ownerId == null || ownerId.equals(player.getUUID())) continue;

                ItemStack recovered = stack.copy();
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                returnToOwner(server, ownerId, recovered);
            }
            recoverGlassesFromOpenMenu(player);
            recoverCarriedGlasses(player);
        }
    }

    private static void recoverCarriedGlasses(ServerPlayer viewer) {
        ItemStack carried = viewer.containerMenu.getCarried();
        if (!carried.is(ModItems.ROSE_COLORED_GLASSES.get())) return;

        viewer.containerMenu.setCarried(ItemStack.EMPTY);
        UUID ownerId = RoseColoredGlassesItem.getOwner(carried);
        if (ownerId != null) {
            returnToOwner(viewer.server, ownerId, carried.copy());
        }
    }

    private static void recoverGlassesFromOpenMenu(ServerPlayer viewer) {
        for (net.minecraft.world.inventory.Slot slot : viewer.containerMenu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()
                    || !slot.getItem().is(ModItems.ROSE_COLORED_GLASSES.get())) {
                continue;
            }
            ItemStack recovered = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);
            UUID ownerId = RoseColoredGlassesItem.getOwner(recovered);
            if (ownerId != null) {
                returnToOwner(viewer.server, ownerId, recovered);
            }
        }
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerId, ItemStack stack) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null && !owner.getInventory().add(stack)) {
            owner.containerMenu.setCarried(stack);
        }
    }

    private static void removeGlasses(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.ROSE_COLORED_GLASSES.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().is(ModItems.ROSE_COLORED_GLASSES.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        deactivate(player);
    }

    private static void clearMatch(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeGlasses(player);
        }
        ACTIVE_PLAYERS.clear();
        matchInitialized = false;
    }
}
