package org.example.maniacrevolution.lich.client;

/** Pixel layout shared by the renderer and headless bounds checks. */
public final class LichHudLayout {
    public static final int PANEL_WIDTH=132, ROW_HEIGHT=22, HEADER_HEIGHT=16;
    private LichHudLayout() {}
    public record Panel(int x,int y,int width,int height,int rows,boolean footer) {}
    public static Panel panel(int screenWidth,int screenHeight,int rows) {
        return panel(screenWidth,screenHeight,rows,53,screenHeight-92);
    }
    public static Panel panel(int screenWidth,int screenHeight,int rows,int reservedTop,int bottom) {
        int top=Math.max(0,Math.min(screenHeight,reservedTop));
        int available=Math.max(0,Math.min(screenHeight,bottom)-top);
        int x=Math.max(0,screenWidth-PANEL_WIDTH-8);
        if(available<HEADER_HEIGHT||screenWidth<PANEL_WIDTH+16)return new Panel(x,top,0,0,0,false);
        rows=Math.max(0,rows);
        int visible=Math.min(rows,(available-HEADER_HEIGHT)/ROW_HEIGHT);
        boolean footer=visible<rows&&available>=HEADER_HEIGHT+12;
        if(footer)visible=Math.min(visible,(available-HEADER_HEIGHT-12)/ROW_HEIGHT);
        return new Panel(x,top,PANEL_WIDTH,HEADER_HEIGHT+visible*ROW_HEIGHT+(footer?12:0),visible,footer);
    }
    public static int segmentStart(int x,int width,int index) { return x+1+index*(width-2)/10; }
    public static int segmentEnd(int x,int width,int index) { return x+1+(index+1)*(width-2)/10-(index<9?1:0); }
    public static int phylacteryY(int barY,int barHeight) { return barY+barHeight+1; }
    public static int health(int hp) { return Math.max(0,Math.min(10,hp)); }
}
