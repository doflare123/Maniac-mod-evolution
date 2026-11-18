uniform sampler2D LightFogSampler;
uniform sampler2D LightFogDistanceSampler;

// Only scattered lamp light is reconstructed; material lighting and analytic fog stay full resolution.
vec3 reconstructedLightFog(vec2 uv, vec3 direction, float distance) {
    if (LightCount == 0) return vec3(0.0);
    if (textureSize(DiffuseDepthSampler, 0).y < 1080) return pointLightFog(direction, distance);
    ivec2 size = textureSize(LightFogSampler, 0);
    if (all(equal(size, textureSize(DiffuseDepthSampler, 0)))) return texture(LightFogSampler, uv).rgb;
    vec2 pixel = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    vec2 f = fract(pixel);
    vec3 total = vec3(0.0);
    float weights = 0.0;
    float center = min(distance, SightRange);
    float tolerance = 0.12 + center * 0.01;
    for (int y = 0; y < 2; ++y) for (int x = 0; x < 2; ++x) {
        ivec2 tap = clamp(base + ivec2(x, y), ivec2(0), size - 1);
        float delta = abs(texelFetch(LightFogDistanceSampler, tap, 0).r - center);
        float weight = (x == 0 ? 1.0 - f.x : f.x) * (y == 0 ? 1.0 - f.y : f.y)
                * (1.0 - smoothstep(tolerance * 0.5, tolerance, delta));
        total += texelFetch(LightFogSampler, tap, 0).rgb * weight;
        weights += weight;
    }
    // Recalculate the original ray when coarse depth misses a thin foreground object.
    return weights > 0.0001 ? total / weights : pointLightFog(direction, distance);
}
