package org.example.maniacrevolution.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.potion.ModPotions;

import java.util.ArrayList;
import java.util.List;

public class RecipeBookScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(Maniacrev.MODID, "textures/gui/recipe_book_background.png");

    // Увеличенные размеры GUI
    private static final int GUI_WIDTH = 252; // 420 * 0.6
    private static final int GUI_HEIGHT = 277; // 462 * 0.6

    private int leftPos;
    private int topPos;
    private int currentPage = 0;
    private static final int RECIPES_PER_PAGE = 5;

    // Список всех рецептов
    private final List<RecipeEntry> recipes = new ArrayList<>();

    // Для хранения позиций предметов для тултипов
    private final List<ItemRenderInfo> itemsToRender = new ArrayList<>();

    public RecipeBookScreen() {
        super(Component.translatable("gui.maniacrev.recipe_book"));
        initRecipes();
    }

    private void initRecipes() {
        recipes.add(new RecipeEntry(
                ModItems.MANIAC_REGEN_FRAGMENT.get().getDefaultInstance(),
                ModPotions.SLOW_REGENERATION_POTION.get(),
                Component.translatable("potion.effect.slow_regeneration"),
                Component.literal("5 HP за 1 минуту")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_STRENGTH_FRAGMENT.get().getDefaultInstance(),
                ModPotions.STRENGTH_POTION.get(),
                Component.translatable("potion.effect.strength"),
                Component.literal("Увеличивает урон")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_SPEED_FRAGMENT.get().getDefaultInstance(),
                ModPotions.SPEED_POTION.get(),
                Component.translatable("potion.effect.speed"),
                Component.literal("Скорость II на 5 сек")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_SLOWNESS_FRAGMENT.get().getDefaultInstance(),
                ModPotions.SLOWNESS_POTION.get(),
                Component.translatable("potion.effect.slowness"),
                Component.literal("Замедление III на 5 сек")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_SLOW_FALLING_FRAGMENT.get().getDefaultInstance(),
                ModPotions.SLOW_FALLING_POTION.get(),
                Component.translatable("potion.effect.slow_falling"),
                Component.literal("Медленное падение 30 сек")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_WEAKNESS_FRAGMENT.get().getDefaultInstance(),
                ModPotions.WEAK_WEAKNESS_POTION.get(),
                Component.translatable("potion.effect.weak_weakness"),
                Component.literal("-2 урона на 15 сек")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_HEALING_FRAGMENT.get().getDefaultInstance(),
                ModPotions.WEAK_HEALING_POTION.get(),
                Component.translatable("potion.effect.weak_healing"),
                Component.literal("Восстанавливает 1 HP")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_BLINDNESS_FRAGMENT.get().getDefaultInstance(),
                ModPotions.BLINDNESS_POTION.get(),
                Component.translatable("potion.effect.blindness"),
                Component.literal("Слепота на 10 сек")
        ));

        recipes.add(new RecipeEntry(
                ModItems.MANIAC_GLOWING_FRAGMENT.get().getDefaultInstance(),
                ModPotions.GLOWING_POTION.get(),
                Component.translatable("potion.effect.glowing"),
                Component.literal("Свечение на 5 сек")
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Кнопки для переключения страниц
        int totalPages = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);

        if (totalPages > 1) {
            // Кнопка "Назад"
            this.addRenderableWidget(Button.builder(
                            Component.literal("<"),
                            button -> {
                                if (currentPage > 0) {
                                    currentPage--;
                                }
                            })
                    .bounds(leftPos + 25, topPos + GUI_HEIGHT - 35, 30, 20)
                    .build()
            );

            // Кнопка "Вперёд"
            this.addRenderableWidget(Button.builder(
                            Component.literal(">"),
                            button -> {
                                if (currentPage < totalPages - 1) {
                                    currentPage++;
                                }
                            })
                    .bounds(leftPos + GUI_WIDTH - 55, topPos + GUI_HEIGHT - 35, 30, 20)
                    .build()
            );
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Очищаем список предметов для рендера
        itemsToRender.clear();

        // Рисуем фон (свиток)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        // Рисуем рецепты текущей страницы
        renderRecipes(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Рисуем тултипы после всего остального
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderRecipes(GuiGraphics graphics) {
        int startX = leftPos + 65;
        int startY = topPos + 60;
        int recipeSpacing = 35;

        int startIndex = currentPage * RECIPES_PER_PAGE;
        int endIndex = Math.min(startIndex + RECIPES_PER_PAGE, recipes.size());

        for (int i = startIndex; i < endIndex; i++) {
            RecipeEntry recipe = recipes.get(i);
            int yOffset = (i - startIndex) * recipeSpacing;
            renderRecipe(graphics, startX, startY + yOffset, recipe);
        }

        // Рисуем номер страницы
        int totalPages = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);
        String pageText = (currentPage + 1) + " / " + totalPages;
        int pageTextWidth = this.font.width(pageText);
        graphics.drawString(this.font, pageText,
                leftPos + (GUI_WIDTH - pageTextWidth) / 2,
                topPos + GUI_HEIGHT - 25,
                0x3F3F3F, false);
    }

    private void renderRecipe(GuiGraphics graphics, int x, int y, RecipeEntry recipe) {
        int itemSize = 20; // Увеличенный размер предметов

        // Awkward Potion (взрывное)
        ItemStack awkwardPotion = new ItemStack(Items.SPLASH_POTION);
        net.minecraft.world.item.alchemy.PotionUtils.setPotion(awkwardPotion,
                net.minecraft.world.item.alchemy.Potions.AWKWARD);
        graphics.renderItem(awkwardPotion, x, y);
        itemsToRender.add(new ItemRenderInfo(awkwardPotion, x, y, itemSize));

        // Знак "+"
        graphics.drawString(this.font, "+", x + 25, y + 6, 0x3F3F3F, false);

        // Ингредиент
        graphics.renderItem(recipe.ingredient, x + 50, y);
        itemsToRender.add(new ItemRenderInfo(recipe.ingredient, x + 50, y, itemSize));

        // Знак "="
        graphics.drawString(this.font, "=", x + 75, y + 6, 0x3F3F3F, false);

        // Результат (взрывное зелье)
        ItemStack resultPotion = new ItemStack(Items.SPLASH_POTION);
        net.minecraft.world.item.alchemy.PotionUtils.setPotion(resultPotion, recipe.potion);
        graphics.renderItem(resultPotion, x + 100, y);
        itemsToRender.add(new ItemRenderInfo(resultPotion, x + 100, y, itemSize,
                recipe.name, recipe.description));
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ItemRenderInfo info : itemsToRender) {
            if (mouseX >= info.x && mouseX <= info.x + info.size &&
                    mouseY >= info.y && mouseY <= info.y + info.size) {

                List<Component> tooltip = new ArrayList<>();

                // Если есть кастомное имя и описание (для результата)
                if (info.customName != null) {
                    tooltip.add(info.customName);
                    if (info.customDescription != null) {
                        tooltip.add(info.customDescription);
                    }
                } else {
                    // Стандартный тултип предмета
                    tooltip.add(info.stack.getHoverName());
                }

                graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Закрытие по ESC
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            this.onClose();
            return true;
        }

        // Закрытие по E (инвентарь)
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Вспомогательный класс для хранения рецепта
    private static class RecipeEntry {
        final ItemStack ingredient;
        final net.minecraft.world.item.alchemy.Potion potion;
        final Component name;
        final Component description;

        RecipeEntry(ItemStack ingredient, net.minecraft.world.item.alchemy.Potion potion,
                    Component name, Component description) {
            this.ingredient = ingredient;
            this.potion = potion;
            this.name = name;
            this.description = description;
        }
    }

    // Вспомогательный класс для хранения информации о рендере предмета
    private static class ItemRenderInfo {
        final ItemStack stack;
        final int x;
        final int y;
        final int size;
        final Component customName;
        final Component customDescription;

        ItemRenderInfo(ItemStack stack, int x, int y, int size) {
            this(stack, x, y, size, null, null);
        }

        ItemRenderInfo(ItemStack stack, int x, int y, int size,
                       Component customName, Component customDescription) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.size = size;
            this.customName = customName;
            this.customDescription = customDescription;
        }
    }
}