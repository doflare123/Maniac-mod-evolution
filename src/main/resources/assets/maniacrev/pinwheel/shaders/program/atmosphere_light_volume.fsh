#include veil:deferred_utils
#include maniacrev:atmosphere_uniforms

in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out float rayDistance;

#include maniacrev:atmosphere_color
#include maniacrev:atmosphere_lighting

void main() {
    fragColor = vec4(0.0);
    rayDistance = SightRange;
    if (Active < 0.5 || LightCount == 0 || textureSize(DiffuseDepthSampler, 0).y < 1080) discard;
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 viewPosition = viewPosFromDepthSample(depth, texCoord);
    float distance = depth >= 0.999999 ? SightRange : length(viewPosition);
    vec3 direction = normalize(viewToWorldSpaceDirection(viewPosition));
    fragColor = vec4(pointLightFog(direction, distance), 0.0);
    rayDistance = min(distance, SightRange);
}
