package org.example.maniacrevolution.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerkPreset {
    private String name;
    private final List<String> perkIds;
    private long createdAt;

    public PerkPreset(String name, List<String> perkIds) {
        this.name = name;
        this.perkIds = new ArrayList<>(perkIds);
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPerkIds() {
        return Collections.unmodifiableList(perkIds);
    }

    public int getPerkCount() { return perkIds.size(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putLong("created", createdAt);

        ListTag perksTag = new ListTag();
        for (String id : perkIds) {
            perksTag.add(StringTag.valueOf(id));
        }
        tag.put("perks", perksTag);

        return tag;
    }

    public static PerkPreset load(CompoundTag tag) {
        String name = tag.getString("name");
        List<String> perks = new ArrayList<>();

        ListTag perksTag = tag.getList("perks", Tag.TAG_STRING);
        for (int i = 0; i < perksTag.size(); i++) {
            perks.add(perksTag.getString(i));
        }

        PerkPreset preset = new PerkPreset(name, perks);
        preset.createdAt = tag.getLong("created");
        return preset;
    }
}

