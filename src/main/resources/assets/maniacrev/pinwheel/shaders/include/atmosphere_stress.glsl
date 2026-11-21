// Shared after fog/light composition in every quality mode. The center stays readable.
float stressEdge(vec2 uv) {
    return smoothstep(0.15, 1.2, length(uv * 2.0 - 1.0));
}

float sanityBreath() {
    return 0.5 + 0.5 * sin(Time * 1.65 + sin(Time * 0.43) * 0.45);
}

float sanityInstability() {
    float onset = smoothstep(0.25, 1.0, Corruption);
    return onset * onset * Comfort;
}

vec2 stressUv(vec2 uv) {
    float edge = stressEdge(uv);
    float pulse = mix(0.3, 1.0, sanityBreath());
    vec2 wave = vec2(sin(uv.y * 29.0 + Time * 1.9), sin(Time * 1.3 + uv.x * 14.0) * 0.45);
    vec2 distortion = wave * (sanityInstability() * pulse + Fear * 0.4) * 0.0022 * edge;
    distortion += (uv - 0.5) * Hurt * Comfort * 0.006 * edge;
    return clamp(uv + distortion, vec2(0.001), vec2(0.999));
}

vec3 stressComposite(vec3 color, vec2 uv) {
    float edge = stressEdge(uv);
    float dread = smoothstep(0.12, 1.0, Corruption);
    float instability = sanityInstability();
    float vignette = edge * edge * (0.10 + dread * 0.16 + instability * sanityBreath() * 0.045);
    // Bound stacking of critical sanity and a fresh hit.
    color *= 1.0 - min(vignette + edge * edge * Hurt * 0.13, 0.48);
    // A perceptible blood-colored rim even over bright fog; no full-screen red wash.
    float blood = edge * edge * Hurt * 0.42;
    color = mix(color, vec3(0.30, 0.022, 0.035), blood);
    return max(color, vec3(0.0));
}

// Applied after lights and soul volumes: neither haze nor emitters can fill blindness with light.
vec3 impairedVision(vec3 color, vec2 uv, float distance) {
    float blindFade = smoothstep(SightRange * 0.25, SightRange, distance);
    float darkFade = smoothstep(SightRange * 0.55, SightRange, distance);
    float obscured = max(Blindness * blindFade, Darkness * darkFade);
    vec3 blackHaze = mix(vec3(0.004, 0.003, 0.009), vec3(0.009, 0.004, 0.015), Corruption);
    color = mix(color, blackHaze, obscured);
    float edge = stressEdge(uv);
    return color * (1.0 - edge * edge * max(Blindness * 0.28, Darkness * 0.16));
}
