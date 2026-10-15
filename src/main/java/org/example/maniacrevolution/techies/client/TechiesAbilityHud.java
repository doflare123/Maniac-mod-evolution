package org.example.maniacrevolution.techies.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.item.ITimedAbility;
import org.example.maniacrevolution.techies.MineType;

/** Uses the existing class-ability slot; all three independent cooldowns remain visible. */
public final class TechiesAbilityHud implements ITimedAbility {
    public static final TechiesAbilityHud INSTANCE=new TechiesAbilityHud();
    private TechiesAbilityHud() {}
    private MineType selected() { return TechiesClient.selected<0?null:MineType.values()[TechiesClient.selected]; }
    public ResourceLocation getAbilityIcon() { return Maniacrev.loc("textures/gui/techies/ability.png"); }
    public String getAbilityName() { return "Мины Течиса"; }
    public String getAbilityDescription() { return "Выбрать красную, зелёную или синюю мину"; }
    public float getManaCost() { var t=selected(); return t==null?0:t.cost; }
    public int getCooldownSeconds(Player p) {
        var t=selected(); return t==null?0:(int)Math.ceil(Math.max(0,TechiesClient.cooldowns[t.ordinal()]-TechiesClient.time())/20);
    }
    public int getMaxCooldownSeconds() { var t=selected(); return t==null?0:t.cooldown/20; }
    public boolean isAbilityActive(Player p) { return selected()!=null && TechiesClient.installStart>=0; }
    public int getDurationSeconds() { var t=selected(); return t==null?0:t.install/20; }
    public int getRemainingDurationSeconds(Player p) { return (int)Math.ceil(getDurationSeconds()*getDurationProgress(p)); }
    public float getDurationProgress(Player p) {
        var t=selected(); return t==null || TechiesClient.installStart<0?0:(float)Math.max(0,1-(TechiesClient.time()-TechiesClient.installStart)/t.install);
    }
}
