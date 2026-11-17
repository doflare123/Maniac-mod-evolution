// The same integration is used by the reduced-resolution pass and its thin-geometry fallback.
vec4 atmosphere(vec3 origin, vec3 direction, float distance) {
    float travel = max(0.0, min(distance, SightRange) - 1.5);
    int count = clamp(FogSamples, 8, 24);
    float stepLength = travel / float(count);
    float transmission = 1.0;
    vec3 scattered = vec3(0.0);
    for (int i = 0; i < 24; ++i) {
        if (i >= count) break;
        vec3 p = origin + direction * (1.5 + (float(i) + 0.5) * stepLength);
        vec4 medium = atmosphereMedium(p);
        vec3 incident = atmosphereHaze(mix(medium.rgb, medium.rgb * vec3(0.91, 1.0, 0.82), Plague * 0.5));
        float absorbed = 1.0 - exp(-medium.a * stepLength);
        scattered += transmission * absorbed * incident;
        transmission *= 1.0 - absorbed;
    }
    return vec4(scattered, transmission);
}

// RGB stores the haze color; alpha stores physical transmission. The gameplay fade stays full resolution.
vec4 atmosphereVolume(vec3 direction, float distance) {
    vec4 fog = atmosphere(VeilCamera.CameraPosition, direction, distance);
    float homogeneousOpacity = 1.0 - exp(-FogDensity * max(0.0, min(distance, SightRange) - 1.5));
    float mediumResponse = clamp((1.0 - fog.a) / max(homogeneousOpacity, 0.00001), 0.25, 2.0);
    fog.rgb += pointLightFog(direction, distance) * mediumResponse;
    fog.rgb /= max(1.0 - fog.a, 0.00001);
    return fog;
}
