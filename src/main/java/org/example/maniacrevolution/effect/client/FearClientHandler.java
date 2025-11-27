package org.example.maniacrevolution.effect.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.maniacrevolution.effect.ModEffects;

/**
 * Клиентская обработка эффекта страха.
 * Блокирует управление и поворачивает камеру.
 */
@OnlyIn(Dist.CLIENT)
public class FearClientHandler {

    private static Vec3 savedFleeDirection = null;
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    private static boolean wasUnderFear = false;
    private static int fearTicks = 0;

    /**
     * Устанавливает направление страха (вызывается из пакета).
     */
    public static void setFearDirection(Vec3 direction) {
        savedFleeDirection = direction;
        calculateTargetRotation(direction);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        // Проверяем наличие эффекта страха
        MobEffectInstance fearEffect = player.getEffect(ModEffects.FEAR.get());

        if (fearEffect != null) {
            handleFearEffect(player);
            wasUnderFear = true;
            fearTicks++;
        } else if (wasUnderFear) {
            // Эффект только что закончился - очищаем
            onFearEnd();
        }
    }

    /**
     * Обрабатывает эффект страха на клиенте.
     */
    private static void handleFearEffect(LocalPlayer player) {
        // Сохраняем направление бегства при первом тике
        if (savedFleeDirection == null) {
            Vec3 lookDirection = player.getLookAngle();
            savedFleeDirection = new Vec3(-lookDirection.x, 0, -lookDirection.z).normalize();

            // Вычисляем целевые углы камеры
            calculateTargetRotation(savedFleeDirection);
        }

        // Плавно поворачиваем камеру в направлении бегства
        smoothRotateCamera(player);

        // КРИТИЧНО: Блокируем движение игрока
        blockPlayerInput(player);
    }

    /**
     * Вычисляет целевые углы поворота камеры для направления бегства.
     */
    private static void calculateTargetRotation(Vec3 direction) {
        // Вычисляем yaw (горизонтальный угол)
        targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Pitch (вертикальный угол) держим прямо
        targetPitch = 0;
    }

    /**
     * Плавно поворачивает камеру к целевому направлению.
     */
    private static void smoothRotateCamera(LocalPlayer player) {
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Быстрая интерполяция в первые тики, затем удержание
        float interpolationSpeed = fearTicks < 10 ? 0.4f : 0.8f;

        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float newYaw = currentYaw + yawDiff * interpolationSpeed;
        float newPitch = currentPitch + pitchDiff * interpolationSpeed;

        // Применяем поворот
        player.setYRot(newYaw);
        player.setXRot(newPitch);
        player.yRotO = newYaw;
        player.xRotO = newPitch;

        // ВАЖНО: Также обновляем yHeadRot для корректного рендера головы
        player.setYHeadRot(newYaw);
        player.yHeadRotO = newYaw;
    }

    /**
     * КРИТИЧНО: Полностью блокирует управление игрока.
     */
    private static void blockPlayerInput(LocalPlayer player) {
        if (player.input == null) return;

        // Полностью обнуляем все входы движения
        player.input.leftImpulse = 0;
        player.input.forwardImpulse = 0;
        player.input.up = false;
        player.input.down = false;
        player.input.left = false;
        player.input.right = false;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;

        // Принудительно останавливаем спринт
        player.setSprinting(false);
    }

    /**
     * Вызывается при окончании эффекта страха.
     */
    private static void onFearEnd() {
        savedFleeDirection = null;
        wasUnderFear = false;
        fearTicks = 0;
    }

    /**
     * Нормализует угол в диапазон [-180, 180].
     */
    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Рендерит затемнение экрана при страхе.
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        MobEffectInstance fearEffect = player.getEffect(ModEffects.FEAR.get());
        if (fearEffect == null) return;

        // Рендерим виньетку страха
        renderFearVignette(event.getGuiGraphics(), mc.getWindow());
    }

    /**
     * Рендерит тёмную виньетку по краям экрана.
     */
    private static void renderFearVignette(GuiGraphics graphics, Window window) {
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Тёмно-зелёная виньетка (цвет страха)
        int color = 0x40001a00; // ARGB: полупрозрачный тёмно-зелёный

        // Верхняя полоса
        graphics.fill(0, 0, screenWidth, 60, color);

        // Нижняя полоса
        graphics.fill(0, screenHeight - 60, screenWidth, screenHeight, color);

        // Левая полоса
        graphics.fill(0, 0, 60, screenHeight, color);

        // Правая полоса
        graphics.fill(screenWidth - 60, 0, screenWidth, screenHeight, color);

        RenderSystem.disableBlend();
    }
}