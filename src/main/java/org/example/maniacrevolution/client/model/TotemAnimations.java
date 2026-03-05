package org.example.maniacrevolution.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Анимации тотема шамана.
 * Извлечены из Blockbench-экспорта (model.java).
 *
 * Hello  — one-shot (6.375 сек), проигрывается при первой встрече с игроком
 * idle   — looping (3 сек), постоянная фоновая анимация
 */
public class TotemAnimations {

    public static final AnimationDefinition Hello = AnimationDefinition.Builder.withLength(6.375F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.0417F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5417F, KeyframeAnimations.posVec(0F, 4F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.75F, KeyframeAnimations.posVec(0F, -3F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.posVec(0F, -6F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.posVec(0F, -6F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(0F, -3.57F, -1.29F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.25F, KeyframeAnimations.degreeVec(-30F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.degreeVec(-30.64501F, 12.98388F, -7.56165F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5417F, KeyframeAnimations.degreeVec(-30.16034F, -8.66597F, 5.02428F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.625F, KeyframeAnimations.degreeVec(-30.64501F, 12.98388F, -7.56165F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.6667F, KeyframeAnimations.degreeVec(-30.16034F, -8.66597F, 5.02428F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.degreeVec(-30.64501F, 12.98388F, -7.56165F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.7917F, KeyframeAnimations.degreeVec(-30.16034F, -8.66597F, 5.02428F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.875F, KeyframeAnimations.degreeVec(-30.64501F, 12.98388F, -7.56165F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.9167F, KeyframeAnimations.degreeVec(-30.16034F, -8.66597F, 5.02428F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3F, KeyframeAnimations.degreeVec(-30.64501F, 12.98388F, -7.56165F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0417F, KeyframeAnimations.degreeVec(-30.16034F, -8.66597F, 5.02428F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.2083F, KeyframeAnimations.degreeVec(-29.78096F, 0.00237F, 0.01917F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.degreeVec(-30.64437F, -12.97918F, 7.59984F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.0417F, KeyframeAnimations.degreeVec(-30.37733F, 10.82996F, -6.26455F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.degreeVec(-22.50262F, 3.90892F, -3.34914F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.degreeVec(-22.50262F, 3.90892F, -3.34914F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.25F, KeyframeAnimations.posVec(0F, 9F, -12F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5417F, KeyframeAnimations.posVec(0F, 9F, -12F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.posVec(0F, 3F, -12F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.posVec(0F, -2F, -12F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.posVec(0F, -2F, -12F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(0F, -3F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.degreeVec(0F, 0F, -12.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.degreeVec(0F, 0F, -12.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.75F, KeyframeAnimations.degreeVec(-7.61435F, 9.91358F, -13.81845F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.8333F, KeyframeAnimations.degreeVec(-0.11435F, 9.91358F, -13.81845F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9583F, KeyframeAnimations.degreeVec(10.12384F, 38.81787F, 2.23543F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.degreeVec(-7.61435F, 9.91358F, -13.81845F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.degreeVec(-0.11435F, 9.91358F, -13.81845F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.degreeVec(10.12384F, 38.81787F, 2.23543F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5F, KeyframeAnimations.degreeVec(8.21612F, 16.59522F, -1.78949F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.6667F, KeyframeAnimations.degreeVec(53.21612F, 16.59522F, -1.78949F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.degreeVec(65.71612F, 16.59522F, -1.78949F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.degreeVec(85.58326F, 1.61761F, -2.62181F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.degreeVec(85.58326F, 1.61761F, -2.62181F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.degreeVec(70.58326F, 1.61761F, -2.62181F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left2", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(3F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.posVec(3F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.75F, KeyframeAnimations.posVec(2F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9583F, KeyframeAnimations.posVec(-1F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.posVec(2F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.posVec(-1F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.posVec(1F, 0F, 7F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.posVec(0F, 0F, 8F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.posVec(0F, 0F, 8F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(0F, 0F, 8F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 0F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.degreeVec(0F, 20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1F, KeyframeAnimations.degreeVec(0F, -20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.25F, KeyframeAnimations.degreeVec(0F, 20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.degreeVec(0F, -20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.degreeVec(0F, 20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2F, KeyframeAnimations.degreeVec(0F, -20F, 65F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.degreeVec(2.65345F, 0.00469F, 93.48042F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.75F, KeyframeAnimations.degreeVec(2.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.875F, KeyframeAnimations.degreeVec(17.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4F, KeyframeAnimations.degreeVec(16.58261F, -3.57772F, 103.41081F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.degreeVec(2.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.degreeVec(17.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.degreeVec(16.58261F, -3.57772F, 103.41081F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5F, KeyframeAnimations.degreeVec(2.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.6667F, KeyframeAnimations.degreeVec(17.78209F, 17.48532F, 94.31672F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.degreeVec(16.58261F, -3.57772F, 103.41081F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.degreeVec(-1.56532F, 16.88327F, -3.79997F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.degreeVec(-1.56532F, 16.88327F, -3.79997F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.degreeVec(-1.56532F, 16.88327F, -3.79997F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(-2F, 10.25F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.posVec(0F, 14.5F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.posVec(0F, 14.5F, 3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1F, KeyframeAnimations.posVec(0F, 12.5F, -2F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.25F, KeyframeAnimations.posVec(0F, 14.5F, 3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.posVec(0F, 12.5F, -2F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.posVec(0F, 14.5F, 3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2F, KeyframeAnimations.posVec(0F, 12.5F, -2F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.posVec(0F, 16.5F, 0.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.75F, KeyframeAnimations.posVec(0F, 16.5F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.875F, KeyframeAnimations.posVec(0F, 14.5F, 3.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.posVec(0F, 16.5F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.posVec(0F, 14.5F, 3.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.posVec(0F, 8.5F, 3.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.posVec(0F, -5.5F, -1.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.posVec(0F, -5.5F, -1.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(0F, -5.5F, -1.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.degreeVec(0F, 0F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.6667F, KeyframeAnimations.degreeVec(0F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.7917F, KeyframeAnimations.degreeVec(15F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9167F, KeyframeAnimations.degreeVec(17.99788F, -39.29559F, -95.77187F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.degreeVec(0F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.degreeVec(15F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.degreeVec(17.99788F, -39.29559F, -95.77187F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5833F, KeyframeAnimations.degreeVec(0F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7083F, KeyframeAnimations.degreeVec(15F, -22.5F, -90F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.8333F, KeyframeAnimations.degreeVec(17.99788F, -39.29559F, -95.77187F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.posVec(0F, 14F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.6667F, KeyframeAnimations.posVec(0F, 14F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9167F, KeyframeAnimations.posVec(0F, 14F, 6.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.posVec(0F, 14F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.posVec(0F, 14F, 6.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.posVec(0F, 5F, 6.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.posVec(0F, -6F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.posVec(0F, -6F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(0F, -6F, -3F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.degreeVec(1.91757F, 4.61854F, 22.57734F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.6667F, KeyframeAnimations.degreeVec(12.06309F, 22.10816F, 23.19962F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.7917F, KeyframeAnimations.degreeVec(12.62829F, 18.16223F, 16.95365F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9167F, KeyframeAnimations.degreeVec(0.12829F, 18.16223F, 16.95365F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.degreeVec(12.06309F, 22.10816F, 23.19962F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.degreeVec(12.62829F, 18.16223F, 16.95365F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.degreeVec(0.12829F, 18.16223F, 16.95365F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.5F, KeyframeAnimations.degreeVec(-2.98647F, 2.92029F, 7.25133F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.6667F, KeyframeAnimations.degreeVec(19.51353F, 2.92029F, 7.25133F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.degreeVec(64.51353F, 2.92029F, 7.25133F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.9583F, KeyframeAnimations.degreeVec(86.59147F, 1.92501F, -2.70806F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(5.5F, KeyframeAnimations.degreeVec(86.59147F, 1.92501F, -2.70806F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.degreeVec(69.09147F, 1.92501F, -2.70806F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right2", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.25F, KeyframeAnimations.posVec(0F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.5F, KeyframeAnimations.posVec(-4F, 6F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.75F, KeyframeAnimations.posVec(-4F, 6F, 1F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.9167F, KeyframeAnimations.posVec(-4F, 6F, -1F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.1667F, KeyframeAnimations.posVec(-4F, 6F, 1F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.25F, KeyframeAnimations.posVec(-4F, 6F, 2F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.375F, KeyframeAnimations.posVec(-4F, 6F, -1F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(4.7917F, KeyframeAnimations.posVec(-1F, 0F, 7F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6F, KeyframeAnimations.posVec(-1F, 0F, 7F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(6.375F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();

    public static final AnimationDefinition idle = AnimationDefinition.Builder.withLength(3.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -7.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -7.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -7.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, -2.25F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, -2.25F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.0F, -2.25F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new net.minecraft.client.animation.Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 22.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new net.minecraft.client.animation.Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(1.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(2.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new net.minecraft.client.animation.Keyframe(3.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();
}