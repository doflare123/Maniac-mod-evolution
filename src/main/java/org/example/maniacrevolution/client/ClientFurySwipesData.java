package org.example.maniacrevolution.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентские данные стаков Fury Swipes.
 * selfStacks   — стаки на самом игроке (HUD рядом с FleshHeap).
 * targetStacks — стаки на жертвах которых бил этот клиент (над головой жертвы).
 */
public class ClientFurySwipesData {

    private static List<Long> selfStacks = new ArrayList<>();
    private static final Map<UUID, List<Long>> targetStacks = new HashMap<>();

    public static void updateSelf(List<Long> expireTicks) {
        selfStacks = new ArrayList<>(expireTicks);
    }

    public static void updateTarget(UUID targetUuid, List<Long> expireTicks) {
        if (expireTicks.isEmpty()) {
            targetStacks.remove(targetUuid);
        } else {
            targetStacks.put(targetUuid, new ArrayList<>(expireTicks));
        }
    }

    public static int getSelfStackCount() { return selfStacks.size(); }

    public static int getTargetStackCount(UUID targetUuid) {
        List<Long> stacks = targetStacks.get(targetUuid);
        return stacks == null ? 0 : stacks.size();
    }

    public static Map<UUID, List<Long>> getAllTargetStacks() { return targetStacks; }

    /** Вызывать каждый клиентский тик — убирает протухшие стаки */
    public static void clientTick(long currentGameTick) {
        selfStacks.removeIf(expiry -> currentGameTick >= expiry);
        targetStacks.values().forEach(list -> list.removeIf(expiry -> currentGameTick >= expiry));
        targetStacks.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}