package org.example.maniacrevolution.techies.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.model.GeoModel;

/** Evaluate supplied linear Bedrock tracks against authoritative elapsed time, including late sync. */
public final class TechiesAnimation {
    private static final Map<String,JsonObject> FILES=new HashMap<>();
    private TechiesAnimation() {}
    private static JsonObject file(String stem) {
        return FILES.computeIfAbsent(stem,k -> {
            try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(Maniacrev.loc("animations/"+stem+".animation.json"))) {
                return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            } catch(Exception e) { Maniacrev.LOGGER.warn("Cannot load Techies animation {}",stem,e); return new JsonObject(); }
        });
    }
    private static double[] sample(JsonObject frames,double time) {
        double before=-Double.MAX_VALUE,after=Double.MAX_VALUE; JsonArray a=null,b=null;
        for(var entry:frames.entrySet()) {
            double t=Double.parseDouble(entry.getKey()); JsonArray v=entry.getValue().getAsJsonArray();
            if(t<=time && t>before) { before=t; a=v; } if(t>=time && t<after) { after=t; b=v; }
        }
        if(a==null) a=b; if(b==null) b=a; if(a==null) return new double[3];
        double factor=after>before && after<Double.MAX_VALUE && before>-Double.MAX_VALUE?(time-before)/(after-before):0;
        double[] result=new double[3]; for(int i=0;i<3;i++) result[i]=a.get(i).getAsDouble()+(b.get(i).getAsDouble()-a.get(i).getAsDouble())*factor;
        return result;
    }
    public static void apply(GeoModel<?> model,String stem,String clip,double seconds,float weight) {
        // Baked models are shared across world, hand and GUI: always restore their base pose first.
        for(var bone:model.getAnimationProcessor().getRegisteredBones()) {
            var initial=bone.getInitialSnapshot();
            bone.setRotX(initial.getRotX()); bone.setRotY(initial.getRotY()); bone.setRotZ(initial.getRotZ());
            bone.setPosX(0); bone.setPosY(0); bone.setPosZ(0);
        }
        JsonObject anim=file(stem).getAsJsonObject("animation."+stem+"."+clip); if(anim==null || weight<=0) return;
        for(var entry:anim.getAsJsonObject("bones").entrySet()) {
            var bone=model.getAnimationProcessor().getBone(entry.getKey()); if(bone==null) continue;
            JsonObject channels=entry.getValue().getAsJsonObject();
            if(channels.has("position")) {
                double[] v=sample(channels.getAsJsonObject("position"),seconds);
                bone.setPosX((float)-v[0]*weight); bone.setPosY((float)v[1]*weight); bone.setPosZ((float)v[2]*weight);
            }
            if(channels.has("rotation")) {
                double[] v=sample(channels.getAsJsonObject("rotation"),seconds); var initial=bone.getInitialSnapshot();
                bone.setRotX(initial.getRotX()+(float)Math.toRadians(-v[0])*weight);
                bone.setRotY(initial.getRotY()+(float)Math.toRadians(-v[1])*weight);
                bone.setRotZ(initial.getRotZ()+(float)Math.toRadians(v[2])*weight);
            }
        }
    }
}
