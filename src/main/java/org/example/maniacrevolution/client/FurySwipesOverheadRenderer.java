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
import org.example.maniacrevolution.data.ClientPlayerData;

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
    private static final String SCOREBOARD_OBJ = "ManiacClass";
    private static final int    REQUIRED_CLASS = 7;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // ── ВРЕМЕННЫЙ ДЕБАГ — убери после починки ────────────────────────────
        Player local = mc.player;
        net.minecraft.world.scores.Team team = local.getTeam();
        Scoreboard sb = mc.level.getScoreboard();
        Objective obj = sb.getObjective(SCOREBOARD_OBJ);

        System.out.println("[FurySwipes] team=" + (team == null ? "NULL" : team.getName()));
        System.out.println("[FurySwipes] obj=" + (obj == null ? "NULL" : obj.getName()));
        if (obj != null) {
            boolean hasScore = sb.hasPlayerScore(local.getScoreboardName(), obj);
            System.out.println("[FurySwipes] hasScore=" + hasScore);
            if (hasScore) {
                int score = sb.getOrCreatePlayerScore(local.getScoreboardName(), obj).getScore();
                System.out.println("[FurySwipes] score=" + score);
            }
        }
        int stacks = ClientFurySwipesData.getTargetStackCount(target.getUUID());
        System.out.println("[FurySwipes] stacks for " + target.getName().getString() + " = " + stacks);
        // ─────────────────────────────────────────────────────────────────────

        if (!isLocalPlayerManiac(mc)) return;

        int stackCount = ClientFurySwipesData.getTargetStackCount(target.getUUID());
        if (stackCount <= 0) return;

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

        String label = "§6§l" + stacks + "§r §cswipes";
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

        // Проверка команды через team (для датапака)
        net.minecraft.world.scores.Team team = local.getTeam();
        if (team == null || !MANIAC_TEAM.equalsIgnoreCase(team.getName())) return false;

        // Проверка класса через клиентские данные мода (надёжно)
        return ClientPlayerData.isManiacClass(REQUIRED_CLASS);
    }
}