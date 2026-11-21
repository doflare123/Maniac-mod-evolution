#include veil:deferred_utils
#include maniacrev:atmosphere_uniforms

in vec2 texCoord;
out vec4 fragColor;

#include maniacrev:atmosphere_color
#include maniacrev:atmosphere_lighting
#include maniacrev:atmosphere_light_reconstruction
#include maniacrev:atmosphere_stress
#include maniacrev:atmosphere_keeper

// Same LIGHT composition, with only scattered lamp light reconstructed from the smaller buffer.
void main() {
    if (Active < 0.5) { fragColor = texture(DiffuseSampler0, texCoord); return; }
    vec2 uv = stressUv(texCoord);
    vec4 source = texture(DiffuseSampler0, uv);
    float depth = texture(DiffuseDepthSampler, uv).r;
    vec3 viewPosition = viewPosFromDepthSample(depth, uv);
    float distance = depth >= 0.999999 ? SightRange : length(viewPosition);
    vec3 direction = normalize(viewToWorldSpaceDirection(viewPosition));
    vec3 color = atmosphereMaterial(source.rgb, distance);
    color = surfaceLighting(color, viewPosition, depth);
    color = mix(color, color * vec3(0.83, 1.04, 0.73), Plague * 0.5);
    float visible = 1.0 - smoothstep(SightFadeStart, SightRange, distance);
    float transmission = exp(-FogDensity * max(0.0, min(distance, SightRange) - 1.5)) * visible;
    vec3 haze = atmosphereHaze(mix(vec3(0.19, 0.215, 0.205), vec3(0.17, 0.215, 0.17), Plague * 0.5));
    float homogeneousOpacity = 1.0 - exp(-FogDensity * max(0.0, min(distance, SightRange) - 1.5));
    haze += reconstructedLightFog(uv, direction, distance) / max(homogeneousOpacity, 0.00001);
    color = color * transmission + haze * (1.0 - transmission);
    color = keeperComposite(color, direction, depth >= 0.999999 ? 100000.0 : length(viewPosition));
    color = stressComposite(color, texCoord);
    color = impairedVision(color, texCoord, distance);
    fragColor = vec4(max(color, vec3(0.0)), source.a);
}
