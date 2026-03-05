package org.example.maniacrevolution.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.entity.TotemEntity;
import org.joml.Vector3f;

/**
 * Модель тотема шамана.
 * Содержит анимации idle (loop) и hello (one-shot).
 * Анимации переключаются через TotemEntity.getAnimState().
 */
public class TotemModel extends HierarchicalModel<TotemEntity> {

    private final ModelPart root;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("maniacrev", "shaman_totem"), "main");

    private final ModelPart legs;
    private final ModelPart right2;
    private final ModelPart left2;
    private final ModelPart body;
    private final ModelPart hands;
    private final ModelPart left;
    private final ModelPart right;
    private final ModelPart head;

    /** Текущее время анимации в тиках (накапливается в setupAnim) */
    private float animTime = 0f;

    public TotemModel(ModelPart root) {
        this.root = root;
        this.legs  = root.getChild("legs");
        this.right2 = this.legs.getChild("right2");
        this.left2  = this.legs.getChild("left2");
        this.body  = root.getChild("body");
        this.hands = root.getChild("hands");
        this.left  = this.hands.getChild("left");
        this.right = this.hands.getChild("right");
        this.head  = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition legs = partdefinition.addOrReplaceChild("legs",
                CubeListBuilder.create(), PartPose.offset(1.0F, 24.0F, 0.0F));

        legs.addOrReplaceChild("right2",
                CubeListBuilder.create().texOffs(0, 59)
                        .addBox(-5.0F, -11.0F, -1.0F, 6.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 0.0F, 0.0F));

        legs.addOrReplaceChild("left2",
                CubeListBuilder.create().texOffs(24, 59)
                        .addBox(-3.0F, -11.0F, -1.0F, 6.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-7.0F, -32.0F, -3.0F, 14.0F, 17.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 28.0F, 0.0F));

        PartDefinition hands = partdefinition.addOrReplaceChild("hands",
                CubeListBuilder.create(), PartPose.offset(-3.0F, 23.0F, 0.0F));

        PartDefinition left = hands.addOrReplaceChild("left",
                CubeListBuilder.create(), PartPose.offset(16.0F, -20.0F, 1.0F));
        left.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(48, 32)
                        .addBox(-5.0F, -9.0F, -2.0F, 6.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition right = hands.addOrReplaceChild("right",
                CubeListBuilder.create(), PartPose.offset(-10.0F, -20.0F, 1.0F));
        right.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-1.0F, -9.0F, -2.0F, 6.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -42.0F, -6.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    // ── Анимации ──────────────────────────────────────────────────────────────

    @Override
    public void setupAnim(TotemEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        resetParts();

        int state = entity.getAnimState();

        if (state == TotemEntity.ANIM_HELLO) {
            // Время hello считается от момента старта анимации, а не от спавна.
            // entity.getHelloStartTick() — tickCount сущности в момент старта.
            // ageInTicks ≈ entity.tickCount на клиенте (с интерполяцией партиалТик).
            int helloStart = entity.getHelloStartTick();
            float elapsed = ageInTicks - helloStart; // тиков прошло с начала hello
            long ms = (long)(elapsed * 1000L / 20L);
            if (ms < 0) ms = 0;
            KeyframeAnimations.animate(this, TotemAnimations.Hello,
                    ms, 1.0f, new Vector3f(3));
        } else {
            // Idle — зациклена, абсолютное время подходит
            KeyframeAnimations.animate(this, TotemAnimations.idle,
                    (long)(ageInTicks * 1000L / 20L), 1.0f, new Vector3f(3));
        }
    }

    private void resetParts() {
        legs.resetPose();
        right2.resetPose();
        left2.resetPose();
        body.resetPose();
        hands.resetPose();
        left.resetPose();
        right.resetPose();
        head.resetPose();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        legs.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        hands.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── Геттеры для AnimationDefinition ──────────────────────────────────────

    // HierarchicalModel.root() — даёт доступ ко всем костям по имени
    // KeyframeAnimations.animate() использует этот метод для поиска костей
    @Override
    public net.minecraft.client.model.geom.ModelPart root() {
        return root;
    }
}