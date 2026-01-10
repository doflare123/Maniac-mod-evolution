package org.example.maniacrevolution.brewing;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.potion.ModPotions;

public class ModBrewingRecipes {
    public static void register() {
        // Регенерация
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_REGEN_FRAGMENT.get()),
                PotionUtils.setPotion(Items.POTION.getDefaultInstance(), ModPotions.SLOW_REGENERATION_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_REGEN_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SLOW_REGENERATION_POTION.get())
        );

        // Сила
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_STRENGTH_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.STRENGTH_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_STRENGTH_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.STRENGTH_POTION.get())
        );

        // Скорость (уровень 2)
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SPEED_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SPEED_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SPEED_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SPEED_POTION.get())
        );

        // Замедление (уровень 3)
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SLOWNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SLOWNESS_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SLOWNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SLOWNESS_POTION.get())
        );

        // Плавное падение
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SLOW_FALLING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SLOW_FALLING_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_SLOW_FALLING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.SLOW_FALLING_POTION.get())
        );

        // Слабость
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_WEAKNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.WEAK_WEAKNESS_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_WEAKNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.WEAK_WEAKNESS_POTION.get())
        );

        // Исцеление
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_HEALING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.WEAK_HEALING_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_HEALING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.WEAK_HEALING_POTION.get())
        );

        // Слепота
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_BLINDNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.BLINDNESS_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_BLINDNESS_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.BLINDNESS_POTION.get())
        );

        // Свечение
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_GLOWING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.GLOWING_POTION.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.AWKWARD)),
                Ingredient.of(ModItems.MANIAC_GLOWING_FRAGMENT.get()),
                PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), ModPotions.GLOWING_POTION.get())
        );
    }
}