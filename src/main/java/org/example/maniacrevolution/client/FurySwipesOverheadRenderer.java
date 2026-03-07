package org.example.maniacrevolution.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Показывает стаки Fury Swipes НАД ГОЛОВОЙ жертвы (выше таблички имени).
 *
 * Условия отображения:
 *   - Локальный игрок в team "maniac"
 *   - Scoreboard SurvivorClass локального игрока == 7
 *   - На жертве есть стаки (ClientFurySwipesData.getTargetStackCount > 0)
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public class FurySwipesOverheadRenderer {

    private static final String MANIAC_TEAM    = "maniac";
    private static final String SCOREBOARD_OBJ = "SurvivorClass";
    private static final int    REQUIRED_CLASS = 7;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!isLocalPlayerManiac(mc)) return;

        int stacks = ClientFurySwipesData.getTargetStackCount(target.getUUID());
        if (stacks <= 0) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();

        poseStack.pushPose();

        // В Forge 1.20.1 RenderNameTagEvent: PoseStack стоит У НОГ сущности.
        // Ванильный nametag рисуется на высоте getBbHeight() + 0.5.
        // Мы рисуем ещё на 0.3 выше таблички с именем.
        float nameTagY = target.getBbHeight() + 0.5f + 0.3f;
        poseStack.translate(0.0, nameTagY, 0.0);

        // Billboard — разворачиваемся к камере
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        String label = "§6§l" + stacks + "§r §cfury";
        float textWidth = mc.font.width(label);
        int bgColor = (int)(0.25f * 255) << 24;

        mc.font.drawInBatch(
                label,
                -textWidth / 2f, -4f,
                0xFFFFFFFF, false,
                poseStack.last().pose(),
                bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                bgColor,
                0xF000F0
        );

        // Важно: сбрасываем буфер чтобы текст отрендерился в этом кадре
        if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }

        poseStack.popPose();
    }

    private static boolean isLocalPlayerManiac(Minecraft mc) {
        Player local = mc.player;
        if (local == null) return false;

        net.minecraft.world.scores.Team team = local.getTeam();
        if (team == null || !MANIAC_TEAM.equalsIgnoreCase(team.getName())) return false;

        Scoreboard sb = mc.level.getScoreboard();
        Objective obj = sb.getObjective(SCOREBOARD_OBJ);
        if (obj == null) return false;
        if (!sb.hasPlayerScore(local.getScoreboardName(), obj)) return false;
        return sb.getOrCreatePlayerScore(local.getScoreboardName(), obj).getScore()
                == REQUIRED_CLASS;
    }
}