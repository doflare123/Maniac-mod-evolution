package org.example.maniacrevolution.event;

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
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncManaPacket;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ManaEvents {

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
                mana.regenerate(0.05f);

                // Синхронизация маны каждые 10 тиков
                if (event.player.tickCount % 10 == 0) {
                    ModNetworking.sendToPlayer(
                            new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                            (ServerPlayer) event.player
                    );

                    // НОВОЕ: Синхронизируем активные способности брони
                    syncActiveArmorAbilities((ServerPlayer) event.player);
                }
            });
        }
    }

    /**
     * НОВОЕ: Синхронизация активных способностей брони
     */
    private static void syncActiveArmorAbilities(ServerPlayer player) {
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (armorSlot.getItem() instanceof ITimedAbility timedAbility) {
                if (timedAbility.isAbilityActive(player)) {
                    int remaining = timedAbility.getRemainingDurationSeconds(player);
                    int cooldown = timedAbility.getCooldownSeconds(player);

                    ArmorAbilityCooldownManager.syncToClient(player, armorSlot.getItem(), remaining);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Очищаем кулдауны брони при смерти
            ArmorAbilityCooldownManager.clearAllCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Очищаем кэш при выходе
            ArmorAbilityCooldownManager.onPlayerLogout(player);
        }
    }
}