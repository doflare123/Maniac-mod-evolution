#include veil:deferred_utils

#include maniacrev:atmosphere_uniforms

in vec2 texCoord;
out vec4 fragColor;

#include maniacrev:atmosphere_color
#include maniacrev:atmosphere_lighting
#include maniacrev:atmosphere_stress

// Color and depth only. Simple fog plus two light samples per sphere, without 3D noise or shadow taps.
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
    haze += pointLightFog(direction, distance) / max(homogeneousOpacity, 0.00001);
    color = color * transmission + haze * (1.0 - transmission);
    color = stressComposite(color, texCoord);
    fragColor = vec4(max(color, vec3(0.0)), source.a);
}
