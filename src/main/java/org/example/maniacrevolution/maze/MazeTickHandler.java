package org.example.maniacrevolution.maze;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Подписывается на серверный тик и вызывает MazeManager.tick().
 *
 * Зарегистрировать в главном классе мода:
 *   MinecraftForge.EVENT_BUS.register(new MazeTickHandler());
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MazeTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Вызываем только в фазе END, чтобы не дублировать
        if (event.phase != TickEvent.Phase.END) return;

        long currentTick = event.getServer().getLevel(
                net.minecraft.world.level.Level.OVERWORLD
        ) != null
                ? event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD).getGameTime()
                : 0L;

        MazeManager.getInstance().tick(currentTick);
    }
}