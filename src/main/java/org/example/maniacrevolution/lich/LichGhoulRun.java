package org.example.maniacrevolution.lich;

/** Transient momentum: standing still cannot charge a sprint, and turns spend it. */
public final class LichGhoulRun {
    public static final double MIN=.8,MAX=1.5;
    public static final int RUN_UP_TICKS=60;
    private double multiplier=MIN;
    public double multiplier() {return multiplier;}
    public float turnLimit() {return (float)(12-4*(multiplier-MIN)/(MAX-MIN));}
    public void reset() {multiplier=MIN;}
    public double step(boolean advancing,boolean grounded,double turn) {
        if(!advancing) {reset();return multiplier;}
        double acceleration=grounded?(MAX-MIN)/RUN_UP_TICKS:0;
        multiplier=Math.max(MIN,Math.min(MAX,multiplier+acceleration-Math.abs(turn)*.008));
        return multiplier;
    }
}
