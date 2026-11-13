package org.example.maniacrevolution.lich.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class CorpsePointMarkers {
    private static final Set<BlockPos> POINTS=new LinkedHashSet<>();
    private static String dimension="";private static long received;private static boolean validMap;
    public static void accept(CompoundTag tag){var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(tag.getBoolean("first"))POINTS.clear();dimension=tag.getString("dimension");received=mc.level.getGameTime();validMap=tag.getBoolean("validMap");
        for(long pos:tag.getLongArray("positions"))POINTS.add(BlockPos.of(pos));
    }
    static boolean validMap() {
        var level=Minecraft.getInstance().level;
        return validMap&&level!=null&&level.getGameTime()-received<=25&&dimension.equals(level.dimension().location().toString());
    }
    static boolean contains(BlockPos pos) {return POINTS.contains(pos);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.player==null||mc.level==null
                ||mc.options.hideGui||mc.screen!=null||!mc.player.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get())||!validMap())return;
        var pose=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();pose.pushPose();
        try{
            pose.translate(-camera.x,-camera.y,-camera.z);var vertices=buffers.getBuffer(RenderType.lines());
            for(var p:POINTS)LevelRenderer.renderLineBox(pose,vertices,p.getX()+.1,p.getY()+.02,p.getZ()+.1,p.getX()+.9,p.getY()+.5,p.getZ()+.9,.84f,1,.93f,.26f);
            buffers.endBatch(RenderType.lines());
        }finally{pose.popPose();}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){POINTS.clear();dimension="";validMap=false;}
}
