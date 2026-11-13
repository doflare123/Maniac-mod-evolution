package org.example.maniacrevolution.lich.client;

public final class LichArmyLayout {
    public static final int WIDTH=640,HEIGHT=400;
    private LichArmyLayout() {}
    public record Frame(double scale,double x,double y,float opacity) {
        public int screenX(double local) { return (int)Math.round(x+local*scale); }
        public int screenY(double local) { return (int)Math.round(y+local*scale); }
        public int size(double local) { return Math.max(1,(int)Math.round(local*scale)); }
    }
    public static Frame frame(int width,int height,double seconds) {
        double fit=Math.max(.01,Math.min(1.25,Math.min((width-24.0)/WIDTH,(height-16.0)/HEIGHT)));
        var motion=LichCorpseLayout.reveal(seconds,0,14);
        double scale=fit*motion.scale();
        return new Frame(scale,(width-WIDTH*scale)/2,(height-HEIGHT*scale)/2+motion.offsetY()*fit,motion.opacity());
    }
}
