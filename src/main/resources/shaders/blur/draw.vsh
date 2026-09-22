#version 120

varying vec4 clipPos;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    clipPos = gl_Position;
}
