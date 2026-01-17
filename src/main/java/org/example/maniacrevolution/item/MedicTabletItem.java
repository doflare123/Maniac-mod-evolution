package org.example.maniacrevolution.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenMedicTabletPacket;
import org.example.maniacrevolution.util.MedicTabletTracker;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MedicTabletItem extends Item {

    // Хранилище кулдаунов для каждого игрока
    private static final Map<UUID, Long> trackingCooldowns = new HashMap<>();
    private static final long COOLDOWN_DURATION = 30000; // 30 секунд в миллисекундах

    public MedicTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Проверяем, смотрит ли игрок на союзника для отображения HP
            Player targetPlayer = getTargetSurvivor(serverPlayer);

            if (targetPlayer != null) {
                // Показываем HP союзника
                showPlayerHealth(serverPlayer, targetPlayer);
            } else {
                // Открываем GUI планшета
                ModNetworking.sendToPlayer(new OpenMedicTabletPacket(), serverPlayer);
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * Находит союзника, на которого смотрит медик
     */
    private Player getTargetSurvivor(ServerPlayer medic) {
        Vec3 lookVec = medic.getViewVector(1.0F);
        Vec3 startPos = medic.getEyePosition();
        Vec3 endPos = startPos.add(lookVec.scale(20.0)); // Дальность 20 блоков

        AABB searchBox = new AABB(startPos, endPos).inflate(2.0);
        List<Player> nearbyPlayers = medic.level().getEntitiesOfClass(Player.class, searchBox,
                p -> p != medic && isValidSurvivor(p));

        // Находим ближайшего игрока, на которого смотрит медик
        Player closest = null;
        double closestDot = 0.95; // Минимальный угол обзора

        for (Player nearby : nearbyPlayers) {
            Vec3 toTarget = nearby.position().add(0, nearby.getEyeHeight() / 2, 0).subtract(startPos).normalize();
            double dot = lookVec.dot(toTarget);
            if (dot > closestDot) {
                closestDot = dot;
                closest = nearby;
            }
        }

        return closest;
    }

    /**
     * Проверяет, является ли игрок валидным союзником (в команде survivors, не spectator/creative)
     */
    private boolean isValidSurvivor(Player player) {
        if (player.isSpectator() || player.isCreative()) return false;

        Team team = player.getTeam();
        return team != null && "survivors".equalsIgnoreCase(team.getName());
    }

    /**
     * Отображает здоровье союзника медику
     */
    private void showPlayerHealth(ServerPlayer medic, Player target) {
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        int hearts = (int) Math.ceil(health / 2.0);
        int maxHearts = (int) Math.ceil(maxHealth / 2.0);

        medic.displayClientMessage(
                Component.literal(String.format("§e%s: §c❤ %d/%d",
                        target.getName().getString(), hearts, maxHearts)),
                true
        );
    }

    /**
     * Начинает отслеживание союзника (вызывается из GUI)
     */
    public static boolean startTracking(ServerPlayer medic, ServerPlayer target) {
        // Проверяем кулдаун
        Long lastUse = trackingCooldowns.get(medic.getUUID());
        long currentTime = System.currentTimeMillis();

        if (lastUse != null && currentTime - lastUse < COOLDOWN_DURATION) {
            long remainingSeconds = (COOLDOWN_DURATION - (currentTime - lastUse)) / 1000;
            medic.displayClientMessage(
                    Component.literal(String.format("§cКулдаун: %d секунд", remainingSeconds)),
                    true
            );
            return false;
        }

        // Проверяем здоровье цели (должно быть < 50%)
        float healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent >= 0.5F) {
            medic.displayClientMessage(
                    Component.literal("§cИгрок должен иметь меньше 50% здоровья"),
                    true
            );
            return false;
        }

        // Начинаем отслеживание
        MedicTabletTracker.startTracking(medic, target);

        // Применяем подсветку на 10 секунд (200 тиков)
        SelectiveGlowingEffect.addGlowing(target, medic, 200);

        // Записываем время использования
        trackingCooldowns.put(medic.getUUID(), currentTime);

        medic.level().playSound(null, medic.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS, 1.0F, 1.5F);
        medic.displayClientMessage(
                Component.literal("§aОтслеживание " + target.getName().getString() + " начато"),
                false
        );

        return true;
    }

    /**
     * Получает оставшееся время кулдаунa в секундах
     */
    public static int getCooldownSeconds(UUID playerId) {
        Long lastUse = trackingCooldowns.get(playerId);
        if (lastUse == null) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUse;

        if (elapsed >= COOLDOWN_DURATION) {
            return 0;
        }

        return (int) ((COOLDOWN_DURATION - elapsed) / 1000);
    }

    /**
     * Сбрасывает кулдауны при выходе игрока
     */
    public static void onPlayerLogout(UUID playerId) {
        trackingCooldowns.remove(playerId);
    }
}