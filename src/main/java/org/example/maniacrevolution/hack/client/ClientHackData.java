package org.example.maniacrevolution.hack.client;

/**
 * Клиентское состояние взломов компьютеров.
 * Обновляется через SyncHackDataPacket (Server → Client).
 */
public class ClientHackData {
    private static int totalHacked = 0;
    private static int goal = 3;

    public static void update(int hacked, int hackGoal) {
        totalHacked = hacked;
        goal = hackGoal;
    }

    public static int getTotalHacked() { return totalHacked; }
    public static int getGoal()        { return goal; }
}