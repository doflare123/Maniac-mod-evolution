package org.example.maniacrevolution.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.DeathScytheItem;
import org.example.maniacrevolution.item.HookItem;
import org.example.maniacrevolution.item.IItemWithAbility;
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAbilityCooldownPacket;
import org.example.maniacrevolution.network.packets.SyncManaPacket;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ManaEvents {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();

            // Очищаем кэши
            ArmorAbilityCooldownManager.onPlayerLogout(player);
            DeathScytheItem.onPlayerLogout(playerId);
            HookItem.clearCooldown(playerId);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(ManaProvider.MANA).isPresent()) {
                event.addCapability(new ResourceLocation(Maniacrev.MODID, "mana"), new ManaProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(ManaProvider.MANA).ifPresent(oldStore -> {
                event.getEntity().getCapability(ManaProvider.MANA).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);

                    // ИСПРАВЛЕНИЕ: Сбрасываем бонусный реген при смерти
                    newStore.setBonusRegenRate(0.0f);

                    System.out.println("[ManaEvents] Reset bonus regen on death for " +
                            event.getEntity().getName().getString());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            event.player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                // ИСПРАВЛЕНО: Проверяем и очищаем некорректный бонусный реген
                if (!mana.isPassiveRegenEnabled() && mana.getBonusRegenRate() > 0) {
                    // Если пассивный реген отключен, но есть бонусный - проверяем источник
                    // Если нет активных эффектов - очищаем
                    if (!hasActiveRegenEffects(event.player)) {
                        mana.setBonusRegenRate(0.0f);
                        System.out.println("[ManaEvents] Cleared invalid bonus regen for " +
                                event.player.getName().getString());
                    }
                }

                mana.regenerate(0.05f);

                // Синхронизация маны каждые 10 тиков
                if (event.player.tickCount % 10 == 0) {
                    ModNetworking.sendToPlayer(
                            new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                            (ServerPlayer) event.player
                    );

                    syncActiveArmorAbilities((ServerPlayer) event.player);
                }
            });
        }
    }

    /**
     * НОВОЕ: Проверка активных эффектов, влияющих на реген маны
     */
    private static boolean hasActiveRegenEffects(Player player) {
        // Проверяем эффекты зелий
        if (player.hasEffect(ModEffects.MANA_FLOW.get())) {
            return true;
        }
        return false;
    }

    /**
     * ОБНОВЛЕНО: Синхронизация активных способностей брони и оружия
     */
    private static void syncActiveArmorAbilities(ServerPlayer player) {
        // Броня
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (armorSlot.getItem() instanceof ITimedAbility timedAbility) {
                int remaining = timedAbility.getRemainingDurationSeconds(player);
                int cooldown = timedAbility.getCooldownSeconds(player);

                if (remaining > 0 || cooldown > 0) {
                    ArmorAbilityCooldownManager.syncToClient(player, armorSlot.getItem(), remaining);
                }
            }
        }

        // Оружие в руках
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof IItemWithAbility ability) {
            int cooldown = ability.getCooldownSeconds(player);
            if (cooldown > 0) {
                ModNetworking.sendToPlayer(
                        new SyncAbilityCooldownPacket(
                                mainHand.getItem(),
                                cooldown,
                                ability.getMaxCooldownSeconds(),
                                0
                        ),
                        player
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                mana.setBonusRegenRate(0.0f);
            });

            // НОВОЕ: Очищаем активные способности брони
            CompoundTag data = player.getPersistentData();
            data.remove("MedicMaskActiveTimestamp");
            data.remove("MedicMaskActiveDuration");

            // Очищаем кулдауны брони при смерти
            ArmorAbilityCooldownManager.clearAllCooldowns(player);
        }
    }
}