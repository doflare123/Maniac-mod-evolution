package org.example.maniacrevolution.item.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cloak.CloakManager;
import org.example.maniacrevolution.item.IItemWithAbility;
import java.util.List;
import java.util.function.Consumer;

public final class DoctorStrangeCloakItem extends ArmorItem implements IActivatableArmor, IItemWithAbility {
    public DoctorStrangeCloakItem() {
        super(ArmorMaterials.LEATHER, Type.CHESTPLATE, new Properties().rarity(Rarity.EPIC));
    }

    @Override public boolean activateAbility(ServerPlayer player) { return CloakManager.throwCloak(player); }
    @Override public boolean canActivate(ServerPlayer player) { return CloakManager.canThrow(player); }
    @Override public float getManaCost() { return 0; }
    @Override public int getDuration() { return 140; }
    @Override public int getCooldown() { return 0; }
    @Override public int getCooldownSeconds(Player player) { return 0; }
    @Override public int getMaxCooldownSeconds() { return 0; }
    @Override public String getAbilityName() { return Component.translatable("ability.maniacrev.cloak.name").getString(); }
    @Override public String getAbilityDescription() { return Component.translatable("ability.maniacrev.cloak.desc").getString(); }
    @Override public ResourceLocation getAbilityIcon() { return Maniacrev.loc("textures/item/doctor_strange_cloak.png"); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.maniacrev.cloak.hover"));
        lines.add(Component.translatable("tooltip.maniacrev.cloak.throw"));
    }

    // The animated entity supplies the complete cloak. Suppress the leather chestplate mesh.
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> empty;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                    EquipmentSlot slot, HumanoidModel<?> original) {
                if (empty == null) {
                    var mesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
                    for (String part : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
                        mesh.getRoot().addOrReplaceChild(part, net.minecraft.client.model.geom.builders.CubeListBuilder.create(),
                                net.minecraft.client.model.geom.PartPose.ZERO);
                    }
                    empty = new HumanoidModel<LivingEntity>(net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh, 64, 32).bakeRoot());
                }
                empty.setAllVisible(false);
                return empty;
            }
        });
    }
}
