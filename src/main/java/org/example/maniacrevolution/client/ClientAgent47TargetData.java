package org.example.maniacrevolution.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ClientAgent47TargetData {
    private static UUID targetUuid;
    private static int healthPercent;

    private ClientAgent47TargetData() {
    }

    public static void update(UUID uuid, int percent) {
        targetUuid = uuid;
        healthPercent = Math.max(0, Math.min(100, percent));
    }

    public static UUID getTargetUuid() {
        return targetUuid;
    }

    public static int getHealthPercent() {
        return healthPercent;
    }
}
