package org.example.maniacrevolution.client.renderer;

import org.example.maniacrevolution.client.model.BongModel;
import org.example.maniacrevolution.item.BongItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class BongItemRenderer extends GeoItemRenderer<BongItem> {
    public BongItemRenderer() {
        super(new BongModel());
    }
}
