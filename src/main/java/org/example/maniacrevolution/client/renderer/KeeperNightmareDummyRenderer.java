package org.example.maniacrevolution.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.example.maniacrevolution.client.model.KeeperNightmareDummyModel;
import org.example.maniacrevolution.entity.KeeperNightmareDummyEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KeeperNightmareDummyRenderer extends GeoEntityRenderer<KeeperNightmareDummyEntity> {
    public KeeperNightmareDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new KeeperNightmareDummyModel());
        withScale(0.95F);
    }
}
