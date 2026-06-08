package org.example.maniacrevolution.ghost;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class GhostStealthManager {
    public static final int STEALTH_DURATION_TICKS = 0;
    public static final int STEALTH_COOLDOWN_TICKS = 0;
    public static final int RECOVERY_TICKS = 50;
    public static final String NBT_HIDDEN_HELMET = "GhostHiddenHelmet";
    public static final String NBT_HIDDEN_LEGGINGS = "GhostHiddenLeggings";

    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RECOVERY_UNTIL = new HashMap<>();
    private GhostStealthManager() {
    }

    public static boolean toggleStealth(ServerPlayer player) {
        if (player == null || !GhostLoadoutManager.isGhostClass(player) || player.getServer() == null) {
            return false;
        }

        long now = player.getServer().getTickCount();
        if (isRecovering(player, now)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Призрак стабилизируется..."), true);
            return false;
        }

        if (isActive(player, now)) {
            beginRecovery(player, now);
            return true;
        }

        ACTIVE_UNTIL.put(player.getUUID(), Long.MAX_VALUE);
        syncKnifeState(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.9f, 1.35f);
        if (player.level() instanceof ServerLevel level) {
            spawnStealthBurst(level, player);
        }
        return true;
    }

    public static int getCooldownSeconds(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide) {
            return ClientAbilityData.getCooldownSeconds(ModItems.TOY_KNIFE.get());
        }

        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return 0;
        }

        return 0;
    }

    public static int getRemainingDurationSeconds(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide) {
            return ClientAbilityData.getRemainingDuration(ModItems.TOY_KNIFE.get());
        }

        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return 0;
        }

        long now = serverPlayer.getServer().getTickCount();
        if (isActive(serverPlayer, now)) {
            return -1;
        }

        long until = RECOVERY_UNTIL.getOrDefault(serverPlayer.getUUID(), 0L);
        return until <= now ? 0 : (int) ((until - now + 19) / 20);
    }

    public static boolean isAbilityActive(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide) {
            return ClientAbilityData.getRemainingDuration(ModItems.TOY_KNIFE.get()) != 0;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }
        long now = serverPlayer.getServer().getTickCount();
        return isActive(serverPlayer, now) || isRecovering(serverPlayer, now);
    }

    public static boolean shouldBlockAttack(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        long now = player.getServer().getTickCount();
        return isActive(player, now) || isRecovering(player, now);
    }

    public static void resetGhostState(ServerPlayer player) {
        ACTIVE_UNTIL.remove(player.getUUID());
        RECOVERY_UNTIL.remove(player.getUUID());
        restoreArmor(player);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        player.getCooldowns().removeCooldown(ModItems.TOY_KNIFE.get());
        syncKnifeState(player);
    }

    private static boolean isActive(ServerPlayer player, long now) {
        return ACTIVE_UNTIL.getOrDefault(player.getUUID(), 0L) > now;
    }

    private static boolean isRecovering(ServerPlayer player, long now) {
        return RECOVERY_UNTIL.getOrDefault(player.getUUID(), 0L) > now;
    }

    private static void beginRecovery(ServerPlayer player, long now) {
        ACTIVE_UNTIL.remove(player.getUUID());
        RECOVERY_UNTIL.put(player.getUUID(), now + RECOVERY_TICKS);
        syncKnifeState(player);

        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.1f, 0.7f);
        if (player.level() instanceof ServerLevel level) {
            spawnRecoveryBurst(level, player);
        }
    }

    private static void syncKnifeState(ServerPlayer player) {
        ModNetworking.sendToPlayer(
                new SyncAbilityCooldownPacket(
                        ModItems.TOY_KNIFE.get(),
                        getCooldownSeconds(player),
                        0,
                        getRemainingDurationSeconds(player)
                ),
                player
        );
    }

    private static void hideArmor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && !data.contains(NBT_HIDDEN_HELMET)) {
            data.put(NBT_HIDDEN_HELMET, helmet.save(new CompoundTag()));
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.isEmpty() && !data.contains(NBT_HIDDEN_LEGGINGS)) {
            data.put(NBT_HIDDEN_LEGGINGS, leggings.save(new CompoundTag()));
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        }
    }

    private static void restoreArmor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (data.contains(NBT_HIDDEN_HELMET) && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.of(data.getCompound(NBT_HIDDEN_HELMET)));
        }
        if (data.contains(NBT_HIDDEN_LEGGINGS) && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.of(data.getCompound(NBT_HIDDEN_LEGGINGS)));
        }

        data.remove(NBT_HIDDEN_HELMET);
        data.remove(NBT_HIDDEN_LEGGINGS);
    }

    private static void spawnStealthBurst(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.35, 0.65, 0.35, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.25, 0.45, 0.25, 0.01);
    }

    private static void spawnRecoveryBurst(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.45, 0.7, 0.45, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.2, 0.5, 0.2, 0.02);
    }

    private static void spawnRecoveryTrail(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.18, 0.3, 0.18, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.15, 0.25, 0.15, 0.01);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        long now = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, Long>> iterator = ACTIVE_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
            }
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!GhostLoadoutManager.isGhostClass(player)) {
                continue;
            }

            boolean active = isActive(player, now);
            boolean recovering = isRecovering(player, now);
            boolean fullyHidden = player.isShiftKeyDown() || active || recovering || GhostPossessionManager.isPossessing(player);

            if (fullyHidden) {
                hideArmor(player);
            } else {
                restoreArmor(player);
            }

            if (recovering) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 255, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 15, 128, false, false));
                if (player.level() instanceof ServerLevel level && player.tickCount % 3 == 0) {
                    spawnRecoveryTrail(level, player);
                }
            } else {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.JUMP);
            }

            if ((active || recovering || player.tickCount % 20 == 0) && player.getMainHandItem().is(ModItems.TOY_KNIFE.get())) {
                syncKnifeState(player);
            }
        }

        RECOVERY_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && shouldBlockAttack(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resetGhostState(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreArmor(player);
        }
    }
}
