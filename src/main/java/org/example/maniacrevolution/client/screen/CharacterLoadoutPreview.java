package org.example.maniacrevolution.client.screen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.character.CharacterClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the text-only character registry data to item stacks used by the
 * character selection preview. This is intentionally presentation-only:
 * gameplay loadouts remain owned by the existing game/datapack logic.
 */
final class CharacterLoadoutPreview {
    private CharacterLoadoutPreview() {
    }

    static List<Entry> forCharacter(CharacterClass character) {
        List<Entry> entries = new ArrayList<>();

        switch (character.getId()) {
            case "alchemist" -> {
                addDefined(entries, character, 0, waterPotions(), Placement.OTHER, "Взрывное зелье воды");
                add(entries, new ItemStack(Items.BLAZE_POWDER), "Огненный порошок",
                        "Топливо для зельеварки.", Placement.OTHER);
                addDefined(entries, character, 1, new ItemStack(Items.BREWING_STAND),
                        Placement.OTHER, null);
                addAlchemyIngredient(entries, ModItems.MANIAC_REGEN_FRAGMENT.get().getDefaultInstance(),
                        "Часть философского камня", "Медленная регенерация: 5 HP за 1 минуту.");
                addAlchemyIngredient(entries, ModItems.MANIAC_STRENGTH_FRAGMENT.get().getDefaultInstance(),
                        "Грязь", "Сила: увеличивает наносимый урон.");
                addAlchemyIngredient(entries, ModItems.MANIAC_SPEED_FRAGMENT.get().getDefaultInstance(),
                        "Колесо Маквина", "Скорость II на 5 секунд.");
                addAlchemyIngredient(entries, ModItems.MANIAC_SLOWNESS_FRAGMENT.get().getDefaultInstance(),
                        "Сердце Лича", "Замедление III на 5 секунд.");
                addAlchemyIngredient(entries, ModItems.MANIAC_SLOW_FALLING_FRAGMENT.get().getDefaultInstance(),
                        "Перо петуха", "Плавное падение на 30 секунд.");
                addAlchemyIngredient(entries, ModItems.MANIAC_WEAKNESS_FRAGMENT.get().getDefaultInstance(),
                        "Увядший цветок", "Слабость: −2 урона на 15 секунд.");
                addAlchemyIngredient(entries, ModItems.MANIAC_HEALING_FRAGMENT.get().getDefaultInstance(),
                        "Святая вода", "Мгновенно восстанавливает 1 HP.");
                addAlchemyIngredient(entries, ModItems.MANIAC_BLINDNESS_FRAGMENT.get().getDefaultInstance(),
                        "Пепел свечи", "Слепота на 10 секунд.");
                addAlchemyIngredient(entries, ModItems.MANIAC_GLOWING_FRAGMENT.get().getDefaultInstance(),
                        "Светлячок в банке", "Свечение на 5 секунд.");
            }
            case "shaman" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.TOTEM_SPAWN_ITEM.get()),
                        Placement.MAIN_HAND, null);
                addDefined(entries, character, 1, new ItemStack(ModItems.ANCESTOR_SOUL.get()),
                        Placement.OFF_HAND, null);
            }
            case "mefedronshchik" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.SYRINGE.get()),
                        Placement.MAIN_HAND, null);
                addDefined(entries, character, 1, new ItemStack(ModItems.BONG.get()),
                        Placement.OFF_HAND, null);
            }
            case "scientist" -> addDefined(entries, character, 0,
                    new ItemStack(ModItems.NETHER_SWAP.get()), Placement.MAIN_HAND, null);
            case "dodepovich" -> {
                String coinDescription = description(character, 0);
                add(entries, new ItemStack(ModItems.COIN_ELUSIVENESS.get()), "Монетка неуловимости",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_INSIGHT.get()), "Монетка прозрения",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_SHACKLES.get()), "Монетка оков",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_HEALTH.get()), "Монетка здоровья",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_EAGLE.get()), "Монетка орла",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_DEBT.get()), "Монетка долга",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_REROLL.get()), "Монетка переброса",
                        coinDescription, Placement.OTHER);
                add(entries, new ItemStack(ModItems.COIN_FATE.get()), "Монеточка судьбы",
                        coinDescription, Placement.OTHER);
                addDefined(entries, character, 1, new ItemStack(ModItems.SLOT_MACHINE.get()),
                        Placement.OTHER, null);
            }
            case "doctor" -> {
                add(entries, new ItemStack(ModItems.MEDICAL_MASK.get()), "Медицинская маска",
                        featureDescription(character, "Спасибо папаша"), Placement.HEAD);
                addDefined(entries, character, 0, new ItemStack(ModItems.BANDAGE.get()),
                        Placement.OTHER, null);
                addDefined(entries, character, 1, new ItemStack(ModItems.MEDIC_TABLET.get()),
                        Placement.OTHER, null);
            }
            case "necromancer" -> {
                add(entries, new ItemStack(ModItems.NECROMANCER_HELMET.get()), "Броня некроманта",
                        featureDescription(character, "Защита от смерти"), Placement.HEAD);
                add(entries, new ItemStack(ModItems.NECROMANCER_CHESTPLATE.get()), "Броня некроманта",
                        featureDescription(character, "Защита от смерти"), Placement.CHEST);
                add(entries, new ItemStack(ModItems.NECROMANCER_LEGGINGS.get()), "Броня некроманта",
                        featureDescription(character, "Защита от смерти"), Placement.LEGS);
                add(entries, new ItemStack(ModItems.NECROMANCER_BOOTS.get()), "Броня некроманта",
                        featureDescription(character, "Защита от смерти"), Placement.FEET);
                addDefined(entries, character, 1, new ItemStack(ModItems.NECROMANCER_STAFF.get()),
                        Placement.MAIN_HAND, null);
                addDefined(entries, character, 0, new ItemStack(ModItems.SALT.get()),
                        Placement.OTHER, null);
            }
            case "agent" -> {
                addDefined(entries, character, 0, agentPistol(),
                        Placement.MAIN_HAND, null);
                addDefined(entries, character, 1, new ItemStack(Items.IRON_SWORD),
                        Placement.OFF_HAND, null);
                addLeatherArmor(entries, character, 2, 0x202020);
                add(entries, new ItemStack(ModItems.AGENT47_TABLET.get()), "Планшет Агента 47",
                        featureDescription(character, "Чёрный рынок"), Placement.OTHER);
            }
            case "pudge" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.HOOK.get()),
                        Placement.MAIN_HAND, null);
                addLeatherArmor(entries, character, 1, 0x5A2F22);
            }
            case "death" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.Scyth.get()),
                        Placement.MAIN_HAND, null);
                addLeatherArmor(entries, character, 1, 0x111111);
            }
            case "ursa" -> {
                addDefined(entries, character, 1, new ItemStack(ModItems.BEAST_CLAW.get()),
                        Placement.MAIN_HAND, null);
                addLeatherArmor(entries, character, 0, 0x6B3F22);
                add(entries, new ItemStack(ModItems.BEEHIVE_FOOD.get()), "Улей",
                        featureDescription(character, "Улей"), Placement.OTHER);
            }
            case "plague_doctor" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.PLAGUE_LANTERN.get()),
                        Placement.MAIN_HAND, null);
                addLeatherArmor(entries, character, 1, 0x273023);
            }
            case "ghost" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.TOY_KNIFE.get()),
                        Placement.MAIN_HAND, null);
                addDefined(entries, character, 1, new ItemStack(ModItems.GHOST_HAND.get()),
                        Placement.OFF_HAND, null);
                addLeatherArmor(entries, character, 2, 0xF2F2F2);
            }
            case "freddy_bear" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.MICROPHONE.get()),
                        Placement.MAIN_HAND, null);
                addLeatherArmor(entries, character, 1, 0x704326);
                add(entries, new ItemStack(ModItems.GENERATOR.get()), "Генератор",
                        featureDescription(character, "Генератор"), Placement.OTHER);
            }
            case "keeper_of_nightmares" -> {
                addDefined(entries, character, 0, new ItemStack(ModItems.NIGHTMARE_LIGHTER.get()),
                        Placement.OTHER, null);
                addDefined(entries, character, 1, new ItemStack(ModItems.AWAKENING_NEEDLE.get()),
                        Placement.OTHER, null);
            }
            default -> {
                for (int index = 0; index < character.getItems().size(); index++) {
                    addDefined(entries, character, index, new ItemStack(Items.PAPER),
                            Placement.OTHER, null);
                }
            }
        }

        return List.copyOf(entries);
    }

    private static ItemStack agentPistol() {
        ResourceLocation pistolId = new ResourceLocation("cgm", "pistol");
        Item pistol = ForgeRegistries.ITEMS.getValue(pistolId);
        if (pistol == null || pistol == Items.AIR || !ForgeRegistries.ITEMS.containsKey(pistolId)) {
            return new ItemStack(Items.CROSSBOW);
        }

        ItemStack stack = new ItemStack(pistol);
        stack.getOrCreateTag().putInt("AmmoCount", 5);
        stack.getOrCreateTag().putInt("Color", 1908001);
        return stack;
    }

    private static ItemStack waterPotions() {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER);
        stack.setCount(5);
        return stack;
    }

    private static void addAlchemyIngredient(List<Entry> entries, ItemStack stack,
                                             String name, String result) {
        add(entries, stack, name, "При варке с неловким зельем: " + result, Placement.OTHER);
    }

    private static void addLeatherArmor(List<Entry> entries, CharacterClass character,
                                        int definedIndex, int color) {
        String name = name(character, definedIndex, "Кожаная броня");
        String description = description(character, definedIndex);
        String lore = lore(character, definedIndex);
        add(entries, dyed(new ItemStack(Items.LEATHER_HELMET), color), name, description, Placement.HEAD, lore);
        add(entries, dyed(new ItemStack(Items.LEATHER_CHESTPLATE), color), name, description, Placement.CHEST, lore);
        add(entries, dyed(new ItemStack(Items.LEATHER_LEGGINGS), color), name, description, Placement.LEGS, lore);
        add(entries, dyed(new ItemStack(Items.LEATHER_BOOTS), color), name, description, Placement.FEET, lore);
    }

    private static ItemStack dyed(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
        return stack;
    }

    private static void addDefined(List<Entry> entries, CharacterClass character, int index,
                                   ItemStack stack, Placement placement, String overrideName) {
        add(entries, stack, overrideName == null ? name(character, index, stack.getHoverName().getString()) : overrideName,
                description(character, index), placement, lore(character, index));
    }

    private static void add(List<Entry> entries, ItemStack stack, String name,
                            String description, Placement placement) {
        add(entries, stack, name, description, placement, "");
    }

    private static void add(List<Entry> entries, ItemStack stack, String name,
                            String description, Placement placement, String lore) {
        entries.add(new Entry(
                stack,
                name,
                description == null ? "" : description,
                placement,
                lore == null ? "" : lore
        ));
    }

    private static String name(CharacterClass character, int index, String fallback) {
        if (index >= 0 && index < character.getItems().size()) {
            return character.getItems().get(index).getName();
        }
        return fallback;
    }

    private static String description(CharacterClass character, int index) {
        if (index >= 0 && index < character.getItems().size()) {
            return character.getItems().get(index).getDescription();
        }
        return "";
    }

    private static String lore(CharacterClass character, int index) {
        if (index >= 0 && index < character.getItems().size()) {
            return character.getItems().get(index).getLore();
        }
        return "";
    }

    private static String featureDescription(CharacterClass character, String featureName) {
        return character.getFeatures().stream()
                .filter(feature -> feature.hasSourceName(featureName))
                .map(CharacterClass.Feature::getDescription)
                .findFirst()
                .orElse("");
    }

    enum Placement {
        MAIN_HAND(EquipmentSlot.MAINHAND),
        OFF_HAND(EquipmentSlot.OFFHAND),
        HEAD(EquipmentSlot.HEAD),
        CHEST(EquipmentSlot.CHEST),
        LEGS(EquipmentSlot.LEGS),
        FEET(EquipmentSlot.FEET),
        OTHER(null);

        private final EquipmentSlot equipmentSlot;

        Placement(EquipmentSlot equipmentSlot) {
            this.equipmentSlot = equipmentSlot;
        }

        EquipmentSlot equipmentSlot() {
            return equipmentSlot;
        }

        boolean isEquipment() {
            return equipmentSlot != null;
        }
    }

    record Entry(ItemStack stack, String name, String description, Placement placement, String lore) {
    }
}
