package org.example.maniacrevolution.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.nightmare.NightmareConfig;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientKeeperFormData {
    private static final Map<UUID, Integer> MANIAC_CLASSES = new ConcurrentHashMap<>();
    private static final Map<Player, Boolean> PREVIEW_KEEPER_OVERRIDES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ClientKeeperFormData() {}

    public static void setManiacClass(UUID playerId, int classId) {
        MANIAC_CLASSES.put(playerId, classId);
    }

    public static void setPreviewKeeper(Player player, boolean keeper) {
        PREVIEW_KEEPER_OVERRIDES.put(player, keeper);
    }

    public static boolean isKeeper(Player player) {
        Boolean previewOverride = PREVIEW_KEEPER_OVERRIDES.get(player);
        if (previewOverride != null) {
            return previewOverride;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(player.getUUID())) {
            return ClientPlayerData.getManiacClassId() == NightmareConfig.KEEPER_CLASS_ID;
        }

        return MANIAC_CLASSES.getOrDefault(player.getUUID(), -1) == NightmareConfig.KEEPER_CLASS_ID;
    }

    public static void clear() {
        MANIAC_CLASSES.clear();
        PREVIEW_KEEPER_OVERRIDES.clear();
    }
}
