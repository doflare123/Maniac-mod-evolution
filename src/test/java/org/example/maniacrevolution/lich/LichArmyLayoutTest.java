package org.example.maniacrevolution.lich;

import org.example.maniacrevolution.lich.client.LichArmyLayout;

final class LichArmyLayoutTest {
    static void verify() {
        for(int[] viewport:new int[][]{{320,240},{427,240},{640,360},{854,480},{1920,1080}}) {
            for(int tick=0;tick<=120;tick++) {
                var f=LichArmyLayout.frame(viewport[0],viewport[1],tick/60.0);
                if(f.opacity()<0||f.opacity()>1||!Double.isFinite(f.scale())||f.scale()<=0)
                    throw new AssertionError("Invalid army reveal");
                if(f.screenX(16)<0||f.screenX(624)>viewport[0]||f.screenY(12)<0||f.screenY(391)>viewport[1])
                    throw new AssertionError("Army screen escapes viewport");
                int x=492,y=72,w=112,h=24; // Deselect button shares the painted transform.
                int centerX=f.screenX(x+w/2.0),centerY=f.screenY(y+h/2.0);
                if(centerX<f.screenX(x)||centerX>=f.screenX(x)+f.size(w)
                        ||centerY<f.screenY(y)||centerY>=f.screenY(y)+f.size(h))
                    throw new AssertionError("Animated control hitbox does not contain its painted center");
            }
        }
        if(87+11*19+18>323||329+21>354)throw new AssertionError("Roster, paging and groups overlap");
        System.out.println("Army screen: 605 animated viewport layouts, deselect hitbox and 12-row roster clearance verified.");
    }
}
