package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.BongItem;

/** Arms, sleeves, armor and the wrist-mounted bong share the server's use timestamp. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class BongRenderEvents {
    public record Frame(String clip, float seconds, boolean mirror) {}
    private static Player renderingPlayer;
    private static final HumanoidModel.ArmPose USING_POSE = HumanoidModel.ArmPose.create(
            "MANIACREV_BONG", true, (model, entity, arm) -> {
                if (!(entity instanceof Player player)) return;
                InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                Frame frame = frame(player, hand, Minecraft.getInstance().getFrameTime());
                if (frame == null || !frame.clip().equals("using")) return;
                ModelPart holding = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                ModelPart supporting = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                BongThirdPersonPose.rotate(holding, true, frame.mirror(), frame.seconds());
                InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                if (player.getItemInHand(other).isEmpty())
                    BongThirdPersonPose.rotate(supporting, false, frame.mirror(), frame.seconds());
            });

    private BongRenderEvents() {}

    public static HumanoidModel.ArmPose armPose(LivingEntity entity, ItemStack stack) {
        return entity instanceof Player player && useSeconds(player, stack, Minecraft.getInstance().getFrameTime()) >= 0
                ? USING_POSE : null;
    }

    static float useSeconds(Player player, ItemStack stack, float partial) {
        if (!(stack.getItem() instanceof BongItem) || !player.isAlive() || player.isSpectator()) return -1;
        long start = BongItem.getUseStartedAt(stack);
        float ticks = player.level().getGameTime() - start + partial;
        return start >= 0 && ticks >= 0 && ticks < BongItem.USING_ANIMATION_TICKS ? ticks / 20F : -1;
    }

    private static Frame frame(Player player, InteractionHand hand, float partial) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BongItem) || !player.isAlive() || player.isSpectator()) return null;
        boolean mirror = (hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
        float seconds = useSeconds(player, stack, partial);
        if (seconds >= 0) return new Frame("using", seconds, mirror);
        boolean running = player.isSprinting() && !player.isPassenger();
        return new Frame(running ? "run" : "idle", (player.level().getGameTime() % (running ? 16 : 64) + partial) / 20F, mirror);
    }

    static boolean isRenderingPlayer() { return renderingPlayer != null; }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        renderingPlayer = event.isCanceled() ? null : event.getEntity();
    }

    @SubscribeEvent
    public static void afterPlayer(RenderPlayerEvent.Post event) { renderingPlayer = null; }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) renderingPlayer = null;
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) renderer.addLayer(new HeldLayer(renderer));
            }
        }
    }

    private static final class HeldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private HeldLayer(PlayerRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           float swing, float amount, float partial, float age, float yaw, float pitch) {
            if (player.isInvisible()) return;
            for (InteractionHand hand : InteractionHand.values()) {
                Frame frame = frame(player, hand, partial);
                if (frame == null) continue;
                ModelPart arm = frame.mirror() ? getParentModel().leftArm : getParentModel().rightArm;
                pose.pushPose();
                arm.translateAndRotate(pose);
                BongItemRenderer.held(pose, buffers, light, arm, frame);
                pose.popPose();
            }
        }
    }
}
