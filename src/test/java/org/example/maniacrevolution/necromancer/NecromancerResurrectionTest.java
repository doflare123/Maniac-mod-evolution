package org.example.maniacrevolution.necromancer;

import java.util.UUID;

public final class NecromancerResurrectionTest {
    public static void main(String[] args) {
        NecromancerData owner = new NecromancerData();
        NecromancerData otherPlayer = new NecromancerData();
        check(!owner.consumeResurrection(UUID.randomUUID(), 100), "Resurrection requires a completed ritual");

        UUID firstRitual = owner.grantResurrection(100);
        check(!otherPlayer.consumeResurrection(firstRitual, 101), "A permit belongs to the player who cast it");
        check(owner.consumeResurrection(firstRitual, 101), "A valid permit can be consumed");
        check(!owner.consumeResurrection(firstRitual, 101), "A consumed permit cannot be used again");

        UUID oldRitual = owner.grantResurrection(200);
        UUID currentRitual = owner.grantResurrection(300);
        check(!owner.consumeResurrection(oldRitual, 301), "A request for an old ritual cannot consume a new permit");
        check(owner.hasPendingResurrection(currentRitual, 899), "The current permit lasts 600 ticks");
        check(!owner.consumeResurrection(currentRitual, 900), "A permit expires at the end of its selection window");

        UUID pendingRitual = owner.grantResurrection(1000);
        otherPlayer.copyFrom(owner);
        check(!otherPlayer.hasPendingResurrection(pendingRitual, 1001), "Respawning does not copy a permit");
        otherPlayer.loadNBTData(owner.saveNBTData());
        check(!otherPlayer.hasPendingResurrection(pendingRitual, 1001), "Player saves do not contain permits");
        owner.clearPendingResurrection();
        check(!owner.hasPendingResurrection(pendingRitual, 1001), "A match reset invalidates the permit");

        System.out.println("NecromancerResurrection: 10 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
