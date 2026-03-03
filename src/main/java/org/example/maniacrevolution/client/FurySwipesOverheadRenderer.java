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

        // Только для маньяка класса 7
        if (!isLocalPlayerManiac(mc)) return;

        int stacks = ClientFurySwipesData.getTargetStackCount(target.getUUID());
        if (stacks <= 0) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();

        poseStack.pushPose();

        // Матрица RenderNameTagEvent стоит в центре AABB (Y=0 = центр сущности, не ноги).
        // Ванильный nametag: entity.getBbHeight() + 0.5 от центра сущности.
        // Встаём ещё на +0.28 выше таблички с именем.
        double y = (target.getBbHeight() / 2.0) + 0.5 + 0.28;
        poseStack.translate(0.0, y, 0.0);

        // Billboard — разворачиваемся к камере
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        String label = "\u00a76\u00a7l" + stacks + "\u00a7r \u00a7cfury";
        float textWidth = mc.font.width(label);
        int bgColor = (int)(0.25f * 255) << 24;

        mc.font.drawInBatch(
                label,
                -textWidth / 2f, -4f,
                0xFFFFFFFF, false,
                poseStack.last().pose(),
                bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                bgColor,
                0xF000F0
        );

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