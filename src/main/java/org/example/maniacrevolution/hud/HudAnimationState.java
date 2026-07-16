package org.example.maniacrevolution.hud;

import net.minecraft.Util;
import net.minecraft.util.Mth;

/** Keeps HUD transitions independent from server packet cadence. */
final class HudAnimationState {
    private long lastUpdateMs = -1L;
    private float health = Float.NaN;
    private float healthTrail = Float.NaN;
    private float mana = Float.NaN;
    private float statusVisibility;
    private float slotPulse;
    private int selectedSlot = -1;

    void update(float targetHealth, float targetMana, int newSelectedSlot,
                boolean hasContextStatus, boolean paused) {
        long now = Util.getMillis();
        if (lastUpdateMs < 0L) {
            health = targetHealth;
            healthTrail = targetHealth;
            mana = targetMana;
            statusVisibility = hasContextStatus ? 1.0f : 0.0f;
            selectedSlot = newSelectedSlot;
            lastUpdateMs = now;
            return;
        }

        float delta = paused ? 0.0f : Mth.clamp((now - lastUpdateMs) / 1000.0f, 0.0f, 0.05f);
        lastUpdateMs = now;

        health = approach(health, targetHealth, delta, 14.0f);
        mana = approach(mana, targetMana, delta, 12.0f);

        if (targetHealth >= healthTrail) {
            healthTrail = targetHealth;
        } else {
            healthTrail = approach(healthTrail, targetHealth, delta, 3.0f);
        }

        statusVisibility = approach(statusVisibility, hasContextStatus ? 1.0f : 0.0f, delta, 10.0f);

        if (selectedSlot != newSelectedSlot) {
            selectedSlot = newSelectedSlot;
            slotPulse = 1.0f;
        } else {
            slotPulse = Math.max(0.0f, slotPulse - delta * 5.0f);
        }
    }

    float health() {
        return health;
    }

    float healthTrail() {
        return Math.max(health, healthTrail);
    }

    float mana() {
        return mana;
    }

    float statusVisibility() {
        return statusVisibility;
    }

    float selectedSlotScale(int slot) {
        return slot == selectedSlot ? 1.0f + slotPulse * 0.12f : 1.0f;
    }

    private static float approach(float current, float target, float delta, float speed) {
        if (delta <= 0.0f) return current;
        float factor = 1.0f - (float) Math.exp(-speed * delta);
        return Mth.lerp(factor, current, target);
    }
}
