package org.example.maniacrevolution.techies.client;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.techies.TechiesItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.core.animation.AnimationState;

public final class TechiesItemRenderer extends GeoItemRenderer<TechiesItem> {
    public TechiesItemRenderer() { super(new GeoModel<TechiesItem>() {
        @Override public ResourceLocation getModelResource(TechiesItem i) { return Maniacrev.loc("geo/"+i.stem+".geo.json"); }
        @Override public ResourceLocation getTextureResource(TechiesItem i) { return Maniacrev.loc("textures/item/"+i.stem+".png"); }
        @Override public ResourceLocation getAnimationResource(TechiesItem i) { return Maniacrev.loc("animations/"+i.stem+".animation.json"); }
        @Override public void setCustomAnimations(TechiesItem i,long id,AnimationState<TechiesItem> state) {
            var stack=state.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            if(stack!=null && stack.hasTag() && stack.getTag().contains("TechiesVisualSeconds")) {
                TechiesAnimation.apply(this,i.stem,"install",stack.getTag().getDouble("TechiesVisualSeconds"),stack.getTag().getFloat("TechiesVisualWeight"));
                return;
            }
            double seconds=i.stem.equals("techies_remote")?(TechiesClient.time()-TechiesClient.remotePress)/20:0;
            if(i.stem.equals("techies_remote") && net.minecraft.client.Minecraft.getInstance().player!=null) {
                var pose=TechiesHands.pose(net.minecraft.client.Minecraft.getInstance().player);
                if(pose!=null) seconds=(TechiesHands.clock()-pose.press)/20;
            }
            if(stack!=null && stack.hasTag() && stack.getTag().contains("TechiesPressSeconds")) seconds=stack.getTag().getDouble("TechiesPressSeconds");
            TechiesAnimation.apply(this,i.stem,"press",seconds,seconds>=0 && seconds<=.3?1:0);
        }
    }); }
}
