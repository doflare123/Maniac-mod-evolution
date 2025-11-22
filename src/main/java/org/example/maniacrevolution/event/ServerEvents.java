package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.perks.common.VampirePerk;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Обработка вампиризма
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PlayerData data = PlayerDataManager.get(attacker);

            for (PerkInstance inst : data.getSelectedPerks()) {
                if (inst.getPerk() instanceof VampirePerk) {
                    VampirePerk.onDamageDealt(attacker, event.getAmount());
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Сохраняем данные при респавне
        if (event.isWasDeath()) {
            // Данные уже хранятся в PlayerDataManager по UUID
            // При респавне они автоматически доступны
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Синхронизируем данные после респавна
            PlayerDataManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataManager.syncToClient(player);
        }
    }
}
