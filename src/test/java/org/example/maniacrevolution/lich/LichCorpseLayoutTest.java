package org.example.maniacrevolution.lich;

import org.example.maniacrevolution.lich.client.LichCorpseLayout;

final class LichCorpseLayoutTest {
    static void verify() {
        double maximum = 0;
        for (int frame=0; frame<=240; frame++) {
            double t=frame/120.0;
            maximum=Math.max(maximum,LichCorpseLayout.spring(t));
            for (int i=0;i<4;i++) {
                double x=LichCorpseLayout.centerX(i,t), scale=LichCorpseLayout.cardScale(i,t);
                if(!Double.isFinite(x)||scale<=0||scale>1.1)throw new AssertionError("Invalid animated card bounds");
                if(x-66*scale<0||x+66*scale>620)throw new AssertionError("Card escapes canvas during overshoot");
            }
        }
        if(maximum<=1.01||maximum>1.08)throw new AssertionError("Expected restrained spring overshoot: "+maximum);
        for(int i=0;i<4;i++) {
            if(Math.abs(LichCorpseLayout.centerX(i,0)-310)>.001)throw new AssertionError("Cards must originate in center");
            double x=LichCorpseLayout.centerX(i,3);
            if(Math.abs(x-(310+(i-1.5)*144))>.001)throw new AssertionError("Cards must settle in row");
            if(i>0&&x-LichCorpseLayout.centerX(i-1,3)<132)throw new AssertionError("Settled cards overlap");
        }
        for(int[] viewport:new int[][]{{320,240},{427,240},{640,360},{854,480},{1920,1080}}) {
            double scale=LichCorpseLayout.scale(viewport[0],viewport[1]);
            if(620*scale>viewport[0]-23||370*scale>viewport[1]-15)throw new AssertionError("Responsive canvas overflow");
        }
        for(double delay:new double[]{0,.02,.04,.12,.16,.22})for(double travel:new double[]{-18,18}) {
            float previous=0;
            for(int frame=0;frame<=240;frame++) {
                double t=frame/120.0;
                var reveal=LichCorpseLayout.reveal(t,delay,travel);
                if(reveal.opacity()<previous||reveal.opacity()>1||!Double.isFinite(reveal.offsetY())
                        ||reveal.scale()<.82||reveal.scale()>1.02)throw new AssertionError("Invalid screen reveal");
                if(t<=delay&&reveal.opacity()!=0)throw new AssertionError("Element visible before its reveal");
                previous=reveal.opacity();
            }
            var settled=LichCorpseLayout.reveal(3,delay,travel);
            if(settled.opacity()!=1||Math.abs(settled.scale()-1)>.0001||Math.abs(settled.offsetY())>.001)
                throw new AssertionError("Screen element did not settle at its original layout");
        }
        System.out.println("Corpse UI: 964 animation samples, center origin, overshoot, settled spacing and five viewport sizes verified.");
        System.out.println("Screen reveal: 2892 samples, staggered opacity, finite spring transforms and original settled layout verified.");
    }
}
