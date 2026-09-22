#version 120

uniform float time;
uniform vec2  resolution;
uniform float progress;

#define sat(a) clamp(a, 0., 1.)
#define PI 3.141592653

mat2 r2d(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float lenny(vec2 v) { return abs(v.x) + abs(v.y); }

float _cir(vec2 uv, float r) {
    return length(uv) - r;
}

float _sqr(vec2 p, vec2 s) {
    vec2 l = abs(p) - s;
    return max(l.x, l.y);
}

float _nimal(vec2 uv) {
    uv.x += sin(uv.y * 25.0 + time * 2.0) * 0.01 * sat(uv.y * 5.0);
    vec2 tuv = (uv - vec2(0.11 + 0.01 * sin(uv.y * 30.0 - time * 4.0), -0.05)) * r2d(PI / 4.0);
    tuv.x = abs(tuv.x);
    tuv = tuv - vec2(-0.07, 0.0);
    float tail = _cir(tuv, 0.1);
    uv -= vec2(0.05, -0.03);
    float body = 10.0;

    float anhears = 0.1;
    vec2 offhears = vec2(0.01, 0.0);
    body = min(body, _sqr((uv + offhears) * r2d(anhears),  vec2(0.025, 0.07)));
    body = min(body, _sqr((uv - offhears) * r2d(-anhears), vec2(0.025, 0.07)));
    body = max(body, -_sqr((uv - vec2(0.0, 0.08)) * r2d(PI / 4.0), vec2(0.03)));
    uv.x = abs(uv.x);
    body = min(body, _cir(uv * vec2(1.0, 0.8) - vec2(0.02,  -0.04),  0.03));
    body = min(body, _cir(uv * vec2(1.0, 0.8) - vec2(0.048, -0.058), 0.005));
    body = min(body, tail);
    return body;
}

float _star(vec2 p, vec2 s) {
    float a = _sqr(p, s.xy);
    float b = _sqr(p, s.yx);
    return min(a, b);
}

float _stars(vec2 uv, vec2 szu) {
    uv *= r2d(PI / 4.0);
    vec2 ouv = uv;
    float th  = 0.002;
    vec2 rep  = vec2(0.1);
    vec2 idx  = floor((uv + rep * 0.5) / rep);

    uv = mod(uv + rep * 0.5, rep) - rep * 0.5;
    float sz = sat(sin(idx.x * 5.0 + idx.y + time)) * sat(length(ouv * 2.0) - 0.5);
    return _star(uv, vec2(20.0 * th, th) * 0.5 * sz * szu);
}

vec3 rdr(vec2 uv) {
    float shp = 400.0;
    vec3 background = vec3(0.431, 0.114, 0.647) * 0.2;

    background = mix(background, vec3(1.000, 0.761, 0.239),
                     1.0 - sat(_stars(uv, vec2(1.0)) * shp));

    vec3 sunCol         = vec3(1.000, 0.761, 0.239);
    vec3 foregroundBack = vec3(0.345, 0.125, 0.494);
    vec3 foreground;

    float sun  = _cir(uv, 0.02);
    float sstp = 0.05;
    sun = floor(sun / sstp) * sstp;
    foreground = mix(foregroundBack, sunCol, 1.0 - sat(sun * 4.0));

    float mount = uv.y - asin(sin(uv.x * 25.0)) * 0.01 + 0.1;
    foreground  = mix(foreground, foreground * 0.3, 1.0 - sat(mount * shp * 0.5));

    float mount2 = uv.y - sin(uv.x * 25.0 + 2.0) * 0.05 + 0.1;
    foreground   = mix(foreground, foreground * 0.5, 1.0 - sat(mount2 * shp * 0.1));

    float hill = _cir(uv - vec2(0.0, -0.9), 0.8);
    foreground = mix(foreground, vec3(0.0), 1.0 - sat(hill * shp));

    float nanimal = _nimal(uv);
    foreground    = mix(foreground, vec3(0.0), 1.0 - sat(nanimal * shp));

    float mask = _cir(uv, 0.25);
    vec3  col  = mix(background, foreground, 1.0 - sat(mask * shp));

    col += (1.0 - sat(length(uv * 3.0))) * sunCol * 0.7;
    col += pow(1.0 - sat(lenny(uv * vec2(1.0, 2.0))), 5.0) * sunCol * 0.7
           * mix(0.95, 1.0, sat(sin(time * 25.0) * 2.0 + sin(time * 40.0)));

    col = mix(col, sunCol,
              sat(length(uv) - 0.1) * (1.0 - sat(_stars(uv * 0.8, vec2(5.0)) * shp * 0.3)) * 0.7);

    return col;
}

void main() {
    vec2 uv  = (gl_FragCoord.xy - vec2(0.5) * resolution.xy) / resolution.xx;
    vec3 col = rdr(uv);

    col *= mix(-1.0, 1.0, 1.0 - sat(lenny(uv * 2.0) - 0.5));
    col  = sat(col);

    gl_FragColor = vec4(col, 1.0);
}