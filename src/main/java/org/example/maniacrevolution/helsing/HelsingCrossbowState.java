package org.example.maniacrevolution.helsing;

import net.minecraft.nbt.CompoundTag;

/** Persistent charge state shared by the item and regression checks. */
final class HelsingCrossbowState {
    static final int USE_DURATION = 72000;
    static boolean loaded(CompoundTag tag) { return tag != null && tag.contains("LoadedBolt"); }
    static boolean shouldLoad(CompoundTag tag, int remaining, int duration) {
        return !loaded(tag) && USE_DURATION - remaining >= duration;
    }
    static void load(CompoundTag tag, int kind) {
        if (loaded(tag)) return;
        tag.putInt("LoadedBolt", Math.floorMod(kind, 4));
        tag.putInt("VisualBolt", Math.floorMod(kind, 4));
    }
    static void fired(CompoundTag tag) {
        if (!loaded(tag)) return;
        tag.putInt("VisualBolt", tag.getInt("LoadedBolt"));
        tag.remove("LoadedBolt");
    }
}
