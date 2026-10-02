package org.example.maniacrevolution.cloak.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cloak.*;
import org.example.maniacrevolution.network.ModNetworking;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class CloakClient {
    private static CloakGamePacket game;
    private static long lastGameTick;
    private static boolean jumpDown;
    private static boolean hoverHeld;
    private static net.minecraft.client.player.LocalPlayer ridingPlayer;
    private static float originalStepHeight;
    private static final Map<AbstractClientPlayer, Pose> POSES = new IdentityHashMap<>();
    private static final class Pose {
        final ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        String clip = "";
        boolean recovering;
        Pose(AbstractClientPlayer player) { PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(2500, layer); }
    }

    public static void game(CloakGamePacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (packet.remaining() == 0) {
            if (game != null && game.cloakId() == packet.cloakId()) game = null;
        } else game = packet;
        lastGameTick = mc.level.getGameTime();
    }

    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var input = event.getInput();
        var mc = Minecraft.getInstance();
        boolean held = input.shiftKeyDown && mc.screen == null && game == null
                && CloakManager.equipped(event.getEntity()) && event.getEntity().isAlive()
                && !event.getEntity().isCreative() && !event.getEntity().isSpectator();
        if (held != hoverHeld || held && event.getEntity().tickCount % 5 == 0) {
            ModNetworking.sendToServer(new CloakHoverInputPacket(held));
        }
        hoverHeld = held;
        CloakEntity cloak = held ? localHover() : null;
        if (cloak != null) {
            var player = event.getEntity();
            if (ridingPlayer == null) {
                ridingPlayer = mc.player;
                originalStepHeight = player.maxUpStep();
            }
            player.setMaxUpStep(0);
            player.setSprinting(false);
            float left = (input.left ? 1 : 0) - (input.right ? 1 : 0);
            float forward = (input.up ? 1 : 0) - (input.down ? 1 : 0);
            var motion = CloakHoverMotion.calculate(left, forward, player.getYRot(),
                    player.getAttributeValue(Attributes.MOVEMENT_SPEED), player.getY(), cloak.hoverHeight());
            player.setDeltaMovement(motion.x(), motion.y(), motion.z());
            player.fallDistance = 0;
            // Run before vanilla travel: no crouch slowdown, edge clamping or jump impulses.
            // Zero acceleration because the motion above already includes WASD input.
            input.forwardImpulse = input.leftImpulse = 0;
            input.jumping = false;
            input.shiftKeyDown = false;
        } else stopRiding();
        if (game == null) return;
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private static CloakEntity localHover() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !hoverHeld) return null;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof CloakEntity cloak && cloak.ownerId() == mc.player.getId()
                    && cloak.stage().hovering()) return cloak;
        }
        return null;
    }

    private static void stopRiding() {
        if (ridingPlayer == null) return;
        ridingPlayer.setMaxUpStep(originalStepHeight);
        ridingPlayer.setDeltaMovement(ridingPlayer.getDeltaMovement().multiply(1, 0, 1));
        ridingPlayer = null;
    }

    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() != Minecraft.getInstance().player) return;
        var cloak = localHover();
        if (cloak == null) return;
        // An auto-jump queued before take-off can otherwise bypass input.jumping=false.
        Vec3 velocity = event.getEntity().getDeltaMovement();
        double y = CloakHoverMotion.calculate(0, 0, 0, 0, event.getEntity().getY(), cloak.hoverHeight()).y();
        event.getEntity().setDeltaMovement(velocity.x, y, velocity.z);
    }
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (game != null) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { clear(); return; }
        if (mc.isPaused()) return;
        if (game != null && (!mc.player.isAlive() || mc.level.getGameTime() - lastGameTick > 40)) game = null;
        boolean jump = mc.options.keyJump.isDown();
        if (game != null && mc.screen == null && jump && !jumpDown) ModNetworking.sendToServer(new CloakFlapPacket(game.cloakId()));
        jumpDown = jump;
        Set<AbstractClientPlayer> animated = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof CloakEntity cloak)) continue;
            if (cloak.stage().hovering() || cloak.stage() == CloakStage.LAND) {
                if (mc.level.getEntity(cloak.ownerId()) instanceof AbstractClientPlayer player) {
                    animate(player, cloak.stage().clip, (int)cloak.ageInStage(), false);
                    animated.add(player);
                }
            }
            if (cloak.stage().binding() || cloak.stage() == CloakStage.RELEASE || cloak.stage() == CloakStage.LAUNCH) {
                if (mc.level.getEntity(cloak.targetId()) instanceof AbstractClientPlayer player) {
                    boolean released = !cloak.stage().binding();
                    animate(player, released ? "release_hold" : cloak.stage().clip,
                            released ? 0 : (int)cloak.ageInStage(), false);
                    animated.add(player);
                    player.yBodyRot = cloak.getYRot();
                    player.yBodyRotO = cloak.getYRot();
                }
            }
        }
        var iterator = POSES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Pose pose = entry.getValue();
            if (!mc.level.players().contains(entry.getKey())) {
                pose.layer.setAnimation(null);
                iterator.remove();
            } else if (!animated.contains(entry.getKey())) {
                if (!pose.recovering && (pose.clip.equals("capture_wrap") || pose.clip.equals("victim_fall")
                        || pose.clip.equals("bound_struggle") || pose.clip.equals("release_hold"))) {
                    animate(entry.getKey(), "victim_recover", 0, true);
                } else if (!pose.recovering || !pose.layer.isActive()) {
                    pose.layer.setAnimation(null);
                    pose.clip = "";
                    pose.recovering = false;
                }
            }
        }
    }
    private static void animate(AbstractClientPlayer player, String clip, int tick, boolean recovering) {
        Pose pose = POSES.computeIfAbsent(player, Pose::new);
        if (pose.clip.equals(clip)) return;
        pose.clip = clip;
        pose.recovering = recovering;
        var animation = PlayerAnimationRegistry.getAnimation(Maniacrev.loc("animation.cloak." + (clip.equals("release_hold") ? "release" : clip)));
        if (animation != null) {
            pose.layer.setAnimation(clip.equals("release_hold") ? new KeyframeAnimationPlayer(animation, 0) {
                @Override public void tick() { /* Hold the prone pose until the cloak has departed. */ }
            } : clip.startsWith("carpet_") ? new CloakRidingAnimation(animation, tick)
                    : new KeyframeAnimationPlayer(animation, tick));
        }
        else Maniacrev.LOGGER.warn("Missing cloak player animation: {}", clip);
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        if (game == null) return;
        Minecraft mc = Minecraft.getInstance();
        var gui = event.getGuiGraphics();
        int x = (gui.guiWidth() - CloakFlappyGame.WIDTH) / 2, y = 32;
        gui.fill(x - 3, y - 19, x + 243, y + 117, 0xE018101B);
        gui.fill(x, y, x + 240, y + 100, 0xEF283647);
        gui.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.cloak.title", game.score(),
                String.format(Locale.ROOT, "%.1f", game.remaining() / 20f)), x + 120, y - 13, 0xFFFFDC83);
        for (int i = 0; i < CloakFlappyGame.GOAL; i++) {
            int px = (int)CloakFlappyGame.pipeX(game.runTicks(), i);
            if (px >= 240 || px + CloakFlappyGame.PIPE_WIDTH <= 0) continue;
            int left = x + Math.max(0, px), right = x + Math.min(240, px + CloakFlappyGame.PIPE_WIDTH);
            int center = CloakFlappyGame.center(i);
            gui.fill(left, y, right, y + center - CloakFlappyGame.GAP / 2, 0xFFA84151);
            gui.fill(left, y + center + CloakFlappyGame.GAP / 2, right, y + 100, 0xFFA84151);
        }
        int bird = y + Math.round(game.bird());
        gui.fill(x + 45, bird - 3, x + 51, bird + 3, 0xFFFFD666);
        gui.drawCenteredString(mc.font, Component.translatable("hud.maniacrev.cloak.hint",
                mc.options.keyJump.getTranslatedKeyMessage()), x + 120, y + 105, 0xFFFFFFFF);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static void clear() {
        stopRiding();
        POSES.values().forEach(p -> p.layer.setAnimation(null));
        POSES.clear();
        game = null;
        jumpDown = false;
        hoverHeld = false;
    }
}
