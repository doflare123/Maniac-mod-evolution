package org.example.maniacrevolution.engineer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.engineer.client.*;
import java.util.concurrent.TimeUnit;

/** Uses a real local player, actual item callbacks, C2S packets and client screens in all three modes. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class EngineerItemsClientProbe {
    private static final GameType[] MODES={GameType.CREATIVE,GameType.SURVIVAL,GameType.ADVENTURE};
    private static boolean loading,done;
    private static int mode,stage,ticks;
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static void slot(int slot) {
        var mc=Minecraft.getInstance(); mc.player.getInventory().selected=slot;
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("maniacrev.engineer.itemsProbe") || done || event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        try {
            if(!loading) {
                if(mc.screen==null || mc.getOverlay()!=null) return;
                loading=true;
                mc.createWorldOpenFlows().createFreshLevel("engineer-items-"+System.currentTimeMillis(),
                        new LevelSettings("Engineer item checks",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L,false,false),WorldPresets::createNormalWorldDimensions);
                return;
            }
            if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || mc.player.tickCount<30) return;
            ticks++;
            if(stage==0) {
                mc.setScreen(null); EngineerClient.ensureConnection();
                mc.getSingleplayerServer().submit(() -> {
                    var server=mc.getSingleplayerServer(); var p=server.getPlayerList().getPlayer(mc.player.getUUID());
                    var board=p.getScoreboard(); var phase=board.getObjective("phaseGame");
                    board.getOrCreatePlayerScore("game",phase).setScore(0);
                    if(p.getTeam()!=null) board.removePlayerFromTeam(p.getScoreboardName());
                    var cls=board.getObjective("SurvivorClass"); if(cls!=null) board.getOrCreatePlayerScore(p.getScoreboardName(),cls).setScore(0);
                    p.setGameMode(MODES[mode]); p.getInventory().setItem(0,new ItemStack(ModItems.ENGINEER_TABLET.get()));
                    p.getInventory().setItem(1,new ItemStack(ModItems.ENGINEER_REMOTE.get())); p.inventoryMenu.broadcastChanges();
                    return null;
                }).get(10,TimeUnit.SECONDS);
                ticks=0; stage=1; return;
            }
            if(ticks<15) return;
            if(stage==1) {
                check(mc.gameMode.getPlayerMode()==MODES[mode],"Client mode did not update");
                check(EngineerClient.eligible(),"Items rejected without a class/match in "+MODES[mode]);
                slot(0); mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                check(mc.screen instanceof EngineerScreen s && !s.remote && !s.isPauseScreen(),"Tablet failed to open in "+MODES[mode]);
                stage=2; ticks=0; return;
            }
            if(stage==2) {
                check(EngineerClient.state!=null && EngineerClient.state.active(),"Server rejected workshop item in "+MODES[mode]);
                mc.setScreen(null); slot(1); mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                check(mc.screen instanceof EngineerScreen s && s.remote && !s.isPauseScreen(),"Remote failed to open in "+MODES[mode]);
                stage=3; ticks=0; return;
            }
            if(stage==3) {
                mc.setScreen(null); Maniacrev.LOGGER.info("ENGINEER_ITEMS_CLIENT_MODE_PASS: {} tablet and remote without class/team/match",MODES[mode]);
                if(++mode<MODES.length) { stage=0; ticks=0; return; }
                check(mc.getResourceManager().getResource(Maniacrev.loc("geo/engineer_player.geo.json")).isEmpty(),"Custom player model still installed");
                stage=4;
            }
        } catch(Exception|AssertionError error) { done=true; Maniacrev.LOGGER.error("ENGINEER_ITEMS_CLIENT_PROBE_FAIL",error); mc.stop(); }
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(!Boolean.getBoolean("maniacrev.engineer.itemsProbe") || done || stage!=4 || event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance(); done=true;
        try {
            org.example.maniacrevolution.hud.EngineerArmorHudProbe.capture(mc);
            Maniacrev.LOGGER.info("ENGINEER_ITEMS_CLIENT_PROBE_PASS: three modes without class/match; real gray HP segments at 0/3/20/13 protection");
        } catch(Exception|AssertionError error) { Maniacrev.LOGGER.error("ENGINEER_ITEMS_CLIENT_PROBE_FAIL",error); }
        finally { mc.stop(); }
    }
}
