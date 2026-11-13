package org.example.maniacrevolution.lich;

import java.io.*;
import java.util.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.*;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.loading.object.*;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.util.JsonUtil;

/** Exercises GeckoLib's real animation processor without Minecraft's window. */
public final class LichRuntimePoseTest {
    static final class Probe implements GeoAnimatable {
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"pose",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.lich.book_idle"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return null;}
        @Override public double getTick(Object o){return 0;}
    }
    public static void verify()throws Exception {
        Model raw;BakedAnimations animations;
        try(var reader=new InputStreamReader(Objects.requireNonNull(LichRuntimePoseTest.class.getResourceAsStream("/assets/maniacrev/geo/lich.geo.json")))){raw=JsonUtil.GEO_GSON.fromJson(reader,Model.class);}
        try(var reader=new InputStreamReader(Objects.requireNonNull(LichRuntimePoseTest.class.getResourceAsStream("/assets/maniacrev/animations/lich.animation.json")))){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");animations=JsonUtil.GEO_GSON.fromJson(json,BakedAnimations.class);
        }
        var baked=BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        var model=new CoreGeoModel<Probe>() {
            final AnimationProcessor<Probe> processor=new AnimationProcessor<>(this);
            @Override public CoreBakedGeoModel getBakedGeoModel(String name){return baked;}
            @Override public AnimationProcessor<Probe> getAnimationProcessor(){return processor;}
            @Override public Animation getAnimation(Probe probe,String name){return animations.getAnimation(name);}
            @Override public void handleAnimations(Probe probe,long id,AnimationState<Probe> state){}
        };
        model.getAnimationProcessor().setActiveModel(baked);
        var probe=new Probe();var manager=new AnimatableManager<Probe>(probe);
        for(int tick=0;tick<=80;tick++) {
            var state=new AnimationState<>(probe,0,0,0,false);
            model.getAnimationProcessor().tickAnimation(probe,model,manager,tick,state,false);
            if(tick==20)for(String name:List.of("right_arm","left_arm","right_forearm","left_forearm")){
                var bone=model.getBone(name).orElseThrow();
                double x=Math.toDegrees(bone.getRotX());
                if(name.endsWith("forearm")?(x<18||x>21):(x<42||x>44))throw new AssertionError("Unexpected book pose: "+name+"="+x);
            }
            if(tick==20)LichMeshPreview.write(baked,"build/lich-runtime-book-idle.png");
        }
        System.out.println("Lich book_idle: actual GeckoLib animation processor completed 81 ticks; arm angles match export");
    }
}
