package org.example.maniacrevolution.perk.perks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        System.out.println("=== ENABLE GLOW ===");
        System.out.println("Received entity IDs: " + entityIds);

        for (Integer id : entityIds) {
            Entity entity = mc.level.getEntity(id);

            if (entity != null) {
                // ВАЖНО: используем hasGlowingTag() для проверки
                boolean wasGlowing = entity.hasGlowingTag();

                entity.setGlowingTag(true);
                glowingEntities.add(id);

                System.out.println("✓ Enabled glow for: " + entity.getName().getString()
                        + " (ID: " + id + ", was glowing: " + wasGlowing + ")");
            } else {
                System.out.println("✗ Entity not found: ID " + id);
            }
        }

        System.out.println("Total glowing entities: " + glowingEntities.size());
    }

    /**
     * Выключает эффект свечения для указанных сущностей.
     */
    public static void disableGlow(Set<Integer> entityIds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        System.out.println("=== DISABLE GLOW ===");

        for (Integer id : entityIds) {
            Entity entity = mc.level.getEntity(id);

            if (entity != null) {
                entity.setGlowingTag(false);
                glowingEntities.remove(id);

                System.out.println("✓ Disabled glow for: " + entity.getName().getString());
            }
        }
    }

    /**
     * Очищает все свечения (например, при выходе из мира).
     */
    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        System.out.println("Clearing all glows (" + glowingEntities.size() + " entities)");

        for (Integer id : new HashSet<>(glowingEntities)) {
            Entity entity = mc.level.getEntity(id);
            if (entity != null) {
                entity.setGlowingTag(false);
            }
        }

        glowingEntities.clear();
    }

    // Для отладки
    public static Set<Integer> getGlowingEntities() {
        return new HashSet<>(glowingEntities);
    }
}