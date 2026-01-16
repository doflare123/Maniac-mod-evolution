package org.example.maniacrevolution.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация магазина Агента 47
 * Позволяет редактировать товары через JSON файл
 * ВАЖНО: Конфиг создается в папке config/ в корне сервера/клиента
 */
public class Agent47ShopConfig {

    // Награда за убийство цели
    public static final int KILL_TARGET_REWARD = 100;

    // Путь к конфигурации магазина (относительно корня игры/сервера)
    private static final String CONFIG_DIR = "config/maniacrevolution";
    private static final String CONFIG_FILE = "agent47_shop.json";

    // Список товаров
    private static List<ShopItem> shopItems = new ArrayList<>();

    // Gson для сериализации
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Класс товара в магазине
     */
    public static class ShopItem {
        public String id;              // Уникальный ID товара
        public String name;            // Название товара
        public String description;     // Описание
        public int price;              // Цена
        public ShopItemType type;      // Тип товара
        public String data;            // Дополнительные данные (зависит от типа)
        public int amount;             // Количество (для предметов)
        public int duration;           // Длительность в секундах (для дебафов)
        public int amplifier;          // Уровень эффекта (для дебафов)

        public ShopItem(String id, String name, String description, int price, ShopItemType type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.type = type;
            this.amount = 1;
            this.duration = 10;
            this.amplifier = 0;
        }
    }

    /**
     * Типы товаров
     */
    public enum ShopItemType {
        ITEM,           // Обычный предмет Minecraft
        DEBUFF_GLOW,    // Подсветка цели
        DEBUFF_SLOW,    // Замедление
        DEBUFF_WEAK,    // Слабость
        CUSTOM          // Кастомный товар (для будущего расширения)
    }

    /**
     * Инициализация конфигурации
     */
    public static void init() {
        loadConfig();
    }

    /**
     * Загружает конфигурацию из файла
     */
    private static void loadConfig() {
        try {
            // Получаем путь к конфигу (в корне игры/сервера)
            File configDir = new File(CONFIG_DIR);
            File configFile = new File(configDir, CONFIG_FILE);

            // Создаем директорию, если не существует
            if (!configDir.exists()) {
                configDir.mkdirs();
                System.out.println("[Agent47Shop] Created config directory: " + configDir.getAbsolutePath());
            }

            if (!configFile.exists()) {
                // Создаем конфигурацию по умолчанию
                createDefaultConfig();
                saveConfig();
                System.out.println("[Agent47Shop] Created default config at: " + configFile.getAbsolutePath());
            } else {
                // Читаем существующий файл
                Reader reader = new FileReader(configFile, StandardCharsets.UTF_8);
                Type listType = new TypeToken<ArrayList<ShopItem>>(){}.getType();
                shopItems = GSON.fromJson(reader, listType);
                reader.close();

                System.out.println("[Agent47Shop] Loaded " + shopItems.size() + " items from: " + configFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("[Agent47Shop] Error loading config: " + e.getMessage());
            e.printStackTrace();
            createDefaultConfig();
        }
    }

    /**
     * Сохраняет конфигурацию в файл
     */
    private static void saveConfig() {
        try {
            File configDir = new File(CONFIG_DIR);
            File configFile = new File(configDir, CONFIG_FILE);

            Writer writer = new FileWriter(configFile, StandardCharsets.UTF_8);
            GSON.toJson(shopItems, writer);
            writer.close();
            System.out.println("[Agent47Shop] Config saved to: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Agent47Shop] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Создает конфигурацию по умолчанию
     */
    private static void createDefaultConfig() {
        shopItems.clear();

        // Дебафы для цели
        ShopItem glowDebuff = new ShopItem(
                "debuff_glow",
                "§eПодсветка цели",
                "§7Подсвечивает текущую цель на 5 секунд",
                50,
                ShopItemType.DEBUFF_GLOW
        );
        glowDebuff.duration = 5;
        shopItems.add(glowDebuff);

        ShopItem slowDebuff = new ShopItem(
                "debuff_slow",
                "§bЗамедление I",
                "§7Замедляет цель на 15 секунд",
                75,
                ShopItemType.DEBUFF_SLOW
        );
        slowDebuff.duration = 15;
        slowDebuff.amplifier = 0;
        shopItems.add(slowDebuff);

        ShopItem weakDebuff = new ShopItem(
                "debuff_weak",
                "§cСлабость I",
                "§7Ослабляет цель на 10 секунд",
                75,
                ShopItemType.DEBUFF_WEAK
        );
        weakDebuff.duration = 10;
        weakDebuff.amplifier = 0;
        shopItems.add(weakDebuff);

        // Оружие и патроны
        ShopItem shotgun = new ShopItem(
                "shotgun",
                "§6Дробовик",
                "§7Для больших городов",
                250,
                ShopItemType.ITEM
        );
        shotgun.data = "cgm:shotgun{AmmoCount:1,Color:1908001}";
        shotgun.amount = 1;
        shopItems.add(shotgun);

        ShopItem pistolAmmo = new ShopItem(
                "pistol_ammo",
                "§fПатроны для пистолета",
                "§7Нужны для стрельбы из пистолета",
                50,
                ShopItemType.ITEM
        );
        pistolAmmo.data = "cgm:basic_bullet";
        pistolAmmo.amount = 2;
        shopItems.add(pistolAmmo);

        ShopItem shotgunAmmo = new ShopItem(
                "shotgun_ammo",
                "§6Патроны для дробовика",
                "§7Чтобы устраивать большие города",
                100,
                ShopItemType.ITEM
        );
        shotgunAmmo.data = "cgm:shell";
        shotgunAmmo.amount = 1;
        shopItems.add(shotgunAmmo);

        ShopItem sniperAmmo = new ShopItem(
                "sniper_ammo",
                "§eПатроны для снайперки",
                "§7Почувствуй себя снайпером",
                150,
                ShopItemType.ITEM
        );
        sniperAmmo.data = "cgm:advanced_bullet";
        sniperAmmo.amount = 1;
        shopItems.add(sniperAmmo);

        ShopItem rifle = new ShopItem(
                "rifle",
                "§eСнайперка",
                "§7Подходит для стрельбы с дальних дистанций",
                300,
                ShopItemType.ITEM
        );
        rifle.data = "cgm:rifle{AmmoCount:1,Color:1908001}";
        rifle.amount = 1;
        shopItems.add(rifle);

        ShopItem bandage = new ShopItem(
                "bandage",
                "§aBинты",
                "§7Помогают залечивать раны, восстанавливают 1 хп",
                25,
                ShopItemType.ITEM
        );
        bandage.data = "maniacrevolution:bandage";
        bandage.amount = 1;
        shopItems.add(bandage);

        System.out.println("[Agent47Shop] Created default config with " + shopItems.size() + " items");
    }

    /**
     * Получает список всех товаров
     */
    public static List<ShopItem> getShopItems() {
        return new ArrayList<>(shopItems);
    }

    /**
     * Получает товар по ID
     */
    public static ShopItem getItem(String id) {
        return shopItems.stream()
                .filter(item -> item.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Покупает товар для игрока
     */
    public static boolean purchaseItem(ServerPlayer buyer, String itemId) {
        ShopItem item = getItem(itemId);
        if (item == null) {
            buyer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cТовар не найден!"),
                    false
            );
            return false;
        }

        // Проверяем деньги
        if (!Agent47MoneyManager.hasMoney(buyer, item.price)) {
            buyer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("§cНедостаточно монет! Нужно: %d, у вас: %d",
                                    item.price, Agent47MoneyManager.getMoney(buyer))
                    ),
                    false
            );
            return false;
        }

        // Получаем текущую цель
        ServerPlayer target = Agent47TargetManager.getCurrentTarget(buyer);

        // Применяем эффект в зависимости от типа
        switch (item.type) {
            case DEBUFF_GLOW:
                if (target == null) {
                    buyer.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cУ вас нет цели!"),
                            false
                    );
                    return false;
                }
                SelectiveGlowingEffect.addGlowing(target, buyer, item.duration * 20);
                buyer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aЦель подсвечена на " + item.duration + " секунд!"),
                        false
                );
                break;

            case DEBUFF_SLOW:
                if (target == null) {
                    buyer.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cУ вас нет цели!"),
                            false
                    );
                    return false;
                }
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        item.duration * 20,
                        item.amplifier,
                        false,
                        true,
                        true
                ));
                buyer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aЦель замедлена!"),
                        false
                );
                target.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cВы замедлены агентом!"),
                        false
                );
                break;

            case DEBUFF_WEAK:
                if (target == null) {
                    buyer.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cУ вас нет цели!"),
                            false
                    );
                    return false;
                }
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS,
                        item.duration * 20,
                        item.amplifier,
                        false,
                        true,
                        true
                ));
                buyer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aЦель ослаблена!"),
                        false
                );
                target.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cВы ослаблены агентом!"),
                        false
                );
                break;

            case ITEM:
                // Даем предмет игроку
                try {
                    ItemStack stack = parseItemStack(item.data, item.amount);
                    if (stack != null) {
                        buyer.getInventory().add(stack);
                        buyer.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§aКуплено: " + item.name),
                                false
                        );
                    } else {
                        buyer.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§cОшибка: предмет не найден!"),
                                false
                        );
                        return false;
                    }
                } catch (Exception e) {
                    buyer.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cОшибка при создании предмета!"),
                            false
                    );
                    return false;
                }
                break;

            default:
                buyer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cНеизвестный тип товара!"),
                        false
                );
                return false;
        }

        // Списываем деньги
        Agent47MoneyManager.removeMoney(buyer, item.price);

        buyer.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§e-%d монет. Баланс: %d", item.price, Agent47MoneyManager.getMoney(buyer))
                ),
                true
        );

        return true;
    }

    /**
     * Парсит ItemStack из строки с поддержкой NBT
     * Формат: "modid:item_id{NBT}" или "modid:item_id"
     */
    private static ItemStack parseItemStack(String data, int amount) {
        try {
            // Проверяем наличие NBT тегов
            String itemId;
            String nbtData = null;

            if (data.contains("{")) {
                int nbtStart = data.indexOf("{");
                itemId = data.substring(0, nbtStart);
                nbtData = data.substring(nbtStart);
            } else {
                itemId = data;
            }

            // Получаем предмет
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    new net.minecraft.resources.ResourceLocation(itemId)
            );

            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = new ItemStack(item, amount);

                // Применяем NBT, если есть
                if (nbtData != null && !nbtData.isEmpty()) {
                    try {
                        // Парсим NBT строку
                        net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.TagParser.parseTag(nbtData);
                        stack.setTag(nbt);
                    } catch (Exception e) {
                        System.err.println("[Agent47Shop] Error parsing NBT for " + itemId + ": " + e.getMessage());
                        // Возвращаем предмет без NBT
                    }
                }

                return stack;
            }
        } catch (Exception e) {
            System.err.println("[Agent47Shop] Error parsing item: " + data);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Перезагружает конфигурацию
     */
    public static void reload() {
        loadConfig();
    }
}