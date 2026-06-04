package org.example.maniacrevolution.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cosmetic.effects.WeaponEffect;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class CosmeticEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        PlayerData data = PlayerDataManager.get(player);
        CosmeticData cosmetics = data.getCosmeticData();

        // Применяем все включённые эффекты
        if (Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        for (String cosmeticId : cosmetics.getEnabledCosmetics()) {
            CosmeticEffect effect = CosmeticRegistry.getEffect(cosmeticId);
            if (effect != null) {
                effect.onTick(player);
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        PlayerData data = PlayerDataManager.get(player);
        CosmeticData cosmetics = data.getCosmeticData();

        // Проверяем предмет в руке
        var weapon = player.getMainHandItem();

        // Применяем эффекты оружия
        for (String cosmeticId : cosmetics.getEnabledCosmetics()) {
            CosmeticEffect effect = CosmeticRegistry.getEffect(cosmeticId);
            if (effect instanceof WeaponEffect weaponEffect) {
                if (weaponEffect.appliesTo(weapon)) {
                    weaponEffect.onAttack(player, weapon);
                }
            }
        }
    }
}

