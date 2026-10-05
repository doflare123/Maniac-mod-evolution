package org.example.maniacrevolution.warden.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.item.ITimedAbility;

/** Class ability adapter for the existing HUD, without an inventory item. */
public final class WardenSniffHud implements ITimedAbility {
    public static final WardenSniffHud INSTANCE = new WardenSniffHud();
    private WardenSniffHud() {}
    public ResourceLocation getAbilityIcon() { return new ResourceLocation("minecraft", "textures/block/sculk_sensor_top.png"); }
    public String getAbilityName() { return "Нюх"; }
    public String getAbilityDescription() { return "Нюх: следы движения выживших видны 5 секунд после подготовки."; }
    public float getManaCost() { return 0; }
    public int getCooldownSeconds(Player player) { return (WardenSniffClient.cooldown() + 19) / 20; }
    public int getMaxCooldownSeconds() { return 10; }
    public int getDurationSeconds() { return 5; }
    public int getRemainingDurationSeconds(Player player) { return (WardenSniffClient.duration() + 19) / 20; }
    public boolean isAbilityActive(Player player) { return WardenSniffClient.duration() > 0; }
    @Override public float getCooldownProgress(Player player) { return WardenSniffClient.cooldown() / 200F; }
    @Override public float getDurationProgress(Player player) { return WardenSniffClient.duration() / 100F; }
}
