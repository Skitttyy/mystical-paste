#version 120

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

const vec3 BLACK     = vec3(0.);
const vec3 TURQUOISE = vec3(3., 229., 243.) / 255.;
const vec3 BLUE      = vec3(35., 125., 195.) / 255.;
const vec3 GREEN     = vec3(0., 79., 83.) / 255.;
const float PI       = 3.14159265;

mat2 rotate(float r) {
    return mat2(cos(r), sin(r), -sin(r), cos(r));
}

vec2 hash21(float p) {
    vec3 p3 = fract(vec3(p) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 hash32(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yzz) * p3.zyx);
}

vec3 layer(float zoom) {
    vec2 h21 = hash21(zoom);
    float t = time;
    vec2 uv = zoom * (2. * gl_FragCoord.xy - resolution.xy) / resolution.y;
    uv.x -= t + h21.x * 999.;
    vec2 s = vec2(2.);
    vec2 id = floor(uv / s + .5);
    vec3 h32 = hash32(id);

    if(h32.x >= 0.7) {
        vec2 phase = h32.yz * 100. + t + h21 * 100.;
        vec2 tv = uv + vec2(cos(phase.x), sin(phase.y)) * .5;
        id = floor(tv / s + .5);
        vec2 p = tv - s * id;
        float presence = sin(id.x + t*2.) * .5 + .5;
        float r  = .4 + h21.y * .2 - .1;
        float r2 = r * (smoothstep(.2, 10. + sin(t*.2)*8., zoom*.5) * .6 + .2);
        float m  = smoothstep(r, r2, length(p));
        vec3 col = vec3(0.);
        if(h32.y < 0.2)       col = TURQUOISE;
        else if(h32.y < 0.7)  col = BLUE;
        else                   col = GREEN;
        return col * m * presence;
    }
    return vec3(0.);
}

float line(float offset) {
    float t = time;
    vec2 uv = (2. * gl_FragCoord.xy - resolution.xy) / resolution.y;
    uv *= rotate(-PI * .2);
    uv.y += sin(uv.x*2. + t*.05) * .1;
    uv.y += sin((uv.x*.5 + offset) * .9 + t*.1) * .3;
    return .02 / (abs(uv.y) + 5.*smoothstep(0., 30., abs(uv.x)));
}

vec3 background_color() {
    float t = time * .5;
    vec3 col = vec3(0.);
    vec2 uv = (2. * gl_FragCoord.xy - resolution.xy) / resolution.y;
    uv *= rotate(PI * .25);
    uv.y *= .5;
    col += mix(GREEN, BLACK, sin(uv.y + t) * .5 + .5);
    col += mix(BLUE * .5, BLACK, sin(uv.y + t + PI) * .5 + .5);
    return col * .5;
}

void main() {
    float rel_x   = gl_FragCoord.x / resolution.x;
    float fade_in  = max(smoothstep(-.5, .5, rel_x), .1);
    float fade_out = max(smoothstep(1.5, .5, rel_x), .1);

    vec3 base_color = background_color();
    vec3 col = base_color;

    float zoom = 2.;
    for(int i = 0; i < 5; i++) {
        col += layer(zoom);
        zoom *= 2.;
    }

    for(int i = 0; i < 8; i++) {
        col += line(PI * (2./8.) * float(i)) * base_color;
    }

    col *= fade_in * fade_out;
    gl_FragColor = vec4(col, 1.);
}