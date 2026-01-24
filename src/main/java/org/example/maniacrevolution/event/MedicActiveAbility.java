package org.example.maniacrevolution.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.item.armor.MedicalMaskItem;
import org.example.maniacrevolution.mana.ManaProvider;

import java.util.List;

/**
 * АКТИВНАЯ способность медика: разделение урона при ношении маски
 */
@Mod.EventBusSubscriber
public class MedicActiveAbility {

    private static final double DAMAGE_SHARE_RADIUS = 4.0;
    private static final String SURVIVORS_TEAM = "survivors";
    private static final String NBT_KEY_ACTIVE = "MedicAbilityActive";
    private static final float MANA_COST_PER_ACTIVATION = 30.0f;

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // ИСПРАВЛЕНО: Используем статический метод из MedicalMaskItem
        if (!MedicalMaskItem.isAbilityActive(victim)) return;

        if (victim.isSpectator() || victim.isCreative()) return;

        ServerPlayer nearestAlly = findNearestAlly(victim);
        if (nearestAlly == null) return;

        float originalDamage = event.getAmount();
        float sharedDamage = originalDamage * 0.5F;

        event.setAmount(sharedDamage);
        nearestAlly.hurt(victim.damageSources().generic(), sharedDamage);

        showDamageShareEffect(victim, nearestAlly);

        victim.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§6Урон разделен с %s (%.1f❤)",
                                nearestAlly.getName().getString(), sharedDamage / 2.0F)
                ),
                true
        );

        nearestAlly.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§6Вы разделили урон с медиком (%.1f❤)", sharedDamage / 2.0F)
                ),
                true
        );
    }

    /**
     * НОВОЕ: Активация способности (вызывается из перка или команды)
     */
    public static boolean activateAbility(ServerPlayer player, int durationTicks) {
        // Проверка маски
        if (!isWearingMask(player)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cНаденьте медицинскую маску!"),
                    true
            );
            return false;
        }

        // Проверка маны
        boolean success = player.getCapability(ManaProvider.MANA).map(mana -> {
            if (!mana.consumeMana(MANA_COST_PER_ACTIVATION)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                String.format("§cНедостаточно маны! (Нужно: %.0f)", MANA_COST_PER_ACTIVATION)
                        ),
                        true
                );
                return false;
            }
            return true;
        }).orElse(false);

        if (!success) return false;

        // Активируем способность
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_KEY_ACTIVE, player.getServer().getTickCount() + durationTicks);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§aСпособность медика активирована! (" + (durationTicks / 20) + "с)"
                ),
                false
        );

        return true;
    }

    /**
     * НОВОЕ: Деактивация способности
     */
    public static void deactivateAbility(ServerPlayer player) {
        player.getPersistentData().remove(NBT_KEY_ACTIVE);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cСпособность медика деактивирована"),
                true
        );
    }

    /**
     * НОВОЕ: Проверка активности способности
     */
    public static boolean isAbilityActive(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_KEY_ACTIVE)) return false;

        int expiryTick = data.getInt(NBT_KEY_ACTIVE);
        int currentTick = player.getServer().getTickCount();

        if (currentTick >= expiryTick) {
            data.remove(NBT_KEY_ACTIVE);
            return false;
        }

        return true;
    }

    /**
     * НОВОЕ: Проверка наличия маски
     */
    private static boolean isWearingMask(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() == ModItems.MEDICAL_MASK.get();
    }

    private static boolean isValidAlly(Player player, Player medic) {
        if (player == medic) return false;
        if (player.isSpectator() || player.isCreative()) return false;

        Team team = player.getTeam();
        if (team == null || !SURVIVORS_TEAM.equalsIgnoreCase(team.getName())) {
            return false;
        }

        return true;
    }

    private static ServerPlayer findNearestAlly(ServerPlayer medic) {
        AABB searchBox = medic.getBoundingBox().inflate(DAMAGE_SHARE_RADIUS);
        List<ServerPlayer> nearbyPlayers = medic.level().getEntitiesOfClass(
                ServerPlayer.class,
                searchBox,
                p -> isValidAlly(p, medic)
        );

        if (nearbyPlayers.isEmpty()) return null;

        ServerPlayer nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ServerPlayer ally : nearbyPlayers) {
            double distance = medic.distanceTo(ally);
            if (distance < minDistance && distance <= DAMAGE_SHARE_RADIUS) {
                minDistance = distance;
                nearest = ally;
            }
        }

        return nearest;
    }

    private static void showDamageShareEffect(ServerPlayer medic, ServerPlayer ally) {
        if (!(medic.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.phys.Vec3 medicPos = medic.position().add(0, 1, 0);
        net.minecraft.world.phys.Vec3 allyPos = ally.position().add(0, 1, 0);
        net.minecraft.world.phys.Vec3 direction = allyPos.subtract(medicPos).normalize();

        double distance = medicPos.distanceTo(allyPos);
        int particleCount = (int) (distance * 5);

        for (int i = 0; i < particleCount; i++) {
            double progress = i / (double) particleCount;
            net.minecraft.world.phys.Vec3 particlePos = medicPos.add(direction.scale(distance * progress));

            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.01
            );
        }

        serverLevel.playSound(null, medic.blockPosition(),
                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
    }
}