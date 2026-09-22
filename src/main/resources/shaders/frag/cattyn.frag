#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;
uniform vec2 resolution;
uniform float time;
uniform vec2 alpha;

vec3 col;

float rect(vec2 p, vec2 c, vec2 rad)
{
    vec2 d = abs(p - c) - rad;
    return max(d.x, d.y);
}

float ring(vec2 p, vec2 c, float r1, float r2)
{
    float d = abs(length(p-c)-r1)-r2;
    if(p.x < c.x) d = 1.;	// semi-ring
    return d;
}


float sub(float a, float b)
{
    return max(a, -b);
}

float add(float a, float b)
{
    return min(a, b);
}

vec2 rot(vec2 v, float angle)
{
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c)*v;
}


float Krym(vec2 uv)
{

    float sd = 1.;

    sd = add(sd, rect(uv, vec2(-1.7,0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(vec2(uv.x+uv.y*0.6,uv.y), vec2(-1.1,0.4), vec2(0.11,0.25)));
    sd = add(sd, rect(vec2(uv.x-uv.y*0.6,uv.y), vec2(-1.88,0.9), vec2(0.11,0.25)));

    sd = add(sd, rect(uv,vec2(-.75,0.65), vec2(0.1,0.5)));
    sd = add(sd, ring(uv, vec2(-.65,0.8), 0.25, 0.1));

    sd = add(sd, rect(uv, vec2(.05,.65), vec2(0.1,0.5)));
    sd = add(sd, ring(uv, vec2(.15,0.5), 0.25, 0.1));
    sd = add(sd, rect(uv, vec2(.7,.65), vec2(0.1,0.5)));

    sd = add(sd, rect(uv, vec2(1.2,0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(vec2(uv.x,uv.x+uv.y*0.5), vec2(1.4,1.77), vec2(0.1,0.1)));
    sd = add(sd, rect(vec2(uv.x,uv.x-uv.y*0.5), vec2(1.6,1.23), vec2(0.1,0.1)));
    sd = add(sd, rect(uv, vec2(1.8,0.65), vec2(0.1,0.5)));

    sd = add(sd, rect(uv,vec2(-1.5,-0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(uv,vec2(-1.25,-0.65), vec2(0.35,0.1)));
    sd = add(sd, rect(uv,vec2(-1.0,-0.65), vec2(0.1,0.5)));

    sd = add(sd, rect(vec2(uv.x-uv.y*0.35,uv.y), vec2(-.05,-0.65), vec2(0.11,0.5)));
    sd = add(sd, rect(uv,vec2(-.1,-0.8), vec2(0.3,0.08)));
    sd = add(sd, rect(vec2(uv.x+uv.y*0.35,uv.y), vec2(-0.15,-0.65), vec2(0.11,0.5)));

    sd = add(sd, rect(uv,vec2(.8,-0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(uv,vec2(1.2,-0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(uv,vec2(1.6,-0.65), vec2(0.1,0.5)));
    sd = add(sd, rect(uv,vec2(1.2,-1.05), vec2(0.5,0.2)));

    return sd;
}


float map( in vec3 pos )
{
    vec2 p = pos.xy;
    float d = Krym(p);
    float dep = 0.1;
    vec2 e = vec2( d, abs(pos.z) - dep );
    d = min(max(e.x,e.y),0.0) + length(max(e,0.0));
    return d;
}


vec3 calcNormal( in vec3 pos )
{
    vec2 e = vec2(1.0,-1.0)*0.5773;
    const float eps = 0.0005;
    return normalize( e.xyy*map( pos + e.xyy*eps ) +
    e.yyx*map( pos + e.yyx*eps ) +
    e.yxy*map( pos + e.yxy*eps ) +
    e.xxx*map( pos + e.xxx*eps ) );
}

vec3 render( vec2 p )
{
    float an = time/2.;
    vec3 ro = vec3(2.0, 0.4, 2.0);
    ro.xy *= mat2(cos(an),-sin(an),sin(an),cos(an));

    vec3 ta = vec3( 0.0, 0.0, 0.0 );

    vec3 ww = normalize( ta - ro );
    vec3 uu = normalize( cross(ww,vec3(0.0,1.0,0.0) ) );
    vec3 vv = normalize( cross(uu,ww));

    vec3 tot = vec3(0.0);

    vec3 rd = normalize( p.x*uu + p.y*vv + 1.5*ww );

    const float tmax = 5.0;
    float t = 0.0;
    for( int i=0; i<256; i++ )
    {
        vec3 pos = ro + t*rd;
        float h = map(pos);
        if( h<0.0001 || t>tmax ) break;
        t += h;
    }




    if( t<tmax )
    {
        vec3 pos = ro + t*rd;
        vec3 nor = calcNormal(pos);
        float dif = clamp( dot(nor,vec3(0.57703)), 0.0, 1.0 );
        float amb = 0.5 + 0.5*dot(nor,vec3(0.0, 0.0, 1.0));
        col = vec3(0.5,0.5,0.5)*amb + vec3(0.8,0.7,0.5)*dif;
    }

    tot += col;
    return tot;
}

void main(void) {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if(centerCol.a != 0) {
        const float PI = 3.1415926535;
        vec2 p = (gl_FragCoord.xy*2.-resolution.xy)/resolution.y;
        //col = p.y>0.34?vec3(1):p.y>-0.34?vec3(0,0,1):vec3(1,0,0); // old version
        vec2 uv = p;
        vec2 st = gl_FragCoord.xy / resolution.xy;
        float w = sin((uv.x + uv.y - time * .75 + sin(1.5 * uv.x + 4.5 * uv.y) * PI * .3) * PI * .6); // fake waviness factor
        uv *= 1. + (.036 - .036 * w);
        col = vec3(0,0,1);
        col = mix(col, vec3(1), smoothstep(.35, .36, uv.y));
        col = mix(col, vec3(1,0,0), smoothstep(-.35, -.36, uv.y));
        col += w * .225;
        col *= .8;
        gl_FragColor = vec4(render(p), alpha.x);
    } else {
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
                if (currentColor.a != 0) {
                    const float PI = 3.1415926535;
                    vec2 p = (gl_FragCoord.xy*2.-resolution.xy)/resolution.y;
                    //col = p.y>0.34?vec3(1):p.y>-0.34?vec3(0,0,1):vec3(1,0,0); // old version
                    vec2 uv = p;
                    vec2 st = gl_FragCoord.xy / resolution.xy;
                    float w = sin((uv.x + uv.y - time * .75 + sin(1.5 * uv.x + 4.5 * uv.y) * PI * .3) * PI * .6); // fake waviness factor
                    uv *= 1. + (.036 - .036 * w);
                    col = vec3(0,0,1);
                    col = mix(col, vec3(1), smoothstep(.35, .36, uv.y));
                    col = mix(col, vec3(1,0,0), smoothstep(-.35, -.36, uv.y));
                    col += w * .225;
                    col *= .8;
                    gl_FragColor = vec4(render(p), alpha.y);
            }
        }
    }
    }
}