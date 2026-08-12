package org.example.maniacrevolution.colorroulette;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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
import org.example.maniacrevolution.entity.RedColorCardProjectile;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.item.ColorCardItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ColorRouletteComputerMarkersPacket;
import org.example.maniacrevolution.network.packets.ColorRouletteStatePacket;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.common.ColorRoulettePerk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Authoritative server state and card actions for Color Roulette. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ColorRouletteManager {
    public static final int CARD_LIFETIME_TICKS =
            ColorRoulettePerk.CARD_LIFETIME_SECONDS * 20;
    public static final int RESULT_ANIMATION_TICKS = 16;
    private static final int GUIDANCE_TICKS =
            ColorRoulettePerk.RED_GUIDANCE_SECONDS * 20;
    private static final float COMPUTER_FRACTION =
            ColorRoulettePerk.COMPUTER_PERCENT / 100.0F;
    private static final Map<UUID, Spin> SPINS = new ConcurrentHashMap<>();

    private ColorRouletteManager() {}

    public static boolean canStart(ServerPlayer player) {
        return !SPINS.containsKey(player.getUUID())
                && player.getInventory().getFreeSlot() >= 0
                && !hasAnyCard(player);
    }

    public static Component getStartFailure(ServerPlayer player) {
        if (SPINS.containsKey(player.getUUID())) {
            return Component.translatable("message.maniacrev.color_roulette.already_spinning");
        }
        if (hasAnyCard(player)) {
            return Component.translatable("message.maniacrev.color_roulette.card_exists");
        }
        return Component.translatable("message.maniacrev.color_roulette.inventory_full");
    }

    public static void start(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        ColorCard initial = ColorCard.values()[player.getRandom().nextInt(ColorCard.values().length)];
        SPINS.put(player.getUUID(), new Spin(now, initial));
        ModNetworking.sendToPlayer(ColorRouletteStatePacket.start(initial), player);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                0.75F, 1.25F);
    }

    public static boolean isRolling(ServerPlayer player) {
        return SPINS.containsKey(player.getUUID());
    }

    public static void stop(ServerPlayer player, ColorCard selected) {
        Spin spin = SPINS.get(player.getUUID());
        if (spin == null) return;
        long age = player.serverLevel().getGameTime() - spin.startedAt();
        if (age < ColorRoulettePerk.INTRO_TICKS
                || age > ColorRoulettePerk.INTRO_TICKS + ColorRoulettePerk.SPIN_TICKS + 5L) {
            return;
        }
        award(player, selected);
    }

    private static void award(ServerPlayer player, ColorCard selected) {
        if (SPINS.remove(player.getUUID()) == null) return;
        ItemStack card = ColorCardItem.createFor(player, selected);
        if (!player.getInventory().add(card)) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.inventory_full"), true);
            ModNetworking.sendToPlayer(ColorRouletteStatePacket.clear(), player);
            return;
        }
        PerkInstance instance = PlayerDataManager.get(player)
                .getPerkInstance(ColorRoulettePerk.ID);
        if (instance != null) {
            instance.setCooldownRemaining(ColorRoulettePerk.COOLDOWN_SECONDS * 20);
            PlayerDataManager.syncToClient(player);
        }
        player.inventoryMenu.broadcastChanges();
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                1.0F, 1.35F);
        player.displayClientMessage(Component.translatable(
                "message.maniacrev.color_roulette.won." + selected.name().toLowerCase()), true);
        ModNetworking.sendToPlayer(ColorRouletteStatePacket.result(selected), player);
    }

    /** Handles red and green card use in air. Blue is handled by ComputerBlock. */
    public static boolean useAirCard(ServerPlayer player, ColorCard card, ItemStack stack) {
        if (!validateCard(player, stack)) return false;
        PerkTeam team = PerkTeam.fromPlayer(player);
        if (team == null) return false;
        if (card == ColorCard.BLUE) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.use_on_computer"), true);
            return false;
        }

        if (card == ColorCard.GREEN) {
            if (team == PerkTeam.SURVIVOR) {
                player.heal(ColorRoulettePerk.SURVIVOR_HEAL_HP);
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                        0.65F, 1.7F);
            } else {
                MobEffectInstance current = player.getEffect(ModEffects.GREEN_CHARGE.get());
                int stacks = current == null ? 1 : current.getAmplifier() + 2;
                player.addEffect(new MobEffectInstance(ModEffects.GREEN_CHARGE.get(), -1,
                        stacks - 1, false, true, true));
                player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            consume(stack);
            return true;
        }

        if (team == PerkTeam.SURVIVOR) {
            RedColorCardProjectile projectile = new RedColorCardProjectile(player.level(), player);
            projectile.setItem(stack.copy());
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, 1.5F, 0.0F);
            player.level().addFreshEntity(projectile);
            player.playNotifySound(SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                    0.8F, 0.8F);
        } else {
            activateGuidance(player);
        }
        consume(stack);
        return true;
    }

    public static boolean useBlueCard(ServerPlayer player, BlockPos pos,
                                      ComputerBlockEntity computer, ItemStack stack) {
        if (!stack.is(ModItems.BLUE_COLOR_CARD.get()) || !validateCard(player, stack)) {
            return false;
        }
        PerkTeam team = PerkTeam.fromPlayer(player);
        boolean used = team == PerkTeam.SURVIVOR
                ? HackManager.get().addColorCardProgress(player, pos, COMPUTER_FRACTION)
                : team == PerkTeam.MANIAC
                && HackManager.get().rollbackColorCardProgress(player, pos, COMPUTER_FRACTION);
        if (!used) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.computer_unavailable"), true);
            return false;
        }
        consume(stack);
        ServerLevel level = player.serverLevel();
        Vector3f rgb = team == PerkTeam.SURVIVOR
                ? new Vector3f(0.12F, 0.64F, 1.0F)
                : new Vector3f(0.08F, 0.24F, 0.55F);
        level.sendParticles(new DustParticleOptions(rgb, 1.3F),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                26, 0.35D, 0.45D, 0.35D, 0.02D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.9F, team == PerkTeam.SURVIVOR ? 1.45F : 0.65F);
        return true;
    }

    private static void activateGuidance(ServerPlayer source) {
        ServerLevel level = source.serverLevel();
        HackManager hacks = HackManager.get();
        Set<Integer> seenIds = new HashSet<>();
        List<ComputerTarget> targets = new ArrayList<>();
        for (BlockPos pos : ComputerBlockEntity.getTrackedPositionsSnapshot()) {
            if (!(level.getBlockEntity(pos) instanceof ComputerBlockEntity computer)) continue;
            int id = computer.getComputerId();
            if (!seenIds.add(id) || hacks.isHacked(id)) continue;
            targets.add(new ComputerTarget(pos.immutable(), hacks.getLiveProgress(id)));
        }
        List<BlockPos> positions = targets.stream()
                .sorted(Comparator.comparingDouble(ComputerTarget::progress).reversed())
                .limit(ColorRoulettePerk.TOP_COMPUTERS_REVEALED)
                .map(ComputerTarget::pos)
                .toList();

        ColorRouletteComputerMarkersPacket packet =
                new ColorRouletteComputerMarkersPacket(positions, GUIDANCE_TICKS);
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
            if (player.level() == level && PerkTeam.fromPlayer(player) == PerkTeam.MANIAC
                    && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                player.addEffect(new MobEffectInstance(ModEffects.RED_GUIDANCE.get(),
                        GUIDANCE_TICKS, 0, false, true, true));
                ModNetworking.sendToPlayer(packet, player);
            }
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.04F, 0.08F), 1.4F),
                source.getX(), source.getY() + 1.0D, source.getZ(),
                32, 0.45D, 0.7D, 0.45D, 0.04D);
    }

    private static boolean validateCard(ServerPlayer player, ItemStack stack) {
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.inactive_phase"), true);
            return false;
        }
        if (!ColorCardItem.belongsTo(stack, player.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.maniacrev.color_roulette.not_owner"), true);
            return false;
        }
        return ColorCardItem.getRemainingTicks(stack) > 0;
    }

    private static void consume(ItemStack stack) {
        stack.shrink(1);
    }

    private static boolean hasAnyCard(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isCard(player.getInventory().getItem(slot))) return true;
        }
        return false;
    }

    private static boolean isCard(ItemStack stack) {
        return stack.is(ModItems.RED_COLOR_CARD.get())
                || stack.is(ModItems.BLUE_COLOR_CARD.get())
                || stack.is(ModItems.GREEN_COLOR_CARD.get());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (GameManager.getPhaseValue() == 0) {
            clearAll(server);
            return;
        }
        for (UUID id : new ArrayList<>(SPINS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Spin spin = SPINS.get(id);
            if (player == null || spin == null || !player.isAlive()
                    || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                SPINS.remove(id);
                if (player != null) {
                    ModNetworking.sendToPlayer(ColorRouletteStatePacket.clear(), player);
                }
                continue;
            }
            long age = player.serverLevel().getGameTime() - spin.startedAt();
            if (age >= ColorRoulettePerk.INTRO_TICKS + ColorRoulettePerk.SPIN_TICKS) {
                ColorCard automatic = ColorCard.values()[player.getRandom()
                        .nextInt(ColorCard.values().length)];
                award(player, automatic);
            }
        }
        tickAndEnforceCards(server);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || PerkTeam.fromPlayer(attacker) != PerkTeam.MANIAC
                || GameManager.getPhaseValue() < 1 || GameManager.getPhaseValue() > 3) {
            return;
        }
        MobEffectInstance charge = attacker.getEffect(ModEffects.GREEN_CHARGE.get());
        if (charge == null) return;
        int stacks = charge.getAmplifier() + 1;
        event.setAmount(event.getAmount()
                + stacks * ColorRoulettePerk.GREEN_DAMAGE_PER_STACK);
        attacker.removeEffect(ModEffects.GREEN_CHARGE.get());
        attacker.playNotifySound(SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 1.0F, 1.1F);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (SPINS.remove(player.getUUID()) != null) {
                ModNetworking.sendToPlayer(ColorRouletteStatePacket.clear(), player);
            }
            removeCards(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SPINS.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!isCard(stack)) return;
        event.setCanceled(true);
        if (!event.getPlayer().getInventory().add(stack.copy())) {
            event.getPlayer().containerMenu.setCarried(stack.copy());
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recoverExternalCards(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getDrops().removeIf(drop -> isCard(drop.getItem()));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SPINS.clear();
    }

    private static void tickAndEnforceCards(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!isCard(stack)) continue;
                UUID owner = ColorCardItem.getOwner(stack);
                if (owner == null || !owner.equals(player.getUUID())) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    if (owner != null) returnToOwner(server, owner, stack.copy());
                    continue;
                }
                int remaining = ColorCardItem.getRemainingTicks(stack) - 1;
                if (remaining <= 0) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    player.displayClientMessage(Component.translatable(
                            "message.maniacrev.color_roulette.card_expired"), true);
                } else {
                    ColorCardItem.setRemainingTicks(stack, remaining);
                }
            }
            recoverExternalCards(player);
        }
    }

    private static void recoverExternalCards(ServerPlayer viewer) {
        for (net.minecraft.world.inventory.Slot slot : viewer.containerMenu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()
                    || !isCard(slot.getItem())) continue;
            ItemStack recovered = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);
            UUID owner = ColorCardItem.getOwner(recovered);
            if (owner != null) returnToOwner(viewer.server, owner, recovered);
        }
        ItemStack carried = viewer.containerMenu.getCarried();
        if (isCard(carried)) {
            viewer.containerMenu.setCarried(ItemStack.EMPTY);
            UUID owner = ColorCardItem.getOwner(carried);
            if (owner != null) returnToOwner(viewer.server, owner, carried.copy());
        }
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerId, ItemStack stack) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null && !hasAnyCard(owner)) {
            if (!owner.getInventory().add(stack)) owner.containerMenu.setCarried(stack);
            owner.inventoryMenu.broadcastChanges();
        }
    }

    private static void removeCards(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isCard(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        if (isCard(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void clearAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SPINS.remove(player.getUUID()) != null) {
                ModNetworking.sendToPlayer(ColorRouletteStatePacket.clear(), player);
            }
            removeCards(player);
            player.removeEffect(ModEffects.RED_GUIDANCE.get());
            player.removeEffect(ModEffects.GREEN_CHARGE.get());
        }
    }

    private record Spin(long startedAt, ColorCard initialCenter) {}
    private record ComputerTarget(BlockPos pos, float progress) {}
}
