package org.example.maniacrevolution;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.item.*;
import org.example.maniacrevolution.item.armor.NecromancerArmorItem;
import org.example.maniacrevolution.item.CharacterSelectionItem;
import org.example.maniacrevolution.item.ReadyItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Maniacrev.MODID);

    public static final RegistryObject<Item> SHOP_TOKEN = ITEMS.register("shop_token",
            () -> new ShopOpenItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PERK_TOKEN = ITEMS.register("perk_token",
            () -> new PerkOpenItem(new Item.Properties().stacksTo(1)));


    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new SaltItem(new Item.Properties()));

    public static final RegistryObject<Item> CLARITY = ITEMS.register("clarity",
            () -> new ClarityItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> HOOK = ITEMS.register("hook",
            () -> new HookItem(new Item.Properties()));

    // Посох некроманта
    public static final RegistryObject<Item> NECROMANCER_STAFF = ITEMS.register("necromancer_staff",
            () -> new NecromancerStaffItem(new Item.Properties()));

    // Броня некроманта
    public static final RegistryObject<Item> NECROMANCER_HELMET = ITEMS.register("necromancer_helmet",
            () -> new NecromancerArmorItem(ModArmorMaterials.NECROMANCER,
                    ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> NECROMANCER_CHESTPLATE = ITEMS.register("necromancer_chestplate",
            () -> new NecromancerArmorItem(ModArmorMaterials.NECROMANCER,
                    ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> NECROMANCER_LEGGINGS = ITEMS.register("necromancer_leggings",
            () -> new NecromancerArmorItem(ModArmorMaterials.NECROMANCER,
                    ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> NECROMANCER_BOOTS = ITEMS.register("necromancer_boots",
            () -> new NecromancerArmorItem(ModArmorMaterials.NECROMANCER,
                    ArmorItem.Type.BOOTS, new Item.Properties()));

    // Материал для крафта (опционально)
    public static final RegistryObject<Item> SOUL_ESSENCE = ITEMS.register("soul_essence",
            () -> new Item(new Item.Properties()));

    public enum ModArmorMaterials implements ArmorMaterial {
        NECROMANCER(
                "necromancer",
                25, // Прочность
                new int[]{0, 0, 0, 0}, // Защита [ботинки, штаны, нагрудник, шлем]
                15, // Зачаровываемость
                SoundEvents.ARMOR_EQUIP_DIAMOND,
                0.0F, // Прочность
                0.0F, // Сопротивление отбрасыванию
                () -> Ingredient.of(ModItems.SOUL_ESSENCE.get())
        );

        private final String name;
        private final int durabilityMultiplier;
        private final int[] protectionAmounts;
        private final int enchantmentValue;
        private final SoundEvent equipSound;
        private final float toughness;
        private final float knockbackResistance;
        private final java.util.function.Supplier<Ingredient> repairIngredient;

        private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

        ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts,
                          int enchantmentValue, SoundEvent equipSound, float toughness,
                          float knockbackResistance, java.util.function.Supplier<Ingredient> repairIngredient) {
            this.name = name;
            this.durabilityMultiplier = durabilityMultiplier;
            this.protectionAmounts = protectionAmounts;
            this.enchantmentValue = enchantmentValue;
            this.equipSound = equipSound;
            this.toughness = toughness;
            this.knockbackResistance = knockbackResistance;
            this.repairIngredient = repairIngredient;
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return BASE_DURABILITY[type.ordinal()] * this.durabilityMultiplier;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return this.protectionAmounts[type.ordinal()];
        }

        @Override
        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }

        @Override
        public SoundEvent getEquipSound() {
            return this.equipSound;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient.get();
        }

        @Override
        public String getName() {
            return Maniacrev.MODID + ":" + this.name;
        }

        @Override
        public float getToughness() {
            return this.toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return this.knockbackResistance;
        }
    }

    // Ингредиенты для зелий
    public static final RegistryObject<Item> MANIAC_REGEN_FRAGMENT = ITEMS.register("maniac_regen_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_STRENGTH_FRAGMENT = ITEMS.register("maniac_strength_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_SPEED_FRAGMENT = ITEMS.register("maniac_speed_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_SLOWNESS_FRAGMENT = ITEMS.register("maniac_slowness_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_SLOW_FALLING_FRAGMENT = ITEMS.register("maniac_slow_falling_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_WEAKNESS_FRAGMENT = ITEMS.register("maniac_weakness_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_HEALING_FRAGMENT = ITEMS.register("maniac_healing_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_BLINDNESS_FRAGMENT = ITEMS.register("maniac_blindness_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANIAC_GLOWING_FRAGMENT = ITEMS.register("maniac_glowing_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RECIPE_BOOK = ITEMS.register("recipe_book",
            () -> new RecipeBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SURVIVOR_SELECTION = ITEMS.register("survivor_selection",
            () -> new CharacterSelectionItem(new Item.Properties(), CharacterType.SURVIVOR));

    public static final RegistryObject<Item> MANIAC_SELECTION = ITEMS.register("maniac_selection",
            () -> new CharacterSelectionItem(new Item.Properties(), CharacterType.MANIAC));

    public static final RegistryObject<Item> READY_ITEM = ITEMS.register("ready_item",
            () -> new ReadyItem(new Item.Properties()));

    public static final RegistryObject<Item> READY_ITEM_ACTIVE = ITEMS.register("ready_item_active",
            () -> new ReadyItemActive(new Item.Properties()));

    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new BandageItem(new Item.Properties()
                    .stacksTo(4)
                    .rarity(Rarity.UNCOMMON)
            ));

    public static final RegistryObject<Item> MEDIC_TABLET = ITEMS.register("medic_tablet",
            () -> new MedicTabletItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
