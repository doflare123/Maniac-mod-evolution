package org.example.maniacrevolution.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.mana.ManaData;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncManaPacket;

public class ManaUtil {

    /**
     * Попытка потратить ману у игрока
     * @return true если мана была успешно потрачена, false если недостаточно маны
     */
    public static boolean consumeMana(Player player, float amount) {
        if (player.level().isClientSide()) {
            return false; // Только на сервере
        }

        boolean[] success = {false};
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            if (mana.consumeMana(amount)) {
                success[0] = true;
                // Синхронизируем с клиентом
                syncManaToClient((ServerPlayer) player, mana);
            }
        });

        return success[0];
    }

    /**
     * Добавить ману игроку
     */
    public static void addMana(Player player, float amount) {
        if (player.level().isClientSide()) return;

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.addMana(amount);
            syncManaToClient((ServerPlayer) player, mana);
        });
    }

    /**
     * Установить максимальную ману
     */
    public static void setMaxMana(Player player, float maxMana) {
        if (player.level().isClientSide()) return;

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.setMaxMana(maxMana);
            syncManaToClient((ServerPlayer) player, mana);
        });
    }

    /**
     * Установить базовый реген маны
     */
    public static void setBaseRegenRate(Player player, float regenRate) {
        if (player.level().isClientSide()) return;

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.setBaseRegenRate(regenRate);
            syncManaToClient((ServerPlayer) player, mana);
        });
    }

    /**
     * Установить бонусный реген маны (от эффектов)
     */
    public static void setBonusRegenRate(Player player, float bonusRegenRate) {
        if (player.level().isClientSide()) return;

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.setBonusRegenRate(bonusRegenRate);
            syncManaToClient((ServerPlayer) player, mana);
        });
    }

    /**
     * Включить/выключить пассивный реген маны
     */
    public static void setPassiveRegenEnabled(Player player, boolean enabled) {
        if (player.level().isClientSide()) return;

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.setPassiveRegenEnabled(enabled);
            syncManaToClient((ServerPlayer) player, mana);
        });
    }

    /**
     * Получить текущую ману
     */
    public static float getMana(Player player) {
        float[] result = {0};
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            result[0] = mana.getMana();
        });
        return result[0];
    }

    /**
     * Проверить, достаточно ли маны
     */
    public static boolean hasMana(Player player, float amount) {
        return getMana(player) >= amount;
    }

    /**
     * Синхронизация маны с клиентом
     */
    private static void syncManaToClient(ServerPlayer player, ManaData mana) {
        ModNetworking.sendToPlayer(
                new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                player
        );
    }

    /**
     * Принудительная синхронизация маны
     */
    public static void forceSync(ServerPlayer player) {
        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            syncManaToClient(player, mana);
        });
    }
}