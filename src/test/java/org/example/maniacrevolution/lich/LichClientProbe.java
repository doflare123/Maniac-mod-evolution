package org.example.maniacrevolution.lich;

import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.entity.ModEntities;

/** Opt-in real-client smoke run in build/lich-client-probe, never the user's world. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class LichClientProbe {
    private static int tick;
    private static int resizeAttempts;
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("maniacrev.lich.clientProbe")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        if(mc.getMainRenderTarget().width<100||mc.getMainRenderTarget().height<100){
            long handle=mc.getWindow().getWindow();org.lwjgl.glfw.GLFW.glfwRestoreWindow(handle);org.lwjgl.glfw.GLFW.glfwShowWindow(handle);org.lwjgl.glfw.GLFW.glfwSetWindowSize(handle,1280,720);mc.resizeDisplay();
            if(++resizeAttempts>100){Maniacrev.LOGGER.error("LICH_CLIENT_PROBE_FAILED: no usable framebuffer");mc.stop();}return;
        }
        tick++;
        if(tick==1){mc.options.guiScale().set(2);mc.resizeDisplay();mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());var level=server.overworld();
            for(var pos:BlockPos.betweenClosed(new BlockPos(-10,64,-10),new BlockPos(10,71,18)))level.setBlockAndUpdate(pos,pos.getY()==64?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            level.setDayTime(6000);p.setGameMode(GameType.SURVIVAL);p.teleportTo(level,.5,65,.5,0,0);
            var board=server.getScoreboard();if(board.getPlayerTeam("maniac")==null)board.addPlayerTeam("maniac");board.addPlayerToTeam(p.getScoreboardName(),board.getPlayerTeam("maniac"));
            PlayerDataManager.get(p).setManiacClassId(14);
            for(String name:java.util.List.of("phaseGame","map"))if(board.getObjective(name)==null)board.addObjective(name,ObjectiveCriteria.DUMMY,Component.literal(name),ObjectiveCriteria.RenderType.INTEGER);
            board.getOrCreatePlayerScore("Game",board.getObjective("map")).setScore(1);
            var points=CorpsePoints.get(server);points.points.clear();for(int x:java.util.List.of(-4,0,4))points.toggle("mansion",level.dimension().location().toString(),new BlockPos(x,65,3));
            board.getOrCreatePlayerScore("game",board.getObjective("phaseGame")).setScore(1);
        });}
        if(tick==30)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            for(int slot=0;slot<9;slot++)if(p.getInventory().getItem(slot).is(ModItems.LICH_BOOK.get())){p.getInventory().selected=slot;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(slot));break;}
            for(var kind:LichRules.Kind.values()){
                var u=ModEntities.LICH_UNIT.get().create(p.serverLevel());u.setPos(-3+kind.ordinal()*2,65,6);u.configure(p.getUUID(),kind);
                u.command(LichRules.Order.STAND,u.position(),null);p.serverLevel().addFreshEntity(u);
            }
        });
        if(tick==125)shot(mc,"lich_first_person.png");
        if(tick==140)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(tick==165)shot(mc,"lich_form.png");
        if(tick==180){mc.options.setCameraType(CameraType.FIRST_PERSON);org.example.maniacrevolution.lich.client.LichClient.request("open");}
        if(tick==200)shot(mc,"lich_army_menu.png");
        if(tick==220)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            LichManager.corpses().stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).ifPresent(c->LichManager.openCorpse(p,c));
        });
        if(tick==240)shot(mc,"lich_corpse_menu.png");
        if(tick==260){mc.setScreen(null);mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());server.getPlayerList().op(p.getGameProfile());p.setGameMode(GameType.CREATIVE);
            p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModItems.LICH_CORPSE_EDITOR.get()));p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        });}
        if(tick==280)mc.getSingleplayerServer().execute(()->LichManager.editor(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()),true));
        if(tick==300)shot(mc,"lich_editor_menu.png");
        if(tick==310)mc.setScreen(null);
        if(tick==330)shot(mc,"lich_editor_points.png");
        if(tick==345){Maniacrev.LOGGER.info("LICH_CLIENT_PROBE_COMPLETE");mc.stop();}
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->Maniacrev.LOGGER.info("Lich render probe: {}",c.getString()));}
}
