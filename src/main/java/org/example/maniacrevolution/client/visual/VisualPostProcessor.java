package org.example.maniacrevolution.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.lwjgl.opengl.GL11;

/** Owns a separate chain: never replaces GameRenderer's vanilla/mod effects. */
final class VisualPostProcessor {
    private static PostChain chain;
    private static PostPass effect;
    private static int width, height;
    private static boolean failed;

    private VisualPostProcessor() {}

    static void render(float partialTick, float time, float injury, float damage,
                       float maniac, float distortion) {
        if (failed || Math.max(injury, Math.max(damage, maniac)) < 0.001f) return;
        Minecraft mc = Minecraft.getInstance();
        var target = mc.getMainRenderTarget();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            if (chain == null) {
                // Empty JSON gives us ownership of pass references without reflection/mixins.
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), target,
                        new ResourceLocation(Maniacrev.MODID, "shaders/post/visual_effects.json"));
                effect = chain.addPass("maniacrev:visual_effects", target, chain.getTempTarget("swap"));
                chain.addPass("blit", chain.getTempTarget("swap"), target);
                width = height = -1;
            }
            if (width != target.width || height != target.height) {
                chain.resize(target.width, target.height);
                width = target.width;
                height = target.height;
            }
            var shader = effect.getEffect();
            shader.safeGetUniform("EffectTime").set(time);
            shader.safeGetUniform("InjuryStrength").set(injury);
            shader.safeGetUniform("DamageStrength").set(damage);
            shader.safeGetUniform("ManiacStrength").set(maniac);
            shader.safeGetUniform("DistortionStrength").set(distortion);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            chain.process(partialTick);
        } catch (Exception ex) {
            close();
            failed = true;
            Maniacrev.LOGGER.error("Visual post-processing disabled until resource reload (F3+T)", ex);
        } finally {
            target.bindWrite(true);
            RenderSystem.depthMask(depthMask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.defaultBlendFunc();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    static void close() {
        if (chain != null) chain.close();
        chain = null;
        effect = null;
        width = height = 0;
    }

    static void reload() {
        close();
        failed = false;
    }
}
