#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float EffectTime;
uniform float InjuryStrength;
uniform float DamageStrength;
uniform float ManiacStrength;
uniform float DistortionStrength;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 centered = texCoord * 2.0 - 1.0;
    float edge = smoothstep(0.28, 1.3, length(centered));
    float pulse = pow(0.5 + 0.5 * sin(EffectTime * 7.54), 4.0);
    vec2 wave = vec2(sin(texCoord.y * 11.0 + EffectTime * 1.7),
                     cos(texCoord.x * 9.0 + EffectTime * 1.3));
    vec2 halfPixel = 0.5 / InSize;
    vec2 uv = clamp(texCoord + wave * 0.0025 * edge * InjuryStrength * DistortionStrength,
                    halfPixel, vec2(1.0) - halfPixel);
    vec4 scene = texture(DiffuseSampler, uv);
    float luminance = dot(scene.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 color = mix(scene.rgb, vec3(luminance), InjuryStrength * 0.28);
    // A restrained cool grade; do not brighten dark areas or reveal hidden players.
    color *= mix(vec3(1.0), vec3(0.95, 0.98, 1.0), ManiacStrength);
    float vignette = edge * (InjuryStrength * (0.11 + pulse * 0.10)
                            + DamageStrength * 0.20 + ManiacStrength * 0.06);
    color *= 1.0 - min(vignette, 0.42);
    float red = edge * min(InjuryStrength * (0.025 + pulse * 0.025) + DamageStrength * 0.12, 0.17);
    color = mix(color, vec3(0.30, 0.025, 0.035), red);
    fragColor = vec4(color, scene.a);
}
