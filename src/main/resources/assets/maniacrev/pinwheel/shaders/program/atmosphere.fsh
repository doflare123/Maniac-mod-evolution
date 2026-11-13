#include veil:deferred_utils

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform vec2 InSize;
uniform float Active;
uniform float Time;
uniform float Corruption;
uniform float Pursuit;
uniform float FogGround;
uniform float FogDensity;
uniform float SightFadeStart;
uniform float SightRange;
uniform float Indoor;
uniform int FogSamples;
uniform float Hurt;
uniform float Fear;
uniform float Plague;
uniform vec3 LampPosition;
uniform float LampStrength;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(cell), hash(cell + vec3(1, 0, 0)), f.x),
                   mix(hash(cell + vec3(0, 1, 0)), hash(cell + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(cell + vec3(0, 0, 1)), hash(cell + vec3(1, 0, 1)), f.x),
                   mix(hash(cell + vec3(0, 1, 1)), hash(cell + vec3(1, 1, 1)), f.x), f.y), f.z);
}

#include maniacrev:atmosphere_base

// Integrate heterogeneous extinction up to the visible surface. Geometry stops the ray.
// The lamp adds unshadowed scattering; this prototype does not promise light occlusion.
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
        float density = medium.a;
        vec3 incident = mix(medium.rgb, medium.rgb * vec3(0.91, 1.0, 0.82), Plague * 0.5);
        float absorbed = 1.0 - exp(-density * stepLength);
        if (LampStrength > 0.0) {
            vec3 lightVector = LampPosition - p;
            float light = exp(-dot(lightVector, lightVector) / 22.0) * LampStrength;
            incident += vec3(0.72, 0.31, 0.08) * light;
        }
        scattered += transmission * absorbed * incident;
        transmission *= 1.0 - absorbed;
    }
    return vec4(scattered, transmission);
}

void main() {
    vec4 original = texture(DiffuseSampler0, texCoord);
    if (Active < 0.5) {
        fragColor = original;
        return;
    }

    vec2 centered = texCoord * 2.0 - 1.0;
    float edge = smoothstep(0.15, 1.2, length(centered));
    float instability = pow(Corruption, 2.2);
    float wave = sin(texCoord.y * 29.0 + Time * 1.9) + sin(texCoord.y * 53.0 - Time * 2.7) * 0.4;
    vec2 uv = clamp(texCoord + vec2(wave, sin(Time * 1.3 + texCoord.x * 14.0))
                    * 0.0018 * (instability + Fear * 0.4) * edge, vec2(0.001), vec2(0.999));
    float separation = instability * edge * 2.5 / InSize.x;
    vec3 color = vec3(texture(DiffuseSampler0, clamp(uv + vec2(separation, 0), vec2(0), vec2(1))).r,
                      texture(DiffuseSampler0, uv).g,
                      texture(DiffuseSampler0, clamp(uv - vec2(separation, 0), vec2(0), vec2(1))).b);
    vec2 ghostOffset = vec2(sin(Time * 1.7) * 5.0, cos(Time * 2.1)) / InSize;
    vec3 ghost = texture(DiffuseSampler0, clamp(uv + ghostOffset, vec2(0), vec2(1))).rgb;
    color = mix(color, ghost, instability * edge * 0.16);

    float depth = texture(DiffuseDepthSampler, uv).r;
    vec3 viewPosition = viewPosFromDepthSample(depth, uv);
    color = atmosphereSurface(color, uv, viewPosition, depth);
    color = mix(color, color * vec3(0.83, 1.04, 0.73), Plague * 0.5);

    vec3 direction = normalize(viewToWorldSpaceDirection(viewPosition));
    float distance = depth >= 0.999999 ? SightRange : length(viewPosition);
    vec4 fog = atmosphere(VeilCamera.CameraPosition, direction, distance);
    vec3 fogColor = fog.rgb / max(1.0 - fog.a, 0.00001);
    // No low-density hole, roof, bright emitter or quality setting can reveal
    // the source image at/behind SightRange. Near space stays readable.
    float visible = 1.0 - smoothstep(SightFadeStart, SightRange, distance);
    float transmission = fog.a * visible;
    color = color * transmission + fogColor * (1.0 - transmission);

    float heartbeat = pow(0.5 + 0.5 * sin(Time * 8.0), 5.0);
    float vignette = edge * edge * (0.10 + Corruption * 0.18 + Pursuit * (0.08 + 0.055 * heartbeat));
    color *= 1.0 - vignette;
    color += vec3(0.07, 0.007, 0.012) * edge * (Hurt * 0.5 + Pursuit * heartbeat * 0.18);
    float grain = hash(vec3(gl_FragCoord.xy, floor(Time * 18.0))) - 0.5;
    color += grain * instability * 0.035;
    fragColor = vec4(max(color, vec3(0.0)), original.a);
}
