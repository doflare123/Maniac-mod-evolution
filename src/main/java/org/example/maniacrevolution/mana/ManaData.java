package org.example.maniacrevolution.mana;

import net.minecraft.nbt.CompoundTag;

public class ManaData {
    private float mana;
    private float maxMana;
    private float regenRate; // Маны в секунду

    public ManaData(float maxMana, float regenRate) {
        this.maxMana = maxMana;
        this.mana = maxMana;
        this.regenRate = regenRate;
    }

    public ManaData() {
        this(100.0f, 1.0f); // Дефолтные значения
    }

    public float getMana() {
        return mana;
    }

    public float getMaxMana() {
        return maxMana;
    }

    public float getRegenRate() {
        return regenRate;
    }

    public void setMana(float mana) {
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public void setMaxMana(float maxMana) {
        this.maxMana = Math.max(1, maxMana);
        if (this.mana > this.maxMana) {
            this.mana = this.maxMana;
        }
    }

    public void setRegenRate(float regenRate) {
        this.regenRate = regenRate;
    }

    public boolean consumeMana(float amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }

    public void addMana(float amount) {
        setMana(mana + amount);
    }

    public void regenerate(float deltaTime) {
        if (mana < maxMana) {
            addMana(regenRate * deltaTime);
        }
    }

    public float getManaPercentage() {
        return maxMana > 0 ? mana / maxMana : 0;
    }

    public void copyFrom(ManaData source) {
        this.mana = source.mana;
        this.maxMana = source.maxMana;
        this.regenRate = source.regenRate;
    }

    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mana", mana);
        tag.putFloat("maxMana", maxMana);
        tag.putFloat("regenRate", regenRate);
        return tag;
    }

    public void loadNBTData(CompoundTag tag) {
        mana = tag.getFloat("mana");
        maxMana = tag.getFloat("maxMana");
        regenRate = tag.getFloat("regenRate");
    }
}