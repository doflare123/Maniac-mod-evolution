package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedData;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class FixedItImOutPerk extends Perk {
    public static final String ID = "fixed_it_im_out";
    public static final float HEAL_AMOUNT_HP = 10.0F;
    public static final int SPEED_BONUS_PERCENT = 20;

    private static final int DISPLAYED_LEVEL_OFFSET = 1;
    private static final int SPEED_EFFECT_AMPLIFIER = SPEED_BONUS_PERCENT - DISPLAYED_LEVEL_OFFSET;
    private static final int SPEED_EFFECT_REFRESH_DURATION_TICKS = 5;
    private static final String PHASE_THREE_HANDLED_FLAG = "phase_three_handled";
    private static final String BONUS_GRANTED_FLAG = "bonus_granted";

    private static final int GREEN_PARTICLE_COUNT = 6;
    private static final int GOLD_PARTICLE_COUNT = 8;
    private static final double PARTICLE_HEIGHT_OFFSET = 0.9D;
    private static final double PARTICLE_HORIZONTAL_SPREAD = 0.45D;
    private static final double PARTICLE_VERTICAL_SPREAD = 0.65D;
    private static final double GREEN_PARTICLE_SPEED = 0.02D;
    private static final double GOLD_PARTICLE_SPEED = 0.04D;
    private static final float SOUND_VOLUME = 0.6F;
    private static final float SOUND_PITCH = 1.1F;

    public FixedItImOutPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.REVERSAL));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                Math.round(HEAL_AMOUNT_HP),
                SPEED_BONUS_PERCENT
        );
    }

    @Override
    public void onPhaseChange(ServerPlayer player, PerkPhase newPhase) {
        if (newPhase != PerkPhase.REVERSAL) {
            return;
        }

        PlayerData data = PlayerDataManager.get(player);
        PerkInstance instance = data.getPerkInstance(ID);
        if (instance == null || instance.hasMatchFlag(PHASE_THREE_HANDLED_FLAG)) {
            return;
        }

        instance.setMatchFlag(PHASE_THREE_HANDLED_FLAG);
        if (!canReceiveBonus(player)) {
            PlayerDataManager.syncToClient(player);
            return;
        }

        instance.setMatchFlag(BONUS_GRANTED_FLAG);
        player.heal(HEAL_AMOUNT_HP);
        refreshSpeedEffect(player);
        playActivationEffects(player);
        PlayerDataManager.syncToClient(player);
    }

    @Override
    public void applyPassiveEffect(ServerPlayer player) {
        updatePermanentSpeed(player);
    }

    @Override
    public void onTick(ServerPlayer player) {
        updatePermanentSpeed(player);
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // Короткий эффект истечёт сам и не затронет ускорение от других источников.
    }

    private static void updatePermanentSpeed(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player);
        PerkInstance instance = data.getPerkInstance(ID);
        boolean granted = instance != null && instance.hasMatchFlag(BONUS_GRANTED_FLAG);
        boolean remainsActive = player.isAlive()
                && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;

        if (granted && remainsActive) {
            refreshSpeedEffect(player);
        }
    }

    private static void refreshSpeedEffect(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                ModEffects.ACCELERATION.get(),
                SPEED_EFFECT_REFRESH_DURATION_TICKS,
                SPEED_EFFECT_AMPLIFIER,
                false,
                false,
                true
        ));
    }

    private static boolean canReceiveBonus(ServerPlayer player) {
        if (!player.isAlive()
                || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(player) != PerkTeam.SURVIVOR) {
            return false;
        }

        DownedData downedData = DownedCapability.get(player);
        return downedData == null || downedData.getState() != DownedState.DOWNED;
    }

    private static void playActivationEffects(ServerPlayer player) {
        player.serverLevel().sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                player.getX(),
                player.getY() + PARTICLE_HEIGHT_OFFSET,
                player.getZ(),
                GREEN_PARTICLE_COUNT,
                PARTICLE_HORIZONTAL_SPREAD,
                PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD,
                GREEN_PARTICLE_SPEED
        );
        player.serverLevel().sendParticles(
                ParticleTypes.WAX_ON,
                player.getX(),
                player.getY() + PARTICLE_HEIGHT_OFFSET,
                player.getZ(),
                GOLD_PARTICLE_COUNT,
                PARTICLE_HORIZONTAL_SPREAD,
                PARTICLE_VERTICAL_SPREAD,
                PARTICLE_HORIZONTAL_SPREAD,
                GOLD_PARTICLE_SPEED
        );
        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                SOUND_VOLUME,
                SOUND_PITCH
        );
    }
}
