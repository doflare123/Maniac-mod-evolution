package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.engineer.EngineerStatePacket;
import org.example.maniacrevolution.engineer.client.EngineerClient;
import org.joml.Matrix4f;
import java.util.List;

/** Captures the real custom health-bar renderer with a real local player. */
public final class EngineerArmorHudProbe {
    public static void capture(Minecraft mc) throws Exception {
        var target = new TextureTarget(960,640,true,Minecraft.ON_OSX);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack(); view.pushPose();
        var state = EngineerClient.state;
        float health = mc.player.getHealth();
        try {
            view.setIdentity(); RenderSystem.applyModelViewMatrix();
            target.setClearColor(.065f,.09f,.12f,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,320,213,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
            var g = new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            var method = CustomHud.class.getDeclaredMethod("renderHealthBar",GuiGraphics.class,Player.class,int.class,int.class);
            method.setAccessible(true);
            var animation = CustomHud.class.getDeclaredField("animation"); animation.setAccessible(true);
            float[] reserves = {0,3,20,13};
            for(int i=0;i<reserves.length;i++) {
                mc.player.setHealth(mc.player.getMaxHealth());
                EngineerClient.accept(new EngineerStatePacket(0,true,40,0,0,reserves[i],-1,-1,BlockPos.ZERO,-1,0,List.of()));
                var screen = new org.example.maniacrevolution.engineer.client.EngineerScreen(false);
                screen.init(mc,300,240); screen.tick();
                var buy = (net.minecraft.client.gui.components.AbstractWidget)screen.children().get(2);
                if(buy.active!=(reserves[i]<20) || buy.getMessage().getString().contains("%s"))
                    throw new AssertionError("Incorrect protection purchase button at reserve "+reserves[i]);
                var hud = new CustomHud();
                ((HudAnimationState)animation.get(hud)).update(1,1,0,false,false);
                g.drawString(mc.font,"Protection: "+(int)reserves[i],20,15+i*47,0xFFD99B4E,false);
                g.pose().pushPose(); g.pose().translate(20,28+i*47,0); g.pose().scale(2,2,2);
                method.invoke(hud,g,mc.player,0,0); g.pose().popPose();
            }
            g.flush();
            try(var image=Screenshot.takeScreenshot(target)) {
                int gray=0;
                for(int x=0;x<image.getWidth();x++) for(int y=0;y<image.getHeight();y++)
                    if((image.getPixelRGBA(x,y)&0xFFFFFF)==0x7D7165) gray++;
                if(gray<1000) throw new AssertionError("Missing gray protection segments: "+gray);
                image.writeToFile(mc.gameDirectory.toPath().resolve("engineer-armor-hud.png"));
            }
        } finally {
            mc.player.setHealth(health); EngineerClient.state=state;
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting);
            mc.getMainRenderTarget().bindWrite(true); target.destroyBuffers();
        }
    }
}
