package org.example.maniacrevolution.lich;

import java.util.HashSet;
import org.example.maniacrevolution.lich.client.LichHudLayout;
import org.example.maniacrevolution.lich.client.LichHudIcons;
import org.example.maniacrevolution.hud.RightHudLayout;

final class LichHudLayoutTest {
    static void verify() throws Exception {
        int checked=0;
        for(int[] physical:new int[][]{{1280,720},{1920,1080},{2560,1440},{3440,1440},{320,240},{220,160}})
            for(int scale=1;scale<=6;scale++) {
                int width=(physical[0]+scale-1)/scale,height=(physical[1]+scale-1)/scale;
                for(boolean computers:new boolean[]{false,true})for(boolean generator:new boolean[]{false,true})
                    for(int reserved:new int[]{97,123,148,111,137,162})for(int rows=0;rows<=12;rows++) {
                        int trackerBottom=RightHudLayout.bottom(computers,generator),top=Math.max(30,trackerBottom+6);
                        var panel=LichHudLayout.panel(width,height,rows,top,height-reserved);checked++;
                        if(panel.height()==0)continue;
                        if(panel.x()<0||panel.x()+panel.width()>width||panel.y()<trackerBottom+6
                                ||panel.y()+panel.height()>height-reserved)
                            throw new AssertionError("Army overlaps trackers or bottom HUD at "+width+"x"+height);
                        int contents=LichHudLayout.HEADER_HEIGHT+panel.rows()*LichHudLayout.ROW_HEIGHT+(panel.footer()?12:0);
                        if(panel.height()!=contents||panel.rows()<0||panel.rows()>rows)
                            throw new AssertionError("Army rows/footer escape reserved rectangle");
                        if(panel.footer()&&panel.rows()>=rows)throw new AssertionError("Unneeded overflow footer");
                    }
            }
        if(RightHudLayout.generatorY(true)<RightHudLayout.TOP+org.example.maniacrevolution.hud.ComputerHackHud.HEIGHT+4)
            throw new AssertionError("Computer and generator stack intersects");
        for(int[] viewport:new int[][]{{320,240},{427,240},{640,360},{854,480},{1920,1080}}) {
            for(int rows=0;rows<=12;rows++) {
                var p=LichHudLayout.panel(viewport[0],viewport[1],rows);
                if(p.x()<0||p.x()+p.width()>viewport[0]||p.y()+p.height()>viewport[1]-92)
                    throw new AssertionError("Army list escapes safe viewport");
                if(p.rows()<0||p.rows()>rows)throw new AssertionError("Invalid visible row count");
                if(rows<=2&&p.rows()!=rows)throw new AssertionError("Small army hidden");
            }
        }
        for(int width:new int[]{100,120,140}) {
            int previous=0;
            for(int i=0;i<10;i++) {
                int left=LichHudLayout.segmentStart(0,width,i),right=LichHudLayout.segmentEnd(0,width,i);
                if(left<=previous||right<=left||right>=width)throw new AssertionError("Invalid phylactery segment");
                previous=right;
            }
            if(LichHudLayout.segmentStart(0,width,0)!=1||LichHudLayout.segmentEnd(0,width,9)!=width-1)
                throw new AssertionError("Phylactery must span the full HP bar interior");
        }
        int strip=LichHudLayout.phylacteryY(0,14);
        if(strip!=15||9+strip+3>=32)throw new AssertionError("Phylactery must sit below HP with clearance above the hotbar");
        for(int hp=-2;hp<=12;hp++) {
            int filled=0;for(int i=0;i<10;i++)if(i<LichHudLayout.health(hp))filled++;
            if(filled!=Math.max(0,Math.min(10,hp)))throw new AssertionError("Phylactery HP segment count mismatch");
        }
        var silhouettes=new HashSet<String>();
        for(int kind=0;kind<4;kind++) {
            var icon=LichHudIcons.unitPixels(kind);
            if(icon.size()!=12)throw new AssertionError("Minimal unit glyph must be 12 pixels tall");
            for(var row:icon)if(row.length()!=12||!row.matches("[.#+]+"))throw new AssertionError("Malformed unit glyph "+kind+": "+row);
            silhouettes.add(String.join("",icon).replace('+','#'));
        }
        if(silhouettes.size()!=4)throw new AssertionError("Unit silhouettes must differ independently of color");
        for(var row:LichHudIcons.heartPixels()) {
            if(row.length()!=7||!row.contentEquals(new StringBuilder(row).reverse()))throw new AssertionError("Heart must be symmetric and pixel aligned");
        }
        var abilityIcon=org.example.maniacrevolution.lich.client.LichArmyHud.INSTANCE.getAbilityIcon();
        try(var stream=LichHudLayoutTest.class.getResourceAsStream("/assets/maniacrev/textures/mob_effect/lich_phylactery_guard.png")) {
            if(stream==null)throw new AssertionError("Phylactery guard status icon missing");
            var icon=javax.imageio.ImageIO.read(stream);
            if(icon==null||icon.getWidth()!=16||icon.getHeight()!=16||(icon.getRGB(0,0)>>>24)!=0)
                throw new AssertionError("Phylactery guard icon must be transparent 16x16 pixel art");
        }
        try(var stream=LichHudLayoutTest.class.getResourceAsStream("/assets/"+abilityIcon.getNamespace()+"/"+abilityIcon.getPath())) {
            if(stream==null)throw new AssertionError("Army ability icon is missing");
            var image=javax.imageio.ImageIO.read(stream);
            if(image==null||image.getWidth()<24||image.getWidth()!=image.getHeight())
                throw new AssertionError("Army ability icon must be square full-slot artwork");
            if((image.getRGB(0,0)>>>24)!=255)throw new AssertionError("Army ability artwork must have its own full-bleed background");
        }
        System.out.println("Lich HUD: "+checked+" viewport/scale/tracker/effect/chat/row combinations, full-width segments below HP, hotbar clearance, four unique 12x12 glyphs and symmetric heart verified.");
    }
}
