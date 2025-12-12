package org.example.maniacrevolution.cosmetic;

import net.minecraft.world.item.Items;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cosmetic.effects.HaloEffect;
import org.example.maniacrevolution.cosmetic.effects.ParticleEffect;
import org.example.maniacrevolution.cosmetic.effects.TrailEffect;
import org.example.maniacrevolution.cosmetic.effects.WeaponEffect;

import java.util.*;
import java.util.stream.Collectors;

public class CosmeticRegistry {
    private static final Map<String, CosmeticEffect> EFFECTS = new LinkedHashMap<>();

    public static void init() {
        // === Частицы (на игроке) ===
        register(new HaloEffect.Builder("halo")
                .type(CosmeticType.PARTICLE)
                .price(250)
                .haloHeight(0.6)
                .haloRadius(0.4)
                .build());
        register(new ParticleEffect.Builder("particles_flame")
                .type(CosmeticType.PARTICLE)
                .price(100)
                .particleType("flame")
                .particleCount(3)
                .build());

        register(new ParticleEffect.Builder("particles_hearts")
                .type(CosmeticType.PARTICLE)
                .price(80)
                .particleType("heart")
                .particleCount(1)
                .build());

        register(new ParticleEffect.Builder("particles_smoke")
                .type(CosmeticType.PARTICLE)
                .price(60)
                .particleType("smoke")
                .particleCount(2)
                .build());

        register(new ParticleEffect.Builder("particles_enchant")
                .type(CosmeticType.PARTICLE)
                .price(120)
                .particleType("enchant")
                .particleCount(5)
                .build());

        // === Эффекты оружия ===
        register(new WeaponEffect.Builder("bleeding_axe")
                .type(CosmeticType.WEAPON_EFFECT)
                .price(150)
                .triggerItem(Items.IRON_AXE)
                .triggerItem(Items.DIAMOND_AXE)
                .triggerItem(Items.NETHERITE_AXE)
                .triggerItem(Items.GOLDEN_AXE)
                .triggerItem(Items.STONE_AXE)
                .triggerItem(Items.WOODEN_AXE)
                .effectColor(0xFF0000) // Красный (кровь)
                .trailLength(5)
                .build());

        register(new WeaponEffect.Builder("frost_sword")
                .type(CosmeticType.WEAPON_EFFECT)
                .price(150)
                .triggerItem(Items.IRON_SWORD)
                .triggerItem(Items.DIAMOND_SWORD)
                .triggerItem(Items.NETHERITE_SWORD)
                .triggerItem(Items.GOLDEN_SWORD)
                .triggerItem(Items.STONE_SWORD)
                .triggerItem(Items.WOODEN_SWORD)
                .effectColor(0x00FFFF) // Голубой (лёд)
                .trailLength(4)
                .build());

        register(new WeaponEffect.Builder("shadow_blade")
                .type(CosmeticType.WEAPON_EFFECT)
                .price(200)
                .triggerItem(Items.IRON_SWORD)
                .triggerItem(Items.DIAMOND_SWORD)
                .triggerItem(Items.NETHERITE_SWORD)
                .triggerItem(Items.IRON_AXE)
                .triggerItem(Items.DIAMOND_AXE)
                .triggerItem(Items.NETHERITE_AXE)
                .effectColor(0x330033) // Тёмно-фиолетовый
                .trailLength(6)
                .build());

        // === Скины перков (простые эффекты без логики) ===
        register(new CosmeticEffect.SimpleBuilder("skin_berserker_fire")
                .type(CosmeticType.PERK_SKIN)
                .price(50)
                .build());

        register(new CosmeticEffect.SimpleBuilder("skin_mimic_shadow")
                .type(CosmeticType.PERK_SKIN)
                .price(50)
                .build());

        register(new CosmeticEffect.SimpleBuilder("skin_predator_blood")
                .type(CosmeticType.PERK_SKIN)
                .price(75)
                .build());

        // === Следы ===
        register(new TrailEffect.Builder("trail_fire")
                .type(CosmeticType.TRAIL)
                .price(100)
                .trailParticle("flame")
                .build());

        register(new TrailEffect.Builder("trail_soul")
                .type(CosmeticType.TRAIL)
                .price(120)
                .trailParticle("soul_fire_flame")
                .build());

        Maniacrev.LOGGER.info("Registered {} cosmetic effects", EFFECTS.size());
    }

    public static void register(CosmeticEffect effect) {
        EFFECTS.put(effect.getId(), effect);
    }

    public static CosmeticEffect getEffect(String id) {
        return EFFECTS.get(id);
    }

    public static Collection<CosmeticEffect> getAllEffects() {
        return Collections.unmodifiableCollection(EFFECTS.values());
    }

    public static List<CosmeticEffect> getEffectsByType(CosmeticType type) {
        return EFFECTS.values().stream()
                .filter(e -> e.getType() == type)
                .collect(Collectors.toList());
    }
}
