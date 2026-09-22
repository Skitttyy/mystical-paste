#version 120

uniform sampler2D texture;
uniform vec2 direction;
uniform vec2 texelSize;
uniform float radius;

varying vec2 texCoord;

const int MAX_RADIUS = 40;

void main() {
    float safeRadius = clamp(radius, 1.0, float(MAX_RADIUS));
    float sigma = max(safeRadius * 0.45, 1.0);
    float twoSigmaSq = 2.0 * sigma * sigma;

    vec4 result = texture2D(texture, texCoord);
    float weightSum = 1.0;

    for (int i = 1; i <= MAX_RADIUS; i++) {
        float fi = float(i);
        float enabled = 1.0 - step(safeRadius + 0.001, fi);
        float weight = exp(-(fi * fi) / twoSigmaSq) * enabled;
        vec2 offset = direction * texelSize * fi;

        result += texture2D(texture, texCoord + offset) * weight;
        result += texture2D(texture, texCoord - offset) * weight;
        weightSum += 2.0 * weight;
    }

    gl_FragColor = result / max(weightSum, 0.0001);
}
