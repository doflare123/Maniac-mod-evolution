package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import org.example.maniacrevolution.client.model.KeeperNightmareModel;
import org.example.maniacrevolution.stamina.client.DashPresentation;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public class KeeperNightmareRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, KeeperNightmareAnimatable> {
    public KeeperNightmareRenderer(EntityRendererProvider.Context context) {
        super(context, new KeeperNightmareModel(), new KeeperNightmareAnimatable());
        withScale(0.71F);
    }

    @Override
    protected void applyRotations(KeeperNightmareAnimatable animatable, PoseStack pose, float age, float yaw, float partialTick) {
        super.applyRotations(animatable, pose, age, yaw, partialTick);
        DashPresentation.rollModel(getCurrentEntity(), pose, partialTick, 1.05);
    }
}
