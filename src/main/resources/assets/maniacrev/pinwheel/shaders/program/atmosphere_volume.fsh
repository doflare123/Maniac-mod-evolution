#include veil:deferred_utils
#include maniacrev:atmosphere_uniforms

in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out float rayDistance;

#include maniacrev:atmosphere_noise
#include maniacrev:atmosphere_base
#include maniacrev:atmosphere_lighting
#include maniacrev:atmosphere_volume

void main() {
    if (Active < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        rayDistance = SightRange;
        return;
    }
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 viewPosition = viewPosFromDepthSample(depth, texCoord);
    float distance = depth >= 0.999999 ? SightRange : length(viewPosition);
    vec3 direction = normalize(viewToWorldSpaceDirection(viewPosition));
    fragColor = atmosphereVolume(direction, distance);
    rayDistance = min(distance, SightRange);
}
