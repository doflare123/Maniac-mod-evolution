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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.ClientAbilityData;
import org.example.maniacrevolution.item.armor.IActivatableArmor;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.DeathTeleportWarningPacket;
import org.example.maniacrevolution.util.ManaUtil;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Коса Смерти (с интеграцией BetterCombat)
 * - Двуручное оружие (через BetterCombat attributes)
 * - Телепортация к случайному выжившему (ПКМ, 30с кд)
 * - Урон: 4.0 (2 сердца) - настраивается в BetterCombat
 * - Эффект "Гонка со смертью" применяется через DeathEventHandler
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DeathScytheItem extends SwordItem implements IItemWithAbility {

    private static final float MANA_COST = 7.0f;
    // Кулдауны телепортации для каждого игрока
    private static final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();
    /** 4.25 s death_embrace + 1.4 s release from bone_arms.animation.json. */
    public static final int TELEPORT_WINDUP_TICKS = 113;
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
        if (pendingTeleports.containsKey(death.getUUID())) return false;

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
        death.level().playSound(null, death.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 0.65F, 0.55F);

        // Предупреждаем только выбранного выжившего и откладываем телепорт до конца анимации.
        ModNetworking.sendToPlayer(new DeathTeleportWarningPacket(TELEPORT_WINDUP_TICKS), target);
        pendingTeleports.put(death.getUUID(), new PendingTeleport(
                death.getUUID(), target.getUUID(),
                death.getServer().overworld().getGameTime() + TELEPORT_WINDUP_TICKS));

        // Записываем время использования
        teleportCooldowns.put(death.getUUID(), currentTime);

        // Звуковые эффекты
        death.level().playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        ManaUtil.consumeMana(death, MANA_COST);

        death.displayClientMessage(
                Component.literal("§5Вы готовитесь явиться к " + target.getName().getString()),
                true
        );

        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingTeleports.isEmpty()) return;

        long gameTime = event.getServer().overworld().getGameTime();
        Iterator<PendingTeleport> iterator = pendingTeleports.values().iterator();
        while (iterator.hasNext()) {
            PendingTeleport pending = iterator.next();
            if (gameTime < pending.executeAtGameTime()) continue;
            iterator.remove();

            ServerPlayer death = event.getServer().getPlayerList().getPlayer(pending.deathId());
            ServerPlayer target = event.getServer().getPlayerList().getPlayer(pending.targetId());
            if (death == null || target == null || !death.isAlive() || !isValidTeleportTarget(target)) continue;
            teleportBehindTarget(death, target);
            continue;
        }

        // The cast is a committed, visible wind-up: Death stands still while a
        // tightening ring of souls and smoke marks that a teleport is charging.
        for (PendingTeleport pending : pendingTeleports.values()) {
            ServerPlayer death = event.getServer().getPlayerList().getPlayer(pending.deathId());
            if (death == null || !death.isAlive()) continue;
            death.setDeltaMovement(Vec3.ZERO);
            death.setSprinting(false);
            if (gameTime % 3L == 0L && death.level() instanceof ServerLevel level) {
                double remaining = Math.max(0.0D, pending.executeAtGameTime() - gameTime);
                double progress = 1.0D - remaining / TELEPORT_WINDUP_TICKS;
                double radius = 1.65D - progress * 0.85D;
                double angle = gameTime * 0.42D;
                for (int i = 0; i < 6; i++) {
                    double a = angle + i * Math.PI / 3.0D;
                    double x = death.getX() + Math.cos(a) * radius;
                    double z = death.getZ() + Math.sin(a) * radius;
                    double y = death.getY() + 0.15D + progress * 1.8D + (i % 2) * 0.25D;
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                            1, 0.02D, 0.04D, 0.02D, 0.0D);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, x, death.getY() + 0.1D, z,
                            1, 0.04D, 0.02D, 0.04D, 0.01D);
                }
            }
        }
    }

    private static boolean isValidTeleportTarget(ServerPlayer target) {
        if (!target.isAlive() || target.isSpectator() || target.isCreative()) return false;
        Team team = target.getTeam();
        if (team == null || !"survivors".equalsIgnoreCase(team.getName())) return false;
        DownedData downedData = DownedCapability.get(target);
        return downedData == null || downedData.getState() != DownedState.DOWNED;
    }

    private static void teleportBehindTarget(ServerPlayer death, ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        Vec3 facing = target.getLookAngle();
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-4D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();

        Vec3 destination = target.position().subtract(horizontal.scale(1.75D));
        double[] distances = {1.75D, 1.25D, 0.75D};
        double[] verticalOffsets = {0.0D, 1.0D, -1.0D};
        outer:
        for (double distance : distances) {
            for (double yOffset : verticalOffsets) {
                Vec3 candidate = target.position().subtract(horizontal.scale(distance)).add(0.0D, yOffset, 0.0D);
                if (level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(candidate))
                        && level.noCollision(death, death.getBoundingBox().move(candidate.subtract(death.position())))) {
                    destination = candidate;
                    break outer;
                }
            }
        }

        death.teleportTo(level, destination.x, destination.y, destination.z, target.getYRot(), 0.0F);
        death.setDeltaMovement(Vec3.ZERO);
        playArrivalAnimation(death, destination);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * Анимация исчезновения (частицы пустоты)
     */
    private static void playDepartureAnimation(ServerPlayer player, Vec3 pos) {
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
    private static void playArrivalAnimation(ServerPlayer player, Vec3 pos) {
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
                // НЕ телепортируемся к лежачим
                DownedData downedData = DownedCapability.get(player);
                if (downedData != null && downedData.getState() == DownedState.DOWNED) continue;

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
        pendingTeleports.remove(playerId);
        pendingTeleports.values().removeIf(pending -> pending.targetId().equals(playerId));
    }

    private record PendingTeleport(UUID deathId, UUID targetId, long executeAtGameTime) {}

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
        return Component.translatable("ability.maniacrev.death_scythe.name").getString();
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
        tooltip.add(Component.translatable("tooltip.maniacrev.ability", getAbilityName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("ability.maniacrev.death_scythe.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.maniacrev.mana_cost", (int) MANA_COST).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.maniacrev.cooldown", getMaxCooldownSeconds()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
