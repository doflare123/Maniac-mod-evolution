package org.example.maniacrevolution.lich;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-thread work queue. Budget limits planning work, never army size. */
@Mod.EventBusSubscriber(modid="maniacrev")
public final class LichPathScheduler {
    static final int BUDGET=2048, SLICE=128;
    private static final ArrayDeque<LichNavigation> combat=new ArrayDeque<>(),orders=new ArrayDeque<>(),prefetch=new ArrayDeque<>();
    private static final Set<LichNavigation> queued=Collections.newSetFromMap(new IdentityHashMap<>());
    private static int turn,lastPolls;
    static void submit(LichNavigation navigation) {
        if(queued.add(navigation))
            (navigation.speculative()?prefetch:navigation.urgent()?combat:orders).addLast(navigation);
    }
    static void cancel(LichNavigation navigation) {
        if(queued.remove(navigation)){combat.remove(navigation);orders.remove(navigation);prefetch.remove(navigation);}
    }
    static int lastPolls() {return lastPolls;}
    static int queuedCount() {return queued.size();}
    static void run() {
        lastPolls=0;
        long deadline=System.nanoTime()+5_000_000;
        // Snapshot sizes: every waiting unit gets at most one slice this server tick.
        int urgent=combat.size(),normal=orders.size();
        while(urgent+normal>0&&lastPolls<BUDGET) {
            boolean priority=urgent>0&&(normal==0||turn++%4!=3);
            var queue=priority?combat:orders;
            var job=queue.removeFirst();
            if(priority)urgent--;else normal--;
            lastPolls+=job.advanceSearch(Math.min(SLICE,BUDGET-lastPolls));
            if(job.pending())queue.addLast(job);else queued.remove(job);
            if(System.nanoTime()>=deadline)return;
        }
        if(!combat.isEmpty()||!orders.isEmpty())return;
        for(int remaining=prefetch.size();remaining>0&&lastPolls<BUDGET;remaining--) {
            if(System.nanoTime()>=deadline)return;
            var job=prefetch.removeFirst();
            lastPolls+=job.advanceSearch(Math.min(SLICE,BUDGET-lastPolls));
            if(job.pending())prefetch.addLast(job);else queued.remove(job);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase==TickEvent.Phase.END)run();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {combat.clear();orders.clear();prefetch.clear();queued.clear();turn=0;}
}
