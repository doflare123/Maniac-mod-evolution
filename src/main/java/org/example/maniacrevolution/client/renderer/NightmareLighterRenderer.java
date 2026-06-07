package org.example.maniacrevolution.client.renderer;

import org.example.maniacrevolution.client.model.NightmareLighterModel;
import org.example.maniacrevolution.item.NightmareLighterItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class NightmareLighterRenderer extends GeoItemRenderer<NightmareLighterItem> {
    public NightmareLighterRenderer() {
        super(new NightmareLighterModel());
    }
}
