package org.example.maniacrevolution.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ghost.GhostStealthManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ToyKnifeItem extends Item implements ITimedAbility {
    public static final float MANA_COST = 3.0f;
    public static final float BASE_DAMAGE = 2.0f;
    private static final UUID DAMAGE_UUID = UUID.fromString("77d6a3a6-ffea-4402-b4af-2a65ac65dfc0");
    private static final UUID SPEED_UUID = UUID.fromString("a4ef69d8-038f-4ef4-87de-5f9342252acf");

    public ToyKnifeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            GhostStealthManager.toggleStealth(serverPlayer);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_UUID, "Toy knife damage", BASE_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_UUID, "Toy knife speed", -2.2, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.maniacrev.toy_knife.title").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.maniacrev.toy_knife.damage", BASE_DAMAGE));
        tooltip.add(Component.translatable("tooltip.maniacrev.toy_knife.invisibility"));
        tooltip.add(Component.translatable("tooltip.maniacrev.toy_knife.recovery",
                GhostStealthManager.RECOVERY_TICKS / 20));
        tooltip.add(Component.translatable("tooltip.maniacrev.toy_knife.cooldown"));
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/ghost_full_invisibility.png");
    }

    @Override
    public String getAbilityName() {
        return Component.translatable("ability.maniacrev.toy_knife.name").getString();
    }

    @Override
    public String getAbilityDescription() {
        return Component.translatable("ability.maniacrev.toy_knife.desc",
                GhostStealthManager.RECOVERY_TICKS / 20).getString();
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public int getCooldownSeconds(Player player) {
        return GhostStealthManager.getCooldownSeconds(player);
    }

    @Override
    public int getMaxCooldownSeconds() {
        return 0;
    }

    @Override
    public int getDurationSeconds() {
        return 0;
    }

    @Override
    public int getRemainingDurationSeconds(Player player) {
        return GhostStealthManager.getRemainingDurationSeconds(player);
    }

    @Override
    public boolean isAbilityActive(Player player) {
        return GhostStealthManager.isAbilityActive(player);
    }
}
