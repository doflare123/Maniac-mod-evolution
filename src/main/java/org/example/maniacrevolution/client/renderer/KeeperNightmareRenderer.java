package org.example.maniacrevolution.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import org.example.maniacrevolution.client.model.KeeperNightmareModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public class KeeperNightmareRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, KeeperNightmareAnimatable> {
    public KeeperNightmareRenderer(EntityRendererProvider.Context context) {
        super(context, new KeeperNightmareModel(), new KeeperNightmareAnimatable());
        withScale(0.71F);
    }
}
