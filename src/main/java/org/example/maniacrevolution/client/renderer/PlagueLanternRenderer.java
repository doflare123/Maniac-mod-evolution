package org.example.maniacrevolution.client.renderer;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import org.example.maniacrevolution.item.PlagueLanternItem;
import org.example.maniacrevolution.client.model.PlagueLanternModel;

/**
 * Рендерер чумной лампы.
 *
 * Зарегистрируйте в вашем ClientSetupEvent:
 *   GeoItem.registerItemRenderer(ModItems.PLAGUE_LANTERN.get(), new PlagueLanternRenderer());
 *
 * Файлы, которые должны быть в ресурсах:
 *   assets/maniacrev/geo/plague_lantern.geo.json       — модель (Blockbench)
 *   assets/maniacrev/animations/plague_lantern.animation.json — анимации
 *   assets/maniacrev/textures/item/plague_lantern.png  — текстура
 */
public class PlagueLanternRenderer extends GeoItemRenderer<PlagueLanternItem> {

    public PlagueLanternRenderer() {
        super(new PlagueLanternModel());
    }

    @Override
    public ResourceLocation getTextureLocation(PlagueLanternItem animatable) {
        // Путь к текстуре предмета
        return new ResourceLocation("maniacrev", "textures/item/plague_lantern.png");
    }
}