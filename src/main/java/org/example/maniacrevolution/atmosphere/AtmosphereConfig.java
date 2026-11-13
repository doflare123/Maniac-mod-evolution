package org.example.maniacrevolution.atmosphere;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AtmosphereConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue FOG_SAMPLES;
    public static final ForgeConfigSpec.DoubleValue DISTORTION;

    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Enable Veil post-processing. The match sight limit remains active when disabled.").define("enabled", true);
        FOG_SAMPLES = builder.comment("Fog integration samples. Higher costs more GPU time.")
                .defineInRange("fogSamples", 12, 8, 24);
        DISTORTION = builder.comment("Comfort setting for distortion, grain and chase pulses.")
                .defineInRange("distortionStrength", 1.0, 0.0, 1.0);
        SPEC = builder.build();
    }

    private AtmosphereConfig() {}
}
