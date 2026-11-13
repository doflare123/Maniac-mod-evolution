package org.example.maniacrevolution.lich;

/** One opening burst per encounter; visibility flicker and order changes cannot refresh it. */
final class LichAggroBurst {
    private boolean engaged;
    private long until,lastCombat;
    void tick(long now,boolean pursuing) {
        if(pursuing)lastCombat=now;
        else if(engaged&&now-lastCombat>=100)engaged=false;
    }
    void see(long now) {
        if(!engaged) {engaged=true;until=now+60;}
        lastCombat=now;
    }
    double multiplier(long now) {return engaged&&now<until?1.25:1;}
}
