package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkInstance;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.example.maniacrevolution.util.ManiacDamageAttribution;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class MagicKickPerk extends Perk {
    public static final String ID = "magic_kick";
    public static final int SPEED_BONUS_PERCENT = 30;
    public static final int EFFECT_DURATION_SECONDS = 20;

    private static final int TICKS_PER_SECOND = 20;
    private static final int DISPLAYED_LEVEL_OFFSET = 1;
    private static final int EFFECT_AMPLIFIER = SPEED_BONUS_PERCENT - DISPLAYED_LEVEL_OFFSET;
    private static final int EFFECT_DURATION_TICKS = EFFECT_DURATION_SECONDS * TICKS_PER_SECOND;
    private static final float MINIMUM_REAL_DAMAGE = 0.0F;
    private static final String USED_THIS_MATCH_FLAG = "used_this_match";

    private static final int MAGIC_PARTICLE_COUNT = 10;
    private static final int CLOUD_PARTICLE_COUNT = 6;
    private static final double PARTICLE_HEIGHT_OFFSET = 0.2D;
    private static final double MAGIC_HORIZONTAL_SPREAD = 0.35D;
    private static final double MAGIC_VERTICAL_SPREAD = 0.25D;
    private static final double MAGIC_PARTICLE_SPEED = 0.08D;
    private static final double CLOUD_HORIZONTAL_SPREAD = 0.45D;
    private static final double CLOUD_VERTICAL_SPREAD = 0.12D;
    private static final double CLOUD_PARTICLE_SPEED = 0.04D;
    private static final float SOUND_VOLUME = 0.6F;
    private static final float SOUND_PITCH = 1.25F;

    public MagicKickPerk() {
        super(new Builder(ID)
                .type(PerkType.PASSIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "perk.maniacrev." + ID + ".desc",
                SPEED_BONUS_PERCENT,
                EFFECT_DURATION_SECONDS
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= MINIMUM_REAL_DAMAGE
                || player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
                || PerkTeam.fromPlayer(player) != PerkTeam.SURVIVOR) {
            return;
        }

        PerkPhase phase = GameManager.getCurrentPhase();
        if (phase != PerkPhase.HUNT && phase != PerkPhase.MIDGAME) {
            return;
        }

        ServerPlayer responsibleManiac = ManiacDamageAttribution.peekResponsibleManiac(
                player,
                event.getSource()
        );
        if (responsibleManiac == null
                || PerkTeam.fromPlayer(responsibleManiac) != PerkTeam.MANIAC) {
            return;
        }

        PlayerData data = PlayerDataManager.get(player);
        PerkInstance instance = data.getPerkInstance(ID);
        if (instance == null || instance.hasMatchFlag(USED_THIS_MATCH_FLAG)) {
            return;
        }

        instance.setMatchFlag(USED_THIS_MATCH_FLAG);
        player.addEffect(new MobEffectInstance(
                ModEffects.ACCELERATION.get(),
                EFFECT_DURATION_TICKS,
                EFFECT_AMPLIFIER,
                false,
                false,
                true
        ));
        playActivationEffects(player);
        PlayerDataManager.syncToClient(player);
    }

    private static void playActivationEffects(ServerPlayer player) {
        player.serverLevel().sendParticles(
                ParticleTypes.WITCH,
                player.getX(),
                player.getY() + PARTICLE_HEIGHT_OFFSET,
                player.getZ(),
                MAGIC_PARTICLE_COUNT,
                MAGIC_HORIZONTAL_SPREAD,
                MAGIC_VERTICAL_SPREAD,
                MAGIC_HORIZONTAL_SPREAD,
                MAGIC_PARTICLE_SPEED
        );
        player.serverLevel().sendParticles(
                ParticleTypes.CLOUD,
                player.getX(),
                player.getY() + PARTICLE_HEIGHT_OFFSET,
                player.getZ(),
                CLOUD_PARTICLE_COUNT,
                CLOUD_HORIZONTAL_SPREAD,
                CLOUD_VERTICAL_SPREAD,
                CLOUD_HORIZONTAL_SPREAD,
                CLOUD_PARTICLE_SPEED
        );
        player.playNotifySound(
                SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS,
                SOUND_VOLUME,
                SOUND_PITCH
        );
    }
}
