package org.example.maniacrevolution.fleshheap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class FleshHeapEventHandler {

    private static final String SCOREBOARD_OBJECTIVE = "ManiacClass";
    private static final int PUDGE_CLASS_ID = 5;

    /**
     * Обработка убийства игрока
     */
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        // Проверяем, что умер игрок
        if (!(event.getEntity() instanceof Player victim)) return;

        // Проверяем, что убийца - игрок
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        // Проверяем класс убийцы
        if (!isPudge(killer)) return;

        // Добавляем стак Flesh Heap
        FleshHeapData.addStack(killer);

        int newStacks = FleshHeapData.getStacks(killer);
        killer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aFlesh Heap: §f" + newStacks + " §7(+1 ❤)"
        ));
    }

    /**
     * Очистка стаков при смерти
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int stacks = FleshHeapData.getStacks(player);
        if (stacks > 0) {
            FleshHeapData.clearStacks(player);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cВсе Flesh Heap потеряны!"
            ));
        }
    }

    /**
     * Восстановление модификаторов после респавна
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FleshHeapData.restoreModifiers(player);
        }
    }

    /**
     * Восстановление модификаторов при логине
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FleshHeapData.restoreModifiers(player);
        }
    }

    /**
     * Проверка, является ли игрок Pudge (ManiacClass = 5)
     */
    private static boolean isPudge(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);

        if (objective == null) return false;

        var score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore() == PUDGE_CLASS_ID;
    }
}