package org.example.maniacrevolution.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Local presentation settings; never changes server gameplay. */
public final class VisualEffectsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the first-person visual effects during a match.")
            .define("enabled", true);
    public static final ForgeConfigSpec.BooleanValue CAMERA = BUILDER
            .comment("Subtle strafe roll and landing/damage impulses. Does not smooth mouse input.")
            .define("cameraMotion", true);
    public static final ForgeConfigSpec.BooleanValue POST_PROCESSING = BUILDER
            .comment("Screen color grading and vignette. Disable if another shader mod conflicts.")
            .define("postProcessing", true);
    public static final ForgeConfigSpec.BooleanValue DISTORTION = BUILDER
            .comment("Allow low-health edge distortion. Turn off to reduce motion.")
            .define("distortion", true);
    public static final ForgeConfigSpec.DoubleValue INTENSITY = BUILDER
            .comment("Overall effect strength, from 0 (off) to 1 (default maximum).")
            .defineInRange("intensity", 1.0, 0.0, 1.0);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private VisualEffectsConfig() {}
}
