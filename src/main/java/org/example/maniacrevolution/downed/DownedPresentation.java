package org.example.maniacrevolution.downed;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.example.maniacrevolution.Maniacrev;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DownedPresentation {
    private static final ResourceLocation LAYER = new ResourceLocation(Maniacrev.MODID, "downed");
    private static final ResourceLocation IDLE = new ResourceLocation(Maniacrev.MODID, "downed_idle");
    private static final float SIDE_LIMIT = 90.0F;
    private static final float UP_LIMIT = -85.0F;

    private DownedPresentation() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 100, DownedLayer::new);
            MinecraftForge.EVENT_BUS.addListener(DownedPresentation::tick);
            MinecraftForge.EVENT_BUS.addListener(DownedPresentation::updateInput);
            MinecraftForge.EVENT_BUS.addListener(DownedPresentation::renderHand);
        });
    }

    static void sync(DownedPosePacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(packet.playerId()) instanceof AbstractClientPlayer player)) return;
        DownedData data = DownedCapability.get(player);
        if (data == null) return;

        boolean downed = Float.isFinite(packet.bodyYaw());
        boolean wasDowned = data.getState() == DownedState.DOWNED;
        data.setState(downed ? DownedState.DOWNED : DownedState.ALIVE);
        data.setBodyYaw(packet.bodyYaw());
        if (player instanceof LocalPlayer local && (downed || wasDowned)) {
            local.setForcedPose(downed ? Pose.SWIMMING : null);
            local.setPose(downed ? Pose.SWIMMING : Pose.STANDING);
            if (downed) clampLook(local);
        }
        updateAnimation(player);
    }

    public static float bodyYaw(AbstractClientPlayer player, float original) {
        DownedData data = DownedCapability.get(player);
        return data != null && data.getState() == DownedState.DOWNED ? data.getBodyYaw() : original;
    }

    public static boolean turn(LocalPlayer player, double yawChange, double pitchChange) {
        if (!DownedCapability.isDowned(player)) return false;
        float body = bodyYaw(player, player.getYRot());
        float yaw = relativeYaw(player.getYRot(), body);
        float pitch = clampPitch(player.getXRot());
        float nextYaw = Mth.clamp(yaw + (float) yawChange * 0.15F, -SIDE_LIMIT, SIDE_LIMIT);
        float nextPitch = clampPitch(pitch + (float) pitchChange * 0.15F);
        player.setYRot(body + nextYaw);
        player.setXRot(nextPitch);
        player.yRotO = body + Mth.clamp(relativeYaw(player.yRotO, body) + nextYaw - yaw, -SIDE_LIMIT, SIDE_LIMIT);
        player.xRotO = clampPitch(player.xRotO + nextPitch - pitch);
        return true;
    }

    private static void clampLook(LocalPlayer player) {
        float body = bodyYaw(player, player.getYRot());
        player.setYRot(body + relativeYaw(player.getYRot(), body));
        player.setXRot(clampPitch(player.getXRot()));
        player.yRotO = body + relativeYaw(player.yRotO, body);
        player.xRotO = clampPitch(player.xRotO);
    }

    private static float relativeYaw(float yaw, float body) {
        return Mth.clamp(Mth.wrapDegrees(yaw - body), -SIDE_LIMIT, SIDE_LIMIT);
    }

    private static float clampPitch(float pitch) {
        return Mth.clamp(pitch, UP_LIMIT, 0.0F);
    }

    private static void updateInput(MovementInputUpdateEvent event) {
        if (DownedCapability.isDowned(event.getEntity())) event.getInput().jumping = false;
    }

    private static void renderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && DownedCapability.isDowned(player)) event.setCanceled(true);
    }

    private static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            if (player instanceof LocalPlayer local && DownedCapability.isDowned(local)) clampLook(local);
            updateAnimation(player);
        }
    }

    private static void updateAnimation(AbstractClientPlayer player) {
        if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof DownedLayer layer) {
            layer.update();
        }
    }

    private static final class DownedLayer extends ModifierLayer<KeyframeAnimationPlayer> {
        private final AbstractClientPlayer player;
        private KeyframeAnimation clip;

        DownedLayer(AbstractClientPlayer player) {
            this.player = player;
        }

        void update() {
            KeyframeAnimation next = DownedCapability.isDowned(player) ? PlayerAnimationRegistry.getAnimation(IDLE) : null;
            if (clip == next) return;
            clip = next;
            setAnimation(clip == null ? null : new KeyframeAnimationPlayer(clip,
                    Math.floorMod(player.level().getGameTime(), clip.endTick + 1)));
        }

        @Override
        public Vec3f get3DTransform(String part, TransformType type, float fraction, Vec3f original) {
            Vec3f pose = super.get3DTransform(part, type, fraction, original);
            if (!part.equals("head") || type != TransformType.ROTATION || clip == null) return pose;

            float body = bodyYaw(player, player.getYRot());
            float yaw = player instanceof LocalPlayer ? relativeYaw(player.getYRot(), body)
                    : Mth.lerp(fraction, relativeYaw(player.yRotO, body), relativeYaw(player.getYRot(), body));
            float pitch = clampPitch(player.getViewXRot(fraction));
            Quaternionf look = new Quaternionf().rotationY(yaw * Mth.DEG_TO_RAD).rotateX(pitch * Mth.DEG_TO_RAD);

            // The vanilla model flips X and Y after PlayerAnimator applies the root.
            Vec3f root = super.get3DTransform("body", TransformType.ROTATION, fraction, Vec3f.ZERO);
            Quaternionf prone = new Quaternionf().rotationZYX(root.getZ(), -root.getY(), -root.getX());
            Quaternionf rest = new Quaternionf().rotationZYX(pose.getZ(), pose.getY(), pose.getX());
            Vector3f up = rest.transform(0.0F, -1.0F, 0.0F, new Vector3f());
            look.transformInverse(prone.transform(up));
            // Keep the side tilt of the resting pose without letting the crown turn into the floor.
            float roll = (float) Math.atan2(up.x, Math.max(0.25F, -up.y));
            Vector3f angles = prone.conjugate().mul(look.rotateZ(roll)).getEulerAnglesZYX(new Vector3f());
            return new Vec3f(angles.x, angles.y, angles.z);
        }
    }
}
