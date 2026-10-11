package org.example.maniacrevolution.techies;

import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Plain item: helmet has no armor attributes or defensive effects. */
public class TechiesItem extends Item implements GeoItem {
    public final String stem;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public TechiesItem(String stem) { super(new Properties().stacksTo(1)); this.stem=stem; }
    @Override public EquipmentSlot getEquipmentSlot(ItemStack stack) { return stem.equals("techies_helmet")?EquipmentSlot.HEAD:null; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(stem.equals("techies_remote")) {
            if(level.isClientSide) org.example.maniacrevolution.techies.client.TechiesClient.remote();
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        return super.use(level,player,hand);
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new org.example.maniacrevolution.techies.client.TechiesItemRenderer();
                return renderer;
            }
        });
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
