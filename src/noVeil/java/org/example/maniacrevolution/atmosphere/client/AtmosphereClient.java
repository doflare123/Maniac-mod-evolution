package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.character.maniac.WardenClass;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.example.maniacrevolution.util.PlayerModeUtil;
import org.example.maniacrevolution.warden.client.WardenVisionPrototype;

/** Build-time replacement: vanilla sight limit, with no post-processing or Veil linkage. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereClient {
    private AtmosphereClient() {}

    public static GraphicsMode graphicsMode() { return ClientGameSettings.getGraphicsMode(); }
    /** No shader profile; the shared fog handler uses its vanilla fallback near plane. */
    public static AtmosphereRules.Profile profile() { return AtmosphereRules.Profile.OFF; }
    public static float cameraCorruption() { return 0; }

    public static boolean hasFogLimit() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (!graphicsMode().enabled() || player == null || mc.level == null || !player.isAlive()
                || !PlayerModeUtil.isSurvivalOrAdventure(player) || mc.getCameraEntity() != player
                || WardenVisionPrototype.inMatch()) return false;
        return AtmosphereRules.profile(true, true, ClientGameState.getPhase(),
                player.getTeam() == null ? null : player.getTeam().getName(),
                ClientPlayerData.getManiacClassId() == NightmareConfig.KEEPER_CLASS_ID,
                ClientPlayerData.getManiacClassId() == WardenClass.SCOREBOARD_ID) != AtmosphereRules.Profile.OFF;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGameSettings.resetGraphicsMode();
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mrvisual")
                .executes(context -> status())
                .then(Commands.literal("status").executes(context -> status())));
    }

    private static int status() {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(
                "Сборка без Veil: ванильный туман, обзор="
                        + (hasFogLimit() ? AtmosphereRules.FOG_END + " блоков" : "обычный")
                        + ". Шейдерные эффекты недоступны."), false);
        return 1;
    }
}
