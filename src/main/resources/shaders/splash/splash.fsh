#version 120

uniform float time;
uniform vec2 resolution;
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
    body = min(body, _sqr((uv + offhears) * r2d(anhears), vec2(0.025, 0.07)));
    body = min(body, _sqr((uv - offhears) * r2d(-anhears), vec2(0.025, 0.07)));
    body = max(body, -_sqr((uv - vec2(0.0, 0.08)) * r2d(PI / 4.0), vec2(0.03)));
    uv.x = abs(uv.x);
    body = min(body, _cir(uv * vec2(1.0, 0.8) - vec2(0.02, -0.04), 0.03));
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
    float th = 0.002;
    vec2 rep = vec2(0.1);
    vec2 idx = floor((uv + rep * 0.5) / rep);

    uv = mod(uv + rep * 0.5, rep) - rep * 0.5;
    float sz = sat(sin(idx.x * 5.0 + idx.y + time)) * sat(length(ouv * 2.0) - 0.5);
    return _star(uv, vec2(20.0 * th, th) * 0.5 * sz * szu);
}

vec3 rdr(vec2 uv) {
    float shp = 400.0;
    vec3 background = vec3(0.431, 0.114, 0.647) * 0.2;

    background = mix(background, vec3(1.000, 0.761, 0.239),
    1.0 - sat(_stars(uv, vec2(1.0)) * shp));

    vec3 sunCol = vec3(1.000, 0.761, 0.239);
    vec3 foregroundBack = vec3(0.345, 0.125, 0.494);
    vec3 foreground;

    float sun = _cir(uv, 0.02);
    float sstp = 0.05;
    sun = floor(sun / sstp) * sstp;
    foreground = mix(foregroundBack, sunCol, 1.0 - sat(sun * 4.0));

    float mount = uv.y - asin(sin(uv.x * 25.0)) * 0.01 + 0.1;
    foreground = mix(foreground, foreground * 0.3, 1.0 - sat(mount * shp * 0.5));

    float mount2 = uv.y - sin(uv.x * 25.0 + 2.0) * 0.05 + 0.1;
    foreground = mix(foreground, foreground * 0.5, 1.0 - sat(mount2 * shp * 0.1));

    float hill = _cir(uv - vec2(0.0, -0.9), 0.8);
    foreground = mix(foreground, vec3(0.0), 1.0 - sat(hill * shp));

    float nanimal = _nimal(uv);
    foreground = mix(foreground, vec3(0.0), 1.0 - sat(nanimal * shp));

    float mask = _cir(uv, 0.25);
    vec3 col = mix(background, foreground, 1.0 - sat(mask * shp));

    col += (1.0 - sat(length(uv * 3.0))) * sunCol * 0.7;
    col += pow(1.0 - sat(lenny(uv * vec2(1.0, 2.0))), 5.0) * sunCol * 0.7
    * mix(0.95, 1.0, sat(sin(time * 25.0) * 2.0 + sin(time * 40.0)));

    col = mix(col, sunCol,
    sat(length(uv) - 0.1) * (1.0 - sat(_stars(uv * 0.8, vec2(5.0)) * shp * 0.3)) * 0.7);

    return col;
}

float memNoise(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 rainbow(float t) {
    return 0.5 + 0.5 * cos(6.28318 * (t + vec3(0.0, 0.33, 0.67)));
}

float charBit(int charId, vec2 p) {
    if (p.x < 0.0 || p.x >= 3.0 || p.y < 0.0 || p.y >= 5.0) return 0.0;
    int x = int(p.x);
    int y = int(p.y);

    if (charId == 0)  return (x == 0 || (y == 0 && x < 3)) ? 1.0 : 0.0; // L
    if (charId == 1)  return (x == 0 || x == 2 || y == 0 || y == 4) ? 1.0 : 0.0; // O / D
    if (charId == 2)  return (x == 0 || x == 2 || y == 4 || y == 2) ? 1.0 : 0.0; // A
    if (charId == 3)  return (x == 1 || y == 0 || y == 4) ? 1.0 : 0.0; // I
    if (charId == 4)  return (x == 0 || x == 2 || (x == 1 && y == 2)) ? 1.0 : 0.0; // N
    if (charId == 5)  return (x == 0 || y == 0 || y == 4 || (x == 2 && y <= 2) || (y == 2 && x == 1)) ? 1.0 : 0.0; // G
    if (charId == 6)  return (x == 0 || y == 0 || y == 4) ? 1.0 : 0.0; // C
    if (charId == 7)  return (x == 0 || y == 4 || y == 2 || (x == 2 && y >= 2) || (x == 1 && y == 1) || (x == 2 && y == 0)) ? 1.0 : 0.0; // R
    if (charId == 8)  return (x == 0 || (x == 1 && y == 2) || (x == 2 && (y == 0 || y == 4 || y == 1 || y == 3))) ? 1.0 : 0.0; // K
    if (charId == 9)  return (x == 0 || y == 0 || y == 2 || y == 4) ? 1.0 : 0.0; // E
    if (charId == 10) return (y == 0 || y == 2 || y == 4 || (x == 0 && y == 3) || (x == 2 && y == 1)) ? 1.0 : 0.0; // S
    if (charId == 11) return (x == 0 || x == 2 || y == 2) ? 1.0 : 0.0; // H
    if (charId == 12) return (y == 4 || x == 1) ? 1.0 : 0.0; // T
    return 0.0;
}

float sampleText(vec2 pos) {
    vec2 cell = floor(pos / vec2(4.0, 6.0));
    int charIndex = int(cell.x);

    if (charIndex < 0 || charIndex > 21) return 0.0;

    int id = -1;
    if (charIndex == 0)  id = 0;  // L
    if (charIndex == 1)  id = 1;  // O
    if (charIndex == 2)  id = 2;  // A
    if (charIndex == 3)  id = 1;  // D
    if (charIndex == 4)  id = 3;  // I
    if (charIndex == 5)  id = 4;  // N
    if (charIndex == 6)  id = 5;  // G

    if (charIndex == 8)  id = 6;  // C
    if (charIndex == 9)  id = 7;  // R
    if (charIndex == 10) id = 2;  // A
    if (charIndex == 11) id = 6;  // C
    if (charIndex == 12) id = 8;  // K
    if (charIndex == 13) id = 9;  // E
    if (charIndex == 14) id = 1;  // D

    if (charIndex == 16) id = 1;  // D
    if (charIndex == 17) id = 1;  // O
    if (charIndex == 18) id = 5;  // G
    if (charIndex == 19) id = 10; // S
    if (charIndex == 20) id = 11; // H
    if (charIndex == 21) id = 3;  // I
    if (charIndex == 22) id = 12; // T

    if (id < 0) return 0.0;

    vec2 localPixel = mod(pos, vec2(4.0, 6.0));
    return charBit(id, localPixel);
}

void main() {
    vec2 frag = gl_FragCoord.xy;

    float bigSlice = floor(frag.y / (resolution.y * 0.15));
    float sliceNoise = memNoise(vec2(bigSlice, floor(time * 6.0)));
    
    if (sliceNoise > 0.4) {
        frag.x += (sliceNoise - 0.5) * resolution.x * 1.5;
    }

    vec2 macroBlock = floor(frag / vec2(80.0, 45.0)); 
    float macroHash = memNoise(macroBlock + floor(time * 12.0));

    if (macroHash > 0.55) {
        frag += (vec2(memNoise(macroBlock + 1.0), memNoise(macroBlock + 2.0)) - 0.5) * 350.0;
    }

    vec2 uv = (frag - vec2(0.5) * resolution.xy) / resolution.xx;

    vec3 col;
    float rOff = (sliceNoise - 0.5) * 0.3;
    float bOff = (macroHash - 0.5) * 0.25;
    
    col.r = rdr(uv + vec2(rOff, 0.0)).r;
    col.g = rdr(uv).g;
    col.b = rdr(uv - vec2(bOff, rOff * 0.5)).b;

    if (macroHash > 0.7) {
        col = 1.0 - col;
    } else if (sliceNoise > 0.75) {
        col.g = 1.0 - col.g;
    }

    if (sliceNoise > 0.65) {
        vec3 rain = rainbow(frag.x * 0.01 + frag.y * 0.005 + time * 5.0);
        col = mix(col, rain, 0.75);
    }

    if (macroHash > 0.82) {
        vec3 staticRainbow = rainbow(memNoise(gl_FragCoord.xy + floor(time * 30.0)) * 10.0);
        col = staticRainbow;
    }

    if (macroHash > 0.88) {
        col = 1.0 - rainbow(uv.y * 20.0 + time * 8.0);
    }

    col *= mix(-1.0, 1.0, 1.0 - sat(lenny(uv * 2.0) - 0.5));

    if (macroHash > 0.6) {
        col = fract(col * 2.5);
    } else {
        col = sat(col);
    }

    vec2 p1 = (gl_FragCoord.xy - vec2(mod(time * 180.0, resolution.x + 300.0) - 150.0, resolution.y - 60.0)) / 2.5;
    float text1 = sampleText(p1);

    vec2 p2 = (gl_FragCoord.xy - vec2(resolution.x * 0.15 + sin(time * 20.0) * 15.0, resolution.y * 0.5 + cos(time * 10.0) * 20.0)) / 3.5;
    float text2 = sampleText(p2);

    vec2 p3 = (gl_FragCoord.xy - vec2(resolution.x - mod(time * 220.0, resolution.x + 300.0), 40.0)) / 2.0;
    float text3 = sampleText(p3);

    vec3 rainbowCol1 = rainbow(gl_FragCoord.x * 0.005 + time * 4.0) * 2.0;
    vec3 rainbowCol2 = rainbow(gl_FragCoord.y * 0.01 - time * 6.0) * 2.5;

    col = mix(col, rainbowCol1, clamp(text1 + text3, 0.0, 1.0));
    col = mix(col, rainbowCol2, clamp(text2, 0.0, 1.0));

    gl_FragColor = vec4(sat(col), 1.0);
}
