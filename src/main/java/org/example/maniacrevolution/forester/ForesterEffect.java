package org.example.maniacrevolution.forester;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;

public final class ForesterEffect extends MobEffect {
    public ForesterEffect(int color, boolean immobilize) {
        super(MobEffectCategory.HARMFUL, color);
        if (immobilize) addAttributeModifier(Attributes.MOVEMENT_SPEED, "18da22f4-8b23-4ae6-b379-e31934e6af61", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
