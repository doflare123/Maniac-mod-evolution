package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.example.maniacrevolution.techies.MineType;

/** HUD-only readiness states share the existing effect grid; no fake potion effects or gameplay modifiers. */
public final class TechiesHud {
    private TechiesHud() {}
    public static long remaining(MineType t) { return Math.max(0,(long)Math.ceil(TechiesClient.cooldowns[t.ordinal()]-TechiesClient.time())); }
    public static int color(MineType t) { return switch(t) { case RED -> 0xFFFF6872; case GREEN -> 0xFF7BDC91; case BLUE -> 0xFF78BCFF; }; }
    public static String stateKey(MineType t) { return "hud.maniacrev.techies."+t.color+"."+(remaining(t)==0?"ready":"recharging"); }
    public static void installation(GuiGraphics g,int cx,int cy,MineType type,float progress) {
        progress=Math.max(0,Math.min(1,progress));
        var v=g.bufferSource().getBuffer(RenderType.gui()); var m=g.pose().last().pose();
        for(int i=0;i<32;i++) {
            double a=-Math.PI/2+i*Math.PI/16+.025,b=a+Math.PI/16-.05;
            segment(v,m,cx,cy,a,b,24,29,i<Math.ceil(progress*32)?color(type):0xBF1C252E);
            segment(v,m,cx,cy,a,b,31,32,(i/2)%2==0?0xDBD9AD4D:0xD020252B);
        }
        g.flush();
        // Squared instrumentation corners reinforce the sapper motif, keeping the crosshair unobscured.
        for(int side:new int[]{-1,1}) {
            int x=cx+side*37; g.fill(x-1,cy-15,x+1,cy-5,0xCCA5B5BF); g.fill(x-1,cy+5,x+1,cy+15,0xCCA5B5BF);
        }
        var font=Minecraft.getInstance().font;
        String label="УСТАНОВКА · "+Math.round(progress*100)+"%";
        g.drawCenteredString(font,label,cx,cy+38,0xFFFFD48A);
    }
    private static void segment(VertexConsumer v,org.joml.Matrix4f m,int x,int y,double a,double b,float inner,float outer,int color) {
        vertex(v,m,x,y,a,outer,color); vertex(v,m,x,y,a,inner,color);
        vertex(v,m,x,y,b,inner,color); vertex(v,m,x,y,b,outer,color);
    }
    private static void vertex(VertexConsumer v,org.joml.Matrix4f m,int x,int y,double a,float r,int c) {
        v.vertex(m,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,0)
                .color((c>>16)&255,(c>>8)&255,c&255,(c>>>24)&255).endVertex();
    }
}
