package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.packets.WardenStaminaPacket;
import org.example.maniacrevolution.warden.WardenMovementManager;
import org.example.maniacrevolution.warden.WardenStamina;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenMovementClient {
    private static ClientLevel level;
    private static LocalPlayer player;
    private static WardenStaminaPacket state;
    private static MobEffectInstance predictedJump;
    private WardenMovementClient() {}
    public static void accept(WardenStaminaPacket packet) {
        var mc = Minecraft.getInstance();
        if (!WardenCombatClient.eligible() || mc.level == null || !packet.valid()
                || !packet.dimension().equals(mc.level.dimension().location())
                || Math.abs(packet.tick() - mc.level.getGameTime()) > 40) return;
        if (level != mc.level || player != mc.player) { reset(); level = mc.level; player = mc.player; }
        if (state == null || packet.tick() >= state.tick()) state = packet;
    }
    @SubscribeEvent public static void beforeMove(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player)
            WardenMovementManager.compensateSprint((LocalPlayer) event.getEntity());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player || !WardenCombatClient.eligible()) reset();
        if (mc.player != null) WardenMovementManager.compensateSprint(mc.player);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !WardenCombatClient.eligible() || mc.player.isInWaterOrBubble()
                || mc.player.isInLava() || mc.player.onClimbable() || mc.player.hasEffect(ModEffects.JUMP_COOLDOWN.get())) return;
        predictedJump = new MobEffectInstance(ModEffects.JUMP_COOLDOWN.get(), WardenMovementManager.JUMP_COOLDOWN, 0, false, false, true);
        mc.player.addEffect(predictedJump);
        player = mc.player; level = mc.level;
    }
    private static void reset() {
        if (player != null && predictedJump != null && player.getEffect(ModEffects.JUMP_COOLDOWN.get()) == predictedJump)
            player.removeEffect(ModEffects.JUMP_COOLDOWN.get());
        state = null; player = null; level = null; predictedJump = null;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    public static void render(GuiGraphics graphics, int width, int height) {
        var mc = Minecraft.getInstance();
        if (!WardenCombatClient.eligible() || mc.options.hideGui || mc.screen != null || state == null
                || mc.level != level || mc.player != player || mc.level.getGameTime() - state.tick() > 40) return;
        int x = width / 2 - 40, y = height / 2 + 37;
        graphics.fill(x - 1, y - 1, x + 81, y + 5, 0xFF24434A);
        graphics.fill(x, y, x + 80, y + 4, 0xFF0A1D22);
        int color = state.exhausted() ? 0xFFD58D51 : state.boosting() ? 0xFF56F0DE : 0xFF329B9C;
        graphics.fill(x, y, x + Math.round(80F * state.amount() / WardenStamina.CAPACITY), y + 4, color);
        for (int i = 1; i < 5; i++) graphics.fill(x + i * 16, y, x + i * 16 + 1, y + 4, 0xFF18343B);
        var text = Component.translatable("hud.maniacrev.warden_stamina");
        graphics.drawString(mc.font, text, width / 2 - mc.font.width(text) / 2, y + 7, color & 0xFFFFFF, true);
    }
}
