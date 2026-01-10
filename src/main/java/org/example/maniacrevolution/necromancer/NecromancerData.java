package org.example.maniacrevolution.necromancer;

import net.minecraft.nbt.CompoundTag;

public class NecromancerData {
    private boolean hasPassiveProtection;
    private long lastPassiveUse; // Время последнего использования пассивки
    private static final long PASSIVE_COOLDOWN = 300000; // 5 минут в миллисекундах

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