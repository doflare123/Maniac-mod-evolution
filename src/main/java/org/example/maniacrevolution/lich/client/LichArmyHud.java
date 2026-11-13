package org.example.maniacrevolution.lich.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.item.IItemWithAbility;

/** Class ability in the same HUD slot as Warden's sniff; opening the army costs nothing. */
public final class LichArmyHud implements IItemWithAbility {
    public static final LichArmyHud INSTANCE=new LichArmyHud();
    private LichArmyHud(){}
    @Override public ResourceLocation getAbilityIcon(){return new ResourceLocation("maniacrev","textures/gui/abilities/lich_army_jade.png");}
    @Override public String getAbilityName(){return "Армия мёртвых";}
    @Override public String getAbilityDescription(){return "Открыть список нежити, выбрать отряд и отдать приказ.";}
    @Override public float getManaCost(){return 0;}
    @Override public int getCooldownSeconds(Player player){return 0;}
    @Override public int getMaxCooldownSeconds(){return 0;}
}
