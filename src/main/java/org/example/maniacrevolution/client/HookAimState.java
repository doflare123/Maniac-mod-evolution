package org.example.maniacrevolution.client;

/** Input state independent of the game window, so cancellation cannot become a release cast. */
public final class HookAimState {
    private boolean aiming;
    private boolean waitForAimRelease;
    private boolean suppressCancel;
    private int slot = -1;
    private Object stack;

    public void begin(int selectedSlot, Object heldStack) {
        if (suppressCancel) {
            waitForAimRelease = true;
            return;
        }
        if (aiming || waitForAimRelease) return;
        aiming = true;
        slot = selectedSlot;
        stack = heldStack;
    }

    public void validate(int selectedSlot, Object heldStack) {
        if (aiming && (slot != selectedSlot || stack != heldStack)) cancel();
    }

    public void cancel() {
        if (aiming) waitForAimRelease = true;
        aiming = false;
        stack = null;
        slot = -1;
    }

    public void cancelWithButton() {
        cancel();
        suppressCancel = true;
    }

    public void updateCancelKey(boolean down) {
        if (!down) suppressCancel = false;
    }

    public boolean release() {
        boolean cast = aiming && !waitForAimRelease;
        aiming = false;
        waitForAimRelease = false;
        stack = null;
        slot = -1;
        return cast;
    }

    public boolean isAiming() { return aiming; }
    public boolean blocksCancel() { return suppressCancel; }

    public void reset() {
        cancel();
        waitForAimRelease = false;
        suppressCancel = false;
    }
}
