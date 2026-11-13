package org.example.maniacrevolution.lich.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Compact jade army list; phylactery health is attached to the player's HP bar. */
public final class LichArmyOverlay {
    private static final int JADE=0xff7bd6b4, MUTED=0xff8caaa0, GOLD=0xffc5ad72;
    private LichArmyOverlay() {}

    public static void render(GuiGraphics g,Font font,int screenWidth,int screenHeight,CompoundTag army) {
        var rows=army.getList("rows",Tag.TAG_COMPOUND);
        var mc=net.minecraft.client.Minecraft.getInstance();
        boolean custom=org.example.maniacrevolution.config.HudConfig.isCustomHudEnabled();
        // Reserve the whole bottom content area, including effect rows, item name and chat offset.
        int reserved=custom?org.example.maniacrevolution.hud.CustomHud.getBottomContentTopOffset(mc.player,
                mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)+6:72;
        int top=Math.max(30,org.example.maniacrevolution.hud.CustomHud.getRightTrackersBottom()+6);
        // Vanilla effect icons occupy the upper right when the custom HUD is disabled.
        if(!custom&&mc.player!=null&&mc.player.getActiveEffects().stream().anyMatch(e->e.showIcon()))top=Math.max(top,54);
        var layout=LichHudLayout.panel(screenWidth,screenHeight,rows.size(),top,screenHeight-reserved);
        if(layout.height()==0)return;
        int x=layout.x(),y=layout.y(),w=layout.width();
        g.fillGradient(x,y,x+w,y+layout.height(),0xe510231e,0xd807100f);
        g.fill(x,y,x+w,y+1,0xff36594c);
        g.fill(x,y+layout.height()-1,x+w,y+layout.height(),0xff284237);
        small(g,font,"АРМИЯ",x+5,y+4,GOLD);
        String total=Integer.toString(Math.max(0,army.getInt("total")));
        small(g,font,total,x+39,y+4,JADE);
        if(army.getInt("pages")>1) {
            String page=(army.getInt("page")+1)+"/"+army.getInt("pages");
            small(g,font,page,x+w-5-smallWidth(font,page),y+4,MUTED);
        }
        for(int i=0;i<layout.rows();i++) {
            var row=rows.getCompound(i);int ry=y+LichHudLayout.HEADER_HEIGHT+i*LichHudLayout.ROW_HEIGHT;
            boolean selected=row.getBoolean("selected"),hurt=row.getBoolean("hurt");
            g.fill(x+1,ry,x+w-1,ry+21,selected?0xe51c3b30:0x70203129);
            g.fill(x+1,ry,x+3,ry+21,hurt?0xffd77f82:selected?JADE:0xff365044);
            int kind=Math.max(0,Math.min(3,row.getInt("kind")));
            LichHudIcons.unit(g,kind,x+8,ry+4);
            String hp=Integer.toString(Math.max(0,(int)Math.ceil(row.getFloat("hp"))));
            int hpColor=hurt?0xffe99a9d:0xffd9e9dd;
            small(g,font,hp,x+28,ry+2,hpColor);
            LichHudIcons.heart(g,x+31+smallWidth(font,hp),ry+2,hpColor);
            String distance=Math.max(0,row.getInt("distance"))+"м";
            small(g,font,distance,x+w-7-smallWidth(font,distance),ry+2,MUTED);
            String order=(row.getBoolean("unloaded")?"Сон · ":"")+row.getString("order");
            // Leave room for the discovery mark without widening the list.
            order=font.plainSubstrByWidth(order,112);
            small(g,font,order,x+28,ry+12,selected?JADE:MUTED);
            if(row.getBoolean("detect")) {
                g.fill(x+w-9,ry+13,x+w-4,ry+16,0xff437767);
                g.fill(x+w-7,ry+12,x+w-6,ry+17,JADE);
            }
        }
        if(layout.footer()) small(g,font,"Ещё "+(rows.size()-layout.rows())+" на странице",x+5,y+layout.height()-9,MUTED);
    }
    private static int smallWidth(Font font,String text) { return (int)Math.ceil(font.width(text)*.8f); }
    private static void small(GuiGraphics g,Font font,String text,int x,int y,int color) {
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.8f,.8f,1);
        g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }

    public static void phylactery(GuiGraphics g,int x,int barY,int width,int height) {
        if(!LichClient.active())return;
        int y=LichHudLayout.phylacteryY(barY,height);
        int hp=LichHudLayout.health(LichClient.army.getInt("hp"));
        boolean placed=LichClient.army.getBoolean("placed");
        g.fill(x+1,y,x+width-1,y+3,0xff25102f);
        for(int i=0;i<10;i++) {
            int left=LichHudLayout.segmentStart(x,width,i),right=LichHudLayout.segmentEnd(x,width,i);
            g.fill(left,y+1,right,y+3,i<hp?(placed?0xffbd64ff:0xffa75ce7):0xff462052);
            if(i<hp)g.fill(left,y+1,right,y+2,placed?0xffe2a5ff:0xffce91fa);
        }
    }
}
