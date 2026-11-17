// Section-cached emitters; short rows share a slot and use two quadrature points.
uniform int LightCount;
uniform int LightSamples;
uniform int HandLightCount;
uniform vec4 LightPositionRadius[64];
uniform vec4 LightColorPower[64];
uniform vec4 LightExtent[64];
uniform float SurfaceLight;
uniform float ContactShadows;

vec3 sourceColor(int index) {
    vec3 emitted = LightColorPower[index].rgb;
    float energy = dot(emitted, vec3(0.2126, 0.7152, 0.0722));
    vec3 color = mix(emitted, vec3(energy), LightCorruption * 0.35)
            * mix(vec3(1.0), vec3(0.72, 0.86, 1.0), LightCorruption);
    return color * LightColorPower[index].a * atmosphereLightLevel();
}

// w packs the row size (low 3 bits) and up to three touching emitter slot references.
int sourceMembers(int index) { return max(1, int(LightExtent[index].w) & 7); }
int sourceTaps(int index) { return sourceMembers(index) > 1 ? 2 : 1; }

vec3 sourcePoint(int index, int tap) {
    // Two points match the energy and spatial variance of the discrete row (exact for two blocks).
    float members = float(max(2, sourceMembers(index)));
    float spread = sqrt((members + 1.0) / (3.0 * (members - 1.0)));
    return LightPositionRadius[index].xyz + LightExtent[index].xyz * (tap == 0 ? -spread : spread);
}

float lightFalloff(vec3 delta, float radius) {
    float d2 = dot(delta, delta);
    float edge = max(0.0, 1.0 - d2 / (radius * radius));
    return edge * edge / (1.0 + d2 * 0.30);
}

bool insideEmitter(vec3 surface, int index) {
    return all(lessThanEqual(abs(surface - LightPositionRadius[index].xyz), abs(LightExtent[index].xyz) + vec3(0.55)));
}

bool emitterSurface(vec3 surface, int index) {
    if (insideEmitter(surface, index)) return true;
    int neighbors = int(LightExtent[index].w) >> 3;
    for (int neighbor = 0; neighbor < 3; ++neighbor) {
        int reference = (neighbors & 127) - 1;
        if (reference < 0) break;
        if (reference < LightCount && insideEmitter(surface, reference)) return true;
        neighbors >>= 7;
    }
    return false;
}

float screenOcclusion(vec3 relative, float nearBias, float farBias, int index) {
    vec3 probe = worldToViewSpaceDirection(relative);
    vec4 clip = VeilCamera.ProjMat * vec4(probe, 1.0);
    if (clip.w <= 0.05) return 0.0;
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    if (any(lessThan(uv, vec2(0.001))) || any(greaterThan(uv, vec2(0.999)))) return 0.0;
    float depth = texture(DiffuseDepthSampler, uv).r;
    if (depth >= 0.999999) return 0.0;
    vec3 surfaceVS = viewPosFromDepthSample(depth, uv);
    float surfaceZ = -surfaceVS.z;
    float blocked = smoothstep(nearBias, farBias, -probe.z - surfaceZ);
    if (blocked <= 0.0) return 0.0;
    if (emitterSurface(viewToWorldSpaceDirection(surfaceVS), index)) return 0.0;
    return blocked;
}

float lightDepthOcclusion(vec3 relative, vec3 end, int index) {
    if (dot(relative - end, relative - end) < 0.81) return 0.0;
    return screenOcclusion(relative, 0.35, 0.85, index);
}

float lightVisibility(vec3 start, vec3 end, int index) {
    float blocked = 0.0;
    for (int step = 1; step <= 4; ++step) {
        blocked = max(blocked, lightDepthOcclusion(mix(start, end, float(step) / 5.0), end, index));
        if (blocked >= 1.0) break;
    }
    return 1.0 - blocked;
}

// Integrate each lamp only inside its influence sphere. The edge has zero intensity and slope.
vec3 pointLightFog(vec3 direction, float distance) {
    float end = min(distance, SightRange);
    if (end <= 1.5 || FogDensity <= 0.0) return vec3(0.0);
    int samples = clamp(LightSamples, 2, 8);
    vec3 result = vec3(0.0);
    for (int index = 0; index < 64; ++index) {
        if (index >= LightCount) break;
        int taps = sourceTaps(index);
        for (int tap = 0; tap < 2; ++tap) {
            if (tap >= taps) break;
            vec3 point = sourcePoint(index, tap);
            float radius = LightPositionRadius[index].w;
            float alongRay = dot(point, direction);
            float perpendicular2 = max(0.0, dot(point, point) - alongRay * alongRay);
            float radius2 = radius * radius;
            if (perpendicular2 >= radius2) continue;
            float halfChord = sqrt(radius2 - perpendicular2);
            float start = max(1.5, alongRay - halfChord);
            float finish = min(end, alongRay + halfChord);
            if (finish <= start) continue;
            // Keep the cheap unobstructed-lamp path; only hidden centers need local fog probes.
            bool checkShadows = index >= HandLightCount && ContactShadows > 0.5
                    && screenOcclusion(point, 0.9, 1.4, index) > 0.001;
            float stepLength = (finish - start) / float(samples);
            float energy = 0.0;
            for (int sampleIndex = 0; sampleIndex < 8; ++sampleIndex) {
                if (sampleIndex >= samples) break;
                float t = start + (float(sampleIndex) + 0.5) * stepLength;
                float falloff = lightFalloff(point - direction * t, radius);
                // One depth tap per integration point keeps occlusion local, avoiding a mask of the whole chord.
                float visibility = !checkShadows ? 1.0
                        : 1.0 - lightDepthOcclusion(mix(direction * t, point, 0.5), point, index);
                energy += falloff * visibility * exp(-FogDensity * (t - 1.5)) * FogDensity * stepLength;
            }
            result += sourceColor(index) * energy * (1.4 / float(taps));
        }
    }
    return result / (vec3(1.0) + result * 0.4);
}

vec3 surfaceLighting(vec3 surface, vec3 viewPosition, float depth) {
    vec3 normalVS = cross(dFdx(viewPosition), dFdy(viewPosition));
    normalVS *= inversesqrt(max(dot(normalVS, normalVS), 1e-20));
    if (dot(normalVS, -viewPosition) < 0.0) normalVS = -normalVS;
    vec3 normal = viewToWorldSpaceDirection(normalVS);
    if ((SurfaceLight < 0.5 && HandLightCount == 0) || depth >= 0.999999) return surface;
    vec3 relative = viewToWorldSpaceDirection(viewPosition);
    vec3 incident = vec3(0.0);
    for (int index = 0; index < 64; ++index) {
        if (index >= LightCount) break;
        if (index >= HandLightCount && SurfaceLight < 0.5) continue;
        int taps = sourceTaps(index);
        for (int tap = 0; tap < 2; ++tap) {
            if (tap >= taps) break;
            vec3 point = sourcePoint(index, tap);
            vec3 delta = point - relative;
            float attenuation = lightFalloff(delta, LightPositionRadius[index].w);
            if (attenuation < 0.00001) continue;
            float facing = max(0.0, dot(normal, delta * inversesqrt(max(dot(delta, delta), 0.001))));
            if (facing <= 0.0) continue;
            float visible = index < HandLightCount || ContactShadows < 0.5 ? 1.0
                    : lightVisibility(relative + normal * 0.12, point, index);
            incident += sourceColor(index) * attenuation * facing * visible
                    * (index < HandLightCount ? 1.7 : 1.0) / float(taps);
        }
    }
    return surface + max(surface, vec3(0.08)) * min(incident, vec3(1.5)) * 1.8;
}
