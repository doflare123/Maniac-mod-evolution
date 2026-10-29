package org.example.maniacrevolution.engineer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.engineer.client.*;
import org.joml.Matrix4f;
import java.util.*;

/** Opt-in real Minecraft rendering of the exported mesh, textures, item models and compact menus. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class EngineerClientRenderProbe {
    private static boolean done;
    private static int frames;
    private static void save(TextureTarget target,String name) throws Exception {
        try(var image=Screenshot.takeScreenshot(target)) {
            int pixels=0;
            for(int x=0;x<image.getWidth();x++) for(int y=0;y<image.getHeight();y++) if((image.getPixelRGBA(x,y)&0xFFFFFF)!=0) pixels++;
            if(pixels<500) throw new AssertionError("Empty engineer render: "+name);
            image.writeToFile(Minecraft.getInstance().gameDirectory.toPath().resolve(name+".png"));
        }
    }
    @SuppressWarnings("unchecked")
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("maniacrev.engineer.renderProbe") || done || event.phase!=TickEvent.Phase.END || mc.screen==null || mc.getOverlay()!=null || ++frames<20) return;
        done=true; var target=new TextureTarget(960,640,true,Minecraft.ON_OSX);
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack(); view.pushPose();
        try {
            view.setIdentity(); RenderSystem.applyModelViewMatrix();
            if(mc.getResourceManager().getResource(Maniacrev.loc("geo/engineer_player.geo.json")).isPresent()) throw new AssertionError("Removed player replacement is still packaged");
            for(boolean remote:new boolean[]{false,true}) {
                var panels=new ArrayList<EngineerStatePacket.Panel>();
                for(int i=0;i<6;i++) panels.add(new EngineerStatePacket.Panel(net.minecraft.world.level.Level.OVERWORLD.location(),new BlockPos(12+i,64,20-i),3-i%3,0,0,false));
                EngineerClient.accept(new EngineerStatePacket(0,true,40,20,2,3,-1,-1,BlockPos.ZERO,-1,0,panels));
                for(int width:new int[]{360,300}) {
                    target.setClearColor(0,0,0,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
                    RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,width,240,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
                    var screen=new EngineerScreen(remote); screen.init(mc,width,240); var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
                    screen.render(graphics,0,0,0); graphics.flush(); save(target,"engineer-"+(remote?"remote":"workshop")+"-"+width);
                }
            }
            target.setClearColor(0,0,0,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,320,213,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            int i=0;
            for(Item item:List.of(ModItems.ENGINEER_TABLET.get(),ModItems.ENGINEER_REMOTE.get(),ModItems.ENGINEER_BARRICADE.get(),ModItems.ENGINEER_PANEL.get(),ModItems.ENGINEER_SCRAP.get())) {
                var stack=new ItemStack(item); if(mc.getItemRenderer().getModel(stack,null,null,0)==mc.getModelManager().getMissingModel()) throw new AssertionError("Missing engineer item model");
                g.pose().pushPose(); g.pose().translate(25+i*58,80,0); g.pose().scale(3,3,3); g.renderItem(stack,0,0); g.pose().popPose(); i++;
            }
            g.flush(); save(target,"engineer-items");
            for(boolean left:new boolean[]{false,true}) {
                target.setClearColor(0,0,0,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
                RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(70),1.5f,.05f,100),VertexSorting.DISTANCE_TO_ORIGIN);
                var pose = new PoseStack(); pose.translate(left?-.56:.56,-.52,-.72);
                mc.getItemRenderer().renderStatic(null,new ItemStack(ModItems.ENGINEER_TABLET.get()),
                        left?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,left,pose,mc.renderBuffers().bufferSource(),null,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);
                mc.renderBuffers().bufferSource().endBatch();
                try(var image=Screenshot.takeScreenshot(target)) {
                    for(int x=0;x<image.getWidth();x++) if((image.getPixelRGBA(x,image.getHeight()-1)&0xFFFFFF)!=0) throw new AssertionError("Tablet clipped by bottom of camera");
                    for(int y=0;y<image.getHeight();y++) if((image.getPixelRGBA(0,y)&0xFFFFFF)!=0 || (image.getPixelRGBA(image.getWidth()-1,y)&0xFFFFFF)!=0) throw new AssertionError("Tablet clipped by side of camera");
                }
                save(target,"engineer-tablet-"+(left?"left":"right"));
            }
            // Real GeckoLib parser/baker, in the client with its complete runtime dependencies.
            for(String stem:new String[]{"engineer_barricade","engineer_panel"}) {
                try(var reader=mc.getResourceManager().openAsReader(Maniacrev.loc("geo/"+stem+".geo.json"))) {
                    var raw=software.bernie.geckolib.util.JsonUtil.GEO_GSON.fromJson(reader,software.bernie.geckolib.loading.json.raw.Model.class);
                    var baked=software.bernie.geckolib.loading.object.BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(software.bernie.geckolib.loading.object.GeometryTree.fromModel(raw));
                    if(baked.topLevelBones().isEmpty()) throw new AssertionError("Empty engineer block model");
                }
            }
            Maniacrev.LOGGER.info("ENGINEER_RENDER_PROBE_PASS: no custom player model, five item models, animated tablet texture, compact workshop/remote at 300/360, GeckoLib device geometry");
        } catch(Exception|AssertionError error) { Maniacrev.LOGGER.error("ENGINEER_RENDER_PROBE_FAIL",error); }
        finally { view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting); mc.getMainRenderTarget().bindWrite(true); target.destroyBuffers(); mc.stop(); }
    }
}
