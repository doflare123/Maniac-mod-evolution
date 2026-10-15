package org.example.maniacrevolution.scp173.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.item.ITimedAbility;

/** Class ability in the same HUD slot as the Warden's sniff. No inventory item required. */
public final class Scp173LightHud implements ITimedAbility {
    public static final Scp173LightHud INSTANCE = new Scp173LightHud();
    private Scp173LightHud() {}
    public ResourceLocation getAbilityIcon() { return new ResourceLocation("maniacrev", "textures/gui/scp173_light.png"); }
    public String getAbilityName() { return Component.translatable("ability.maniacrev.scp173.light").getString(); }
    public String getAbilityDescription() { return Component.translatable("ability.maniacrev.scp173.light.description").getString(); }
    public float getManaCost() { return Scp173GameplayClient.lightCost(); }
    public int getCooldownSeconds(Player player) { return (int) Math.ceil(Scp173GameplayClient.lightCooldown() / 20); }
    public int getMaxCooldownSeconds() { return 180; }
    public int getDurationSeconds() { return 3; }
    public int getRemainingDurationSeconds(Player player) { return (Scp173GameplayClient.lightDuration() + 19) / 20; }
    public boolean isAbilityActive(Player player) { return Scp173GameplayClient.lightDuration() > 0; }
    @Override public float getCooldownProgress(Player player) { return (float) Math.min(1, Scp173GameplayClient.lightCooldown() / 3600); }
    @Override public float getDurationProgress(Player player) { return Scp173GameplayClient.lightDuration() / 60F; }
}
