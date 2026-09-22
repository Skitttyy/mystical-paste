#version 330
in vec2 Position;
out vec2 texCoord;

void main() {
    texCoord = Position * 0.5 + 0.5;
    gl_Position = vec4(Position, 0.0, 1.0);
}