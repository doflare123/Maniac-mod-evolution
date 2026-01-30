package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.util.ManaUtil;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Коса Смерти (с интеграцией BetterCombat)
 * - Двуручное оружие (через BetterCombat attributes)
 * - Телепортация к случайному выжившему (ПКМ, 30с кд)
 * - Урон: 4.0 (2 сердца) - настраивается в BetterCombat
 * - Эффект "Гонка со смертью" применяется через DeathEventHandler
 */
public class DeathScytheItem extends SwordItem implements IItemWithAbility {

    private static final float MANA_COST = 7.0f;
    // Кулдауны телепортации для каждого игрока
    private static final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final long TELEPORT_COOLDOWN = 60000; // 30 секунд

    // Базовый урон (BetterCombat переопределит через JSON)
    private static final int SCYTHE_DAMAGE = -2;
    private static final float SCYTHE_SPEED = -2.4F;

    public DeathScytheItem(Properties properties) {
        super(Tiers.NETHERITE, SCYTHE_DAMAGE-1, SCYTHE_SPEED, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Проверяем, что коса в главной руке
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Телепортация к случайному выжившему
            if (tryTeleport(serverPlayer)) {
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Пытается телепортировать игрока к случайному выжившему
     */
    private boolean tryTeleport(ServerPlayer death) {
        // Проверяем кулдаун
        Long lastUse = teleportCooldowns.get(death.getUUID());
        long currentTime = System.currentTimeMillis();

        if (lastUse != null && currentTime - lastUse < TELEPORT_COOLDOWN) {
            long remainingSeconds = (TELEPORT_COOLDOWN - (currentTime - lastUse)) / 1000;
            death.displayClientMessage(
                    Component.literal(String.format("§cКулдаун телепортации: %d секунд", remainingSeconds)),
                    true
            );
            return false;
        }

        // Получаем случайного выжившего
        ServerPlayer target = getRandomSurvivor(death);
        if (target == null) {
            death.displayClientMessage(
                    Component.literal("§cНет доступных целей для телепортации"),
                    true
            );
            return false;
        }

        // Сохраняем старую позицию для анимации
        Vec3 startPos = death.position();

        // Анимация исчезновения
        playDepartureAnimation(death, startPos);

        // Телепортируем
        Vec3 targetPos = target.position();
        death.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        // Анимация появления
        playArrivalAnimation(death, targetPos);

        // Записываем время использования
        teleportCooldowns.put(death.getUUID(), currentTime);

        // Звуковые эффекты
        death.level().playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        death.level().playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

        ManaUtil.consumeMana(death, MANA_COST);

        death.displayClientMessage(
                Component.literal("§5Вы телепортировались к " + target.getName().getString()),
                true
        );

        return true;
    }

    /**
     * Анимация исчезновения (частицы пустоты)
     */
    private void playDepartureAnimation(ServerPlayer player, Vec3 pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Спираль из темных частиц, уходящая вниз
        for (int i = 0; i < 50; i++) {
            double angle = i * Math.PI / 8;
            double radius = 1.5 - (i * 0.03);
            double height = i * 0.1;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Темные частицы пустоты
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + 1.0 + height, pos.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.02);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX, pos.y + 1.0 + height, pos.z + offsetZ,
                    2, 0.05, 0.05, 0.05, 0.01);
        }

        // Взрыв темных частиц
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 1.0, pos.z,
                1, 0, 0, 0, 0);

        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1.0, pos.z,
                30, 0.5, 1.0, 0.5, 0.1);
    }

    /**
     * Анимация появления (частицы из пустоты)
     */
    private void playArrivalAnimation(ServerPlayer player, Vec3 pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Спираль из темных частиц, поднимающаяся вверх
        for (int i = 0; i < 50; i++) {
            double angle = i * Math.PI / 8;
            double radius = (i * 0.03);
            double height = 3.0 - (i * 0.06);

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Темные частицы, появляющиеся снизу
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.02);

            serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        // Взрыв при появлении
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 1.0, pos.z,
                1, 0, 0, 0, 0);

        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1.0, pos.z,
                30, 0.5, 1.0, 0.5, 0.1);

        // Дополнительные эффекты - души вокруг
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;

            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                    5, 0.2, 0.5, 0.2, 0.05);
        }
    }

    /**
     * Получает случайного выжившего
     */
    private ServerPlayer getRandomSurvivor(ServerPlayer death) {
        List<ServerPlayer> survivors = new ArrayList<>();

        for (ServerPlayer player : death.getServer().getPlayerList().getPlayers()) {
            if (player == death) continue;
            if (player.isSpectator() || player.isCreative()) continue;

            Team team = player.getTeam();
            if (team != null && "survivors".equalsIgnoreCase(team.getName())) {
                survivors.add(player);
            }
        }

        if (survivors.isEmpty()) return null;

        Random random = new Random();
        return survivors.get(random.nextInt(survivors.size()));
    }

    public static int getCooldownSeconds(UUID playerId) {
        Long lastUse = teleportCooldowns.get(playerId);
        if (lastUse == null) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUse;

        if (elapsed >= TELEPORT_COOLDOWN) {
            return 0;
        }

        return (int) ((TELEPORT_COOLDOWN - elapsed) / 1000);
    }

    /**
     * Очистка кулдаунов при выходе игрока
     */
    public static void onPlayerLogout(UUID playerId) {
        teleportCooldowns.remove(playerId);
    }

    @Override
    public ResourceLocation getAbilityIcon() {
        return new ResourceLocation(Maniacrev.MODID, "textures/gui/abilities/death_scythe.png");
    }

    @Override
    public float getManaCost() {
        return MANA_COST;
    }

    @Override
    public String getAbilityName() {
        return "Телепортация к жертве";
    }

    @Override
    public String getAbilityDescription() {
        return "";
    }

    @Override
    public int getCooldownSeconds(Player player) {
        if (player.level().isClientSide) {
            return ClientAbilityData.getCooldownSeconds(this);
        }
        return DeathScytheItem.getCooldownSeconds(player.getUUID());
    }

    @Override
    public int getMaxCooldownSeconds() {
        return 30; // 30 секунд
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6Способность: §5" + getAbilityName() + " (Правый клик)").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7ПКМ: Телепортация к случайному выжившему").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§9Стоимость: §b" + (int)MANA_COST + " маны").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§9Кулдаун: §b" + getMaxCooldownSeconds() + "с").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}