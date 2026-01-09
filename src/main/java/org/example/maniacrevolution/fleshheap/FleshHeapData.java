package org.example.maniacrevolution.fleshheap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FleshHeapData {
    private static final String NBT_KEY = "FleshHeapStacks";
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "flesh_heap_bonus";

    private static final double HEALTH_PER_STACK = 2.0;

    /**
     * Получить количество стаков Flesh Heap
     */
    public static int getStacks(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getInt(NBT_KEY);
    }

    /**
     * Установить количество стаков
     */
    public static void setStacks(ServerPlayer player, int stacks) {
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_KEY, Math.max(0, stacks));
        updateHealthModifier(player, stacks);
    }

    /**
     * Добавить один стак
     */
    public static void addStack(ServerPlayer player) {
        addStacks(player, 1);
    }

    /**
     * Добавить несколько стаков
     */
    public static void addStacks(ServerPlayer player, int amount) {
        int current = getStacks(player);
        setStacks(player, current + amount);
    }

    /**
     * Очистить все стаки
     */
    public static void clearStacks(ServerPlayer player) {
        setStacks(player, 0);
    }

    /**
     * Обновить модификатор здоровья
     */
    private static void updateHealthModifier(ServerPlayer player, int stacks) {
        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return;

        // Удаляем старый модификатор
        AttributeModifier oldModifier = healthAttr.getModifier(HEALTH_MODIFIER_UUID);
        if (oldModifier != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
        }

        // Добавляем новый, если стаки > 0
        if (stacks > 0) {
            double bonusHealth = stacks * HEALTH_PER_STACK;
            AttributeModifier modifier = new AttributeModifier(
                    HEALTH_MODIFIER_UUID,
                    MODIFIER_NAME,
                    bonusHealth,
                    AttributeModifier.Operation.ADDITION
            );
            healthAttr.addPermanentModifier(modifier);
        }

        // Обновляем здоровье
        player.setHealth(player.getHealth());

        // Отправляем пакет клиенту
        org.example.maniacrevolution.network.ModNetworking.sendToPlayer(
                new org.example.maniacrevolution.network.FleshHeapSyncPacket(stacks),
                player
        );
    }

    /**
     * Восстановить модификаторы после смерти/релога
     */
    public static void restoreModifiers(ServerPlayer player) {
        int stacks = getStacks(player);
        if (stacks > 0) {
            updateHealthModifier(player, stacks);
        }
    }
}