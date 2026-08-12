package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.example.maniacrevolution.entity.PinkOrchidIllusionEntity;
import org.example.maniacrevolution.perk.perks.survivor.PinkOrchidPerk;

import javax.annotation.Nullable;
import java.util.UUID;

/** Рисует иллюзию обычной моделью игрока с его скином, бронёй и предметами. */
public final class PinkOrchidIllusionRenderer extends LivingEntityRenderer<
        PinkOrchidIllusionEntity, PlayerModel<PinkOrchidIllusionEntity>> {
    private static final float SHADOW_RADIUS = 0.5F;
    private static final float MINIMUM_SCALE = 0.04F;
    private static final float FULL_SCALE = 0.9375F;

    private final PlayerModel<PinkOrchidIllusionEntity> normalModel;
    private final PlayerModel<PinkOrchidIllusionEntity> slimModel;

    public PinkOrchidIllusionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                SHADOW_RADIUS);
        normalModel = model;
        slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(PinkOrchidIllusionEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffer,
                       int packedLight) {
        model = isSlim(entity.getOwnerUUID()) ? slimModel : normalModel;
        configureArmPoses(entity);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void configureArmPoses(PinkOrchidIllusionEntity entity) {
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        if (!entity.isVisuallyUsingItem()) return;

        InteractionHand usedHand = entity.getVisualUsedHand();
        ItemStack stack = entity.getItemInHand(usedHand);
        HumanoidModel.ArmPose pose = resolveArmPose(stack);
        boolean right = usedHand == InteractionHand.MAIN_HAND
                ? entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT
                : entity.getMainArm() != net.minecraft.world.entity.HumanoidArm.RIGHT;
        if (right) model.rightArmPose = pose;
        else model.leftArmPose = pose;
    }

    private static HumanoidModel.ArmPose resolveArmPose(ItemStack stack) {
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        UseAnim animation = stack.getUseAnimation();
        return switch (animation) {
            case BLOCK -> HumanoidModel.ArmPose.BLOCK;
            case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
            case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
            case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
            case BRUSH -> HumanoidModel.ArmPose.BRUSH;
            default -> HumanoidModel.ArmPose.ITEM;
        };
    }

    @Override
    protected void scale(PinkOrchidIllusionEntity entity, PoseStack poseStack,
                         float partialTick) {
        float progress = 1.0F - Mth.clamp(
                (entity.getMaterializeTicks() - partialTick)
                        / PinkOrchidPerk.MATERIALIZE_TICKS,
                0.0F,
                1.0F
        );
        float pop = Math.max(MINIMUM_SCALE, easeOutBack(progress));
        float scale = FULL_SCALE * pop;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(PinkOrchidIllusionEntity entity) {
        UUID ownerId = entity.getOwnerUUID();
        PlayerInfo info = getPlayerInfo(ownerId);
        return info != null ? info.getSkinLocation()
                : DefaultPlayerSkin.getDefaultSkin(ownerId == null ? entity.getUUID() : ownerId);
    }

    @Override
    protected boolean shouldShowName(PinkOrchidIllusionEntity entity) {
        return false;
    }

    private static boolean isSlim(@Nullable UUID ownerId) {
        PlayerInfo info = getPlayerInfo(ownerId);
        return info != null && "slim".equals(info.getModelName());
    }

    @Nullable
    private static PlayerInfo getPlayerInfo(@Nullable UUID ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        return ownerId == null || minecraft.getConnection() == null
                ? null : minecraft.getConnection().getPlayerInfo(ownerId);
    }

    private static float easeOutBack(float value) {
        float shifted = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        return 1.0F + 2.70158F * shifted * shifted * shifted
                + 1.70158F * shifted * shifted;
    }
}
