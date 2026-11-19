// Shared across all quality levels. Comfort only suppresses motion/pulses, not the steady light response.
vec3 atmosphereLightTint() {
    return mix(vec3(1.10, 0.96, 0.78), vec3(0.66, 0.79, 1.0), smoothstep(0.10, 1.0, LightCorruption));
}

float atmosphereLightLevel() {
    float dread = smoothstep(0.12, 1.0, LightCorruption);
    float critical = smoothstep(0.55, 1.0, LightCorruption);
    float slowDip = pow(0.5 + 0.5 * sin(Time * 1.65 + sin(Time * 0.43) * 0.45), 3.0);
    return (1.0 - dread * 0.25) * (1.0 - critical * Comfort * slowDip * 0.09);
}

vec3 atmosphereHaze(vec3 color) {
    return color * mix(vec3(1.035, 1.0, 0.94), vec3(0.72, 0.82, 1.0), smoothstep(0.10, 1.0, LightCorruption))
            * atmosphereLightLevel();
}

// Preserve source colors, but give illuminated materials a warm response at healthy sanity.
vec3 atmosphereMaterial(vec3 source, float distance) {
    float luma = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float highlight = smoothstep(0.48, 0.86, max(source.r, max(source.g, source.b)));
    float distant = smoothstep(5.0, SightRange, distance);
    float saturation = mix(mix(0.64, 0.18, distant), 1.0, highlight);
    saturation *= 1.0 - smoothstep(0.15, 1.0, LightCorruption) * mix(0.55, 0.25, highlight);
    vec3 surface = mix(vec3(luma), source, saturation);
    surface = pow(max(surface, vec3(0.0)), vec3(1.14));
    surface *= mix(vec3(0.86, 0.93, 0.94), atmosphereLightTint(), 0.35 + highlight * 0.65);
    return surface * atmosphereLightLevel();
}
