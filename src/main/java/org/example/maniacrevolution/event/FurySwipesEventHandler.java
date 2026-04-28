package org.example.maniacrevolution.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.FurySwipesCapability;
import org.example.maniacrevolution.capability.FurySwipesCapabilityProvider;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.item.BeastClawItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncFurySwipesTargetPacket;
import org.example.maniacrevolution.network.packets.SyncPlayerClassPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class FurySwipesEventHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            FurySwipesCapabilityProvider provider = new FurySwipesCapabilityProvider();
            event.addCapability(FurySwipesCapabilityProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        long tick = sp.level().getGameTime();

        FurySwipesCapability cap = FurySwipesCapabilityProvider.get(sp);
        if (cap != null) {
            boolean changed = cap.tickAndPrune(tick);
            if (changed || tick % 20 == 0) {
                // Синхронизируем жертве её собственные стаки (для HUD)
                cap.syncToClient(sp);

                // При изменении — уведомляем всех игроков на сервере
                // (маньяк отфильтрует нужное на клиенте сам)
                if (changed) {
                    sp.level().players().forEach(p -> {
                        if (p instanceof ServerPlayer other && !other.getUUID().equals(sp.getUUID())) {
                            ModNetworking.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> other),
                                    new SyncFurySwipesTargetPacket(sp.getUUID(), cap.getStackExpireTicks())
                            );
                        }
                    });
                }
            }
        }

        BeastClawItem.onPlayerTick(sp);
    }

    private static final Map<UUID, Long> lastHitTick = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(attacker.getMainHandItem().getItem() instanceof BeastClawItem)) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!BeastClawItem.isInSurvivorsTeam(victim)) return;
        if (attacker.getAttackStrengthScale(0.5F) < 0.9F) return;

        // ── Защита от двойного срабатывания ──────────────────────────────────
        long tick = attacker.level().getGameTime();
        UUID key = attacker.getUUID(); // или victim.getUUID() — оба подходят
        Long last = lastHitTick.get(key);
        if (last != null && tick == last) return; // тот же тик — пропускаем
        lastHitTick.put(key, tick);
        // ─────────────────────────────────────────────────────────────────────

        FurySwipesCapability cap = FurySwipesCapabilityProvider.get(victim);
        if (cap == null) return;

        cap.addStack(tick);
        cap.syncToClient(victim);

        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> attacker),
                new SyncFurySwipesTargetPacket(victim.getUUID(), cap.getStackExpireTicks())
        );

        event.setAmount(event.getAmount() + cap.getBonusDamage());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        Scoreboard sb = sp.getServer().getScoreboard();

        for (CharacterType type : CharacterType.values()) {
            Objective obj = sb.getObjective(type.getScoreboardName());
            if (obj == null) continue;
            if (!sb.hasPlayerScore(sp.getScoreboardName(), obj)) continue;

            int score = sb.getOrCreatePlayerScore(sp.getScoreboardName(), obj).getScore();
            if (score <= 0) continue;

            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncPlayerClassPacket(type, score)
            );
        }
    }
}
