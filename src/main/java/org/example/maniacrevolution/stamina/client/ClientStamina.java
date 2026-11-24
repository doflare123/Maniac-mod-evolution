package org.example.maniacrevolution.stamina.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.StaminaSyncPacket;
import org.example.maniacrevolution.stamina.Stamina;
import org.example.maniacrevolution.stamina.StaminaMovement;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ClientStamina {
    private static final long DENIAL_DURATION_MS = 350;
    private static LocalPlayer player;
    private static int amount = Stamina.MAX;
    private static boolean needsSprintRelease;
    private static boolean sprintDenialShown;
    private static boolean jumpDenialShown;
    private static long denialUntil;

    private ClientStamina() {}

    public static void accept(StaminaSyncPacket packet) {
        LocalPlayer current = Minecraft.getInstance().player;
        if (current == null || current.getId() != packet.playerId()) return;
        usePlayer(current);
        amount = packet.amount();
        if (amount == 0) needsSprintRelease = true;
        if (packet.denied() && StaminaMovement.usesStamina(current)) showDenial();
    }

    public static float cells(LocalPlayer current) {
        usePlayer(current);
        return (float) amount / Stamina.CELL;
    }

    public static float denialRemaining(LocalPlayer current) {
        usePlayer(current);
        return Mth.clamp((float) (denialUntil - Util.getMillis()) / DENIAL_DURATION_MS, 0.0f, 1.0f);
    }

    public static boolean trySprint(LocalPlayer current) {
        if (!StaminaMovement.usesStamina(current)) return true;
        usePlayer(current);
        if (!sprintKeyHeld() || PlayerLocomotionAnimations.isDashing(current)) return false;
        if (amount == 0 && !sprintDenialShown) {
            sprintDenialShown = true;
            showDenial();
        }
        return !needsSprintRelease && amount > 0;
    }

    public static boolean usesHeldSprintKey(KeyMapping binding) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.options != null && binding == mc.options.keySprint
                && binding.getKey().getType() != InputConstants.Type.SCANCODE
                && StaminaMovement.usesStamina(mc.player);
    }

    public static boolean tryLocalJump(Player entity) {
        if (!(entity instanceof LocalPlayer current) || !StaminaMovement.usesStamina(current)) return true;
        if (PlayerLocomotionAnimations.isDashing(current)) return false;
        usePlayer(current);
        if (amount < Stamina.CELL) {
            if (!jumpDenialShown) {
                jumpDenialShown = true;
                showDenial();
            }
            return false;
        }
        // Vanilla movement carries the jump and the server synchronizes its actual cost.
        amount -= Stamina.CELL;
        if (amount == 0) needsSprintRelease = true;
        return true;
    }

    @SubscribeEvent
    public static void updateInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer current)) return;
        usePlayer(current);
        if (!sprintKeyHeld()) {
            needsSprintRelease = false;
            sprintDenialShown = false;
        }
        if (!event.getInput().jumping) jumpDenialShown = false;
        if (current.isSprinting() && !trySprint(current)) current.setSprinting(false);
        if (PlayerLocomotionAnimations.isDashing(current)) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        usePlayer(null);
    }

    private static void usePlayer(LocalPlayer current) {
        if (player == current) return;
        player = current;
        amount = Stamina.MAX;
        needsSprintRelease = false;
        sprintDenialShown = false;
        jumpDenialShown = false;
        denialUntil = 0;
    }

    private static void showDenial() {
        denialUntil = Util.getMillis() + DENIAL_DURATION_MS;
    }

    public static boolean sprintKeyHeld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !mc.isWindowActive()) return false;
        var binding = mc.options.keySprint;
        if (binding.isUnbound()) return false;
        var key = binding.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) return InputConstants.isKeyDown(window, key.getValue());
        return binding.isDown();
    }
}
