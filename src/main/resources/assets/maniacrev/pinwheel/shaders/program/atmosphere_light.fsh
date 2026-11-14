#include veil:deferred_utils

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform float Active;
uniform float Time;
uniform float Corruption;
uniform float Pursuit;
uniform float FogDensity;
uniform float SightFadeStart;
uniform float SightRange;
uniform float Hurt;
uniform float Fear;
uniform float Plague;

in vec2 texCoord;
out vec4 fragColor;

#include maniacrev:atmosphere_color

// Two texture samples, no ray-marching, 3D noise, silhouette taps or dynamic lights.
void main() {
    if (Active < 0.5) { fragColor = texture(DiffuseSampler0, texCoord); return; }
    float edge = smoothstep(0.15, 1.2, length(texCoord * 2.0 - 1.0));
    float unrest = Corruption * Corruption;
    vec2 uv = clamp(texCoord + vec2(sin(texCoord.y * 29.0 + Time * 1.9), 0.0)
            * 0.0018 * (unrest + Fear * 0.4) * edge, vec2(0.001), vec2(0.999));
    vec4 source = texture(DiffuseSampler0, uv);
    float depth = texture(DiffuseDepthSampler, uv).r;
    float distance = depth >= 0.999999 ? SightRange : length(viewPosFromDepthSample(depth, uv));
    vec3 color = atmosphereMaterial(source.rgb, distance);
    color = mix(color, color * vec3(0.83, 1.04, 0.73), Plague * 0.5);
    float visible = 1.0 - smoothstep(SightFadeStart, SightRange, distance);
    float transmission = exp(-FogDensity * max(0.0, min(distance, SightRange) - 1.5)) * visible;
    vec3 haze = mix(vec3(0.19, 0.215, 0.205), vec3(0.17, 0.215, 0.17), Plague * 0.5);
    color = color * transmission + haze * (1.0 - transmission);
    float heartbeat = pow(0.5 + 0.5 * sin(Time * 8.0), 5.0);
    color *= 1.0 - edge * edge * (0.10 + Corruption * 0.18 + Pursuit * (0.08 + heartbeat * 0.055));
    color += vec3(0.07, 0.007, 0.012) * edge * (Hurt * 0.5 + Pursuit * heartbeat * 0.18);
    fragColor = vec4(max(color, vec3(0.0)), source.a);
}
