#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform vec2 resolution;
uniform float time;
uniform vec2 alpha;

float FALLING_SPEED = 0.25;
float STRIPES_FACTOR = 5.0;

//get sphere
float sphere(vec2 coord, vec2 pos, float r) {
    vec2 d = pos - coord;
    return smoothstep(60.0, 0.0, dot(d, d) - r * r);
}


void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        vec3 iResolution = vec3(resolution, 1);
        float iTime = time;
        vec4 fragCoord = gl_FragCoord;


        vec2 uv         = fragCoord.xy / iResolution.xy;

        vec2 clamped_uv = (floor(fragCoord.xy / STRIPES_FACTOR + .5) * STRIPES_FACTOR) / iResolution.xy;

        float value		= fract(sin(clamped_uv.x) * 43758.5453123);

        vec3 col        = vec3(1.0 - mod(uv.y * 0.5 + (iTime * (FALLING_SPEED + value / 5.0)) + value, 0.5));

        col       *= clamp(cos(iTime * 2.0 + uv.xyx + vec3(0, 2, 4)), 0.0, 1.0);

        col 	   += vec3(sphere(fragCoord.xy,
        vec2(clamped_uv.x, (1.0 - 2.0 * mod((iTime * (FALLING_SPEED + value / 5.0)) + value, 0.5))) * iResolution.xy,
        0.9)) / 2.0;

        col       *= vec3(exp(-pow(abs(uv.y - 0.5), 6.0) / pow(2.0 * 0.05, 2.0)));

        gl_FragColor=vec4(col, alpha.x);
    } else {
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
                if (currentColor.a != 0) {
                    vec3 iResolution = vec3(resolution, 1);
                    float iTime = time;
                    vec4 fragCoord = gl_FragCoord;


                    vec2 uv         = fragCoord.xy / iResolution.xy;

                    vec2 clamped_uv = (floor(fragCoord.xy / STRIPES_FACTOR + .5) * STRIPES_FACTOR) / iResolution.xy;

                    float value		= fract(sin(clamped_uv.x) * 43758.5453123);

                    vec3 col        = vec3(1.0 - mod(uv.y * 0.5 + (iTime * (FALLING_SPEED + value / 5.0)) + value, 0.5));

                    col       *= clamp(cos(iTime * 2.0 + uv.xyx + vec3(0, 2, 4)), 0.0, 1.0);

                    col 	   += vec3(sphere(fragCoord.xy,
                    vec2(clamped_uv.x, (1.0 - 2.0 * mod((iTime * (FALLING_SPEED + value / 5.0)) + value, 0.5))) * iResolution.xy,
                    0.9)) / 2.0;

                    col       *= vec3(exp(-pow(abs(uv.y - 0.5), 6.0) / pow(2.0 * 0.05, 2.0)));

                    gl_FragColor=vec4(col, alpha.y);
            }
        }
    }
    }
}