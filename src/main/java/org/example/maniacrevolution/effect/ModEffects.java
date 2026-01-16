package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "maniacrev");

    public static final RegistryObject<MobEffect> FEAR = MOB_EFFECTS.register(
            "fear",
            FearEffect::new
    );

    public static final RegistryObject<MobEffect> OPEN_WOUND =
            MOB_EFFECTS.register("open_wound", OpenWoundEffect::new);

    public static final RegistryObject<MobEffect> MANA_FLOW =
            MOB_EFFECTS.register("mana_flow", ManaFlowEffect::new);

    // Новые эффекты для зелий
    public static final RegistryObject<MobEffect> SLOW_REGENERATION =
            MOB_EFFECTS.register("slow_regeneration", SlowRegenEffect::new);

    public static final RegistryObject<MobEffect> WEAK_WEAKNESS =
            MOB_EFFECTS.register("weak_weakness", WeakWeaknessEffect::new);

    public static final RegistryObject<MobEffect> WEAK_INSTANT_HEALTH =
            MOB_EFFECTS.register("weak_instant_health", WeakInstantHealthEffect::new);

    public static final RegistryObject<MobEffect> TARGET_EFFECT = MOB_EFFECTS.register("target",
            TargetEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}