package org.example.maniacrevolution.engineer.client;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.engineer.EngineerDeviceBlockEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class EngineerDeviceRenderer extends GeoBlockRenderer<EngineerDeviceBlockEntity> {
    public EngineerDeviceRenderer() { super(new GeoModel<>() {
        private String stem(EngineerDeviceBlockEntity be) { return be.barricade() ? "engineer_barricade" : "engineer_panel"; }
        @Override public ResourceLocation getModelResource(EngineerDeviceBlockEntity be) { return Maniacrev.loc("geo/" + stem(be) + ".geo.json"); }
        @Override public ResourceLocation getTextureResource(EngineerDeviceBlockEntity be) { return Maniacrev.loc("textures/block/" + stem(be) + ".png"); }
        @Override public ResourceLocation getAnimationResource(EngineerDeviceBlockEntity be) { return Maniacrev.loc("animations/" + stem(be) + ".animation.json"); }
        @Override public void setCustomAnimations(EngineerDeviceBlockEntity be, long id, software.bernie.geckolib.core.animation.AnimationState<EngineerDeviceBlockEntity> state) {
            for (var bone : getAnimationProcessor().getRegisteredBones()) {
                var initial = bone.getInitialSnapshot(); bone.setScaleX(1); bone.setScaleY(1); bone.setScaleZ(1);
                bone.setRotX(initial.getRotX()); bone.setRotY(initial.getRotY()); bone.setRotZ(initial.getRotZ());
            }
            if (be.barricade() || !be.playing()) return;
            var speaker = getAnimationProcessor().getBone("speaker");
            if (speaker != null) { float pulse = 1 + .08f * (float) Math.sin((be.getLevel().getGameTime() + state.getPartialTick()) * 1.3); speaker.setScaleX(pulse); speaker.setScaleY(pulse); }
            var antenna = getAnimationProcessor().getBone("antenna"); if (antenna != null) antenna.setRotZ(.07f * (float) Math.sin((be.getLevel().getGameTime() + state.getPartialTick()) * .3));
        }
    }); }
}
