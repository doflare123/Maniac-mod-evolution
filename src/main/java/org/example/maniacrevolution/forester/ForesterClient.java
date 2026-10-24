package org.example.maniacrevolution.forester;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.helsing.HelsingManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ForesterClient {
    private static ForesterStatePacket state;
    private static ClientLevel world;
    private static long received;
    private static boolean wasSpace;
    private static boolean wasUse, pendingWork;
    private static float workYaw, workPitch;
    private ForesterClient() {}
    public static void receive(ForesterStatePacket packet) {
        var mc = Minecraft.getInstance();
        world = mc.level; state = packet; received = world == null ? 0 : world.getGameTime();
        pendingWork = packet.work() >= 0;
    }
    private static boolean fresh() {
        var mc = Minecraft.getInstance();
        return state != null && world != null && mc.level == world && world.getGameTime() - received < 10;
    }
    private static double age(float partial) { return state.age() + world.getGameTime() - received + partial; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.level != world) {
            state = null; world = mc.level; wasSpace = false; wasUse = false; pendingWork = false; ForesterWorkAnimation.clear();
        }
        if (mc.player == null || mc.level == null) return;
        boolean space = GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (mc.screen == null && space && !wasSpace && fresh() && state.age() >= 0)
            ModNetworking.sendToServer(new ForesterInputPacket(ForesterInputPacket.Action.SPACE, BlockPos.ZERO, InteractionHand.MAIN_HAND, state.sequence(), (int) age(0)));
        wasSpace = space;
        boolean use = mc.options.keyUse.isDown();
        if (!use && wasUse) request(ForesterInputPacket.Action.RELEASE, BlockPos.ZERO, InteractionHand.MAIN_HAND);
        if (pendingWork && (mc.screen != null || Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - workYaw)) > .05F
                || Math.abs(mc.player.getXRot() - workPitch) > .05F)) cancelWork();
        if (mc.screen == null && use && !wasUse && !pendingWork && ForesterManager.canUse(mc.player) && mc.hitResult instanceof BlockHitResult hit) {
            InteractionHand hand = mc.player.getMainHandItem().getItem() instanceof ForesterTrapItem ? InteractionHand.MAIN_HAND
                    : mc.player.getOffhandItem().getItem() instanceof ForesterTrapItem ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var block = mc.level.getBlockState(hit.getBlockPos());
            if (block.is(ModBlocks.FORESTER_TRAP.get()) && block.getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.SPENT) {
                long handle = mc.getWindow().getWindow();
                boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                begin(ctrl ? ForesterInputPacket.Action.COLLECT : ForesterInputPacket.Action.REARM, hit.getBlockPos(), hand);
            } else if (mc.player.getItemInHand(hand).getItem() instanceof ForesterTrapItem && !block.is(ModBlocks.FORESTER_TRAP.get())) {
                BlockPos target = ForesterManager.placementTarget(mc.player, hand);
                if (target != null) begin(ForesterInputPacket.Action.PLACE, target, hand);
            }
        }
        wasUse = use;
    }
    private static void request(ForesterInputPacket.Action action, BlockPos pos, InteractionHand hand) {
        ModNetworking.sendToServer(new ForesterInputPacket(action, pos, hand, 0, 0));
    }
    private static void begin(ForesterInputPacket.Action action, BlockPos pos, InteractionHand hand) {
        var player = Minecraft.getInstance().player;
        workYaw = player.getYRot(); workPitch = player.getXRot(); pendingWork = true;
        request(action, pos, hand);
    }
    private static void cancelWork() {
        pendingWork = false;
        request(ForesterInputPacket.Action.CANCEL, BlockPos.ZERO, InteractionHand.MAIN_HAND);
    }
    @SubscribeEvent public static void useInput(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        if (pendingWork || mc.player.getMainHandItem().getItem() instanceof ForesterTrapItem
                || mc.player.getOffhandItem().getItem() instanceof ForesterTrapItem
                || mc.hitResult instanceof BlockHitResult hit && mc.level.getBlockState(hit.getBlockPos()).is(ModBlocks.FORESTER_TRAP.get())) {
            event.setCanceled(true); event.setSwingHand(false);
        }
    }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var movement = event.getInput();
        if (pendingWork && (movement.up || movement.down || movement.left || movement.right || movement.jumping || movement.shiftKeyDown
                || movement.forwardImpulse != 0 || movement.leftImpulse != 0)) cancelWork();
        if (!ForesterManager.trapped(event.getEntity())) return;
        var input = event.getInput(); input.forwardImpulse = 0; input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        var gui = event.getGuiGraphics(); int width = gui.guiWidth(), height = gui.guiHeight();
        if (fresh() && state.age() >= 0) {
            double age = age(event.getPartialTick());
            int x = width / 2, y = height / 2 + 44;
            if (age >= ForesterRules.TRAP_DELAY) {
                int left = x - 105, barY = y;
                gui.fill(left, barY, left + 210, barY + 11, 0xFF39413B);
                int zone = Math.round((state.target() - ForesterRules.ZONE_WIDTH / 2) * 210);
                gui.fill(left + zone, barY, left + zone + Math.round(ForesterRules.ZONE_WIDTH * 210), barY + 11, 0xFF93C773);
                int needle = Math.round(ForesterRules.needle(age) * 210);
                gui.fill(left + needle - 1, barY - 3, left + needle + 2, barY + 14, 0xFFFFE4AB);
            }
        } else if (fresh() && state.work() >= 0) {
            int duration = state.collecting() ? ForesterRules.COLLECT : ForesterRules.REARM;
            float progress = Math.min(1, (state.work() + world.getGameTime() - received + event.getPartialTick()) / duration);
            int x = width / 2, y = height / 2 + 40;
            gui.fill(x - 82, y, x + 82, y + 4, 0xFF39413B);
            gui.fill(x - 82, y, x - 82 + (int) (164 * progress), y + 4, 0xFF93C773);
        } else if (mc.hitResult instanceof BlockHitResult hit && ForesterManager.canUse(mc.player)) {
            var block = mc.level.getBlockState(hit.getBlockPos());
            if (block.is(ModBlocks.FORESTER_TRAP.get()) && block.getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.SPENT)
                gui.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.forester.trap_hint"), width / 2, height / 2 + 35, 0xF1D8A3);
        }
        if (HelsingManager.isManiac(mc.player) && mc.player.hasEffect(ModEffects.FORESTER_BLIND.get())) {
            // Cover the whole scene and HUD for the full server-controlled effect duration.
            gui.pose().pushPose(); gui.pose().translate(0, 0, 1000);
            gui.fill(0, 0, width, height, 0xFFFFFFFF); gui.pose().popPose();
        }
    }
    @SubscribeEvent public static void trajectory(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !ForesterManager.canUse(mc.player)
                || !mc.player.isUsingItem() || !(mc.player.getUseItem().getItem() instanceof PepperCanisterItem)) return;
        Vec3 position = mc.player.getEyePosition(event.getPartialTick()).add(0, -(double) .1F, 0);
        Vec3 velocity = PepperCanisterEntity.launchVelocity(mc.player);
        Vec3 camera = event.getCamera().getPosition();
        var pose = event.getPoseStack(); pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        var buffer = mc.renderBuffers().bufferSource(); var line = buffer.getBuffer(RenderType.lines());
        Vec3 landing = position;
        for (int step = 0; step < 160; step++) {
            Vec3 next = position.add(velocity);
            var block = mc.level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            Vec3 collisionEnd = end;
            boolean hit = block.getType() != HitResult.Type.MISS;
            double distance = position.distanceToSqr(end);
            for (Entity entity : mc.level.getEntities(mc.player, new AABB(position, next).inflate(1.125), e -> e.canBeHitByProjectile() && !e.isSpectator())) {
                var intersection = entity.getBoundingBox().inflate((double) .3F).clip(position, collisionEnd);
                if (intersection.isPresent() && position.distanceToSqr(intersection.get()) < distance) {
                    // Vanilla ThrowableProjectile's entity impact uses the target's position.
                    distance = position.distanceToSqr(intersection.get()); end = entity.position(); hit = true;
                }
            }
            segment(line, pose, position, end, .83F, .94F, .58F);
            landing = end;
            if (hit) break;
            double drag = mc.level.getFluidState(net.minecraft.core.BlockPos.containing(position)).is(net.minecraft.tags.FluidTags.WATER) ? ForesterRules.WATER_DRAG : ForesterRules.DRAG;
            velocity = velocity.scale(drag).add(0, -ForesterRules.GRAVITY, 0); position = next;
        }
        // Impact ring and affected radius on the landing plane.
        for (double radius : new double[]{.25, ForesterRules.PEPPER_RADIUS}) {
            for (int i = 0; i < 64; i++) {
                double a = i * Math.PI / 32, b = (i + 1) * Math.PI / 32;
                segment(line, pose, landing.add(Math.cos(a) * radius, .04, Math.sin(a) * radius),
                        landing.add(Math.cos(b) * radius, .04, Math.sin(b) * radius), .94F, .67F, .32F);
            }
        }
        buffer.endBatch(RenderType.lines()); pose.popPose();
    }
    private static void segment(VertexConsumer line, PoseStack pose, Vec3 a, Vec3 b, float r, float g, float blue) {
        Vec3 normal = b.subtract(a).normalize();
        line.vertex(pose.last().pose(), (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, .85F)
                .normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        line.vertex(pose.last().pose(), (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, .85F)
                .normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
