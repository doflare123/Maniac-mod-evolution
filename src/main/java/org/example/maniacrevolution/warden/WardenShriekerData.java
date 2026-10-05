package org.example.maniacrevolution.warden;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One persistent flag for all dimensions, including unloaded chunks. Default: disabled. */
public final class WardenShriekerData extends SavedData {
    private boolean enabled;
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean value) { if (enabled != value) { enabled = value; setDirty(); } }
    public static WardenShriekerData load(CompoundTag tag) {
        var data = new WardenShriekerData(); data.enabled = tag.getBoolean("enabled"); return data;
    }
    @Override public CompoundTag save(CompoundTag tag) { tag.putBoolean("enabled", enabled); return tag; }
    public static WardenShriekerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WardenShriekerData::load,
                WardenShriekerData::new, "maniacrev_warden_shriekers");
    }
}
