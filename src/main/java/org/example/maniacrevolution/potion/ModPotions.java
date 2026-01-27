package org.example.maniacrevolution.potion;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.effect.ModEffects;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, "maniacrev");

    // Слабая регенерация (5 хп за 60 секунд = 1200 тиков)
    public static final RegistryObject<Potion> SLOW_REGENERATION_POTION = POTIONS.register("slow_regeneration",
            () -> new Potion("slow_regeneration", new MobEffectInstance(ModEffects.SLOW_REGENERATION.get(), 1200, 0)));

    // Сила (1 минута для взрывного)
    public static final RegistryObject<Potion> STRENGTH_POTION = POTIONS.register("strength",
            () -> new Potion("strength", new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0)));

    // Скорость уровень 2 (5 секунд для взрывного)
    public static final RegistryObject<Potion> SPEED_POTION = POTIONS.register("speed",
            () -> new Potion("speed", new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1)));

    // Замедление уровень 3 (10 секунд для взрывного)
    public static final RegistryObject<Potion> SLOWNESS_POTION = POTIONS.register("slowness",
            () -> new Potion("slowness", new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2)));

    // Плавное падение (30 секунд для взрывного)
    public static final RegistryObject<Potion> SLOW_FALLING_POTION = POTIONS.register("slow_falling",
            () -> new Potion("slow_falling", new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0)));

    // Слабая слабость (15 секунд для взрывного)
    public static final RegistryObject<Potion> WEAK_WEAKNESS_POTION = POTIONS.register("weak_weakness",
            () -> new Potion("weak_weakness", new MobEffectInstance(ModEffects.WEAK_WEAKNESS.get(), 300, 0)));

    // Слабое исцеление (моментальное)
    public static final RegistryObject<Potion> WEAK_HEALING_POTION = POTIONS.register("weak_healing",
            () -> new Potion("weak_healing", new MobEffectInstance(ModEffects.WEAK_INSTANT_HEALTH.get(), 1, 0)));

    // Слепота (10 секунд для взрывного)
    public static final RegistryObject<Potion> BLINDNESS_POTION = POTIONS.register("blindness",
            () -> new Potion("blindness", new MobEffectInstance(MobEffects.BLINDNESS, 200, 0)));

    // Подсветка (15 секунд для взрывного)
    public static final RegistryObject<Potion> GLOWING_POTION = POTIONS.register("glowing",
            () -> new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 300, 0)));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}