package org.example.maniacrevolution.necromancer;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class NecromancerData {
    private static final int RESURRECTION_SELECTION_TICKS = 30 * 20;
    private boolean hasPassiveProtection;
    private long lastPassiveUse; // Время последнего использования пассивки
    private static final long PASSIVE_COOLDOWN = 300000; // 5 минут в миллисекундах

    // Разрешение действует только для текущего экземпляра игрока и не сохраняется в NBT.
    private UUID pendingResurrectionId;
    private int pendingResurrectionExpiresAt;

    public NecromancerData() {
        this.hasPassiveProtection = true;
        this.lastPassiveUse = 0;
    }

    public boolean hasPassiveProtection() {
        return hasPassiveProtection;
    }

    public void usePassiveProtection() {
        this.hasPassiveProtection = false;
        this.lastPassiveUse = System.currentTimeMillis();
    }

    public void restorePassiveProtection() {
        this.hasPassiveProtection = true;
    }

    public boolean canRestorePassive() {
        if (hasPassiveProtection) return false;
        return (System.currentTimeMillis() - lastPassiveUse) >= PASSIVE_COOLDOWN;
    }

    public float getPassiveCooldownProgress() {
        if (hasPassiveProtection) return 1.0f;
        long elapsed = System.currentTimeMillis() - lastPassiveUse;
        return Math.min(1.0f, (float) elapsed / PASSIVE_COOLDOWN);
    }

    public UUID grantResurrection(int currentTick) {
        pendingResurrectionId = UUID.randomUUID();
        pendingResurrectionExpiresAt = currentTick + RESURRECTION_SELECTION_TICKS;
        return pendingResurrectionId;
    }

    public boolean hasPendingResurrection(UUID ritualId, int currentTick) {
        return ritualId.equals(pendingResurrectionId) && pendingResurrectionExpiresAt - currentTick > 0;
    }

    public boolean consumeResurrection(UUID ritualId, int currentTick) {
        if (!hasPendingResurrection(ritualId, currentTick)) {
            return false;
        }
        clearPendingResurrection();
        return true;
    }

    public void clearPendingResurrection() {
        pendingResurrectionId = null;
    }

    public void copyFrom(NecromancerData source) {
        this.hasPassiveProtection = source.hasPassiveProtection;
        this.lastPassiveUse = source.lastPassiveUse;
    }

    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("hasPassiveProtection", hasPassiveProtection);
        tag.putLong("lastPassiveUse", lastPassiveUse);
        return tag;
    }

    public void loadNBTData(CompoundTag tag) {
        hasPassiveProtection = tag.getBoolean("hasPassiveProtection");
        lastPassiveUse = tag.getLong("lastPassiveUse");
    }
}
