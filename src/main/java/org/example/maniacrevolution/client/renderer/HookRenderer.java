package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.entity.HookTrajectory;
import org.joml.Vector3f;

public class HookRenderer extends EntityRenderer<HookEntity> {
    public static final ResourceLocation HEAD_MODEL = new ResourceLocation(Maniacrev.MODID, "item/hook_head");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/block/iron_block.png");
    private static final int LINK_SIDES = 10;
    private static final float MODEL_SCALE = 1.25f;
    private static final float FLIGHT_TILT = 127.2f;
    private static final Vec3 MODEL_TIP = new Vec3(8, 12.35674, 1.66523).scale(1.0 / 16);
    private static final Vec3 MODEL_CHAIN_EYE = new Vec3(8, 7.57738, 7.90631).scale(1.0 / 16);

    public HookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(HookEntity hook, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(hook, frustum, x, y, z)) return true;
        return hook.getOwner() instanceof Player player
                && frustum.isVisible(new AABB(hook.position(), player.getEyePosition()).inflate(1));
    }

    @Override
    public void render(HookEntity hook, float entityYaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int light) {
        float yaw = Mth.rotLerp(partialTick, hook.yRotO, hook.getYRot());
        float pitch = Mth.lerp(partialTick, hook.xRotO, hook.getXRot());
        float age = hook.tickCount + partialTick;
        float roll = Mth.sin(age * 0.65f) * (hook.isReturning() ? 5 : 13);
        Vec3 eye = MODEL_CHAIN_EYE.subtract(MODEL_TIP).scale(MODEL_SCALE);
        Vector3f attachment = new Vector3f((float) eye.x, (float) eye.y, (float) eye.z);
        attachment.rotateX(FLIGHT_TILT * Mth.DEG_TO_RAD).rotateZ(roll * Mth.DEG_TO_RAD)
                .rotateX(-pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD);
        renderChain(hook, partialTick, new Vec3(attachment.x, attachment.y, attachment.z), poses, buffers, light);

        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.mulPose(Axis.XP.rotationDegrees(-pitch));
        // Heavy wobble settles as the chain is reeled back; the point keeps facing outward.
        poses.mulPose(Axis.ZP.rotationDegrees(roll));
        poses.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        // Align the grip-to-point axis with travel, with the claw leading and its eye behind it.
        poses.mulPose(Axis.XP.rotationDegrees(FLIGHT_TILT));
        poses.translate(0.5 - MODEL_TIP.x, 0.5 - MODEL_TIP.y, 0.5 - MODEL_TIP.z);
        // The blade flies away; its grip stays in the owner's hand.
        var minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().render(new ItemStack(ModItems.HOOK.get()), ItemDisplayContext.NONE,
                false, poses, buffers, light, OverlayTexture.NO_OVERLAY,
                minecraft.getModelManager().getModel(HEAD_MODEL));
        poses.popPose();
        super.render(hook, entityYaw, partialTick, poses, buffers, light);
    }

    private Vec3 handPosition(Player player, HookEntity hook, float partialTick) {
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (hook.isOffHand()) side = -side;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getCameraEntity() == player && minecraft.options.getCameraType().isFirstPerson()) {
            var camera = minecraft.gameRenderer.getMainCamera();
            return camera.getPosition().add(HookTrajectory.handOffset(camera.getYRot(), camera.getXRot(), side == 1));
        }
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        return player.getPosition(partialTick).add(
                -side * Mth.cos(bodyYaw) * 0.36 - Mth.sin(bodyYaw) * 0.12,
                player.getEyeHeight() - (player.isCrouching() ? 0.48 : 0.58),
                -side * Mth.sin(bodyYaw) * 0.36 + Mth.cos(bodyYaw) * 0.12);
    }

    private void renderChain(HookEntity hook, float partialTick, Vec3 eye,
                             PoseStack poses, MultiBufferSource buffers, int light) {
        if (!(hook.getOwner() instanceof Player player)) return;
        Vec3 hand = handPosition(player, hook, partialTick).subtract(hook.getPosition(partialTick));
        double distance = eye.distanceTo(hand);
        if (distance < 0.08) return;
        int links = Mth.clamp((int) Math.ceil(distance / 0.24), 1, 160);
        float age = hook.tickCount + partialTick;
        double sag = Math.min(0.65, distance * (hook.isReturning() ? 0.008 : 0.024));
        Vec3 direction = hand.subtract(eye).normalize();
        Vec3 lateral = direction.cross(Math.abs(direction.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int i = 0; i < links; i++) {
            double t = (i + 0.5) / links;
            Vec3 center = chainPoint(eye, hand, lateral, t, sag, age);
            double dt = 0.5 / links;
            Vec3 tangent = chainPoint(eye, hand, lateral, Math.min(1, t + dt), sag, age)
                    .subtract(chainPoint(eye, hand, lateral, Math.max(0, t - dt), sag, age)).normalize();
            Vec3 across = tangent.cross(lateral).normalize();
            Vec3 width = (i & 1) == 0 ? lateral : across;
            Vec3 normal = tangent.cross(width).normalize();
            renderLink(poses, vertices, center, tangent, width, normal, light);
        }
    }

    private static Vec3 chainPoint(Vec3 start, Vec3 end, Vec3 lateral, double t, double sag, float age) {
        double envelope = Math.sin(Math.PI * t);
        double wave = Math.sin(t * 15 - age * 0.8) * Math.min(0.045, sag * 0.2) * envelope;
        return start.lerp(end, t).add(0, -4 * sag * t * (1 - t), 0).add(lateral.scale(wave));
    }

    /** Hollow oval rings with square metal cross sections, alternating by ninety degrees. */
    private static void renderLink(PoseStack poses, VertexConsumer vertices, Vec3 center,
                                   Vec3 along, Vec3 across, Vec3 normal, int light) {
        for (int i = 0; i < LINK_SIDES; i++) {
            double a = 2 * Math.PI * i / LINK_SIDES;
            double b = 2 * Math.PI * (i + 1) / LINK_SIDES;
            Vec3 outerA = center.add(along.scale(Math.cos(a) * 0.17)).add(across.scale(Math.sin(a) * 0.078));
            Vec3 outerB = center.add(along.scale(Math.cos(b) * 0.17)).add(across.scale(Math.sin(b) * 0.078));
            Vec3 innerA = center.add(along.scale(Math.cos(a) * 0.125)).add(across.scale(Math.sin(a) * 0.036));
            Vec3 innerB = center.add(along.scale(Math.cos(b) * 0.125)).add(across.scale(Math.sin(b) * 0.036));
            Vec3 thickness = normal.scale(0.019);
            quad(poses, vertices, outerA.add(thickness), outerB.add(thickness),
                    innerB.add(thickness), innerA.add(thickness), light);
            quad(poses, vertices, innerA.subtract(thickness), innerB.subtract(thickness),
                    outerB.subtract(thickness), outerA.subtract(thickness), light);
            quad(poses, vertices, outerA.subtract(thickness), outerB.subtract(thickness),
                    outerB.add(thickness), outerA.add(thickness), light);
            quad(poses, vertices, innerA.add(thickness), innerB.add(thickness),
                    innerB.subtract(thickness), innerA.subtract(thickness), light);
        }
    }

    private static void quad(PoseStack poses, VertexConsumer vertices,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int light) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(poses, vertices, a, normal, 0, 0, light);
        vertex(poses, vertices, b, normal, 1, 0, light);
        vertex(poses, vertices, c, normal, 1, 1, light);
        vertex(poses, vertices, d, normal, 0, 1, light);
    }

    private static void vertex(PoseStack poses, VertexConsumer vertices, Vec3 point, Vec3 normal,
                               float u, float v, int light) {
        vertices.vertex(poses.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(112, 104, 91, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(poses.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(HookEntity entity) {
        return TEXTURE;
    }
}
