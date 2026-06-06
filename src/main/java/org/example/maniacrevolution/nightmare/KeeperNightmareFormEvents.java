package org.example.maniacrevolution.nightmare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class KeeperNightmareFormEvents {
    private static final UUID WALK_SPEED_MODIFIER_ID = UUID.fromString("3a87b5d4-5e0f-4fd5-b854-1b5dd042d613");
    private static final AttributeModifier WALK_SPEED_MODIFIER = new AttributeModifier(
            WALK_SPEED_MODIFIER_ID,
            "Keeper nightmare form walk speed",
            -0.40D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    private KeeperNightmareFormEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean keeper = NightmareManager.getInstance().isKeeper(player);
        if (keeper && isGameplayMode(player)) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            addWalkSpeed(player);
        } else {
            removeWalkSpeed(player);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isKeeper(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isKeeper(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker && isKeeper(attacker)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    private static boolean isKeeper(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && NightmareManager.getInstance().isKeeper(serverPlayer)
                && isGameplayMode(serverPlayer);
    }

    private static boolean isGameplayMode(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }

    private static void addWalkSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(WALK_SPEED_MODIFIER_ID) == null) {
            speed.addTransientModifier(WALK_SPEED_MODIFIER);
        }
    }

    private static void removeWalkSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(WALK_SPEED_MODIFIER_ID) != null) {
            speed.removeModifier(WALK_SPEED_MODIFIER_ID);
        }
    }
}
