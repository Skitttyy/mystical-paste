#version 330
layout(std140) uniform SkyConfig {
    vec4 Color1;
    vec4 Color2;
    vec4 Neb1;
    vec4 Neb2;
    float Time;
    float Speed;
    float Scale;
    float Tilt;
    int CameraSync;
    float Yaw;
    float Pitch;
    float StarDensity;
    float CloudDensity;
    float NebulaSpeed;
    float Pad1;
    float Pad2;
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord;
    if (CameraSync == 1) {
        uv.x += Yaw / 360.0;
        uv.y -= Pitch / 180.0;
    }

    vec2 tiltedUV;
    tiltedUV.x = uv.x * cos(Tilt) - uv.y * sin(Tilt);
    tiltedUV.y = uv.y * cos(Tilt) + uv.x * sin(Tilt);

    float t = Time * NebulaSpeed * 0.1;
    float n = fbm(tiltedUV * CloudDensity * 5.0 + t);
    vec3 cloudColor = mix(Neb1.rgb, Neb2.rgb, n);

    float starSeed = hash(tiltedUV * 150.0);
    float starThreshold = 1.0 - (StarDensity * 0.005);
    float starBright = step(starThreshold, starSeed);

    fragColor = vec4(cloudColor + vec3(starBright), 1.0);
}