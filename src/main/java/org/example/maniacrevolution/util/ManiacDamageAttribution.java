package org.example.maniacrevolution.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.nightmare.NightmareManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Сохраняет ответственного маньяка для урона, который намеренно использует
 * безличный ванильный DamageSource (чума и испытания кошмара).
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class ManiacDamageAttribution {
    private static final Map<UUID, UUID> SCOPED_SOURCES = new HashMap<>();
    private static final Map<UUID, TimedSource> NIGHTMARE_SOURCES = new HashMap<>();

    private ManiacDamageAttribution() {
    }

    public static boolean hurtWithSource(ServerPlayer victim, UUID responsibleManiacId,
                                         BooleanSupplier hurtAction) {
        if (responsibleManiacId == null) {
            return hurtAction.getAsBoolean();
        }

        UUID victimId = victim.getUUID();
        UUID previous = SCOPED_SOURCES.put(victimId, responsibleManiacId);
        try {
            return hurtAction.getAsBoolean();
        } finally {
            if (previous == null) {
                SCOPED_SOURCES.remove(victimId);
            } else {
                SCOPED_SOURCES.put(victimId, previous);
            }
        }
    }

    /**
     * Урон сущностей внутри испытания может убить игрока до того, как остальные
     * обработчики прочитают состояние испытания. Запоминаем владельца на этот тик.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cacheNightmareSource(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        UUID keeperId = NightmareManager.getInstance().getTrialResponsibleKeeperId(victim);
        if (keeperId != null) {
            NIGHTMARE_SOURCES.put(victim.getUUID(),
                    new TimedSource(keeperId, victim.level().getGameTime()));
        } else {
            NIGHTMARE_SOURCES.remove(victim.getUUID());
        }
    }

    public static ServerPlayer resolveResponsibleManiac(ServerPlayer victim, DamageSource damageSource) {
        ServerPlayer responsibleManiac = peekResponsibleManiac(victim, damageSource);
        NIGHTMARE_SOURCES.remove(victim.getUUID());
        return responsibleManiac;
    }

    /** Читает источник, не расходуя временную атрибуцию текущего удара. */
    public static ServerPlayer peekResponsibleManiac(ServerPlayer victim, DamageSource damageSource) {
        UUID victimId = victim.getUUID();

        if (damageSource.getEntity() instanceof ServerPlayer directPlayer) {
            return directPlayer;
        }

        UUID scopedSource = SCOPED_SOURCES.get(victimId);
        if (scopedSource != null) {
            return victim.server.getPlayerList().getPlayer(scopedSource);
        }

        TimedSource timedSource = NIGHTMARE_SOURCES.get(victimId);
        if (timedSource != null && timedSource.gameTime == victim.level().getGameTime()) {
            return victim.server.getPlayerList().getPlayer(timedSource.maniacId);
        }

        UUID keeperId = NightmareManager.getInstance().getTrialResponsibleKeeperId(victim);
        return keeperId == null ? null : victim.server.getPlayerList().getPlayer(keeperId);
    }

    private record TimedSource(UUID maniacId, long gameTime) {
    }
}
