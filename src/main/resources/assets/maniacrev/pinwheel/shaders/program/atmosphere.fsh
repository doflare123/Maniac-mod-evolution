#include veil:deferred_utils

#include maniacrev:atmosphere_uniforms
uniform sampler2D FogSampler;
uniform sampler2D FogDistanceSampler;

in vec2 texCoord;
out vec4 fragColor;

#include maniacrev:atmosphere_noise
#include maniacrev:atmosphere_base
#include maniacrev:atmosphere_lighting
#include maniacrev:atmosphere_volume
#include maniacrev:atmosphere_stress
#include maniacrev:atmosphere_keeper

vec4 reconstructedVolume(vec2 uv, vec3 direction, float distance) {
    ivec2 size = textureSize(FogSampler, 0);
    if (all(equal(size, textureSize(DiffuseDepthSampler, 0)))) return texture(FogSampler, uv);
    vec2 pixel = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    vec2 f = fract(pixel);
    vec4 total = vec4(0.0);
    float weights = 0.0;
    float center = min(distance, SightRange);
    float tolerance = 0.12 + center * 0.01;
    for (int y = 0; y < 2; ++y) for (int x = 0; x < 2; ++x) {
        ivec2 tap = clamp(base + ivec2(x, y), ivec2(0), size - 1);
        float delta = abs(texelFetch(FogDistanceSampler, tap, 0).r - center);
        float weight = (x == 0 ? 1.0 - f.x : f.x) * (y == 0 ? 1.0 - f.y : f.y)
                * (1.0 - smoothstep(tolerance * 0.5, tolerance, delta));
        total += texelFetch(FogSampler, tap, 0) * weight;
        weights += weight;
    }
    // A thin foreground object can miss every coarse depth sample. Integrate its own ray in that case.
    return weights > 0.0001 ? total / weights : atmosphereVolume(direction, distance);
}

void main() {
    vec4 original = texture(DiffuseSampler0, texCoord);
    if (Active < 0.5) {
        fragColor = original;
        return;
    }

    float edge = stressEdge(texCoord);
    float instability = sanityInstability();
    vec2 uv = stressUv(texCoord);
    float separation = instability * edge * mix(0.5, 2.5, sanityBreath()) / InSize.x;
    vec3 color = vec3(texture(DiffuseSampler0, clamp(uv + vec2(separation, 0), vec2(0), vec2(1))).r,
                      texture(DiffuseSampler0, uv).g,
                      texture(DiffuseSampler0, clamp(uv - vec2(separation, 0), vec2(0), vec2(1))).b);
    vec2 ghostOffset = vec2(sin(Time * 1.7) * 5.0, cos(Time * 2.1)) / InSize;
    vec3 ghost = texture(DiffuseSampler0, clamp(uv + ghostOffset, vec2(0), vec2(1))).rgb;
    color = mix(color, ghost, instability * edge * sanityBreath() * 0.12);

    float depth = texture(DiffuseDepthSampler, uv).r;
    vec3 viewPosition = viewPosFromDepthSample(depth, uv);
    vec3 direction = normalize(viewToWorldSpaceDirection(viewPosition));
    float distance = depth >= 0.999999 ? SightRange : length(viewPosition);
    color = atmosphereSurface(color, uv, viewPosition, depth);
    color = surfaceLighting(color, viewPosition, depth);
    color = mix(color, color * vec3(0.83, 1.04, 0.73), Plague * 0.5);

    vec4 fog = reconstructedVolume(uv, direction, distance);
    // No low-density hole, roof, bright emitter or quality setting can reveal
    // the source image at/behind SightRange. Near space stays readable.
    float visible = 1.0 - smoothstep(SightFadeStart, SightRange, distance);
    float transmission = fog.a * visible;
    color = color * transmission + fog.rgb * (1.0 - transmission);

    color = keeperComposite(color, direction, depth >= 0.999999 ? 100000.0 : length(viewPosition));
    color = stressComposite(color, texCoord);
    float grain = hash(vec3(gl_FragCoord.xy, floor(Time * 18.0))) - 0.5;
    color += grain * instability * mix(0.008, 0.020, sanityBreath()) * edge;
    color = impairedVision(color, texCoord, distance);
    fragColor = vec4(max(color, vec3(0.0)), original.a);
}
