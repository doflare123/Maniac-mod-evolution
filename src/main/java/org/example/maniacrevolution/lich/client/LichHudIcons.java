package org.example.maniacrevolution.lich.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** Flat, integer-aligned HUD glyphs. No texture downsampling or font glyphs. */
public final class LichHudIcons {
    private static final List<List<String>> UNITS=List.of(
        List.of("............", "..########..", ".##########.", ".##########.",
                ".##++##++##.", ".##########.", ".##########.", "..##....##..",
                "..########..", "...######...", "...##..##...", "............"),
        List.of("...######...", "..########..", "..########..", "..#++##++#..",
                "..#..##..#..", "..########..", "...##..##...", "...######...",
                "...#.#..#...", "....####....", ".....##.....", "............"),
        List.of("............", "#..######..#", "##.######.##", ".##########.",
                "..#++##++#..", "..########..", "...#....#...", "...##..##...",
                "...##..##...", "....#..#....", ".....##.....", "............"),
        List.of("..########..", ".##########.", "######+#####", "######+#####",
                "##++####++##", "############", "###......###", "####.##.####",
                ".##########.", "..########..", "...######...", "............")
    );
    private static final List<String> HEART=List.of(".##.##.","#######","#######",".#####.","..###..","...#...");
    private static final int[] BODY={0xff99b899,0xffd8dfcd,0xff73b9ad,0xffb3a9c3};
    public static List<String> unitPixels(int kind) { return UNITS.get(Math.max(0,Math.min(3,kind))); }
    public static List<String> heartPixels() { return HEART; }
    private LichHudIcons() {}
    public static void unit(GuiGraphics g,int kind,int x,int y) {
        unit(g,kind,x,y,1);
    }
    public static void unit(GuiGraphics g,int kind,int x,int y,float opacity) {
        int index=Math.max(0,Math.min(3,kind));
        draw(g,unitPixels(index),x,y,fade(BODY[index],opacity),fade(index==3?0xffe2a2ff:0xff90edc8,opacity));
    }
    public static void heart(GuiGraphics g,int x,int y,int color) { draw(g,HEART,x,y,color,color); }
    private static int fade(int color,float opacity) { return (color&0xffffff)|(Math.round((color>>>24)*opacity)<<24); }
    private static void draw(GuiGraphics g,List<String> pixels,int x,int y,int body,int accent) {
        for(int row=0;row<pixels.size();row++) {
            String line=pixels.get(row);
            for(int column=0;column<line.length();) {
                char pixel=line.charAt(column);int end=column+1;
                while(end<line.length()&&line.charAt(end)==pixel)end++;
                if(pixel!='.')g.fill(x+column,y+row,x+end,y+row+1,pixel=='+'?accent:body);
                column=end;
            }
        }
    }
}
