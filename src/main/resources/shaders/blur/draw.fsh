#version 120

uniform sampler2D blurTexture;
uniform vec4 tintColor;
uniform float opacity;

varying vec4 clipPos;

void main() {
    vec2 ndc = clipPos.xy / clipPos.w;
    vec2 uv = ndc * 0.5 + 0.5;
    vec4 blurred = texture2D(blurTexture, uv);
    vec3 tinted = mix(blurred.rgb, tintColor.rgb, tintColor.a);
    gl_FragColor = vec4(tinted, clamp(opacity, 0.0, 1.0));
}
