package org.example.maniacrevolution.effect.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.util.PlayerModeUtil;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class JumpCooldownClient {
    private JumpCooldownClient() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restrictJump(MovementInputUpdateEvent event) {
        if (PlayerModeUtil.isSurvivalOrAdventure(event.getEntity()) && event.getEntity().hasEffect(ModEffects.JUMP_COOLDOWN.get()))
            event.getInput().jumping = false;
    }
}
