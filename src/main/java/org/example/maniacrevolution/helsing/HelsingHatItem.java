package org.example.maniacrevolution.helsing;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.item.armor.ArmorAbilityCooldownManager;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

public final class HelsingHatItem extends ArmorItem implements IActivatableArmor, org.example.maniacrevolution.item.IItemWithAbility, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public HelsingHatItem() { super(ModItems.ModArmorMaterials.NECROMANCER, Type.HELMET, new Properties().stacksTo(1).durability(0)); }
    @Override public boolean activateAbility(ServerPlayer player) { return HelsingManager.protect(player); }
    @Override public boolean canActivate(ServerPlayer player) {
        return HelsingManager.isHelsing(player) && player.getItemBySlot(EquipmentSlot.HEAD).is(this) && !ArmorAbilityCooldownManager.isOnCooldown(player, this);
    }
    public float getManaCost() { return HelsingConfig.COST.get().floatValue(); }
    public int getDuration() { return HelsingConfig.PROTECTION.get(); }
    public int getCooldown() { return HelsingConfig.COOLDOWN.get(); }
    public String getAbilityName() { return Component.translatable("ability.maniacrev.helsing.name").getString(); }
    public String getAbilityDescription() { return Component.translatable("ability.maniacrev.helsing.desc").getString(); }
    public ResourceLocation getAbilityIcon() { return Maniacrev.loc("textures/gui/abilities/helsing_blood.png"); }
    public int getCooldownSeconds(Player player) {
        return player instanceof ServerPlayer server ? (ArmorAbilityCooldownManager.getRemainingCooldown(server, this) + 19) / 20 : ClientAbilityData.getCooldownSeconds(this);
    }
    public int getMaxCooldownSeconds() { return (getCooldown() + 19) / 20; }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getAbilityName()).withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(Component.literal(getAbilityDescription()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.mana_cost", (int) getManaCost()));
        tooltip.add(Component.translatable("tooltip.maniacrev.duration", getDuration() / 20));
        tooltip.add(Component.translatable("tooltip.maniacrev.cooldown", getMaxCooldownSeconds()));
    }
    @Override public boolean isAbilityActive(Player player) { return false; }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HelsingHatRenderer renderer;
            private HelsingHatItemRenderer itemRenderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (itemRenderer == null) itemRenderer = new HelsingHatItemRenderer();
                return itemRenderer;
            }
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (renderer == null) renderer = new HelsingHatRenderer();
                renderer.prepForRender(entity, stack, slot, original);
                return renderer;
            }
        });
    }
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "hat", 0, state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
