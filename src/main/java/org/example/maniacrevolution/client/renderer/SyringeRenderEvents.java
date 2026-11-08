package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.SyringeItem;
import org.example.maniacrevolution.network.packets.SyringeAnimationPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Player skins and the paired held-item export share the same server-timed clip. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class SyringeRenderEvents {
    private static final int INJECT_TICKS = 64;
    private record Injection(InteractionHand hand, long startedAt, int slot) {}
    public record Frame(String clip, float seconds, boolean mirror) {}
    private static final Map<UUID, Injection> INJECTIONS = new HashMap<>();
    private static final Map<String, PlayerModel<AbstractClientPlayer>> ARM_MODELS = new HashMap<>();
    private static ClientLevel world;
    private static Player renderingPlayer;
    private static final HumanoidModel.ArmPose SYRINGE_POSE = HumanoidModel.ArmPose.create(
            "MANIACREV_SYRINGE", false, (model, entity, arm) -> {
                if (!(entity instanceof Player player)) return;
                Frame frame = armFrame(player, arm, Minecraft.getInstance().getFrameTime());
                if (frame != null && frame.clip().equals("inject"))
                    rotate(arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm, frame, arm, false);
            });

    private SyringeRenderEvents() {}

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws java.io.IOException {
            SyringeHandRenderType.register(event);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) renderer.addLayer(new HeldLayer(renderer));
            }
        }

        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                SyringeAnimationClips.clear();
                ARM_MODELS.clear();
            });
        }
    }

    public static void start(SyringeAnimationPacket packet) {
        resetWorld();
        if (world == null || !(world.getEntity(packet.playerId()) instanceof Player player)) return;
        INJECTIONS.put(player.getUUID(), new Injection(packet.hand(), packet.startedAt(), player.getInventory().selected));
    }

    private static void resetWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != world) {
            INJECTIONS.clear();
            renderingPlayer = null;
            world = current;
        }
    }

    private static Injection injection(Player player) {
        resetWorld();
        Injection injection = INJECTIONS.get(player.getUUID());
        if (injection == null || world == null) return null;
        var held = player.getItemInHand(injection.hand());
        // Keep a cosmetic copy after the last syringe is consumed; stop on death or switching items.
        if (!player.isAlive() || player.isSpectator() || world.getGameTime() - injection.startedAt() >= INJECT_TICKS
                || (!held.isEmpty() && !(held.getItem() instanceof SyringeItem))
                || (player == Minecraft.getInstance().player && injection.hand() == InteractionHand.MAIN_HAND
                    && player.getInventory().selected != injection.slot())) {
            INJECTIONS.remove(player.getUUID());
            return null;
        }
        return injection;
    }

    private static boolean mirrored(Player player, InteractionHand hand) {
        return (hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
    }

    private static Frame frame(Player player, InteractionHand hand, float partial) {
        if (!player.isAlive() || player.isSpectator()) return null;
        Injection injection = injection(player);
        if (injection != null && injection.hand() == hand) {
            float elapsed = Math.max(0, Math.min(INJECT_TICKS, world.getGameTime() - injection.startedAt() + partial));
            return new Frame("inject", elapsed / 20F, mirrored(player, hand));
        }
        if (!(player.getItemInHand(hand).getItem() instanceof SyringeItem)) return null;
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        boolean moving = !player.isPassenger() && (player.getDeltaMovement().horizontalDistanceSqr() > .00001
                || dx * dx + dz * dz > .00001);
        int length = moving ? 16 : 64;
        return new Frame(moving ? "run" : "idle", (player.level().getGameTime() % length + partial) / 20F, mirrored(player, hand));
    }

    private static Frame armFrame(Player player, HumanoidArm arm, float partial) {
        Injection injection = injection(player);
        if (injection != null) return frame(player, injection.hand(), partial);
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return frame(player, hand, partial);
    }

    private static void rotate(ModelPart part, Frame frame, HumanoidArm arm, boolean firstPerson) {
        boolean sourceArm = (arm == HumanoidArm.LEFT) == frame.mirror();
        if (firstPerson) {
            float[] base = SyringeHeldPose.firstPersonArm(sourceArm, arm == HumanoidArm.LEFT,
                    frame.clip().equals("run"), frame.seconds());
            part.xRot = (float) Math.toRadians(base[0]);
            part.yRot = (float) Math.toRadians(base[1]);
            part.zRot = (float) Math.toRadians(base[2]);
        }
        if (!frame.clip().equals("inject")) return;
        String bone = (arm == HumanoidArm.RIGHT) != frame.mirror() ? "right_arm" : "left_arm";
        float[] angles = SyringeAnimationClips.sample("syringe_player", frame.clip(), bone, "rotation", frame.seconds());
        float weight = SyringeHeldPose.injectionWeight(frame.seconds());
        part.xRot += ((float) Math.toRadians(angles[0]) - part.xRot) * weight;
        part.yRot += ((float) Math.toRadians(frame.mirror() ? -angles[1] : angles[1]) - part.yRot) * weight;
        part.zRot += ((float) Math.toRadians(frame.mirror() ? -angles[2] : angles[2]) - part.zRot) * weight;
    }

    static boolean isRenderingPlayer() { return renderingPlayer != null; }

    @SubscribeEvent
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        resetWorld();
        renderingPlayer = event.getEntity();
        var model = event.getRenderer().getModel();
        float partial = event.getPartialTick();
        Frame right = armFrame(event.getEntity(), HumanoidArm.RIGHT, partial);
        Frame left = armFrame(event.getEntity(), HumanoidArm.LEFT, partial);
        if (right != null && right.clip().equals("inject")) model.rightArmPose = SYRINGE_POSE;
        if (left != null && left.clip().equals("inject")) model.leftArmPose = SYRINGE_POSE;
    }

    @SubscribeEvent
    public static void afterPlayer(RenderPlayerEvent.Post event) { renderingPlayer = null; }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        resetWorld();
        renderingPlayer = null;
        if (world != null) INJECTIONS.entrySet().removeIf(entry ->
                world.getGameTime() - entry.getValue().startedAt() >= INJECT_TICKS || world.getPlayerByUUID(entry.getKey()) == null);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) return;
        Injection injection = injection(player);
        Frame frame = frame(player, injection == null ? event.getHand() : injection.hand(), event.getPartialTick());
        if (frame == null) return;
        event.setCanceled(true);
        // MAIN_HAND is also rendered when the consumed offhand stack has become empty.
        if (injection != null && event.getHand() != InteractionHand.MAIN_HAND) return;

        PlayerModel<AbstractClientPlayer> model = ARM_MODELS.computeIfAbsent(player.getModelName(), skin ->
                new PlayerModel<>(mc.getEntityModels().bakeLayer(skin.equals("slim") ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), skin.equals("slim")));
        renderFirstPerson(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), model,
                player.getSkinTextureLocation(), frame,
                player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE),
                player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));
    }

    static void renderFirstPerson(PoseStack pose, MultiBufferSource buffers, int light,
                                  PlayerModel<AbstractClientPlayer> model, net.minecraft.resources.ResourceLocation skin,
                                  Frame frame, boolean rightSleeve, boolean leftSleeve) {
        boolean injecting = frame.clip().equals("inject");
        pose.pushPose();
        SyringeHeldPose.firstPersonCamera(pose, frame.mirror(), injecting, frame.seconds(), frame.clip().equals("run"));
        for (HumanoidArm arm : HumanoidArm.values()) {
            boolean sourceArm = (arm == HumanoidArm.LEFT) == frame.mirror();
            if (!sourceArm && !injecting) continue;
            ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            ModelPart sleeve = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
            part.resetPose();
            rotate(part, frame, arm, true);
            sleeve.copyFrom(part);
            var vertices = buffers.getBuffer(SyringeHandRenderType.skin(skin));
            part.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
            if (arm == HumanoidArm.RIGHT ? rightSleeve : leftSleeve) {
                sleeve.render(pose, buffers.getBuffer(SyringeHandRenderType.texture(skin)), light, OverlayTexture.NO_OVERLAY);
            }
            if (sourceArm) {
                pose.pushPose();
                part.translateAndRotate(pose);
                SyringeItemRenderer.held(pose, buffers, light, frame, true);
                pose.popPose();
            }
        }
        pose.popPose();
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
                pose.pushPose();
                (frame.mirror() ? getParentModel().leftArm : getParentModel().rightArm).translateAndRotate(pose);
                SyringeItemRenderer.held(pose, buffers, light, frame);
                pose.popPose();
            }
        }
    }
}
