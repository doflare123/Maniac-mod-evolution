package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.maniacrevolution.entity.HookEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.Level;

/** A visual follow-through, independent of item use duration and projectile launch. */
public final class HookItemClientExtensions implements IClientItemExtensions {
    private static final int ANIMATION_TICKS = 12;
    private static final Map<UUID, CastAnimation> CASTS = new HashMap<>();
    private static Level animationLevel;

    private record CastAnimation(InteractionHand hand, long startTick) {
        float age(LivingEntity player, float partialTick) {
            return player.level().getGameTime() - startTick + partialTick;
        }
    }
    private static final HumanoidModel.ArmPose THROW = HumanoidModel.ArmPose.create(
            "MANIACREV_HOOK_THROW", false, (model, entity, arm) -> {
                InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                CastAnimation cast = activeCast(entity, hand);
                if (cast == null) return;
                float t = Mth.clamp(cast.age(entity, Minecraft.getInstance().getFrameTime()) / ANIMATION_TICKS, 0, 1);
                float recovery = t * t * (3 - 2 * t);
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                part.xRot = Mth.lerp(recovery, -2.45f, -0.3f) + model.head.xRot * (1 - recovery);
                part.yRot = (arm == HumanoidArm.RIGHT ? -1 : 1) * (1 - recovery) * 0.3f;
                part.zRot = (arm == HumanoidArm.RIGHT ? 1 : -1) * Mth.sin(t * Mth.PI) * 0.3f;
            });

    private static CastAnimation activeCast(LivingEntity player, InteractionHand hand) {
        if (animationLevel != player.level()) {
            CASTS.clear();
            animationLevel = player.level();
        }
        CASTS.values().removeIf(cast -> cast.age(player, 0) >= ANIMATION_TICKS);
        CastAnimation existing = CASTS.get(player.getUUID());
        if (existing != null) return existing.hand() == hand ? existing : null;
        for (HookEntity hook : player.level().getEntitiesOfClass(HookEntity.class,
                player.getBoundingBox().inflate(HookEntity.MAX_DISTANCE + 8),
                hook -> hook.tickCount < ANIMATION_TICKS && hook.getOwner() == player
                        && hook.isOffHand() == (hand == InteractionHand.OFF_HAND))) {
            CastAnimation cast = new CastAnimation(hand, player.level().getGameTime() - hook.tickCount);
            CASTS.put(player.getUUID(), cast);
            return cast;
        }
        return null;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity player, InteractionHand hand, ItemStack stack) {
        return activeCast(player, hand) != null ? THROW : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poses, LocalPlayer player, HumanoidArm arm,
                                           ItemStack stack, float partialTick, float equip, float swing) {
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        CastAnimation cast = activeCast(player, hand);
        if (cast == null) return false;
        float t = Mth.clamp(cast.age(player, partialTick) / ANIMATION_TICKS, 0, 1);
        float thrust = (1 - t) * (1 - t);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poses.translate(side * (0.56 - thrust * 0.12), -0.52 - equip * 0.6 + thrust * 0.15, -0.72 - thrust * 0.38);
        poses.mulPose(Axis.XP.rotationDegrees(-55 * thrust));
        poses.mulPose(Axis.YP.rotationDegrees(side * -15 * thrust));
        return true;
    }
}
