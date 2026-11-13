package org.example.maniacrevolution.lich;

import net.minecraft.world.phys.Vec3;

/** Low arc matching the vanilla arrow's air drag/gravity; spread is applied afterwards. */
final class LichArrowAim {
    static final float SPEED=1.6f;
    private LichArrowAim() {}
    static Vec3 direction(Vec3 delta) {
        double distance=delta.horizontalDistance();if(distance<.1)return delta;
        double low=-Math.PI/3,high=Math.PI/4;
        for(int iteration=0;iteration<18;iteration++) {
            double angle=(low+high)/2;
            if(height(distance,angle)<delta.y)low=angle;else high=angle;
        }
        return new Vec3(delta.x,Math.tan((low+high)/2)*distance,delta.z);
    }
    private static double height(double distance,double angle) {
        double x=0,y=0,vx=Math.cos(angle)*SPEED,vy=Math.sin(angle)*SPEED;
        for(int tick=0;tick<100;tick++) {
            if(x+vx>=distance)return y+vy*(distance-x)/vx;
            x+=vx;y+=vy;vx*=.99;vy=vy*.99-.05;
        }
        return -Double.MAX_VALUE;
    }
}
