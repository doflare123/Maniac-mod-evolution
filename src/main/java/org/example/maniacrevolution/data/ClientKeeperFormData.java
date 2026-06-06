package org.example.maniacrevolution.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.nightmare.NightmareConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientKeeperFormData {
    private static final Map<UUID, Integer> MANIAC_CLASSES = new ConcurrentHashMap<>();

    private ClientKeeperFormData() {}

    public static void setManiacClass(UUID playerId, int classId) {
        MANIAC_CLASSES.put(playerId, classId);
    }

    public static boolean isKeeper(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(player.getUUID())) {
            return ClientPlayerData.getManiacClassId() == NightmareConfig.KEEPER_CLASS_ID;
        }

        return MANIAC_CLASSES.getOrDefault(player.getUUID(), -1) == NightmareConfig.KEEPER_CLASS_ID;
    }

    public static void clear() {
        MANIAC_CLASSES.clear();
    }
}
