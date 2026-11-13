package org.example.maniacrevolution.lich;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class LichItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    public enum Role { BOOK, PHYLACTERY, EDITOR }
    public final Role role;
    public LichItem(Role role) { super(new Item.Properties().stacksTo(1)); this.role = role; software.bernie.geckolib.animatable.GeoItem.registerSyncedAnimatable(this); }
    @Override public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar c) {
        if (role != Role.EDITOR) {
        var controller=new software.bernie.geckolib.core.animation.AnimationController<>(this, "idle",0,s -> {
            ItemStack stack=s.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            String action=stack!=null&&stack.hasTag()?stack.getTag().getString("LichCast"):"",stem=role==Role.BOOK?"lich_book":"lich_phylactery";
            String clip=action.isEmpty()?"idle":role==Role.BOOK?"cast_"+action:action.equals("repair")?"repair":"idle";
            return s.setAndContinue(action.isEmpty()?software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation."+stem+"."+clip):software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation."+stem+"."+clip));
        });
        String stem=role==Role.BOOK?"lich_book":"lich_phylactery";
        controller.triggerableAnim("reset",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation."+stem+".idle"));
        if(role==Role.BOOK){
            controller.triggerableAnim("page_turn",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation.lich_book.page_turn"));
            for(String action:java.util.List.of("raise","absorb","repair"))controller.triggerableAnim("cast_"+action,software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.lich_book.cast_"+action));
        }else controller.triggerableAnim("repair",software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.lich_phylactery.repair"));
        c.add(controller);
        }
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        if (role == Role.EDITOR) return;
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new org.example.maniacrevolution.lich.client.LichRenderers.ItemRenderer();
                return renderer;
            }
        });
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && p instanceof ServerPlayer player) {
            if (role == Role.BOOK) LichManager.quick(player);
            if (role == Role.EDITOR && player.hasPermissions(2)) LichManager.editor(player, true);
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext c) {
        if (c.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (c.getPlayer() instanceof ServerPlayer p) {
            if (role == Role.BOOK) LichManager.quick(p);
            if (role == Role.PHYLACTERY) LichManager.place(p, c.getClickedPos(), c.getClickedFace());
            if (role == Role.EDITOR && p.hasPermissions(2)) {
                if (p.isShiftKeyDown()) LichManager.editor(p, true);
                else LichManager.editPoint(p, c.getClickedPos().relative(c.getClickedFace()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
