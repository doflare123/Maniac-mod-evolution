package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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

    public static final RegistryObject<MobEffect> DEATH_RACE_EFFECT = MOB_EFFECTS.register("death_race",
            DeathRaceEffect::new);

    public static final RegistryObject<MobEffect> PLAGUE =
            MOB_EFFECTS.register("plague", PlagueEffect::new);

    public static final RegistryObject<MobEffect> THORN =
            MOB_EFFECTS.register("thorn", ThornEffect::new);

    public static final RegistryObject<MobEffect> FROZEN =
            MOB_EFFECTS.register("frozen", FrozenEffect::new);

    public static final RegistryObject<MobEffect> ALTRUIST_BOOST =
            MOB_EFFECTS.register("altruist_boost", AltruistBoostEffect::new);

    public static final RegistryObject<MobEffect> SILENCE =
            MOB_EFFECTS.register("silence", SilenceEffect::new);

    public static final RegistryObject<MobEffect> POSSESSION_TIMER =
            MOB_EFFECTS.register("possession_timer", PossessionTimerEffect::new);

    public static final RegistryObject<MobEffect> STUN =
            MOB_EFFECTS.register("stun", StunEffect::new);

    public static final RegistryObject<MobEffect> FULL_INVISIBILITY =
            MOB_EFFECTS.register("full_invisibility", FullInvisibilityEffect::new);

    public static final RegistryObject<MobEffect> DODEPOVICH_INSURANCE =
            MOB_EFFECTS.register("dodepovich_insurance",
                    () -> new DodepovichSimpleEffect(MobEffectCategory.BENEFICIAL, 0x52D273));

    public static final RegistryObject<MobEffect> DODEPOVICH_DAMAGE_BLOCK =
            MOB_EFFECTS.register("dodepovich_damage_block",
                    () -> new DodepovichSimpleEffect(MobEffectCategory.BENEFICIAL, 0x5ED7FF));

    public static final RegistryObject<MobEffect> DODEPOVICH_DOUBLE_DAMAGE =
            MOB_EFFECTS.register("dodepovich_double_damage",
                    () -> new DodepovichSimpleEffect(MobEffectCategory.HARMFUL, 0xB3212A));

    public static final RegistryObject<MobEffect> DODEPOVICH_CREDIT =
            MOB_EFFECTS.register("dodepovich_credit", DodepovichCreditEffect::new);

    public static final RegistryObject<MobEffect> DODEPOVICH_SLOT_COOLDOWN =
            MOB_EFFECTS.register("dodepovich_slot_cooldown",
                    () -> new DodepovichSimpleEffect(MobEffectCategory.NEUTRAL, 0xC78B42));

    public static final RegistryObject<MobEffect> JACKPOT =
            MOB_EFFECTS.register("jackpot", JackpotEffect::new);

    public static final RegistryObject<MobEffect> ACCELERATION =
            MOB_EFFECTS.register("acceleration", AccelerationEffect::new);

    public static final RegistryObject<MobEffect> SCREAM =
            MOB_EFFECTS.register("scream", ScreamEffect::new);

    public static final RegistryObject<MobEffect> SLOWDOWN =
            MOB_EFFECTS.register("slowdown", SlowdownEffect::new);

    public static final RegistryObject<MobEffect> PAINT_PROTECTION =
            MOB_EFFECTS.register("paint_protection", PaintProtectionEffect::new);

    public static final RegistryObject<MobEffect> BOUQUET =
            MOB_EFFECTS.register("bouquet", BouquetEffect::new);

    public static final RegistryObject<MobEffect> SHAKEN_MEMORY =
            MOB_EFFECTS.register("shaken_memory", ShakenMemoryEffect::new);

    public static final RegistryObject<MobEffect> FORGET_ME_NOT_CALL =
            MOB_EFFECTS.register("forget_me_not_call", ForgetMeNotCallEffect::new);

    public static final RegistryObject<MobEffect> ROSE_COLORED_GLASSES =
            MOB_EFFECTS.register("rose_colored_glasses", RoseColoredGlassesEffect::new);

    public static final RegistryObject<MobEffect> ORCHID_RECORDING =
            MOB_EFFECTS.register("orchid_recording", OrchidRecordingEffect::new);

    public static final RegistryObject<MobEffect> SBER_SPROUT_SESSION =
            MOB_EFFECTS.register("sber_sprout_session", SberSproutSessionEffect::new);

    public static final RegistryObject<MobEffect> RED_GUIDANCE =
            MOB_EFFECTS.register("red_guidance", RedGuidanceEffect::new);

    public static final RegistryObject<MobEffect> GREEN_CHARGE =
            MOB_EFFECTS.register("green_charge", GreenChargeEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
