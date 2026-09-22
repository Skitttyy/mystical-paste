#version 120

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

void main()
{
    float i = .2, a;
    vec2 r = resolution,
         p = ( gl_FragCoord.xy + gl_FragCoord.xy - r ) / r.y / .7,
         d = vec2(-1,1),
         b = p - i*d,
         c = p * mat2(1, 1, d/(.1 + i/dot(b,b))),
         v = c * mat2(cos(.5*log(a=dot(c,c)) + time*i + vec4(0,33,11,0))/i),
         w;

    for(; i++<9.; w += 1.+sin(v) )
        v += .7*sin(v.yx*i+time) / i + .5;

    i = length( sin(v/.3)*.4 + c*(3.+d) );

    gl_FragColor = 1. - exp( -exp( c.x * vec4(.6,-.4,-1,0) )
                   / w.xyyx
                   / ( 2. + i*i/4. - i )
                   / ( .5 + 1. / a )
                   / ( .03 + abs( length(p)-.7 ) )
             );
}