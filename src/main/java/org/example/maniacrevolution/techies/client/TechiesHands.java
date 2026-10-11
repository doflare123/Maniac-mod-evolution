package org.example.maniacrevolution.techies.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.techies.*;

/** Supplied art-direction tracks applied additively to the ordinary skin arms. */
public final class TechiesHands {
    public static final class Pose {
        public int type=-1, recoverType=-1;
        public double start=-1, press=-100, recovery=-100, recoveryElapsed;
    }
    private static final Map<UUID,Pose> POSES=new HashMap<>();
    private static JsonObject channels;
    private TechiesHands() {}
    public static void clear() { POSES.clear(); }
    public static void accept(TechiesPosePacket packet) {
        TechiesClient.ensureConnection();
        if(packet.type() < -1 || packet.type()>2) return;
        var mc=Minecraft.getInstance(); if(mc.level==null) return;
        Pose pose=POSES.computeIfAbsent(packet.player(),k->new Pose());
        double local=mc.level.getGameTime()+mc.getFrameTime();
        if(pose.start>=0 && packet.start()<0) {
            pose.recoverType=pose.type; pose.recovery=local; pose.recoveryElapsed=(local-pose.start)/20;
        }
        pose.type=packet.type(); pose.start=packet.start()<0?-1:local-(packet.tick()-packet.start());
        pose.press=local-(packet.tick()-packet.press());
    }
    public static Pose pose(AbstractClientPlayer player) {
        var mc=Minecraft.getInstance();
        if(player!=mc.player && mc.level!=null && mc.level.getPlayerByUUID(player.getUUID())==mc.player) return null;
        if(player==mc.player) {
            Pose p=new Pose(); p.type=TechiesClient.selected;
            p.start=TechiesClient.installStart<0?-1:mc.level.getGameTime()+mc.getFrameTime()-(TechiesClient.time()-TechiesClient.installStart);
            var remote=POSES.get(player.getUUID()); if(remote!=null) { p.press=remote.press; p.recovery=remote.recovery; p.recoverType=remote.recoverType; p.recoveryElapsed=remote.recoveryElapsed; }
            return p;
        }
        return POSES.get(player.getUUID());
    }
    public static double clock() { var mc=Minecraft.getInstance(); return mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime(); }
    public static boolean displays(Pose p) { return p!=null && (p.type>=0 || clock()-p.recovery<3); }
    public static int type(Pose p) { return p.type>=0?p.type:p.recoverType; }
    public static float weight(Pose p) { return p.type>=0?1:(float)Math.max(0,1-(clock()-p.recovery)/3); }
    public static double elapsed(Pose p) { return p.start>=0?(clock()-p.start)/20:p.recoveryElapsed; }
    private static JsonObject channels() {
        if(channels==null) try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Maniacrev.loc("techies/techies_hands.motion.json"))) {
            channels=JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("channels");
        } catch(Exception e) { Maniacrev.LOGGER.warn("Cannot read Techies hand tracks",e); channels=new JsonObject(); }
        return channels;
    }
    public static float[] sample(Pose p,String field) {
        if(p==null || type(p)<0 || p.start<0 && p.type>=0) return new float[3];
        var channel=channels().getAsJsonObject("install_"+MineType.values()[type(p)].color); if(channel==null) return new float[3];
        double t=elapsed(p); JsonObject a=null,b=null; double ta=-Double.MAX_VALUE,tb=Double.MAX_VALUE;
        for(var key:channel.getAsJsonArray("keys")) {
            var frame=key.getAsJsonObject(); double ft=frame.get("time").getAsDouble();
            if(ft<=t && ft>ta) { a=frame; ta=ft; } if(ft>=t && ft<tb) { b=frame; tb=ft; }
        }
        if(a==null) a=b; if(b==null) b=a; if(a==null || !a.has(field)) return new float[3];
        double factor=tb>ta && ta>-Double.MAX_VALUE && tb<Double.MAX_VALUE?(t-ta)/(tb-ta):0;
        float[] result=new float[3];
        for(int i=0;i<3;i++) result[i]=(float)(a.getAsJsonArray(field).get(i).getAsDouble()+(b.getAsJsonArray(field).get(i).getAsDouble()-a.getAsJsonArray(field).get(i).getAsDouble())*factor)*weight(p);
        return result;
    }
}
