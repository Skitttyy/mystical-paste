#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform vec2 resolution;
uniform float time;
uniform vec2 alpha;

#define complexity 40.0
#define fluid_speed 100.0
#define color_intensity 0.5

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        vec2 p = (2.0 * gl_FragCoord.xy - resolution) / max(resolution.x, resolution.y);

        for (float i = 1.0; i < complexity; i++) {
            vec2 np = p + time * 0.001;
            np.x += 0.6 / i * sin(i * p.y + time / fluid_speed + 20.3 * i) + 0.5;
            np.y += 0.6 / i * sin(i * p.x + time / fluid_speed + 0.3 * (i + 10.0)) - 0.5;
            p = np;
        }

        float f1 = color_intensity * sin(3.0 * p.y) + color_intensity;
        float f2 = 0.001 * color_intensity * sin(p.x + p.y) + color_intensity;

        vec3 col = vec3(f1 / 2.0 + f2, f1 / 2.0, 0.0); // play here for colors

        gl_FragColor = vec4(col, alpha.x);
    } else {
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));

                if (currentColor.a != 0) {
                    vec2 p = (2.0 * gl_FragCoord.xy - resolution) / max(resolution.x, resolution.y);

                    for (float i = 1.0; i < complexity; i++) {
                        vec2 np = p + time * 0.001;
                        np.x += 0.6 / i * sin(i * p.y + time / fluid_speed + 20.3 * i) + 0.5;
                        np.y += 0.6 / i * sin(i * p.x + time / fluid_speed + 0.3 * (i + 10.0)) - 0.5;
                        p = np;
                    }

                    float f1 = color_intensity * sin(3.0 * p.y) + color_intensity;
                    float f2 = 0.001 * color_intensity * sin(p.x + p.y) + color_intensity;

                    vec3 col = vec3(f1 / 2.0 + f2, f1 / 2.0, 0.0); // play here for colors

                    gl_FragColor = vec4(col, alpha.y);
                }
            }
        }
    }
}