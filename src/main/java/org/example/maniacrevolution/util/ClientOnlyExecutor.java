package org.example.maniacrevolution.util;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.network.packets.RequestDeadPlayersPacket;
import org.example.maniacrevolution.system.Agent47ShopConfig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientOnlyExecutor {
    private static final String HELPER_CLASS = "org.example.maniacrevolution.client.ClientScreenHelper";

    private ClientOnlyExecutor() {
    }

    public static void openRecipeBookScreen() {
        runNoArgs("openRecipeBookScreen");
    }

    public static void openCharacterSelectionScreen(CharacterType type) {
        run("openCharacterSelectionScreen", new Class<?>[]{CharacterType.class}, new Object[]{type});
    }

    public static void openPerkScreen() {
        runNoArgs("openPerkScreen");
    }

    public static void openShopScreen() {
        runNoArgs("openShopScreen");
    }

    public static void openGuiByType(String typeName) {
        run("openGuiByType", new Class<?>[]{String.class}, new Object[]{typeName});
    }

    public static void openResurrectionScreen() {
        runNoArgs("openResurrectionScreen");
    }

    public static void openMedicTabletScreen() {
        runNoArgs("openMedicTabletScreen");
    }

    public static void openSettingsScreen() {
        runNoArgs("openSettingsScreen");
    }

    public static void openGuidePage(int pageTypeId) {
        run("openGuidePage", new Class<?>[]{int.class}, new Object[]{pageTypeId});
    }

    public static void closePerkScreenIfOpen() {
        runNoArgs("closePerkScreenIfOpen");
    }

    public static void handleMapVotingPacket(boolean open, int timeRemaining,
                                             Map<String, Integer> voteCount, String playerVotedMapId) {
        run("handleMapVotingPacket",
                new Class<?>[]{boolean.class, int.class, Map.class, String.class},
                new Object[]{open, timeRemaining, voteCount, playerVotedMapId});
    }

    public static void showMapVotingResult(String winnerMapId, Map<String, Integer> finalVoteCount) {
        run("showMapVotingResult",
                new Class<?>[]{String.class, Map.class},
                new Object[]{winnerMapId, finalVoteCount});
    }

    public static void updateAgent47Money(int money) {
        run("updateAgent47Money", new Class<?>[]{int.class}, new Object[]{money});
    }

    public static void updateAgent47Data(String targetName, int money, List<Agent47ShopConfig.ShopItem> shopItems) {
        run("updateAgent47Data",
                new Class<?>[]{String.class, int.class, List.class},
                new Object[]{targetName, money, shopItems});
    }

    public static void updateDeadPlayers(List<RequestDeadPlayersPacket.DeadPlayerInfo> deadPlayers) {
        run("updateDeadPlayers", new Class<?>[]{List.class}, new Object[]{deadPlayers});
    }

    public static void syncGenerator(BlockPos pos, int charge, boolean powered) {
        run("syncGenerator",
                new Class<?>[]{BlockPos.class, int.class, boolean.class},
                new Object[]{pos, charge, powered});
    }

    public static void showTabletCooldown(int cooldownSeconds) {
        run("showTabletCooldown", new Class<?>[]{int.class}, new Object[]{cooldownSeconds});
    }

    public static void startQTE(int generatorNumber, boolean hasQuickReflexes) {
        run("startQTE",
                new Class<?>[]{int.class, boolean.class},
                new Object[]{generatorNumber, hasQuickReflexes});
    }

    public static void stopQTE() {
        runNoArgs("stopQTE");
    }

    public static void setQTEQuickReflexes(boolean hasQuickReflexes) {
        run("setQTEQuickReflexes", new Class<?>[]{boolean.class}, new Object[]{hasQuickReflexes});
    }

    public static void setWallhackHighlightedPlayers(Set<UUID> highlightedPlayers, int durationTicks) {
        run("setWallhackHighlightedPlayers",
                new Class<?>[]{Set.class, int.class},
                new Object[]{highlightedPlayers, durationTicks});
    }

    private static void runNoArgs(String methodName) {
        run(methodName, new Class<?>[0], new Object[0]);
    }

    private static void run(String methodName, Class<?>[] parameterTypes, Object[] args) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> invoke(methodName, parameterTypes, args));
    }

    private static void invoke(String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Class<?> helper = Class.forName(HELPER_CLASS);
            Method method = helper.getMethod(methodName, parameterTypes);
            method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            Maniacrev.LOGGER.error("Failed to run client-only helper method {}", methodName, e);
        }
    }
}
