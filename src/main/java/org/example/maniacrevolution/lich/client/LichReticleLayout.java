package org.example.maniacrevolution.lich.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LichReticleLayout {
    public static final int JADE=0xff8cddbf,GOLD=0xffdec17a,RED=0xffff878d,MUTED=0xff79978c;
    public record Pixel(int x,int y,int width,int height,int color) {}
    public record Plate(int x,int y,int width,int height) {}
    public static Plate caption(int textWidth) {int width=textWidth+24;return new Plate(-width/2,16,width,12);}
    public static Plate counter(int textWidth) {return new Plate(11,-3,textWidth+8,10);}
    public static Plate phylacteryHint(int textWidth) {int width=textWidth+24;return new Plate(-width/2,16,width,24);}
    public static String cooldownText(int ticks) {
        int tenths=(Math.max(0,Math.min(40,ticks))+1)/2;
        return ticks<=0?"":"ЛКМ · "+tenths/10+","+tenths%10+" с";
    }
    public static List<Pixel> cooldown(int ticks) {
        if(ticks<=0)return List.of();
        int width=(int)Math.ceil(Math.min(40,ticks)/2.0);
        return List.of(new Pixel(-11,-18,22,4,0xc5071210),new Pixel(-10,-17,20,2,0xff304a40),
                new Pixel(-10,-17,width,2,JADE));
    }
    private static final List<List<String>> ORDERS=List.of(
            List.of("...#...","..###..",".#.#.#.","...#...","...#...","...#...","......."),
            List.of(".......",".##.##.",".##.##.",".##.##.",".##.##.",".##.##.","......."),
            List.of(".#####.",".#...#.",".#.#.#.",".#.#.#.","..#.#..","...#...","......."),
            List.of("..###..",".#...#.","#.....#","#.....#","#.....#",".#...#.","..###.."),
            List.of(".......","..###..",".#...#.","#..#..#",".#...#.","..###..","......."),
            List.of("...#...",".#...#.","..#.#..","#.....#","..#.#..",".#...#.","...#..."),
            List.of("...#...","...#...","...#...",".#.#.#.","..###..","...#...","......."),
            List.of(".......","....#..",".....#.",".######",".....#.","....#..",".......")
    );
    private LichReticleLayout() {}
    public static List<String> orderMark(int order) {return ORDERS.get(order);}
    public static double rayDistance(Vec3 eye,Vec3 look,AABB bounds) {
        double length=eye.distanceTo(bounds.getCenter())+3;
        return bounds.inflate(.15).clip(eye,eye.add(look.scale(length))).map(eye::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
    }
    public static List<Pixel> build(boolean book,int color,boolean focus,boolean progress,float fraction,double ticks) {
        var ink=new ArrayList<Pixel>();
        int bone=focus?0xffebdfbd:0xffb7b29e,eye=focus?color:JADE;
        // One exact aiming pixel, with open space around it in both silhouettes.
        ink.add(new Pixel(0,0,1,1,eye));
        if(book) {
            // Two sparse page strokes, jade covers and a gold binding: a tiny open grimoire.
            for(int side:new int[]{-1,1}) {
                for(int i=1;i<=7;i++) {
                    int y=6-(i+1)/2;
                    ink.add(new Pixel(side*i,y,1,1,bone));
                    ink.add(new Pixel(side*i,y+2,1,1,0xff44685b));
                }
            }
            ink.add(new Pixel(0,6,1,1,GOLD));
        } else {
            // The chosen bone sign: detached brows and a jaw, only 11 x 10 pixels of ink.
            for(int side:new int[]{-1,1}) {
                for(int i=0;i<4;i++)ink.add(new Pixel(side*(5-i),-4+i,1,1,bone));
                ink.add(new Pixel(side*2,3,1,1,bone));
                ink.add(new Pixel(side,4,1,1,bone));
            }
            ink.add(new Pixel(0,4,1,2,bone));
        }
        var pixels=new ArrayList<Pixel>();
        // Draw every shadow before the glyph, so neighbouring pixels stay crisp.
        for(var p:ink)pixels.add(new Pixel(p.x-1,p.y-1,p.width+2,p.height+2,0xc5071210));
        pixels.addAll(ink);
        if(progress)pixels.addAll(ring(color,fraction));
        return pixels;
    }
    public static List<Pixel> ring(int color,float fraction) {
        var pixels=new ArrayList<Pixel>();
        float amount=Float.isFinite(fraction)?Math.max(0,Math.min(1,fraction)):0;
        for(int i=0;i<48;i++) {
            double angle=-Math.PI/2+i*Math.PI*2/48;
            boolean filled=i<Math.floor(amount*48);
            pixels.add(new Pixel((int)Math.round(Math.cos(angle)*13),(int)Math.round(Math.sin(angle)*13),1,1,filled?color:0x7056776c));
        }
        return pixels;
    }
}
