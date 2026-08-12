package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.hack.ComputerBlockEntity;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

/**
 * Высокое напряжение — ускоряет маньяка рядом с сильно заряженным компьютером.
 */
public class HighVoltagePerk extends Perk {
    public static final String ID = "high_voltage";

    public static final double HORIZONTAL_RADIUS_BLOCKS = 8.0D;
    public static final int VERTICAL_RANGE_BLOCKS = 2;
    public static final double MIN_PROGRESS_FRACTION = 0.80D;
    public static final int SPEED_BONUS_PERCENT = 20;

    private static final double PERCENT_SCALE = 100.0D;
    private static final int DISPLAYED_LEVEL_OFFSET = 1;
    private static final int SPEED_EFFECT_AMPLIFIER = SPEED_BONUS_PERCENT - DISPLAYED_LEVEL_OFFSET;
    private static final int SPEED_EFFECT_REFRESH_DURATION_TICKS = 5;
    private static final double HORIZONTAL_RADIUS_SQUARED =
            HORIZONTAL_RADIUS_BLOCKS * HORIZONTAL_RADIUS_BLOCKS;
    private static final float MIN_PROGRESS_DENOMINATOR = 0.0001F;

    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int PARTICLE_COUNT = 1;
    private static final double PARTICLE_HEIGHT_OFFSET = 0.20D;
    private static final double PARTICLE_HORIZONTAL_SPREAD = 0.25D;
    private static final double PARTICLE_VERTICAL_SPREAD = 0.10D;
    private static final double PARTICLE_SPEED = 0.0D;

    public HighVoltagePerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev.high_voltage.desc",
                Math.round(MIN_PROGRESS_FRACTION * PERCENT_SCALE),
                SPEED_BONUS_PERCENT,
                Math.round(HORIZONTAL_RADIUS_BLOCKS),
                VERTICAL_RANGE_BLOCKS);
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        updateSpeedState(player);
    }

    @Override
    public void onTick(ServerPlayer player) {
        boolean active = updateSpeedState(player);
        if (active && player.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
            spawnElectricParticle(player);
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Короткий эффект истечёт сам и не затронет ускорение от других источников.
    }

    private boolean updateSpeedState(ServerPlayer player) {
        boolean shouldBeActive = isNearChargedComputer(player);
        if (shouldBeActive) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.ACCELERATION.get(),
                    SPEED_EFFECT_REFRESH_DURATION_TICKS,
                    SPEED_EFFECT_AMPLIFIER,
                    false,
                    false,
                    true
            ));
        }
        return shouldBeActive;
    }

    private boolean isNearChargedComputer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        HackManager manager = HackManager.get();
        boolean managerInitialized = HackManager.isInitialized();
        float pointsRequired = Math.max(MIN_PROGRESS_DENOMINATOR, HackConfig.HACK_POINTS_REQUIRED);

        for (BlockPos computerPos : ComputerBlockEntity.getTrackedPositionsSnapshot()) {
            if (Math.abs(computerPos.getY() - playerPos.getY()) > VERTICAL_RANGE_BLOCKS) {
                continue;
            }

            double deltaX = computerPos.getX() + 0.5D - player.getX();
            double deltaZ = computerPos.getZ() + 0.5D - player.getZ();
            if (deltaX * deltaX + deltaZ * deltaZ > HORIZONTAL_RADIUS_SQUARED) {
                continue;
            }

            if (!(level.getBlockEntity(computerPos) instanceof ComputerBlockEntity computer)) {
                continue;
            }

            double progress = computer.getHackProgress();
            boolean completed = computer.isHacked();
            if (managerInitialized) {
                progress = Math.max(progress, manager.getProgress(computer.getComputerId()) / pointsRequired);
                completed = completed || manager.isHacked(computer.getComputerId());
            }

            if (completed || progress >= MIN_PROGRESS_FRACTION) {
                return true;
            }
        }
        return false;
    }

    private static void spawnElectricParticle(ServerPlayer player) {
        player.serverLevel().sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX(),
                player.getY() + PARTICLE_HEIGHT_OFFSET,
                player.getZ(),
                PARTICLE_COUNT,
                PARTICLE_HORIZONTAL_SPREAD,
                PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD,
                PARTICLE_SPEED);
    }
}
