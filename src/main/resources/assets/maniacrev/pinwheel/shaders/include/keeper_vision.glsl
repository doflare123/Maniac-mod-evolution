// Keeper vision is based on visible geometry and world-space fog, not an RGB wash.
// This include follows the shared noise() function and atmosphere uniforms.

vec4 keeperMedium(vec3 p) {
    vec3 drift = p * vec3(0.11, 0.18, 0.11) + vec3(Time * 0.018, 0.0, -Time * 0.012);
    float curl = noise(drift);
    float floorHeight = p.y - FogGround;
    float bankDistance = (floorHeight - 0.25 - curl * 0.65) / 0.85;
    float ribbonDistance = (floorHeight - 1.1 - (curl - 0.5) * 1.3) / 0.34;
    float bank = exp(-bankDistance * bankDistance);
    float ribbon = exp(-ribbonDistance * ribbonDistance);
    float broken = smoothstep(0.25, 0.75, noise(drift * 1.9 + vec3(0.0, Time * 0.012, 0.0)));
    float density = FogDensity * (0.055 + bank * (0.8 + broken * 2.4) + ribbon * broken * 1.8);
    // Ash is lit where the low banks accumulate. The upper air remains almost clear.
    float ash = clamp(bank * 0.65 + ribbon * broken * 0.65, 0.0, 1.0);
    vec3 incident = mix(vec3(0.018, 0.025, 0.028), vec3(0.34, 0.38, 0.35), ash);
    return vec4(incident, density);
}

vec2 keeperRefraction(vec2 uv, float edge) {
    float depth = texture(DiffuseDepthSampler, uv).r;
    vec3 positionVS = viewPosFromDepthSample(depth, uv);
    vec3 positionWS = viewToWorldSpace(positionVS);
    float distant = smoothstep(12.0, 45.0, length(positionVS));
    float phase = positionWS.y * 0.65 + positionWS.x * 0.12;
    // At most about a pixel of motion at 1080p, confined to distant peripheral air.
    return vec2(sin(phase + Time * 0.7), cos(phase * 0.6 - Time * 0.45))
            * 0.00065 * distant * edge * KeeperMotion;
}

float keeperContour(vec2 uv, vec3 positionVS, float depth) {
    if (depth >= 0.999999) return 0.0;
    vec2 pixel = 1.25 / InSize;
    float center = -positionVS.z;
    float left = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv - vec2(pixel.x, 0)).r, uv - vec2(pixel.x, 0)).z;
    float right = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv + vec2(pixel.x, 0)).r, uv + vec2(pixel.x, 0)).z;
    float down = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv - vec2(0, pixel.y)).r, uv - vec2(0, pixel.y)).z;
    float up = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv + vec2(0, pixel.y)).r, uv + vec2(0, pixel.y)).z;
    // Suppress ordinary depth slopes (floors/walls) and light only the nearer side
    // of a visible silhouette. No hidden geometry or entity buffers are sampled.
    float horizontal = max(left - center, right - center) - min(abs(left - center), abs(right - center));
    float vertical = max(down - center, up - center) - min(abs(down - center), abs(up - center));
    float contour = smoothstep(0.22 + center * 0.025, 0.7 + center * 0.06, max(horizontal, vertical));
    return contour * smoothstep(1.5, 4.0, center) * (1.0 - smoothstep(45.0, 80.0, center));
}

vec3 keeperSurface(vec3 source, vec2 uv, vec3 positionVS, float depth) {
    float luma = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float highlight = smoothstep(0.48, 0.86, max(source.r, max(source.g, source.b)));
    float distant = smoothstep(5.0, 65.0, length(positionVS));
    float saturation = mix(mix(0.64, 0.18, distant), 1.0, highlight);
    vec3 surface = mix(vec3(luma), source, saturation);
    surface = pow(max(surface, vec3(0.0)), vec3(1.14));
    surface *= mix(vec3(0.86, 0.93, 0.94), vec3(1.02, 1.0, 0.98), highlight);
    if (depth >= 0.999999) return surface;
    vec3 positionWS = viewToWorldSpace(positionVS);
    // Slow, world-anchored hollows in unlit materials, independent of survivor sanity.
    float soot = noise(positionWS * 0.24 + vec3(Time * KeeperMotion * 0.025, 0.0, 0.0));
    float hollow = smoothstep(0.35, 0.8, soot) * (1.0 - highlight)
            * (1.0 - smoothstep(0.12, 0.5, luma));
    surface *= 1.0 - hollow * 0.23;
    // A narrow bone-white rim separates silhouettes from the ink-dark background.
    float contour = keeperContour(uv, positionVS, depth);
    surface += vec3(0.31, 0.38, 0.35) * contour * (1.0 - highlight * 0.7) * mix(0.10, 0.24, soot);
    return surface;
}
