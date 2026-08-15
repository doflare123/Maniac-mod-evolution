package org.example.maniacrevolution.client;

/** Client-side snapshot of the current pre-game readiness vote. */
public final class ClientPreGameReadyData {
    private static boolean active;
    private static String initiatorName = "";
    private static int readyCount;
    private static int totalCount;
    private static boolean localReady;

    private ClientPreGameReadyData() {}

    public static void update(boolean active, String initiatorName, int readyCount,
                              int totalCount, boolean localReady) {
        ClientPreGameReadyData.active = active;
        ClientPreGameReadyData.initiatorName = initiatorName;
        ClientPreGameReadyData.readyCount = readyCount;
        ClientPreGameReadyData.totalCount = totalCount;
        ClientPreGameReadyData.localReady = localReady;
    }

    public static boolean isActive() { return active; }
    public static String getInitiatorName() { return initiatorName; }
    public static int getReadyCount() { return readyCount; }
    public static int getTotalCount() { return totalCount; }
    public static boolean isLocalReady() { return localReady; }
}
