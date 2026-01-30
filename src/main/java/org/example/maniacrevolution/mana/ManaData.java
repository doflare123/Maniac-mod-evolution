package org.example.maniacrevolution.mana;

import net.minecraft.nbt.CompoundTag;

public class ManaData {
    private float mana;
    private float maxMana;
    private float baseRegenRate; // Базовая регенерация маны в секунду
    private float bonusRegenRate; // Бонусная регенерация (от эффектов)
    private boolean passiveRegenEnabled; // Включена ли пассивная регенерация

    public ManaData(float maxMana, float baseRegenRate) {
        this.maxMana = maxMana;
        this.mana = maxMana;
        this.baseRegenRate = baseRegenRate;
        this.bonusRegenRate = 0.0f;
        this.passiveRegenEnabled = true;
    }

    public ManaData() {
        this(100.0f, 0.0f); // Дефолтные значения: 100 маны, 1 ед/сек
    }

    public float getMana() {
        return mana;
    }

    public float getMaxMana() {
        return maxMana;
    }

    public float getBaseRegenRate() {
        return baseRegenRate;
    }

    public float getBonusRegenRate() {
        return bonusRegenRate;
    }


    public float getTotalRegenRate() {
        float total = bonusRegenRate; // Бонус всегда работает
        if (passiveRegenEnabled) {
            total += baseRegenRate; // Базовый реген только если включен
        }
        return total;
    }

    public boolean isPassiveRegenEnabled() {
        return passiveRegenEnabled;
    }

    public void setPassiveRegenEnabled(boolean enabled) {
        this.passiveRegenEnabled = enabled;
        if (!enabled && baseRegenRate > 0) {
            System.out.println("[ManaData] Passive regen disabled, baseRegenRate was: " + baseRegenRate);
        }
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

    public void setBaseRegenRate(float regenRate) {
        this.baseRegenRate = Math.max(0, regenRate);
    }

    public void setBonusRegenRate(float bonusRegenRate) {
        this.bonusRegenRate = Math.max(0, bonusRegenRate);
        if (bonusRegenRate != this.bonusRegenRate) {
            System.out.println("[ManaData] Bonus regen changed: " + this.bonusRegenRate + " -> " + bonusRegenRate);
        }
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
            float totalRegen = getTotalRegenRate();
            if (totalRegen > 0) {
                addMana(totalRegen * deltaTime);
            }
        }
    }

    public float getManaPercentage() {
        return maxMana > 0 ? mana / maxMana : 0;
    }

    public void copyFrom(ManaData source) {
        this.mana = source.mana;
        this.maxMana = source.maxMana;
        this.baseRegenRate = source.baseRegenRate;
        this.bonusRegenRate = source.bonusRegenRate;
        this.passiveRegenEnabled = source.passiveRegenEnabled;
    }

    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mana", mana);
        tag.putFloat("maxMana", maxMana);
        tag.putFloat("baseRegenRate", baseRegenRate);
        tag.putFloat("bonusRegenRate", bonusRegenRate);
        tag.putBoolean("passiveRegenEnabled", passiveRegenEnabled);
        return tag;
    }

    public void loadNBTData(CompoundTag tag) {
        mana = tag.getFloat("mana");
        maxMana = tag.getFloat("maxMana");
        baseRegenRate = tag.getFloat("baseRegenRate");
        bonusRegenRate = tag.getFloat("bonusRegenRate");
        passiveRegenEnabled = tag.getBoolean("passiveRegenEnabled");
    }
}