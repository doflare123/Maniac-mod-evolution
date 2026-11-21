package org.example.maniacrevolution.perk.perks.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class WallhackGlowHandler {

    private static final Set<Integer> glowingEntities = new HashSet<>();

    /**
     * Включает эффект свечения для указанных сущностей.
     */
    public static void enableGlow(Set<Integer> entityIds) {
        // ClientSelectiveGlowState applies the real render bit and restores server metadata each frame.
        glowingEntities.addAll(entityIds);
    }

    /**
     * Выключает эффект свечения для указанных сущностей.
     */
    public static void disableGlow(Set<Integer> entityIds) {
        glowingEntities.removeAll(entityIds);
    }

    /**
     * Очищает все свечения (например, при выходе из мира).
     */
    public static void clearAll() {
        glowingEntities.clear();
    }

    // Для отладки
    public static Set<Integer> getGlowingEntities() {
        return new HashSet<>(glowingEntities);
    }
}
