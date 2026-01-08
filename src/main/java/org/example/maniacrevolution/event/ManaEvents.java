package org.example.maniacrevolution.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
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
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            event.player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                // ИСПРАВЛЕНО: Регенерация учитывает настройки (пассивный реген + бонусы)
                // regenerate() внутри себя уже проверяет getTotalRegenRate(), который учитывает passiveRegenEnabled
                mana.regenerate(0.05f);

                // Синхронизация с клиентом каждые 10 тиков
                if (event.player.tickCount % 10 == 0) {
                    ModNetworking.sendToPlayer(
                            new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                            (ServerPlayer) event.player
                    );
                }
            });
        }
    }
}