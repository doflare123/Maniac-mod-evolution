package org.example.maniacrevolution;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.maniacrevolution.item.PerkOpenItem;
import org.example.maniacrevolution.item.SaltItem;
import org.example.maniacrevolution.item.ShopOpenItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Maniacrev.MODID);

    public static final RegistryObject<Item> SHOP_TOKEN = ITEMS.register("shop_token",
            () -> new ShopOpenItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PERK_TOKEN = ITEMS.register("perk_token",
            () -> new PerkOpenItem(new Item.Properties().stacksTo(1)));


    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new SaltItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
