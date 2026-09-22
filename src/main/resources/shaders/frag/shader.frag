#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float divider;
uniform float maxSample;

uniform vec2 resolution;
uniform float time;

uniform vec4 insideColor;
uniform int insideMode;

uniform vec4 outsideColor;
uniform int outsideMode;

uniform int glow;

// Rainbow uniforms
uniform int rainbowEnabled;
uniform int rainbowType;
uniform float rainbowSpeed;
uniform float rainbowSpread;
uniform float rainbowSaturation;
uniform float rainbowLightness;
uniform float rainbowHueOffset;

// Bloom uniforms
uniform int   bloomEnabled;
uniform float bloomIntensity;
uniform float bloomRadius;
uniform float outputAlpha;
uniform int   bloomQuality;

// Gradient uniforms
uniform int   gradientEnabled;
uniform vec4  gradientColor1;
uniform vec4  gradientColor2;
uniform float gradientSpread;
uniform float gradientSpeed;

const float INV_RESOLUTION_SCALE = 1.0 / 800.0;

// ==================== RAINBOW ====================

vec3 hsl2rgb(float h, float s, float l) {
    float c = (1.0 - abs(2.0 * l - 1.0)) * s;
    float x = c * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
    float m = l - c / 2.0;
    vec3 rgb;
    if      (h < 1.0/6.0) rgb = vec3(c, x, 0.0);
    else if (h < 2.0/6.0) rgb = vec3(x, c, 0.0);
    else if (h < 3.0/6.0) rgb = vec3(0.0, c, x);
    else if (h < 4.0/6.0) rgb = vec3(0.0, x, c);
    else if (h < 5.0/6.0) rgb = vec3(x, 0.0, c);
    else                   rgb = vec3(c, 0.0, x);
    return rgb + m;
}

vec3 rainbowNormal() {
    float hue = fract((time * 100.0 * rainbowSpeed) / 360.0);
    return hsl2rgb(hue, rainbowSaturation, rainbowLightness);
}

vec3 rainbowGradient() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    float positionOffset = (uv.x + uv.y) * rainbowSpread * rainbowHueOffset * 100.0;
    float hue = fract((time * 100.0 * rainbowSpeed + positionOffset) / 360.0);
    return hsl2rgb(hue, rainbowSaturation, rainbowLightness);
}

vec3 getRainbowColor() {
    if (rainbowType == 0) return rainbowNormal();
    return rainbowGradient();
}

// ==================== GRADIENT ====================

vec3 getGradientColor() {
    // Fixed framebuffer UVs keep the gradient locked to the screen instead of
    // rotating or changing direction with the camera.
    vec2 uv = gl_TexCoord[0].xy;
    float vertical = 1.0 - uv.y;

    // Move the two-color gradient continuously along the fixed vertical axis.
    // A triangle wave lets color 1 flow into color 2 and back into color 1
    // without creating a hard seam when the animation wraps.
    float phase = fract(vertical * gradientSpread - time * gradientSpeed);
    float t = 1.0 - abs(phase * 2.0 - 1.0);
    t = smoothstep(0.0, 1.0, t);

    return mix(gradientColor1.rgb, gradientColor2.rgb, t);
}

// ==================== JITTER HELPER ====================

// Rotates a 2D offset by angle (ca=cos, sa=sin).
// Applied per-pixel with a hash angle to break up the fixed sample ring pattern.
vec2 rot2(vec2 v, float ca, float sa) {
    return vec2(v.x * ca - v.y * sa, v.x * sa + v.y * ca);
}

// Per-pixel pseudo-random angle in [0, 2*PI) based on screen position.
float jitterAngle() {
    float h = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    return h * 6.28318;
}

// ==================== BLOOM ====================

vec4 computeBloom(vec3 bloomSourceColor) {
    if (bloomEnabled == 0) return vec4(0.0);

    vec2 rawTexel = vec2(1.0 / resolution.x, 1.0 / resolution.y);
    vec2 uv       = gl_TexCoord[0].xy;

    // Per-pixel jitter: rotate all sample rings by a random angle so the fixed
    // octagonal grid pattern becomes imperceptible noise.
    float jAngle = jitterAngle();
    float jCa = cos(jAngle), jSa = sin(jAngle);

    float gr = bloomRadius;
    bool nearEntity =
        texture2D(texture, uv + rot2(vec2( gr,  0.0), jCa, jSa) * rawTexel).a > 0.0 ||
        texture2D(texture, uv + rot2(vec2(-gr,  0.0), jCa, jSa) * rawTexel).a > 0.0 ||
        texture2D(texture, uv + rot2(vec2( 0.0,  gr), jCa, jSa) * rawTexel).a > 0.0 ||
        texture2D(texture, uv + rot2(vec2( 0.0, -gr), jCa, jSa) * rawTexel).a > 0.0;
    if (!nearEntity) return vec4(0.0);

    float accumW = 0.0;
    float totalW = 0.0;

    totalW += 0.30;
    accumW += 0.30 * step(0.001, texture2D(texture, uv).a);

    {
        float r1 = bloomRadius * 0.25;
        float d1 = r1 * 0.707;
        totalW += 0.80;
        float hits =
            step(0.001, texture2D(texture, uv + rot2(vec2( r1, 0.0), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( d1,  d1), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(0.0,  r1), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-d1,  d1), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r1, 0.0), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-d1, -d1), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(0.0, -r1), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( d1, -d1), jCa, jSa) * rawTexel).a);
        accumW += 0.10 * hits;
    }

    if (bloomQuality >= 1) {
        float r2  = bloomRadius * 0.55;
        float r2a = r2 * 0.924;
        float r2b = r2 * 0.383;
        totalW += 0.40;
        float hits =
            step(0.001, texture2D(texture, uv + rot2(vec2( r2a,  r2b), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( r2b,  r2a), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r2b,  r2a), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r2a,  r2b), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r2a, -r2b), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r2b, -r2a), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( r2b, -r2a), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( r2a, -r2b), jCa, jSa) * rawTexel).a);
        accumW += 0.05 * hits;
    }

    if (bloomQuality >= 2) {
        float r3 = bloomRadius * 0.90;
        float d3 = r3 * 0.707;
        totalW += 0.16;
        float hits =
            step(0.001, texture2D(texture, uv + rot2(vec2( r3, 0.0), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( d3,  d3), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(0.0,  r3), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-d3,  d3), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-r3, 0.0), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(-d3, -d3), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2(0.0, -r3), jCa, jSa) * rawTexel).a) +
            step(0.001, texture2D(texture, uv + rot2(vec2( d3, -d3), jCa, jSa) * rawTexel).a);
        accumW += 0.02 * hits;
    }

    if (accumW <= 0.0) return vec4(0.0);

    float coverage   = accumW / max(totalW, 0.0001);
    // sqrt spreads the glow outward (concave lift), smoothstep eases hard edges —
    // together they approximate a natural gaussian falloff instead of a linear ramp.
    float curved     = smoothstep(0.0, 1.0, sqrt(clamp(coverage, 0.0, 1.0)));
    float bloomAlpha = clamp(curved * bloomIntensity, 0.0, 1.0);
    return vec4(bloomSourceColor, bloomAlpha);
}

float computeInnerBloom(vec3 bloomColor) {
    if (bloomEnabled == 0) return 0.0;

    vec2 rawTexel = vec2(1.0 / resolution.x, 1.0 / resolution.y);
    vec2 uv       = gl_TexCoord[0].xy;

    // Same jitter as computeBloom — identical hash so both sides rotate consistently.
    float jAngle = jitterAngle();
    float jCa = cos(jAngle), jSa = sin(jAngle);

    float gr = bloomRadius;
    bool nearEdge =
        texture2D(texture, uv + rot2(vec2( gr,  0.0), jCa, jSa) * rawTexel).a == 0.0 ||
        texture2D(texture, uv + rot2(vec2(-gr,  0.0), jCa, jSa) * rawTexel).a == 0.0 ||
        texture2D(texture, uv + rot2(vec2( 0.0,  gr), jCa, jSa) * rawTexel).a == 0.0 ||
        texture2D(texture, uv + rot2(vec2( 0.0, -gr), jCa, jSa) * rawTexel).a == 0.0;
    if (!nearEdge) return 0.0;

    float emptyW = 0.0;
    float totalW = 0.0;

    totalW += 0.30;

    {
        float r1 = bloomRadius * 0.25;
        float d1 = r1 * 0.707;
        totalW += 0.80;
        float hits =
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r1, 0.0), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( d1,  d1), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(0.0,  r1), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-d1,  d1), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r1, 0.0), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-d1, -d1), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(0.0, -r1), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( d1, -d1), jCa, jSa) * rawTexel).a));
        emptyW += 0.10 * hits;
    }

    if (bloomQuality >= 1) {
        float r2  = bloomRadius * 0.55;
        float r2a = r2 * 0.924;
        float r2b = r2 * 0.383;
        totalW += 0.40;
        float hits =
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r2a,  r2b), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r2b,  r2a), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r2b,  r2a), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r2a,  r2b), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r2a, -r2b), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r2b, -r2a), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r2b, -r2a), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r2a, -r2b), jCa, jSa) * rawTexel).a));
        emptyW += 0.05 * hits;
    }

    if (bloomQuality >= 2) {
        float r3 = bloomRadius * 0.90;
        float d3 = r3 * 0.707;
        totalW += 0.16;
        float hits =
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( r3, 0.0), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( d3,  d3), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(0.0,  r3), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-d3,  d3), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-r3, 0.0), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(-d3, -d3), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2(0.0, -r3), jCa, jSa) * rawTexel).a)) +
            (1.0 - step(0.001, texture2D(texture, uv + rot2(vec2( d3, -d3), jCa, jSa) * rawTexel).a));
        emptyW += 0.02 * hits;
    }

    if (emptyW <= 0.0) return 0.0;

    float edgeProximity = emptyW / max(totalW, 0.0001);
    // Exact same curve as computeBloom — sqrt + smoothstep — for perfect symmetry.
    float curved        = smoothstep(0.0, 1.0, sqrt(clamp(edgeProximity, 0.0, 1.0)));
    return clamp(curved * bloomIntensity, 0.0, 1.0);
}

// ==================== EXISTING SHADER FUNCTIONS ====================

vec4 simple(vec4 color) {
    return color;
}

vec4 matter(vec4 color) {
    #define iterations  14
    #define formuparam  0.530
    #define volsteps    18
    #define stepsize    0.2
    #define zoom        0.800
    #define tile        0.850
    #define speed       0.00001
    #define brightness  0.00045
    #define darkmatter  0.400
    #define distfading  0.760
    #define saturation  0.800

    const float DIR_Z      = 100.33467;
    const float DARK_MULT  =   1.26016;
    const float FADE_DECAY =   0.63333;

    vec2 uv = gl_FragCoord.xy / resolution.xy;
    uv.y   *= resolution.y / resolution.x;

    vec3 dir  = vec3(uv * zoom, DIR_Z);

    float time1 = cos(time * 0.2);

    vec3 from = vec3(-93.0, -102.0, -2.0);

    float formParamCalc = formuparam * (abs(sin(time1 * 0.2)) + 0.6);

    const vec3 tileVec  = vec3(tile);
    const vec3 tile2Vec = vec3(tile * 2.0);

    float s = 0.4, fade = 0.2;
    vec3  v = vec3(0.9);

    for (int r = 0; r < volsteps; r++) {
        vec3 p = abs(tileVec - mod(from + s * dir * 0.5, tile2Vec));
        float pa, a = pa = 16.0;

        for (int i = 0; i < iterations; i++) {
            float dotPP = dot(p, p);
            p  = abs(p) / dotPP - formParamCalc;
            float lenP = length(p);
            a += abs(lenP - pa);
            pa = lenP;
        }

        float dm = max(0.0, darkmatter - a * a * DARK_MULT);
        a *= a * a * 2.0;
        if (r > 3) fade *= 1.0 - dm;
        v    += fade;
        float s2 = s * s;
        v    += vec3(s2, s2 * s, s2 * s2) * (a * brightness * fade);
        fade *= FADE_DECAY;
        s    += stepsize;
    }

    v = mix(vec3(length(v)), v, saturation);
    return vec4(v * 0.01, color.a);
}

float field(in vec3 p, float s, int idx) {
    float strength = 25. + .1 * log(2.e-6 + fract(sin(time) * 4373.11));
    float accum = s/2.;
    float prev = 0.;
    float tw = 0.;
    for (int i = 0; i < 26; ++i) {
        float mag = dot(p, p);
        p = abs(p) / mag + vec3(-.6, -.2, -1.1);
        float w = exp(-float(i) * 0.25);
        accum += w * exp(-strength * pow(abs(mag - prev), 3.2));
        tw += w;
        prev = mag;
    }
    return max(0., 5.6 * accum / tw - .6);
}

vec3 nrand3(vec2 co) {
    vec3 a = fract(cos(co.x*4.3e-3 + co.y)*vec3(1.3e5, 4.7e5, 2.9e5));
    vec3 b = fract(sin(co.x*0.3e-3 + co.y)*vec3(8.1e5, 1.0e5, 0.1e5));
    return mix(a, b, .9);
}

vec4 pinkGalaxy(vec4 color) {
    vec2 uv = 2. * gl_FragCoord.xy / resolution.xy - 1.;
    float maxRes = max(resolution.x, resolution.y);
    vec2 uvs = uv * resolution.xy / maxRes;
    vec3 p = vec3(uvs / 4., 0) + vec3(1., -1.3, 0.);
    p += .2 * vec3(sin(time/999.), sin(time/9999999.), sin(time/9999999.));

    float freqs[4];
    freqs[0] = 0.15;
    freqs[1] = 0.0;
    freqs[2] = 0.4;
    freqs[3] = 0.7;

    float t = field(p, freqs[3], 126);
    float expX = exp((abs(uv.x) - 1.) * 6.);
    float expY = exp((abs(uv.y) - 1.) * 6.);
    float v = (1. - expX) * (1. - expY);

    vec3 p2 = vec3(uvs / (4.+sin(time*.11)*0.2+0.2+sin(time*0.15)*0.3+0.1), 1.5) + vec3(2., -1.3, -1.);
    p2 += 0.1 * vec3(sin(time/99999999999.), sin(time/99999999999.), sin(time/9999999999999.));
    float t2 = field(p2, freqs[3], 18);
    vec4 c2 = mix(.1, .2, v) * vec4(1.3*t2*t2*t2, 1.8*t2*t2, t2*freqs[0], t2);

    vec4 starcolor = vec4(pow(nrand3(floor(p.xy * resolution.x)).y, 50.0));
    starcolor += vec4(pow(nrand3(floor(p2.xy * 3.0 * resolution.x)).y, 400.0));

    return vec4(mix(freqs[3]-.5, 1., 3.0) * vec4(1.5*freqs[2]*t, .2*freqs[1]*t, freqs[3]*t, 1.0) + c2 + starcolor);
}

vec4 deepSpace(vec4 color) {
    float time_ = (time+10.) * 30.0;
    vec2 uv = (-resolution.xy + 2.0 * gl_FragCoord.xy) / resolution;
    float s = 0.0;
    float v = -0.010;
    uv.x += sin(time_*0.005) * 0.15;
    vec3 col_ = vec3(0.0);
    vec3 init = vec3(-0.25, -0.25 + sin(time_*0.001)*0.4, floor(time_)*0.0008);
    vec3 uvVec = vec3(uv, 0.143);
    for (int r = 0; r < 100; r++) {
        vec3 p = init + s * uvVec;
        p.z = mod(-p.z, 2.0) - 1.0;
        for (int i = 0; i < 10; i++) {
            p = abs(p * 2.003) / dot(p,p) - 0.75;
        }
        v += length(p*p) * smoothstep(0.0, 0.1, 0.9 - s) * .0005;
        col_ += vec3(v*0.02);
        s += .005;
    }
    return vec4(col_, color.a);
}

float colormap_red(float x) {
    if (x < 0.0) return 54.0/255.0;
    else if (x < 20049.0/82979.0) return (829.79*x + 54.51)/255.0;
    return 1.0;
}
float colormap_green(float x) {
    if (x < 20049.0/82979.0) return 0.0;
    else if (x < 327013.0/810990.0) return (8546482679670.0/10875673217.0*x - 2064961390770.0/10875673217.0)/255.0;
    else if (x <= 1.0) return (103806720.0/483977.0*x + 19607415.0/483977.0)/255.0;
    return 1.0;
}
float colormap_blue(float x) {
    if (x < 0.0) return 54.0/255.0;
    else if (x < 7249.0/82979.0) return (829.79*x + 54.51)/255.0;
    else if (x < 20049.0/82979.0) return 127.0/255.0;
    else if (x < 327013.0/810990.0) return (792.022*x - 64.365)/255.0;
    return 1.0;
}
vec4 colormap_fbm(float x) { return vec4(colormap_red(x), colormap_green(x), colormap_blue(x), 1.0); }

float rand_fbm(vec2 n) { return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453); }
float noise_fbm(vec2 p) {
    vec2 ip = floor(p), u = fract(p);
    u = u*u*(3.0-2.0*u);
    return mix(mix(rand_fbm(ip), rand_fbm(ip+vec2(1,0)), u.x),
               mix(rand_fbm(ip+vec2(0,1)), rand_fbm(ip+vec2(1,1)), u.x), u.y);
}
const mat2 mtx_fbm = mat2(0.80, 0.60, -0.60, 0.80);
float fbm(vec2 p) {
    float f = 0.0;
    f += 0.500000*noise_fbm(p+time);   p = mtx_fbm*p*2.02;
    f += 0.031250*noise_fbm(p);        p = mtx_fbm*p*2.01;
    f += 0.250000*noise_fbm(p);        p = mtx_fbm*p*2.03;
    f += 0.125000*noise_fbm(p);        p = mtx_fbm*p*2.01;
    f += 0.062500*noise_fbm(p);        p = mtx_fbm*p*2.04;
    f += 0.015625*noise_fbm(p+sin(time));
    return f/0.96875;
}
float pattern_fbm(in vec2 p) { return fbm(p + fbm(p + fbm(p))); }
vec4 pinkWarp(vec4 color) {
    vec2 uv = gl_FragCoord.xy / resolution.x;
    return vec4(colormap_fbm(pattern_fbm(uv)).rgb, color.a);
}

mat2 makem2(in float theta) { float c=cos(theta),s=sin(theta); return mat2(c,-s,s,c); }
float hash_watery(vec2 p) { vec3 p3 = fract(vec3(p.xyx)*0.13); p3 += dot(p3,p3.yzx+3.333); return fract((p3.x+p3.y)*p3.z); }
float noise_watery(in vec2 x) {
    vec2 p=x*0.01, i=floor(p), f=fract(p);
    f = f*f*(3.0-2.0*f);
    return mix(mix(hash_watery(i),hash_watery(i+vec2(1,0)),f.x),
               mix(hash_watery(i+vec2(0,1)),hash_watery(i+vec2(1,1)),f.x),f.y);
}
mat2 m2_watery = mat2(0.80, 0.60, -0.60, 0.80);
float fbm_watery(in vec2 p) {
    float z=2., rz=0.;
    for (float i=1.; i<7.; i++) { rz+=abs((noise_watery(p)-0.5)*2.)/z; z*=2.; p=p*2.*m2_watery; }
    return rz;
}
vec4 watery(vec4 color) {
    float tw = time*0.2;
    vec2 p = gl_FragCoord.xy/resolution.xy*2.-1.;
    p.x *= resolution.x/resolution.y;
    vec2 bp = p;
    p = (p+5.)*0.6;
    float rb = sqrt(fbm_watery(p*.5+tw*.17)*.1);
    p *= makem2(rb*.2 + atan(p.y,p.x));
    float rz = fbm_watery(p*.9-tw*.7) * (dot(bp*5.,bp)+.5) * sin(p.x*.5+tw*4.)*1.5;
    return vec4(sqrt(abs(vec3(.04,0.07,0.45)/(.1-rz))), color.a);
}

// ==================== PURPLE GALAXY ====================

float pg_Star(vec2 uv, float flare) {
    float d = length(uv);
    float m = sin(0.025 * 1.2) / d;
    float rays = max(0.0, 0.5 - abs(uv.x * uv.y * 1000.0));
    m += (rays * flare) * 2.0;
    m *= smoothstep(1.0, 0.1, d);
    return m;
}

float pg_Hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 pg_StarLayer(vec2 uv) {
    vec3 col = vec3(0.0);
    vec2 gv = fract(uv);
    vec2 id = floor(uv);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offs  = vec2(float(x), float(y));
            float n    = pg_Hash21(id + offs);
            float size = fract(n);
            float star = pg_Star(
                gv - offs - vec2(n, fract(n * 34.0)) + 0.5,
                smoothstep(0.1, 0.9, size) * 0.46
            );
            vec3 starColor = sin(vec3(0.2, 0.3, 0.9) * fract(n * 2345.2) * 6.28318) * 0.25 + 0.75;
            starColor *= vec3(0.9, 0.59, 0.9 + size);
            star *= sin(time * 0.6 + n * 6.28318) * 0.5 + 0.5;
            col += star * size * starColor;
        }
    }
    return col;
}

vec4 purpleGalaxy(vec4 color) {
    vec2 uv = (gl_FragCoord.xy - 0.5 * resolution.xy) / resolution.y;
    vec2 M  = vec2(-sin(time * 0.22), cos(time * 0.22));
    float t = time * 0.025;
    vec3 col = vec3(0.0);
    for (float i = 0.0; i < 1.0; i += 1.0 / 8.0) {
        float depth = fract(i + t);
        float scale = mix(20.0, 0.5, depth);
        float fade  = depth * smoothstep(1.0, 0.9, depth);
        col += pg_StarLayer(uv * scale + i * 453.2 - time * 0.05 + M) * fade;
    }
    return vec4(col, color.a);
}

// ==================== MAIN ====================

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);
    vec4 color = vec4(1.0);

    if (centerCol.a != 0.0) {
        // ---- INSIDE: entity pixel ----
        vec3 finalRGB;
        if (gradientEnabled == 1) {
            finalRGB = getGradientColor();
        } else if (rainbowEnabled == 1) {
            finalRGB = getRainbowColor();
        } else {
            if      (insideMode == 0) color = simple(insideColor);
            else if (insideMode == 1) color = matter(insideColor);
            else if (insideMode == 2) color = pinkGalaxy(insideColor);
            else if (insideMode == 3) color = deepSpace(insideColor);
            else if (insideMode == 4) color = pinkWarp(insideColor);
            else if (insideMode == 5) color = watery(insideColor);
            else if (insideMode == 6) color = purpleGalaxy(insideColor);
            finalRGB = color.rgb;
        }

        float finalPixelAlpha = insideColor.a;

        if (bloomEnabled == 1) {
            // Determine the inner bloom color — mirrors whatever the outside/outline
            // color source is, but brightened so it reads as a glow against the fill.
            vec3 innerBloomColor;
            if (gradientEnabled == 1) {
                innerBloomColor = getGradientColor();
            } else if (rainbowEnabled == 1) {
                innerBloomColor = getRainbowColor();
            } else {
                // Use the current inside color as-is — the color itself doesn't need
                // to shift, only the additive alpha below creates the rim glow effect.
                innerBloomColor = finalRGB;
            }

            float edgeProx      = computeInnerBloom(innerBloomColor);
            finalRGB            = mix(finalRGB, innerBloomColor, edgeProx);

            // FIX: additive alpha instead of max() so the inner rim actually brightens
            // symmetrically with the outer halo rather than being clamped to the base alpha.
            finalPixelAlpha     = clamp(insideColor.a + edgeProx, 0.0, 1.0);
        }

        gl_FragColor = vec4(finalRGB, finalPixelAlpha);

    } else {
        // ---- OUTSIDE: outline / bloom halo ----

        float alpha    = 0.0;
        bool foundEdge = false;
        for (float x = -radius; x <= radius && (glow == 1 || !foundEdge); x++) {
            for (float y = -radius; y <= radius && (glow == 1 || !foundEdge); y++) {
                vec4 s = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x*x, texelSize.y*y));
                if (s.a != 0.0) {
                    foundEdge = true;
                    if (glow == 0) { alpha = 1.0; break; }
                    else { float d = distance(vec2(x,y), vec2(0)); alpha += divider > 0.0 ? max(0.0,(maxSample-d)/divider) : 1.0; }
                }
            }
        }

        vec3 outlineRGB;
        if (gradientEnabled == 1) {
            outlineRGB = getGradientColor();
        } else if (rainbowEnabled == 1) {
            outlineRGB = getRainbowColor();
        } else {
            if      (outsideMode == 0) color = simple(outsideColor);
            else if (outsideMode == 1) color = matter(outsideColor);
            else if (outsideMode == 2) color = pinkGalaxy(outsideColor);
            else if (outsideMode == 3) color = deepSpace(outsideColor);
            else if (outsideMode == 4) color = pinkWarp(outsideColor);
            else if (outsideMode == 5) color = watery(outsideColor);
            else if (outsideMode == 6) color = purpleGalaxy(outsideColor);
            outlineRGB = color.rgb;
        }

        float bloomAlpha = 0.0;
        if (glow == 0) {
            vec4 bloomContrib = computeBloom(outlineRGB);
            bloomAlpha = bloomContrib.a;
        }

        float finalAlpha = clamp(alpha + bloomAlpha, 0.0, 1.0);
        gl_FragColor = vec4(finalAlpha > 0.0 ? outlineRGB : vec3(0.0), finalAlpha);
    }

    // Per-pass opacity. Defaults to 1.0, so the shader keeps its original
    // appearance everywhere except callers that explicitly request a fade.
    gl_FragColor.a *= clamp(outputAlpha, 0.0, 1.0);
}
