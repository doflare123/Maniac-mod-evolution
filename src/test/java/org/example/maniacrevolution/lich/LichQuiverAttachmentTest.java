package org.example.maniacrevolution.lich;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/** Calls the real preRender hook without OpenGL; the body owns the attachment origin. */
final class LichQuiverAttachmentTest {
    static void verify() throws Exception {
        for(String name:new String[]{"LichQuiver","LichMagic"}) {
        var type = Class.forName("org.example.maniacrevolution.lich.client."+name);
        var field = type.getDeclaredField("RENDERER"); field.setAccessible(true);
        var renderer = (GeoObjectRenderer<?>)field.get(null);
        for(int frame=0;frame<=60;frame++) {
            var pose = new PoseStack();
            pose.translate(.2, -.4, .7);
            pose.mulPose(com.mojang.math.Axis.XP.rotation(frame*.02f));
            pose.mulPose(com.mojang.math.Axis.YP.rotation(frame*.03f));
            pose.scale(-1, -1, 1); pose.translate(0, -1.5, 0);
            var expected = new Matrix4f(pose.last().pose());
            renderer.preRender(pose, null, null, null, null, false, 0, 0, 0, 1, 1, 1, 1);
            if(!expected.equals(pose.last().pose(), .000001f))
                throw new AssertionError(name+" preRender introduced a block offset at body pose " + frame);
        }
        System.out.println(name+": actual GeoObjectRenderer preRender preserves origin in 61 rotated poses.");
        }
    }
}
