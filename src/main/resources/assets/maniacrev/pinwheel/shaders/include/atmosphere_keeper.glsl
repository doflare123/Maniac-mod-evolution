// Only this composition sees souls: the world still disappears at its ordinary 28-block limit.
uniform float Keeper;
uniform int SoulCount;
uniform vec4 SoulCenterSanity[16];
uniform vec4 SoulShape[16]; // torso width, half-height, confirmed feeding, stable seed

// Analytic ellipsoid, with a soft interior and a real depth test against the nearest surface.
float soulVolume(vec3 ray, vec3 center, vec3 radii, float sceneDistance) {
    vec3 q = center / radii;
    vec3 v = ray / radii;
    float vv = dot(v, v);
    float along = dot(q, v) / vv;
    vec3 offset = v * along - q;
    float radial = dot(offset, offset);
    if (along <= 0.0 || radial >= 1.0) return 0.0;
    float entry = along - sqrt((1.0 - radial) / vv);
    if (entry > sceneDistance + 0.025) return 0.0;
    return exp(-radial * 3.4) * (1.0 - smoothstep(0.65, 1.0, radial));
}

vec3 keeperComposite(vec3 color, vec3 ray, float sceneDistance) {
    if (Keeper < 0.5) return color;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    // Stable dead-blue materials and a trace of bruised violet in the haze.
    color = mix(color, vec3(luma) * vec3(0.76, 0.83, 1.0) + vec3(0.016, 0.008, 0.021), 0.82);
    for (int i = 0; i < 16; ++i) {
        if (i >= SoulCount) break;
        vec3 center = SoulCenterSanity[i].xyz;
        vec4 shape = SoulShape[i];
        float range = length(center);
        // Reject most rays before evaluating the smaller inner volumes.
        if (dot(ray, center) <= 0.0 || length(cross(ray, center)) > shape.y * 1.8) continue;
        float sanity = SoulCenterSanity[i].w;
        // Exhaustion changes hue and brightness smoothly, without cutting stripes into the soul.
        vec3 soulTint = mix(vec3(0.24, 0.045, 0.40), vec3(0.60, 0.35, 0.76), smoothstep(0.0, 0.5, sanity));
        soulTint = mix(soulTint, vec3(1.0, 0.90, 0.74), smoothstep(0.5, 1.0, sanity));
        float breath = sin(Time * 1.6 + shape.w * 12.0) * Comfort;
        float drawIn = 1.0 - shape.z * (0.10 + breath * 0.04);
        vec3 radii = vec3(shape.x, shape.y, shape.x) * drawIn;
        float torso = soulVolume(ray, center, radii, sceneDistance);
        float head = soulVolume(ray, center + vec3(0, shape.y * 1.18, 0),
                vec3(shape.x * 1.05), sceneDistance);
        // The shell is the depth-tested boundary of this translucent volume. Its inner core
        // must remain visible inside a player's chest even when the chest surface wrote depth.
        float core = torso > 0.0 ? soulVolume(ray, center + vec3(0, shape.y * 0.12, 0),
                radii * vec3(0.50, 0.34, 0.50), 100000.0) : 0.0;
        float shell = torso + head * 0.65;
        float distanceFade = 1.0 - smoothstep(36.0, 40.0, range);
        float glow = shell * 0.40 + core * 0.42;
        glow *= distanceFade * (1.0 + shape.z * (0.16 + breath * 0.06));
        color += soulTint * glow;
        // A few short inward wisps near the chest, never a beam across the map.
        if (shape.z > 0.005 && Comfort > 0.0) {
            vec3 towardKeeper = -normalize(center);
            for (int w = 0; w < 3; ++w) {
                float progress = fract(Time * 0.32 + float(w) / 3.0 + shape.w);
                vec3 mote = center + towardKeeper * (progress * 1.05)
                        + vec3(sin(progress * 5.0 + shape.w * 12.0) * 0.08, progress * 0.18, 0);
                float fade = sin(progress * 3.14159265);
                color += soulTint * 0.75 * soulVolume(ray, mote, vec3(0.045, 0.075, 0.045), sceneDistance)
                        * fade * shape.z * Comfort * distanceFade * 0.45;
            }
        }
    }
    return color;
}
