package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenCombatHud {
    private WardenCombatHud() {}
    public static boolean visible() {
        var mc = Minecraft.getInstance();
        return WardenCombatClient.eligible() && !mc.options.hideGui && mc.options.getCameraType().isFirstPerson() && mc.screen == null;
    }
    @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre e) {
        if (visible() && e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) e.setCanceled(true);
    }
    public static final IGuiOverlay INSTANCE = (gui, graphics, partial, width, height) -> {
        WardenMovementClient.render(graphics, width, height);
        if (!visible()) return;
        var mc = Minecraft.getInstance(); int cx = width / 2, cy = height / 2;
        for (var r : WardenReticleLayout.build(WardenCombatClient.charge(partial), mc.level.getGameTime() + partial))
            graphics.fill(cx + r.x(), cy + r.y(), cx + r.x() + r.width(), cy + r.y() + r.height(), r.color());
        for (var r : WardenReticleLayout.strike(WardenCombatClient.strike(partial), WardenCombatClient.strikeHit()))
            graphics.fill(cx + r.x(), cy + r.y(), cx + r.x() + r.width(), cy + r.y() + r.height(), r.color());
        for (var label : WardenReticleLayout.cooldowns(WardenCombatClient.meleeCooldown(), WardenCombatClient.waveCooldown(), mc.font::width))
            graphics.drawString(mc.font, label.text(), cx + label.x(), cy + 18, label.wave() ? 0x39CFC3 : 0x8BA7AE, true);
    };
}
