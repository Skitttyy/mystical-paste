#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform vec2 resolution;
uniform float time;
uniform vec2 alpha;

float field(in vec3 p, float s, int idx) {
    float strength = 25. + .1 * log(2.e-6 + fract(sin(time) * 4373.11));
    float accum = s/2.;
    float prev = 0.;
    float tw = 0.;
    for (int i = 0; i < 26; ++i) {
        float mag = dot(p, p);
        p = abs(p) / mag + vec3(-.6, -.2, -1.1);
        float w = exp(-float(i) / 4.);
        accum += w * exp(-strength * pow(abs(mag - prev), 3.2));
        tw += w;
        prev = mag;
    }
    return max(0., 5.6 * accum / tw - .6);
}


vec3 nrand3(vec2 co) {
    vec3 a = fract( cos( co.x*4.3e-3 + co.y )*vec3(1.3e5, 4.7e5, 2.9e5) );
    vec3 b = fract( sin( co.x*0.3e-3 + co.y )*vec3(8.1e5, 1.0e5, 0.1e5) );
    vec3 c = mix(a, b, .9);
    return c;
}

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        vec2 uv = 2. * gl_FragCoord.xy / resolution.xy - 1.;
        vec2 uvs = uv * resolution.xy / max(resolution.x, resolution.y);
        vec3 p = vec3(uvs / 4., 0) + vec3(1., -1.3, 0.);
        p += .2 * vec3(sin(time / 999.), sin(time / 9999999.), sin(time / 9999999.));

        float freqs[4];
        freqs[0] = 0.15;
        freqs[1] = 0.0;
        freqs[2] = 0.4;
        freqs[3] = 0.7;

        float t = field(p, freqs[3],126);
        float v = (1. - exp((abs(uv.x) - 1.) * 6.)) * (1. - exp((abs(uv.y) - 1.) * 6.));

        vec3 p2 = vec3(uvs / (4.+sin(time*.11)*0.2+0.2+sin(time*0.15)*0.3+0.1), 1.5) + vec3(2., -1.3, -1.);
        p2 += 0.1 * vec3(sin(time / 99999999999.), sin(time / 99999999999.), sin(time / 9999999999999.)); //speed
        float t2 = field(p2, freqs[3], 18);
        vec4 c2 =  mix(.1, .2, v) * vec4(1.3 * t2 * t2 * t2, 1.8  * t2 * t2, t2* freqs[0], t2);

        vec2 seed = p.xy;
        seed = floor(seed * resolution.x);
        vec3 rnd = nrand3(seed);
        vec4 starcolor = vec4(pow(rnd.y, 50.0));

        vec2 seed2 = p2.xy * 3.0;
        seed2 = floor(seed2 * resolution.x);
        vec3 rnd2 = nrand3(seed2);
        starcolor += vec4(pow(rnd2.y, 400.0));

        vec3 backclr = vec3(0.1,0,0);

        gl_FragColor = vec4(vec4(mix(freqs[3]-.5, 1., 3.0) * vec4(1.5*freqs[2] * t + backclr.r * t* t, .2*freqs[1] * t + backclr.g * t, freqs[3]*t + backclr.b, 1.0) +c2+starcolor).rgb, alpha.x);
    } else {
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
                if (currentColor.a != 0) {
                    vec2 uv = 2. * gl_FragCoord.xy / resolution.xy - 1.;
                    vec2 uvs = uv * resolution.xy / max(resolution.x, resolution.y);
                    vec3 p = vec3(uvs / 4., 0) + vec3(1., -1.3, 0.);
                    p += .2 * vec3(sin(time / 999.), sin(time / 9999999.), sin(time / 9999999.));

                    float freqs[4];
                    freqs[0] = 0.15;
                    freqs[1] = 0.0;
                    freqs[2] = 0.4;
                    freqs[3] = 0.7;

                    float t = field(p, freqs[3],126);
                    float v = (1. - exp((abs(uv.x) - 1.) * 6.)) * (1. - exp((abs(uv.y) - 1.) * 6.));

                    vec3 p2 = vec3(uvs / (4.+sin(time*.11)*0.2+0.2+sin(time*0.15)*0.3+0.1), 1.5) + vec3(2., -1.3, -1.);
                    p2 += 0.1 * vec3(sin(time / 99999999999.), sin(time / 99999999999.), sin(time / 9999999999999.)); //speed
                    float t2 = field(p2, freqs[3], 18);
                    vec4 c2 =  mix(.1, .2, v) * vec4(1.3 * t2 * t2 * t2, 1.8  * t2 * t2, t2* freqs[0], t2);

                    vec2 seed = p.xy;
                    seed = floor(seed * resolution.x);
                    vec3 rnd = nrand3(seed);
                    vec4 starcolor = vec4(pow(rnd.y, 50.0));

                    vec2 seed2 = p2.xy * 3.0;
                    seed2 = floor(seed2 * resolution.x);
                    vec3 rnd2 = nrand3(seed2);
                    starcolor += vec4(pow(rnd2.y, 400.0));

                    vec3 backclr = vec3(0.1,0,0);

                    gl_FragColor = vec4(vec4(mix(freqs[3]-.5, 1., 3.0) * vec4(1.5*freqs[2] * t + backclr.r * t* t, .2*freqs[1] * t + backclr.g * t, freqs[3]*t + backclr.b, 1.0) +c2+starcolor).rgb, alpha.y);
            }
        }
    }
    }
}