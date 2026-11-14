// Same material response in light and volumetric modes; bright fire keeps its color.
vec3 atmosphereMaterial(vec3 source, float distance) {
    float luma = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float highlight = smoothstep(0.48, 0.86, max(source.r, max(source.g, source.b)));
    float distant = smoothstep(5.0, SightRange, distance);
    float saturation = mix(mix(0.64, 0.18, distant), 1.0, highlight);
    vec3 surface = mix(vec3(luma), source, saturation);
    surface = pow(max(surface, vec3(0.0)), vec3(1.14));
    return surface * mix(vec3(0.86, 0.93, 0.94), vec3(1.02, 1.0, 0.98), highlight);
}
