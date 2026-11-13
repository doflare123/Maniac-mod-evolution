package org.example.maniacrevolution.lich;

import java.util.HashSet;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.lich.client.LichReticleLayout;
import org.example.maniacrevolution.lich.client.LichRitualProgress;

final class LichReticleTest {
    private static CompoundTag packet(String state,long serial,long start,long finish) {
        var tag=new CompoundTag();tag.putString("mode","cast");tag.putString("state",state);tag.putString("action","raise");
        tag.putLong("serial",serial);tag.putLong("start",start);tag.putLong("finish",finish);tag.putInt("kind",3);tag.putInt("entity",17);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try {LichSnapshot.encode(new LichSnapshot(tag),buffer);return LichSnapshot.decode(buffer).data();}finally{buffer.release();}
    }
    static void verify() throws Exception {
        for(int width:new int[]{30,48,70}) {
            var hint=LichReticleLayout.phylacteryHint(width);
            if(Math.abs(hint.x()+hint.width()/2)>1||hint.y()<15||hint.height()!=24||hint.width()<width+24)
                throw new AssertionError("Phylactery actions must fit two compact rows below the reticle");
        }
        if(!LichReticleLayout.cooldown(0).isEmpty()||!LichReticleLayout.cooldownText(0).isEmpty())
            throw new AssertionError("Ready attack should not clutter the reticle");
        for(int ticks=1;ticks<=40;ticks++) {
            var bar=LichReticleLayout.cooldown(ticks);
            if(bar.size()!=3||bar.get(2).width()!=(ticks+1)/2||bar.stream().anyMatch(p->p.y()+p.height()>-14||Math.abs(p.x())>11))
                throw new AssertionError("Cooldown bar overlaps crown or has wrong remaining duration");
        }
        if(!LichReticleLayout.cooldownText(40).equals("ЛКМ · 2,0 с")||!LichReticleLayout.cooldownText(1).equals("ЛКМ · 0,1 с"))
            throw new AssertionError("Cooldown timer must show tenths of a second");
        var progress=new LichRitualProgress();
        if(progress.frame(0).visible()||!Float.isFinite(progress.frame(0).progress()))throw new AssertionError("Idle progress invalid");
        progress.accept(packet("casting",1,100,110));
        if(progress.frame(100).progress()!=0||Math.abs(progress.frame(105).progress()-.5f)>.001)throw new AssertionError("Cast does not follow server clock");
        if(progress.frame(115).progress()>=1||!progress.frame(115).casting())throw new AssertionError("Client invented cast success without server result");
        progress.accept(packet("cancelled",1,106,122));
        if(!progress.frame(107).failed())throw new AssertionError("Cancellation missing");
        progress.accept(packet("casting",1,100,110));
        if(!progress.frame(108).failed())throw new AssertionError("Stale start revived cancelled ritual");
        progress.accept(packet("casting",2,120,130));progress.accept(packet("emerge",2,130,230));
        if(Math.abs(progress.frame(180).progress()-.5f)>.001||progress.frame(180).entity()!=17)throw new AssertionError("Five-second emergence timing invalid");
        if(!progress.frame(230).state().equals("ready")||progress.frame(250).visible())throw new AssertionError("Emergence did not finish/expire");
        if(progress.frame(180).showAtReticle()||progress.frame(230).showAtReticle())throw new AssertionError("Post-cast emergence must not cover the reticle");
        progress.accept(packet("casting",1,100,110));
        if(progress.frame(180).casting())throw new AssertionError("Older ritual overwrote newer one");
        progress.lostEmergingUnit(181,true);
        if(!progress.frame(182).failed())throw new AssertionError("Death during emergence not shown");
        progress.clear();progress.accept(packet("complete",1,100,116));
        if(progress.frame(105).progress()!=1)throw new AssertionError("Confirmed success not displayed");
        progress.clear();if(progress.frame(105).visible())throw new AssertionError("Progress survives session clear");
        for(int percent=0;percent<=100;percent++) {
            var pixels=LichReticleLayout.build(true,LichReticleLayout.JADE,true,true,percent/100f,percent);
            long filled=LichReticleLayout.ring(LichReticleLayout.JADE,percent/100f).stream().filter(p->p.color()==LichReticleLayout.JADE).count();
            if(Math.abs(filled-Math.floor((percent/100f)*48))>1)throw new AssertionError("Ring progress coverage invalid");
            for(var p:pixels)if(p.width()<=0||p.height()<=0||Math.abs(p.x())>15||Math.abs(p.y())>14)throw new AssertionError("Compact reticle pixel outside bounds");
        }
        for(int width:new int[]{6,18,48,188}) {
            var caption=LichReticleLayout.caption(width);var counter=LichReticleLayout.counter(width);
            if(Math.abs(caption.x()+caption.width()/2)>1||caption.y()!=16||caption.height()>12||caption.width()>220)
                throw new AssertionError("Context caption is detached or not centered");
            if(counter.x()!=11||counter.y()!=-3||counter.height()>10||counter.width()<width+8)throw new AssertionError("Selection badge overlaps grimoire");
        }
        for(boolean book:new boolean[]{false,true}) {
            var glyph=LichReticleLayout.build(book,LichReticleLayout.JADE,false,false,0,0);
            if(glyph.stream().anyMatch(p->p.x()<-8||p.x()+p.width()>9||p.y()<-5||p.y()+p.height()>9))
                throw new AssertionError("Idle Lich glyph grew beyond its miniature footprint");
            if(glyph.stream().noneMatch(p->p.color()==0xffb7b29e)||glyph.stream().anyMatch(p->p.color()==0xff9860c9))
                throw new AssertionError("Bone/grimoire identity must not include a phylactery");
            if(glyph.stream().noneMatch(p->p.x()==0&&p.y()==0&&p.width()==1&&p.height()==1&&p.color()==LichReticleLayout.JADE))
                throw new AssertionError("Exact aiming pixel missing");
        }
        if(LichReticleLayout.build(false,LichReticleLayout.JADE,false,false,0,0)
                .equals(LichReticleLayout.build(true,LichReticleLayout.JADE,false,false,0,0)))
            throw new AssertionError("Book must switch the bone sign to the grimoire");
        var marks=new HashSet<String>();
        for(int order=0;order<8;order++) {
            var mark=LichReticleLayout.orderMark(order);
            if(mark.size()!=7||mark.stream().anyMatch(row->row.length()!=7))throw new AssertionError("Order mark malformed");
            marks.add(String.join("",mark));
        }
        if(marks.size()!=8)throw new AssertionError("Every order needs its own reticle mark");
        var eye=new Vec3(0,1,0);var look=new Vec3(0,0,1);
        if(!Double.isFinite(LichReticleLayout.rayDistance(eye,look,new AABB(-.5,0,999,.5,2,1000))))throw new AssertionError("Reticle imposes vanilla targeting range");
        if(Double.isFinite(LichReticleLayout.rayDistance(eye,look,new AABB(4,0,10,5,2,11))))throw new AssertionError("Off-axis entity selected");
        System.out.println("Lich reticle: packet round-trip, cast/emergence/cancel/death clocks, 101 ring states, eight marks and long-range ray verified.");
        preview();
    }
    private static void preview() throws Exception {
        var image=new java.awt.image.BufferedImage(960,600,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.scale(3,3);g.setFont(new java.awt.Font(java.awt.Font.MONOSPACED,java.awt.Font.PLAIN,7));
        String[] names={"bone / light","bone / dark","book / command","book / casting"};
        for(int state=0;state<4;state++) {
            int left=(state%2)*160,top=(state/2)*100,cx=left+80,cy=top+42;
            g.setColor(new java.awt.Color(state%2==0?0xffb7b19b:0xff1d2923,true));g.fillRect(left,top,160,100);
            g.setColor(new java.awt.Color(0xff56645b,true));g.drawString(names[state],left+5,top+10);
            int color=state==2?LichReticleLayout.GOLD:LichReticleLayout.JADE;
            for(var p:LichReticleLayout.build(state>=2,color,state>=2,state==3,.5f,0)) {
                g.setColor(new java.awt.Color(p.color(),true));g.fillRect(cx+p.x(),cy+p.y(),p.width(),p.height());
            }
            if(state>=2) {
                var p=LichReticleLayout.counter(5);
                g.setColor(new java.awt.Color(0xff44685b,true));g.fillRect(cx+8,cy+1,p.x()-8,1);previewPlate(g,p,cx,cy);
                g.setColor(new java.awt.Color(LichReticleLayout.JADE,true));g.drawString("2",cx+p.x()+5,cy+p.y()+8);
            }
            if(state>=2) {
                String text=state==2?"MOVE":"RAISE 50%";
                var p=LichReticleLayout.caption(text.length()*5);previewPlate(g,p,cx,cy);
                g.setColor(new java.awt.Color(0xff3b5e50,true));g.fillRect(cx,cy+10,1,6);
                g.setColor(new java.awt.Color(color,true));g.drawString(state==2?"R":"+",cx+p.x()+4,cy+p.y()+9);
                g.drawString(text,cx+p.x()+20,cy+p.y()+9);
            }
        }
        g.dispose();javax.imageio.ImageIO.write(image,"PNG",new java.io.File("build/lich-reticle-design.png"));
        System.out.println("Lich bone sign and grimoire: miniature footprints, selection badge and CPU design preview verified.");
    }
    private static void previewPlate(java.awt.Graphics2D g,LichReticleLayout.Plate p,int cx,int cy) {
        g.setColor(new java.awt.Color(0xd50a1614,true));g.fillRect(cx+p.x(),cy+p.y(),p.width(),p.height());
        g.setColor(new java.awt.Color(0xff3b5e50,true));g.drawRect(cx+p.x(),cy+p.y(),p.width(),p.height());
    }
}
