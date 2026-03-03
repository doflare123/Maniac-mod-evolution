package org.example.maniacrevolution.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.FurySwipesCapability;
import org.example.maniacrevolution.capability.FurySwipesCapabilityProvider;
import org.example.maniacrevolution.item.BeastClawItem;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class FurySwipesEventHandler {

    // ── Привязка capability ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            FurySwipesCapabilityProvider provider = new FurySwipesCapabilityProvider();
            event.addCapability(FurySwipesCapabilityProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    // ── Тик игрока ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        long tick = sp.level().getGameTime();

        // 1. Убираем протухшие стаки у этого игрока (как жертвы)
        FurySwipesCapability cap = FurySwipesCapabilityProvider.get(sp);
        if (cap != null) {
            boolean changed = cap.tickAndPrune(tick);
            // Синхронизируем раз в секунду или при изменении
            if (changed || tick % 20 == 0) {
                cap.syncToClient(sp);
            }
        }

        // 2. Проверяем приземление после прыжка
        BeastClawItem.onPlayerTick(sp);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(attacker.getMainHandItem().getItem() instanceof BeastClawItem)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!BeastClawItem.isInSurvivorsTeam((Player) victim)) return;
        FurySwipesCapability cap = FurySwipesCapabilityProvider.get((Player) victim);
        if (cap == null) return;
        event.setAmount(event.getAmount() + cap.getBonusDamage());
    }
}