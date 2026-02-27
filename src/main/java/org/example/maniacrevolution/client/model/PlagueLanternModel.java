package org.example.maniacrevolution.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import org.example.maniacrevolution.item.PlagueLanternItem;

/**
 * Модель чумной лампы (GeckoLib).
 *
 * Убедитесь что файлы лежат по этим путям:
 *   assets/maniacrev/geo/plague_lantern.geo.json
 *   assets/maniacrev/animations/plague_lantern.animation.json
 *   assets/maniacrev/textures/item/plague_lantern.png
 */
public class PlagueLanternModel extends GeoModel<PlagueLanternItem> {

    @Override
    public ResourceLocation getModelResource(PlagueLanternItem animatable) {
        return new ResourceLocation("maniacrev", "geo/plague_lantern.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PlagueLanternItem animatable) {
        return new ResourceLocation("maniacrev", "textures/item/plague_lantern.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PlagueLanternItem animatable) {
        return new ResourceLocation("maniacrev", "animations/plague_lantern.animation.json");
    }
}