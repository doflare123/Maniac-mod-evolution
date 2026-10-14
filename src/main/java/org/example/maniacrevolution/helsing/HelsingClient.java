package org.example.maniacrevolution.helsing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.joml.Vector3f;
import java.util.*;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class HelsingClient {
    private static HelsingStatePacket snapshot;
    private static ClientLevel world;
    private static long received;
    private static MobEffectInstance protection;
    private static final Map<Integer, HelsingSeal> SEALS = new HashMap<>();
    private static final net.minecraft.client.model.HumanoidModel.ArmPose CAST_POSE =
            net.minecraft.client.model.HumanoidModel.ArmPose.create("MANIACREV_HELSING_CAST", false, (model, entity, arm) -> {
                float tick = castTick(entity.getId(), Minecraft.getInstance().getFrameTime());
                if (tick < 0) return;
                boolean right = arm == net.minecraft.world.entity.HumanoidArm.RIGHT;
                float[] rotation = HelsingGesture.rotation(right ? "right_arm" : "left_arm", tick / 20F);
                var part = right ? model.rightArm : model.leftArm;
                part.xRot = (float) Math.toRadians(rotation[0]);
                part.yRot = (float) Math.toRadians(rotation[1]);
                part.zRot = (float) Math.toRadians(rotation[2]);
            });
    public static void receive(HelsingStatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (world != mc.level) clear();
        world = mc.level; received = world.getGameTime(); snapshot = packet;
        int ticks = HelsingManager.isSurvivor(mc.player) ? packet.shields().getOrDefault(mc.player.getId(), 0) : 0;
        if (ticks == 0) protection = null;
        else if (protection == null || ticks > protection.getDuration() + 5)
            protection = new MobEffectInstance(ModEffects.HELSING_PROTECTION.get(), ticks, 0, false, false, true);
        SEALS.keySet().retainAll(packet.shields().keySet());
    }
    private static boolean fresh() {
        Minecraft mc = Minecraft.getInstance();
        return snapshot != null && mc.level == world && world != null && world.getGameTime() - received <= 20;
    }
    public static Stream<MobEffectInstance> displayedEffects(Player player) {
        Stream<MobEffectInstance> effects = player.getActiveEffects().stream();
        if (fresh() && protection != null && player == Minecraft.getInstance().player && HelsingManager.isSurvivor(player))
            return Stream.concat(effects, Stream.of(protection));
        return effects;
    }
    public static float castTick(int id, float partial) {
        if (!fresh()) return -1;
        Integer remaining = snapshot.gestures().get(id);
        if (remaining == null) return -1;
        float tick = HelsingManager.GESTURE_TICKS - remaining + (world.getGameTime() - received) + partial;
        return tick >= HelsingManager.GESTURE_TICKS ? -1 : tick;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { clear(); return; }
        while (ModKeybinds.HELSING_SWITCH_BOLT.consumeClick()) {
            if (mc.screen == null && !mc.player.isUsingItem() && HelsingManager.isSurvivor(mc.player)
                    && ClientPlayerData.isSurvivorClass(HelsingConfig.CLASS_ID)) {
                var hand = mc.player.getMainHandItem().getItem() instanceof HelsingCrossbowItem ? net.minecraft.world.InteractionHand.MAIN_HAND
                        : mc.player.getOffhandItem().getItem() instanceof HelsingCrossbowItem ? net.minecraft.world.InteractionHand.OFF_HAND : null;
                if (hand != null) mc.setScreen(new HelsingBoltWheelScreen(hand));
            }
        }
        if (mc.level != world) { clear(); return; }
        if (!fresh()) { protection = null; SEALS.clear(); return; }
        if (protection != null && !protection.tick(mc.player, () -> {})) protection = null;
        if (HelsingManager.isSurvivor(mc.player) && world.getGameTime() % 4 == 0) {
            snapshot.shields().forEach((id, ticks) -> {
                var entity = world.getEntity(id);
                if (entity == null || ticks <= world.getGameTime() - received) return;
                double angle = world.getGameTime() * 0.16;
                world.addParticle(new DustParticleOptions(new Vector3f(0.8F, 0.05F, 0.15F), 0.8F),
                        entity.getX() + Math.cos(angle) * 0.45, entity.getY() + 1, entity.getZ() + Math.sin(angle) * 0.45, 0, 0.015, 0);
            });
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !HelsingManager.isSurvivor(mc.player)
                || !ClientPlayerData.isSurvivorClass(HelsingConfig.CLASS_ID)) return;
        var gui = event.getGuiGraphics(); int cx = mc.getWindow().getGuiScaledWidth() / 2, cy = mc.getWindow().getGuiScaledHeight() / 2;
        if (!fresh()) return;
        if (snapshot.prey() < 0) return;
        int x = cx + 18, y = cy - 12, width = Math.min(208, mc.getWindow().getGuiScaledWidth() - x - 4);
        if (width < 80) return;
        int rows = snapshot.resource() < 0 ? 0 : Math.min(snapshot.abilities().size(), Math.max(0, (mc.getWindow().getGuiScaledHeight() - y - 45) / 12));
        gui.fill(x - 4, y - 4, x + width, y + 27 + rows * 12, 0xC010131B);
        gui.drawString(mc.font, mc.font.plainSubstrByWidth(snapshot.preyName().getString(), width - 8), x, y, 0xFFF4DDAD);
        if (snapshot.resource() < 0) {
            gui.fill(x, y + 15, x + width - 8, y + 18, 0xFF333844);
            gui.fill(x, y + 15, x + (int) ((width - 8) * snapshot.progress()), y + 18, 0xFFCE3853);
            return;
        }
        String resource = Component.translatable(snapshot.blood() ? "hud.maniacrev.helsing.blood" : "hud.maniacrev.helsing.mana",
                Math.round(snapshot.resource()), Math.round(snapshot.maximum())).getString();
        gui.drawString(mc.font, resource, x, y + 13, snapshot.blood() ? 0xFFED7185 : 0xFF89D4ED);
        for (int i = 0; i < rows; i++) {
            var row = snapshot.abilities().get(i);
            String ready = row.seconds() <= 0 ? Component.translatable("hud.maniacrev.helsing.ready").getString() : row.seconds() + "s";
            gui.drawString(mc.font, mc.font.plainSubstrByWidth(row.name().getString(), width - mc.font.width(ready) - 18), x, y + 27 + i * 12, 0xFFCBCED5);
            gui.drawString(mc.font, ready, x + width - 8 - mc.font.width(ready), y + 27 + i * 12, row.seconds() <= 0 ? 0xFF90DEAA : 0xFFCC9CA5);
        }
    }
    @SubscribeEvent public static void highlight(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !fresh() || mc.player == null
                || !HelsingManager.isSurvivor(mc.player) || !ClientPlayerData.isSurvivorClass(HelsingConfig.CLASS_ID)
                || !(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof HelsingHatItem)) return;
        var ally = world.getEntity(snapshot.ally()); if (ally == null) return;
        PoseStack pose = event.getPoseStack(); Vec3 camera = event.getCamera().getPosition();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(pose, mc.renderBuffers().bufferSource().getBuffer(RenderType.lines()), ally.getBoundingBox().inflate(0.07), 1, 0.76F, 0.28F, 0.9F);
        pose.popPose();
    }
    @SubscribeEvent public static void beforePlayer(RenderPlayerEvent.Pre event) {
        if (castTick(event.getEntity().getId(), event.getPartialTick()) < 0) return;
        var m = event.getRenderer().getModel();
        // Forge applies these inside setupAnim, before skin, armor and held items are rendered.
        m.rightArmPose = CAST_POSE;
        m.leftArmPose = CAST_POSE;
    }
    @SubscribeEvent public static void afterPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!fresh() || mc.player == null || !HelsingManager.isSurvivor(mc.player) || !snapshot.shields().containsKey(event.getEntity().getId())
                || snapshot.shields().get(event.getEntity().getId()) <= world.getGameTime() - received) return;
        HelsingSeal seal = SEALS.computeIfAbsent(event.getEntity().getId(), id -> new HelsingSeal());
        seal.render(event.getPoseStack(), event.getMultiBufferSource(), event.getEntity(), event.getPartialTick());
    }
    @SubscribeEvent public static void hand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance(); if (mc.player == null) return;
        float tick = castTick(mc.player.getId(), event.getPartialTick()); if (tick < 0) return;
        if (!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer)) return;
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack(); pose.pushPose();
        boolean right = (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) == (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        float[] rotation = HelsingGesture.rotation(right ? "right_arm" : "left_arm", tick / 20F);
        pose.translate(right ? -0.15 : 0.15, -0.15, -0.25);
        pose.mulPose(Axis.XP.rotationDegrees(rotation[0] * 0.30F));
        pose.mulPose(Axis.ZP.rotationDegrees(rotation[2] * 0.6F));
        if (right) renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), mc.player);
        else renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), mc.player);
        pose.popPose();
    }
    private static void clear() { snapshot = null; world = null; protection = null; SEALS.clear(); }
    private HelsingClient() {}
}
