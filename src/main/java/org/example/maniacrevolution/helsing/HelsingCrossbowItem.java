package org.example.maniacrevolution.helsing;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;
import java.util.function.Consumer;

public final class HelsingCrossbowItem extends Item implements GeoItem {
    private static final RawAnimation LOAD = RawAnimation.begin().thenPlay("load");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public HelsingCrossbowItem() { super(new Properties().stacksTo(1)); GeoItem.registerSyncedAnimatable(this); }
    public static HelsingBoltItem.Kind selected(ItemStack stack) {
        return HelsingBoltItem.Kind.values()[Math.floorMod(stack.hasTag() ? stack.getTag().getInt("BoltKind") : 0, 4)];
    }
    public static boolean loaded(ItemStack stack) { return HelsingCrossbowState.loaded(stack.getTag()); }
    public static HelsingBoltItem.Kind loadedKind(ItemStack stack) {
        return loaded(stack) ? HelsingBoltItem.Kind.values()[Math.floorMod(stack.getTag().getInt("LoadedBolt"), 4)] : selected(stack);
    }
    public static HelsingBoltItem.Kind visualKind(ItemStack stack) {
        if (loaded(stack)) return loadedKind(stack);
        return stack.hasTag() && stack.getTag().contains("VisualBolt")
                ? HelsingBoltItem.Kind.values()[Math.floorMod(stack.getTag().getInt("VisualBolt"), 4)] : selected(stack);
    }
    public static void select(ServerPlayer player, InteractionHand hand, HelsingBoltItem.Kind kind) {
        if (!HelsingManager.isHelsing(player) || player.isUsingItem()) return;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof HelsingCrossbowItem)) return;
        stack.getOrCreateTag().putInt("BoltKind", kind.ordinal());
        player.displayClientMessage(Component.translatable("message.maniacrev.helsing.bolt", Component.translatable(selected(stack).key())), true);
        player.inventoryMenu.broadcastChanges();
    }
    private static ItemStack ammo(Player player, HelsingBoltItem.Kind kind) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(kind.item())) return candidate;
        }
        return ItemStack.EMPTY;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!HelsingManager.isSurvivor(player) || (!level.isClientSide && !HelsingManager.isHelsing((ServerPlayer) player))) return InteractionResultHolder.fail(stack);
        if (loaded(stack)) {
            if (player instanceof ServerPlayer server) {
                int kind = Math.floorMod(stack.getTag().getInt("LoadedBolt"), 4);
                stack.getOrCreateTag().putInt("VisualBolt", kind);
                HelsingBoltEntity bolt = new HelsingBoltEntity(level, player, HelsingBoltItem.Kind.values()[kind]);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0F, 0.3F);
                level.addFreshEntity(bolt);
                HelsingCrossbowState.fired(stack.getTag());
                triggerAnim(server, GeoItem.getOrAssignId(stack, server.serverLevel()), "crossbow", "shoot");
                level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, player.getSoundSource(), 1, 0.8F);
                player.getCooldowns().addCooldown(this, 6);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (ammo(player, selected(stack)).isEmpty()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.maniacrev.helsing.no_bolts"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer server) {
            stack.getOrCreateTag().putInt("VisualBolt", selected(stack).ordinal());
            server.inventoryMenu.broadcastChanges();
            triggerAnim(server, GeoItem.getOrAssignId(stack, server.serverLevel()), "crossbow", "load");
        }
        return InteractionResultHolder.consume(stack);
    }
    private void load(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && HelsingManager.isHelsing(player)) {
            HelsingBoltItem.Kind kind = selected(stack);
            ItemStack ammo = ammo(player, kind);
            if (!ammo.isEmpty() && !loaded(stack)) {
                ammo.shrink(1);
                HelsingCrossbowState.load(stack.getOrCreateTag(), kind.ordinal());
                level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, player.getSoundSource(), 0.8F, 0.85F);
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
    @Override public int getUseDuration(ItemStack stack) { return HelsingCrossbowState.USE_DURATION; }
    @Override public boolean useOnRelease(ItemStack stack) { return true; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (HelsingCrossbowState.shouldLoad(stack.getTag(), remaining, HelsingConfig.RELOAD.get())) load(stack, level, entity);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (HelsingCrossbowState.shouldLoad(stack.getTag(), remaining, HelsingConfig.RELOAD.get())) load(stack, level, entity);
        if (!loaded(stack) && entity instanceof ServerPlayer server)
            triggerAnim(server, GeoItem.getOrAssignId(stack, server.serverLevel()), "crossbow", "cancel");
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.CROSSBOW; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> text, TooltipFlag flag) {
        text.add(Component.translatable("tooltip.maniacrev.helsing.crossbow"));
        text.add(Component.translatable("message.maniacrev.helsing.bolt", Component.translatable(selected(stack).key())));
        if (loaded(stack)) text.add(Component.translatable("tooltip.maniacrev.helsing.loaded_type", Component.translatable(loadedKind(stack).key())));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new HelsingCrossbowRenderer();
                return renderer;
            }
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    ItemStack stack, float partialTick, float equip, float swing) {
                var usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
                int side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
                boolean charging = player.isUsingItem() && usedArm == arm && player.getUseItem().getItem() == stack.getItem();
                float progress = charging ? net.minecraft.util.Mth.clamp((stack.getUseDuration() - player.getUseItemRemainingTicks() + partialTick)
                        / HelsingConfig.RELOAD.get(), 0, 1) : 0;
                // Use the same forward-facing attachment for idle and charging.
                pose.translate(side * 0.44, -0.58 - equip * 0.6 - Math.sin(progress * Math.PI) * 0.035, -0.76 + progress * 0.035);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-5));
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(side * -3));
                return true;
            }
        });
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<HelsingCrossbowItem> controller = new AnimationController<>(this, "crossbow", 2, state -> {
            ItemStack stack = state.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            return state.setAndContinue(RawAnimation.begin().thenLoop(stack != null && loaded(stack) ? "loaded" : "idle"));
        });
        controller.triggerableAnim("load", LOAD)
                .triggerableAnim("shoot", RawAnimation.begin().thenPlay("shoot"))
                .triggerableAnim("cancel", RawAnimation.begin().thenPlay("cancel"));
        controller.setAnimationSpeedHandler(item -> controller.getCurrentRawAnimation() == LOAD ? 30.0 / HelsingConfig.RELOAD.get() : 1.0);
        controllers.add(controller);
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
