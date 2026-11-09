package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.model.BongModel;
import org.example.maniacrevolution.item.BongItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BongItemRenderer extends GeoItemRenderer<BongItem> {
    private static final HeldVisual HELD = new HeldVisual();
    private static final GeoObjectRenderer<HeldVisual> HELD_RENDERER = new GeoObjectRenderer<>(new HeldModel());

    public BongItemRenderer() {
        super(new BongModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if ((context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                && BongRenderEvents.isRenderingPlayer()) return;
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }

    static void held(PoseStack pose, MultiBufferSource buffers, int light, ModelPart arm, BongRenderEvents.Frame frame) {
        pose.pushPose();
        BongThirdPersonPose.mount(pose, arm, frame.mirror());
        HELD.frame = frame;
        ResourceLocation texture = Maniacrev.loc("textures/item/bong.png");
        RenderType type = RenderType.entityCutoutNoCull(texture);
        pose.translate(-.5, -.51, -.5); // Cancel GeoObjectRenderer's block-centering offset.
        HELD_RENDERER.render(pose, HELD, buffers, type, buffers.getBuffer(type), light);
        pose.popPose();
    }

    private static final class HeldVisual implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private BongRenderEvents.Frame frame;
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public double getTick(Object object) { return frame == null ? 0 : frame.seconds() * 20; }
    }

    private static final class HeldModel extends GeoModel<HeldVisual> {
        @Override public ResourceLocation getModelResource(HeldVisual visual) { return Maniacrev.loc("geo/bong.geo.json"); }
        @Override public ResourceLocation getTextureResource(HeldVisual visual) { return Maniacrev.loc("textures/item/bong.png"); }
        @Override public ResourceLocation getAnimationResource(HeldVisual visual) { return Maniacrev.loc("animations/bong.animation.json"); }

        @Override
        public void handleAnimations(HeldVisual visual, long id, AnimationState<HeldVisual> state) {
            // Each player can be at a different action time in the same rendered frame.
            for (String name : new String[]{"bone", "liquid"}) {
                float[] rotation = SyringeAnimationClips.sample("bong", visual.frame.clip(), name, "rotation", visual.frame.seconds());
                if (name.equals("bone") && visual.frame.clip().equals("using")) rotation = new float[3];
                // The arm supplies all movement; never repeat the first-person root translation.
                SyringeAnimationClips.pose(getBone(name).orElseThrow(), rotation, new float[3], new float[]{1, 1, 1});
            }
        }
    }
}
