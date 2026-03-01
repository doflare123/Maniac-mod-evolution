package org.example.maniacrevolution.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.capability.AddictionCapability;
import org.example.maniacrevolution.capability.AddictionCapabilityProvider;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncAddictionVisibilityPacket;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class AddictionEventHandler {

    public static final String SURVIVOR_CLASS_OBJ   = "SurvivorClass";
    public static final int    ADDICT_CLASS_VALUE    = 9;
    private static final Random RANDOM = new Random();

    // ── Привязка capability ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            AddictionCapabilityProvider provider = new AddictionCapabilityProvider();
            event.addCapability(AddictionCapabilityProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    // ── Главный тик ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // Игра должна быть активна (phase != 0)
        if (GameManager.getPhaseValue() == 0) return;

        AddictionCapability cap = AddictionCapabilityProvider.get(player);
        if (cap == null) return;

        boolean isAddict = isAddictClass(player);

        // Синхронизируем видимость шкалы раз в секунду
        if (player.tickCount % 20 == 0) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncAddictionVisibilityPacket(isAddict)
            );
        }

        if (!isAddict) return;

        // ── Пауза от бонка ────────────────────────────────────────────────────
        if (cap.isPaused()) {
            cap.setBongPauseTicks(cap.getBongPauseTicks() - 1);
        } else {
            // ── Заполнение шкалы ──────────────────────────────────────────────
            cap.setAddiction(cap.getAddiction() + cap.getCurrentFillRate());
        }

        // ── Применяем дебафы по стадии ────────────────────────────────────────
        applyStageEffects(player, cap.getStage());

        // ── Шанс смерти на стадии 3 при 3+ шприцах ───────────────────────────
        if (cap.getStage() == 3 && cap.getTotalSyringeCount() >= 3) {
            cap.tickDeathCheck();
            if (cap.getDeathCheckTimer() >= AddictionCapability.DEATH_CHECK_INTERVAL) {
                cap.resetDeathCheck();
                if (RANDOM.nextFloat() < AddictionCapability.STAGE3_DEATH_CHANCE) {
                    killWithMessage(player, "§4§l" + player.getName().getString()
                            + " §c— сердце не выдержало давления зависимости");
                    return;
                }
            }
        }

        // ── Синхронизация данных каждые 4 тика ───────────────────────────────
        if (player.tickCount % 4 == 0) {
            cap.syncToClient(player);
        }
    }

    // ── Дебафы по стадии ─────────────────────────────────────────────────────

    private static void applyStageEffects(ServerPlayer player, int stage) {
        if (stage == 0) {
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.DARKNESS);
            return;
        }
        // Стадия 1+: слабость
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, false));

        if (stage >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
        } else {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        if (stage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
        } else {
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    // ── Вспомогательные ──────────────────────────────────────────────────────

    public static boolean isAddictClass(ServerPlayer player) {
        if (player.getServer() == null) return false;
        Scoreboard sb = player.getServer().getScoreboard();
        Objective obj = sb.getObjective(SURVIVOR_CLASS_OBJ);
        if (obj == null) return false;
        if (!sb.hasPlayerScore(player.getScoreboardName(), obj)) return false;
        return sb.getOrCreatePlayerScore(player.getScoreboardName(), obj).getScore()
                == ADDICT_CLASS_VALUE;
    }

    public static void killWithMessage(ServerPlayer player, String message) {
        // Убиваем через пустоту (игнорирует броню)
        player.hurt(player.level().damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        if (player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(message), false
            );
        }
    }
}