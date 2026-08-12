package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import org.example.maniacrevolution.client.screen.BudDispatcherScreen;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.network.packets.BudDispatcherPacket;
import org.example.maniacrevolution.perk.perks.maniac.BudDispatcherPerk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Клиентский снимок без координат и прочих данных о расположении компьютеров. */
public final class BudDispatcherClientData {
    private static final Map<Integer, FlowerState> STATE_BY_COMPUTER = new LinkedHashMap<>();

    private BudDispatcherClientData() {
    }

    public static void apply(List<BudDispatcherPacket.Entry> entries,
                             boolean replaceState, boolean openScreen) {
        if (replaceState) {
            STATE_BY_COMPUTER.clear();
        }
        for (BudDispatcherPacket.Entry entry : entries) {
            int flower = Math.max(0, Math.min(BudDispatcherPerk.FLOWER_VARIANT_COUNT - 1,
                    entry.flowerIndex()));
            int stage = Math.max(0, Math.min(BudDispatcherPerk.WILT_STAGE_COUNT - 1,
                    entry.stage()));
            STATE_BY_COMPUTER.put(entry.computerId(), new FlowerState(
                    entry.computerId(), flower, stage));
        }
        if (openScreen) {
            Minecraft.getInstance().setScreen(new BudDispatcherScreen());
        }
    }

    public static int getFlowerIndex(int computerId) {
        FlowerState state = STATE_BY_COMPUTER.get(computerId);
        return state == null ? -1 : state.flowerIndex();
    }

    public static int getStage(int computerId) {
        FlowerState state = STATE_BY_COMPUTER.get(computerId);
        return state == null ? -1 : state.stage();
    }

    public static List<FlowerState> getStates() {
        List<FlowerState> states = new ArrayList<>(STATE_BY_COMPUTER.values());
        states.sort(Comparator.comparingInt(FlowerState::computerId));
        return states;
    }

    public static boolean canSeeFlowers() {
        int phase = ClientGameState.getPhase();
        if (phase != 1 && phase != 2) return false;
        return ClientPlayerData.getSelectedPerks().stream()
                .anyMatch(perk -> BudDispatcherPerk.ID.equals(perk.id()));
    }

    public record FlowerState(int computerId, int flowerIndex, int stage) {
    }
}
