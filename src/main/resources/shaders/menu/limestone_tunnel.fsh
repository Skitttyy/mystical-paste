#version 120

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

#define T time
#define R resolution
#define rot(a) mat2(cos(a), cos((a)+11.), cos((a)+33.), cos(a))
#define P(x,y) pow(abs(x),y)
#define X(a,b) max(dot(a,b),0.)
#define FK(w)  (1.+.1*(sin(T*25.+(w)*93.1)*cos(T*14.+(w)*17.4)))
#define n(p)   (sin((p).x*3.+sin((p).y*2.7))*cos((p).y*1.1+cos((p).x*2.3)))

float f(vec3 p) {
    float v=0., a=1.;
    for(int i=0; i<7; i++) {
        v += n(p.xy + p.z*.5) * a;
        p *= 2.;
        a *= .5;
    }
    return v;
}

float m(vec3 p) {
    p.xy *= rot(p.z*1.1);
    return .2*(1.-length(p.xy)) - f(p+T*.1)*.06;
}

vec4 gb(float z) {
    float i = floor((z+2.5)*.2);
    return vec4(cos(i*2.4)*.6, sin(i*2.4)*.6, i*5., i);
}

vec3 bc(float i) {
    float h = fract(sin(i*13.54)*453.21);
    return h<.33 ? vec3(1,8,9)*.1 : (h<.66 ? vec3(9,2,6)*.1 : vec3(10,6,1)*.1);
}

void main() {
    vec2 U = gl_FragCoord.xy;
    vec3 d = normalize(vec3((U - .5*R) / R.y, 1.));
    vec3 o = vec3(sin(T*.3)*.2, cos(T*.2)*.2, T*1.2);
    vec3 p = vec3(0.);
    vec3 c = vec3(0.);
    vec3 g2 = vec3(0.);
    vec3 nn = vec3(0.);
    vec3 l  = vec3(0.);
    vec3 b  = vec3(0.);

    d.xy *= rot(T*.15);

    bool ht = false;
    float t=0., w=0., hi=0., g1=0., dc=0., db=0.;
    vec4 bi = vec4(0.);

    for(int i=0; i<250; i++) {
        p  = o + d*t;
        dc = m(p);
        bi = gb(p.z);
        db = length(p - bi.xyz) - .03;
        w  = min(dc, db);

        g1 += .002 / (.01 + abs(dc));
        g2 += (vec3(.0003/(.001+db*db)) + bc(bi.w)*.005/(.02+abs(db))) * FK(bi.w);

        if(abs(w) < .001 + t/1000. || t > 25.) {
            if(db < dc) { ht = true; hi = bi.w; }
            break;
        }
        t += w*.8;
    }

    if(t <= 25.) {
        if(ht) {
            c = vec3(12.) + bc(hi)*5.;
        } else {
            vec2 e = vec2(.001+t/1000., 0.);
            nn = normalize(vec3(
                m(p+e.xyy)-m(p-e.xyy),
                m(p+e.yxy)-m(p-e.yxy),
                m(p+e.yyx)-m(p-e.yyx)
            ));

            vec3 q = p;
            q.xy *= rot(q.z*1.1);
            c = mix(vec3(1,3,8)*.05, vec3(9,4,1)*.1,
                    P(max(min(f(q+T*.1)+.5, 1.), 0.), 2.));

            l = o + vec3(0,0,5) - p;
            float d1 = length(l); l /= d1;

            c = c*.03 + (c*X(nn,l)*1.5
                + vec3(1,.8,.6)*P(X(nn,normalize(l-d)), 24.)
                * smoothstep(20.,5.,t)*1.5) / (1.+d1*d1*.08);

            bi = gb(p.z);
            l  = bi.xyz - p;
            float d2 = length(l); l /= d2;
            b  = bc(bi.w);

            c += ((c*X(nn,l)*2.5) + (b*P(X(nn,normalize(l-d)),16.)*4.))
                * b * FK(bi.w)
                * (.5+.5*fract(sin(bi.w*88.1)*12.3))
                / (1.+d2*d2*1.5);

            float oa=0., s=1.;
            for(int j=0; j<4; j++) {
                float h = .01 + .03*float(j+2);
                oa += (h - m(p + h*nn)) * s;
                s  *= .9;
                if(oa > .33) break;
            }

            c = (c + vec3(5,3,8)*.1
                * P(1.-X(nn,-d), 4.) * .6
                / (1.+d1*d1*.08)) * max(1.-3.*oa, 0.);
        }
    }

    c = mix(vec3(2,0,5)*.01, c, 1./exp(.12*t))
      + vec3(9,3,1)*.1 * g1 * .02 / exp(.05*t)
      + g2 / exp(.03*t);

    // Filmic tonemapping
    c = c*(2.51*c+.03) / (c*(2.43*c+.59)+.14);

    // Vignette
    U /= R; U *= 1.-U;
    gl_FragColor = vec4(P(c * P(16.*U.x*U.y, .25), vec3(2.5)), 1.);
}