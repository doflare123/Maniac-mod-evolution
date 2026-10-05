package org.example.maniacrevolution.warden;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import org.example.maniacrevolution.effect.WardenArmorEffect;

public final class WardenArmorTest {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        run();
    }
    public static void run() {
        int ironArmor = 0;
        for (var type : ArmorItem.Type.values()) ironArmor += ArmorMaterials.IRON.getDefenseForType(type);
        check(ironArmor == WardenArmorEffect.ARMOR && ArmorMaterials.IRON.getToughness() == 0, "same armor and toughness as a complete iron set");
        var effect = new WardenArmorEffect();
        var attributes = new AttributeMap(AttributeSupplier.builder().add(Attributes.ARMOR).add(Attributes.ARMOR_TOUGHNESS).build());
        var armor = attributes.getInstance(Attributes.ARMOR);
        effect.addAttributeModifiers(null, attributes, 0);
        check(armor.getValue() == 15 && attributes.getValue(Attributes.ARMOR_TOUGHNESS) == 0, "effect grants fifteen armor without toughness");
        effect.addAttributeModifiers(null, attributes, 0);
        check(armor.getValue() == 15 && armor.getModifiers().size() == 1, "refresh never stacks armor");
        effect.addAttributeModifiers(null, attributes, 3);
        check(armor.getValue() == 15, "passive armor remains fixed at higher command amplifiers");
        for (float damage : new float[]{3, 6, 9, 20, 50}) {
            float actual = CombatRules.getDamageAfterAbsorb(damage, (float) armor.getValue(), 0);
            check(Math.abs(actual - CombatRules.getDamageAfterAbsorb(damage, ironArmor, 0)) < 0.0001F, "damage matches iron armor across hit strengths");
        }
        check(Math.abs(CombatRules.getDamageAfterAbsorb(9, 15, 0) - 5.22F) < 0.0001F, "nine damage becomes 5.22 before other modifiers");
        var other = new AttributeModifier(java.util.UUID.randomUUID(), "other armor", 2, AttributeModifier.Operation.ADDITION);
        armor.addTransientModifier(other);
        effect.removeAttributeModifiers(null, attributes, 0);
        check(armor.getValue() == 2 && armor.hasModifier(other), "removal preserves unrelated armor modifiers");
        armor.removeModifier(other); check(armor.getValue() == 0, "unarmored baseline restored");
        for (boolean alive : new boolean[]{false, true}) for (boolean mode : new boolean[]{false, true})
            for (int phase = 0; phase <= 4; phase++) for (int classId : new int[]{0, 9, 10})
                for (String team : new String[]{null, "maniac", "survivors"})
                    check(WardenMatchRules.active(alive, mode, phase, classId, team)
                            == (alive && mode && phase >= 1 && phase <= 3 && classId == 9 && "maniac".equals(team)), "armor follows Warden match eligibility");
        System.out.println("Warden armor: iron-set equivalence, fixed passive strength, nonstacking refresh, modifier cleanup and match eligibility passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
