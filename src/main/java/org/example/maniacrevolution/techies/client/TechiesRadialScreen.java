package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.mana.ClientManaData;
import org.example.maniacrevolution.techies.*;

/** Three shaded annular sectors with real 3D mine geometry. */
public final class TechiesRadialScreen extends Screen {
    private final TechiesVisual[] models=new TechiesVisual[3];
    public TechiesRadialScreen() { super(Component.literal("Мины Течиса")); for(MineType t:MineType.values()) models[t.ordinal()]=new TechiesVisual("techies_mine_"+t.color); }
    public int hover(double x,double y) {
        double dx=x-width/2.0,dy=y-height/2.0,r=Math.hypot(dx,dy),outer=Math.min(105,Math.min(width,height)*.43);
        if(r<outer*.42 || r>outer) return -1;
        double angle=(Math.atan2(dy,dx)+Math.PI/2+2*Math.PI)%(2*Math.PI);
        return Math.min(2,(int)(angle/(2*Math.PI/3)));
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        int hovered=hover(mouseX,mouseY); float radius=Math.min(105,Math.min(width,height)*.43f);
        for(int sector=0;sector<3;sector++) {
            int color=sector==hovered?new int[]{0xDF7B343E,0xDF356B49,0xDF355D8C}[sector]:0xED17202D;
            VertexConsumer out=g.bufferSource().getBuffer(RenderType.gui()); var matrix=g.pose().last().pose();
            for(int i=0;i<32;i++) {
                double a=-Math.PI/2+sector*2*Math.PI/3+.025+i*(2*Math.PI/3-.05)/32;
                double b=a+(2*Math.PI/3-.05)/32;
                vertex(out,matrix,a,radius,color); vertex(out,matrix,a,radius*.42,color);
                vertex(out,matrix,b,radius*.42,color); vertex(out,matrix,b,radius,color);
            }
            g.flush();
            double angle=-Math.PI/2+(sector+.5)*2*Math.PI/3;
            float x=width/2f+(float)Math.cos(angle)*radius*.7f, y=height/2f+(float)Math.sin(angle)*radius*.7f;
            g.pose().pushPose(); g.pose().translate(x,y+16,160); float scale=radius*.56f; g.pose().scale(scale,-scale,scale); g.pose().mulPose(Axis.XP.rotationDegrees(25)); g.pose().mulPose(Axis.YP.rotationDegrees(-25));
            var texture=Maniacrev.loc("textures/item/"+models[sector].stem+".png");
            TechiesClient.RENDERER.render(g.pose(),models[sector],g.bufferSource(),RenderType.entityCutoutNoCull(texture),null,LightTexture.FULL_BRIGHT);
            g.pose().popPose(); g.flush();
            MineType t=MineType.values()[sector]; long cd=Math.max(0,TechiesClient.cooldowns[sector]-(long)TechiesClient.time());
            if(cd>0 || ClientManaData.getMana()<t.cost) g.drawCenteredString(font,cd>0?(int)Math.ceil(cd/20.0)+" с":"Нет маны",(int)x,(int)y+22,0xE9B5B5);
        }
        if(hovered>=0) {
            MineType t=MineType.values()[hovered];
            g.drawCenteredString(font,t.label,width/2,height/2-13,0xFFFFFF);
            g.drawCenteredString(font,t.cost+" маны",width/2,height/2,0xAAC9E9);
            g.drawCenteredString(font,t.install/20+" с",width/2,height/2+13,0xBAC6CF);
        } else g.drawCenteredString(font,"Выберите мину",width/2,height/2,0xCCD5E0);
        g.drawCenteredString(font,"ЛКМ — выбор · ПКМ — закрыть",width/2,height/2+(int)radius+12,0x9BA9B9);
    }
    private void vertex(VertexConsumer v,org.joml.Matrix4f m,double a,double r,int color) {
        v.vertex(m,width/2f+(float)(Math.cos(a)*r),height/2f+(float)(Math.sin(a)*r),0)
                .color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).endVertex();
    }
    // This Screen supplies drawing only; TechiesClient owns the nonblocking overlay input.
    @Override public boolean isPauseScreen() { return false; }
}
