package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One geometry for inventory and hands; the hand mount bypasses item display transforms. */
public final class SyringeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Visual VISUAL = new Visual();
    private static final GeoObjectRenderer<Visual> RENDERER = new GeoObjectRenderer<>(new Model());

    public SyringeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        // A skin-aware player layer renders held syringes, including the last consumed syringe.
        if ((context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                && SyringeRenderEvents.isRenderingPlayer()) return;
        pose.pushPose();
        pose.translate(.5, .5, .5); // Undo ItemRenderer's block-model centering.
        float ticks = Minecraft.getInstance().level == null ? 0 :
                Minecraft.getInstance().level.getGameTime() % 64 + Minecraft.getInstance().getFrameTime();
        render(pose, buffers, light, new SyringeRenderEvents.Frame("idle", ticks / 20F, false), false, false);
        pose.popPose();
    }

    static void held(PoseStack pose, MultiBufferSource buffers, int light, SyringeRenderEvents.Frame frame) {
        held(pose, buffers, light, frame, false);
    }

    static void held(PoseStack pose, MultiBufferSource buffers, int light, SyringeRenderEvents.Frame frame, boolean firstPerson) {
        // The supplied injection uses the back of the fist (+Z); ordinary holding
        // uses the front (-Z), so the upright needle cannot disappear inside the forearm.
        // Vanilla skin-model coordinates reflect Blockbench X and Y.
        pose.pushPose();
        boolean injecting = frame.clip().equals("inject");
        pose.translate((frame.mirror() ? 1 : -1) / 16F, 8.5F / 16F,
                SyringeHeldPose.gripZ(injecting, frame.seconds()) / 16F);
        pose.scale(frame.mirror() ? -.5F : .5F, -.5F, .5F);
        float[] rotation = SyringeAnimationClips.sample(injecting ? "syringe_held" : "syringe", frame.clip(),
                "bone", "rotation", frame.seconds());
        pose.mulPose(SyringeHeldPose.itemRotation(rotation, injecting, frame.seconds(), firstPerson));
        render(pose, buffers, light, frame, true, firstPerson);
        pose.popPose();
    }

    private static void render(PoseStack pose, MultiBufferSource buffers, int light,
                               SyringeRenderEvents.Frame frame, boolean held, boolean firstPerson) {
        VISUAL.frame = frame;
        VISUAL.held = held;
        ResourceLocation texture = Maniacrev.loc("textures/item/syringe.png");
        RenderType type = firstPerson ? SyringeHandRenderType.texture(texture) : RenderType.entityTranslucent(texture);
        pose.pushPose();
        // GeoObjectRenderer 4.4.7 adds a block-centering offset in preRender.
        pose.translate(-.5, -.51, -.5);
        RENDERER.render(pose, VISUAL, buffers, type, buffers.getBuffer(type), light);
        pose.popPose();
    }

    private static final class Visual implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private SyringeRenderEvents.Frame frame;
        private boolean held;
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public double getTick(Object object) {
            var level = Minecraft.getInstance().level;
            return level == null ? 0 : level.getGameTime() + Minecraft.getInstance().getFrameTime();
        }
    }

    private static final class Model extends GeoModel<Visual> {
        @Override public ResourceLocation getModelResource(Visual visual) { return Maniacrev.loc("geo/syringe.geo.json"); }
        @Override public ResourceLocation getTextureResource(Visual visual) { return Maniacrev.loc("textures/item/syringe.png"); }
        @Override public ResourceLocation getAnimationResource(Visual visual) { return Maniacrev.loc("animations/syringe.animation.json"); }

        @Override
        public void handleAnimations(Visual visual, long id, AnimationState<Visual> state) {
            // Sample every draw: inventory and several players may render different clips in one tick.
            setCustomAnimations(visual, id, state);
        }

        @Override
        public void setCustomAnimations(Visual visual, long id, AnimationState<Visual> state) {
            String file = visual.held ? "syringe_held" : "syringe";
            for (String name : new String[]{"bone", "barrel", "plunger", "liquid", "bubble_01", "bubble_02", "bubble_03", "needle"}) {
                var bone = getBone(name).orElseThrow();
                float[] r = SyringeAnimationClips.sample(file, visual.frame.clip(), name, "rotation", visual.frame.seconds());
                // held() applies the wrist orientation once, independently of the shoulder pose.
                if (visual.held && name.equals("bone")) r = new float[3];
                float[] p = SyringeAnimationClips.sample(file, visual.frame.clip(), name, "position", visual.frame.seconds());
                float[] s = SyringeAnimationClips.sample(file, visual.frame.clip(), name, "scale", visual.frame.seconds());
                SyringeAnimationClips.pose(bone, r, p, s);
            }
        }
    }
}
