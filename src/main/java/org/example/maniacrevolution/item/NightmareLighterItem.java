package org.example.maniacrevolution.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.client.renderer.NightmareLighterRenderer;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class NightmareLighterItem extends Item implements GeoItem {
    private static final String LIT_TAG = "Lit";
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("Idle");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("animation");
    private static final RawAnimation FIRE = RawAnimation.begin().thenLoop("fire");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlay("Close");
    private static final Map<UUID, BlockPos> LIGHT_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> LIGHTER_FUEL = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NightmareLighterItem() {
        super(new Properties().stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static void tickHeld(ServerPlayer player, ItemStack stack, EquipmentSlot breakSlot) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!(stack.getItem() instanceof NightmareLighterItem)) {
            removeLight(player);
            return;
        }
        if (!isLit(stack)) {
            removeLight(player);
            return;
        }

        moveLight(player, level, findLightPos(player));

        if (player.tickCount % NightmareConfig.LIGHTER_DAMAGE_INTERVAL_TICKS == 0) {
            UUID uuid = player.getUUID();
            int fuel = LIGHTER_FUEL.getOrDefault(uuid, NightmareConfig.LIGHTER_DURABILITY) - 1;
            if (fuel <= 0) {
                LIGHTER_FUEL.remove(uuid);
                setLit(stack, false);
                stack.shrink(1);
                player.broadcastBreakEvent(breakSlot);
                removeLight(player);
            } else {
                LIGHTER_FUEL.put(uuid, fuel);
            }
        }
    }

    public static void removeLight(ServerPlayer player) {
        BlockPos old = LIGHT_POSITIONS.remove(player.getUUID());
        if (old != null && player.level().getBlockState(old).is(Blocks.LIGHT)) {
            player.level().setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void resetFuel(ServerPlayer player) {
        LIGHTER_FUEL.put(player.getUUID(), NightmareConfig.LIGHTER_DURABILITY);
    }

    public static void clearState(ServerPlayer player) {
        removeLight(player);
        LIGHTER_FUEL.remove(player.getUUID());
    }

    public static boolean isLit(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(LIT_TAG);
    }

    private static void setLit(ItemStack stack, boolean lit) {
        stack.getOrCreateTag().putBoolean(LIT_TAG, lit);
    }

    private static BlockPos findLightPos(ServerPlayer player) {
        BlockPos head = player.blockPosition().above();
        if (canReplaceWithLight(player.level(), head)) return head;

        BlockPos feet = player.blockPosition();
        if (canReplaceWithLight(player.level(), feet)) return feet;

        return head;
    }

    private static boolean canReplaceWithLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.LIGHT);
    }

    private static void moveLight(ServerPlayer player, ServerLevel level, BlockPos lightPos) {
        BlockPos old = LIGHT_POSITIONS.get(player.getUUID());
        if (old != null && !old.equals(lightPos) && level.getBlockState(old).is(Blocks.LIGHT)) {
            level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        }

        if (canReplaceWithLight(level, lightPos)) {
            level.setBlock(lightPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 6), 3);
            LIGHT_POSITIONS.put(player.getUUID(), lightPos.immutable());
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isLit(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean lit = !isLit(stack);
            setLit(stack, lit);
            if (!lit && player instanceof ServerPlayer serverPlayer) {
                removeLight(serverPlayer);
            }
            player.getInventory().setChanged();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.inventoryMenu.broadcastChanges();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new NightmareLighterRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "lighter", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            boolean lit = stack != null && isLit(stack);

            if (lit) {
                if (state.isCurrentAnimation(FIRE)) {
                    return state.setAndContinue(FIRE);
                }
                if (state.isCurrentAnimation(OPEN)) {
                    return state.getController().hasAnimationFinished()
                            ? state.setAndContinue(FIRE)
                            : state.setAndContinue(OPEN);
                }
                return state.setAndContinue(OPEN);
            }

            if (state.isCurrentAnimation(FIRE) || state.isCurrentAnimation(OPEN)) {
                return state.setAndContinue(CLOSE);
            }
            if (state.isCurrentAnimation(CLOSE) && !state.getController().hasAnimationFinished()) {
                return state.setAndContinue(CLOSE);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
