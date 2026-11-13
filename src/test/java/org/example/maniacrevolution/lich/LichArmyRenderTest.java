package org.example.maniacrevolution.lich;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.example.maniacrevolution.lich.client.LichArmyScreen;
import org.joml.Matrix4f;

/** Runs the real screen and every button renderer; only GPU drawing and glyph metrics are replaced. */
final class LichArmyRenderTest {
    private static final class HeadlessGraphics extends GuiGraphics {
        int draws;
        HeadlessGraphics() {super(null,null);}
        @Override public void fill(int x1,int y1,int x2,int y2,int color) {draws++;}
        @Override public void fillGradient(int x1,int y1,int x2,int y2,int a,int b) {draws++;}
        @Override public int drawString(Font font,String text,int x,int y,int color,boolean shadow) {draws++;return 0;}
        @Override public int drawString(Font font,Component text,int x,int y,int color,boolean shadow) {draws++;return 0;}
        @Override public void drawCenteredString(Font font,String text,int x,int y,int color) {draws++;}
    }
    static void verify() throws Exception {
        Font font=new Font(id->null,false) {
            @Override public int width(String text) {return text.length()*6;}
            @Override public int width(FormattedText text) {return text.getString().length()*6;}
            @Override public String plainSubstrByWidth(String text,int width) {return text.substring(0,Math.min(text.length(),Math.max(0,width/6)));}
        };
        var fontField=Screen.class.getDeclaredField("font");fontField.setAccessible(true);
        var opened=LichArmyScreen.class.getDeclaredField("opened");opened.setAccessible(true);
        var init=LichArmyScreen.class.getDeclaredMethod("init");init.setAccessible(true);
        int frames=0;
        for(int count:new int[]{0,1,12}) {
            var tag=new CompoundTag();tag.putString("mode","army");tag.putInt("hp",9);tag.putInt("total",count);tag.putInt("selected",count);tag.putInt("pages",2);
            var rows=new ListTag();
            for(int index=0;index<count;index++) {
                var row=new CompoundTag();row.putInt("kind",index%4);row.putString("name",LichRules.Kind.values()[index%4].title);
                row.putFloat("hp",20);row.putInt("distance",15);row.putString("order","Следовать");
                row.putBoolean("selected",index%2==0);row.putBoolean("hurt",index%3==0);
                row.putBoolean("detect",index%4==0);row.putBoolean("attack",true);row.putBoolean("unloaded",index%5==0);rows.add(row);
            }
            tag.put("rows",rows);
            var screen=new LichArmyScreen(tag);fontField.set(screen,font);screen.width=854;screen.height=459;
            opened.setLong(screen,System.nanoTime()-2_000_000_000L);init.invoke(screen);
            var g=new HeadlessGraphics();g.pose().pushPose();g.pose().translate(.25,.5,.75);
            Matrix4f parent=new Matrix4f(g.pose().last().pose());
            for(int frame=0;frame<4;frame++) {
                if(frame==1)screen.update(tag);
                if(frame==2) {screen.width=320;screen.height=240;init.invoke(screen);}
                screen.render(g,-1000,-1000,0);
                if(!parent.equals(g.pose().last().pose()))throw new AssertionError("Army UI changed the parent transform");
                frames++;
            }
            if(g.draws<100)throw new AssertionError("Screen/button drawing was skipped");
            g.pose().popPose();
            if(!g.pose().clear())throw new AssertionError("Army render leaked stack entries");
            g.pose().pushPose();g.pose().popPose(); // Base entry must still exist for the next overlay.
        }
        System.out.println("Army render: "+frames+" real screen frames, 0/1/12 units, snapshots and resize preserve PoseStack.");
    }
}
