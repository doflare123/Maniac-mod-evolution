package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.nightmare.NightmareManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuardianHeadItem extends ArmorItem implements IActivatableArmor, IItemWithAbility {
    private static final ArmorMaterial MATERIAL = new HeadMaterial();

    public GuardianHeadItem() {
        super(MATERIAL, Type.HELMET, new Properties().stacksTo(1));
    }

    @Override
    public boolean activateAbility(ServerPlayer player) {
        if (!canActivate(player)) return false;
        boolean cast = NightmareManager.getInstance().castConcentratedNightmare(player);
        if (!cast) return false;
        ArmorAbilityCooldownManager.setCooldown(player, this, NightmareConfig.CONCENTRATED_NIGHTMARE_COOLDOWN_TICKS);
        ArmorAbilityCooldownManager.syncToClient(player, this, 0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                player.getSoundSource(), 0.45F, 0.6F);
        return true;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() != this) return false;
        if (ArmorAbilityCooldownManager.isOnCooldown(player, this)) {
            player.displayClientMessage(Component.literal("Кошмар восстанавливается: " +
                    ArmorAbilityCooldownManager.getRemainingCooldown(player, this) / 20 + "с"), true);
            return false;
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Способность: " + getAbilityName()).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal(getAbilityDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Кулдаун: " + getMaxCooldownSeconds() + "с").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return Maniacrev.loc("textures/gui/abilities/concentrated_nightmare.png");
    }

    @Override
    public String getAbilityName() {
        return "Концентрированный кошмар";
    }

    @Override
    public String getAbilityDescription() {
        return "Резкий кошмар у цели во взгляде. Снимает 10% рассудка.";
    }

    @Override
    public float getManaCost() {
        return 0.0F;
    }

    @Override
    public int getCooldownSeconds(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return ArmorAbilityCooldownManager.getRemainingCooldown(serverPlayer, this) / 20;
        }
        return ClientAbilityData.getCooldownSeconds(this);
    }

    @Override
    public int getMaxCooldownSeconds() {
        return NightmareConfig.CONCENTRATED_NIGHTMARE_COOLDOWN_TICKS / 20;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCooldown() {
        return NightmareConfig.CONCENTRATED_NIGHTMARE_COOLDOWN_TICKS;
    }

    private static final class HeadMaterial implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) { return 0; }
        @Override public int getDefenseForType(ArmorItem.Type type) { return 0; }
        @Override public int getEnchantmentValue() { return 0; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        @Override public String getName() { return Maniacrev.MODID + ":guardian_head"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    }
}
