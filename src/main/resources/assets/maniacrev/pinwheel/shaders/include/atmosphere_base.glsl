// Shared world-space medium and material treatment for every atmosphere profile.

vec4 atmosphereMedium(vec3 p) {
    vec3 drift = p * vec3(0.11, 0.18, 0.11) + vec3(Time * 0.018, 0.0, -Time * 0.012);
    float curl = noise(drift);
    float floorHeight = p.y - FogGround;
    float bankDistance = (floorHeight - 0.25 - curl * 0.65) / 0.85;
    float ribbonDistance = (floorHeight - 1.1 - (curl - 0.5) * 1.3) / 0.34;
    float bank = exp(-bankDistance * bankDistance);
    float ribbon = exp(-ribbonDistance * ribbonDistance);
    float broken = smoothstep(0.25, 0.75, noise(drift * 1.9 + vec3(0.0, Time * 0.012, 0.0)));
    // Eye-level haze never disappears above low banks or under roofs. Indoor only
    // softens the extra floor wisps; it does not extend the gameplay sight range.
    float eyeLevel = mix(0.8, 1.25, curl);
    float lowLayer = (bank * (0.6 + broken * 0.8) + ribbon * broken * 0.75) * (1.0 - Indoor * 0.65);
    float density = FogDensity * (eyeLevel + lowLayer);
    float ash = clamp(bank * 0.65 + ribbon * broken * 0.65, 0.0, 1.0);
    vec3 incident = mix(vec3(0.16, 0.185, 0.18), vec3(0.29, 0.32, 0.30), ash * 0.75);
    return vec4(incident, density);
}

float atmosphereContour(vec2 uv, vec3 positionVS, float depth) {
    if (depth >= 0.999999) return 0.0;
    vec2 pixel = 1.25 / InSize;
    float center = -positionVS.z;
    float left = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv - vec2(pixel.x, 0)).r, uv - vec2(pixel.x, 0)).z;
    float right = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv + vec2(pixel.x, 0)).r, uv + vec2(pixel.x, 0)).z;
    float down = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv - vec2(0, pixel.y)).r, uv - vec2(0, pixel.y)).z;
    float up = -viewPosFromDepthSample(texture(DiffuseDepthSampler, uv + vec2(0, pixel.y)).r, uv + vec2(0, pixel.y)).z;
    float horizontal = max(left - center, right - center) - min(abs(left - center), abs(right - center));
    float vertical = max(down - center, up - center) - min(abs(down - center), abs(up - center));
    float contour = smoothstep(0.22 + center * 0.025, 0.7 + center * 0.06, max(horizontal, vertical));
    return contour * smoothstep(1.5, 4.0, center);
}

vec3 atmosphereSurface(vec3 source, vec2 uv, vec3 positionVS, float depth) {
    float luma = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float highlight = smoothstep(0.48, 0.86, max(source.r, max(source.g, source.b)));
    float distant = smoothstep(5.0, SightRange, length(positionVS));
    float saturation = mix(mix(0.64, 0.18, distant), 1.0, highlight);
    vec3 surface = mix(vec3(luma), source, saturation);
    surface = pow(max(surface, vec3(0.0)), vec3(1.14));
    surface *= mix(vec3(0.86, 0.93, 0.94), vec3(1.02, 1.0, 0.98), highlight);
    float contour = atmosphereContour(uv, positionVS, depth);
    surface += vec3(0.31, 0.38, 0.35) * contour * (1.0 - highlight * 0.7) * 0.15;
    return surface;
}
