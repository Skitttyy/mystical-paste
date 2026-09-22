#version 120

uniform sampler2D texture;
uniform sampler2D image;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform vec2 resolution;
uniform float time;
uniform vec2 alpha;

const vec3 tint = vec3(0.1,.9,.4);
const vec3 bgColor = vec3(1,0,1);

#define MODE_1
#define ITERATIONS 20.

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        vec2 p=(3.0*gl_FragCoord.xy-resolution)/min(resolution.x,resolution.y);

        for(float i = 1.; i < ITERATIONS; ++i)
        {
            p.x += .50 / i * sin(i * p.y) + 1. + time * 0.1;
            p.y += .50 / i * cos(i * p.x) + 2. + time * 0.001;
        }
        vec3 col;

        #ifdef MODE_1
        col=vec3(sin(p.x), sin(p.y), 1);
        #endif

        #ifdef MODE_2
        col = bgColor - vec3(sin(p.x) - sin(p.y));
        #endif

        #ifdef MODE_3
        col=vec3(sin(p.x) - sin(p.y), sin(p.y) - sin(p.x), cos(p.x) - cos(p.y));
        #endif

        gl_FragColor=vec4(col, alpha.x);
    } else {
    for (float x = -radius; x <= radius; x++) {
        for (float y = -radius; y <= radius; y++) {
            vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
            if (currentColor.a != 0) {
                vec2 p=(3.0*gl_FragCoord.xy-resolution)/min(resolution.x,resolution.y);

                for(float i = 1.; i < ITERATIONS; ++i)
                {
                    p.x += .50 / i * sin(i * p.y) + 1. + time * 0.1;
                    p.y += .50 / i * cos(i * p.x) + 2. + time * 0.001;
                }
                vec3 col;

                #ifdef MODE_1
                col=vec3(sin(p.x), sin(p.y), 1);
                #endif

                #ifdef MODE_2
                col = bgColor - vec3(sin(p.x) - sin(p.y));
                #endif

                #ifdef MODE_3
                col=vec3(sin(p.x) - sin(p.y), sin(p.y) - sin(p.x), cos(p.x) - cos(p.y));
                #endif

                gl_FragColor=vec4(col, alpha.y);
            }
        }
    }
    }
}