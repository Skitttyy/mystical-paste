package fat.vonware.util.shader.menu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL20;

public class MenuGLSLShader {
   private final int programId;
   private final int timeUniform;
   private final int mouseUniform;
   private final int resolutionUniform;

   public MenuGLSLShader(String fragmentShaderLocation) throws IOException {
      InputStream vshStream = MenuGLSLShader.class.getResourceAsStream("/shaders/menu/passthrough/passthrough.vsh");
      InputStream fshStream = MenuGLSLShader.class.getResourceAsStream(fragmentShaderLocation);
      int program = GL20.glCreateProgram();
      GL20.glAttachShader(program, this.createShader("/shaders/menu/passthrough/passthrough.vsh", vshStream, 35633));
      GL20.glAttachShader(program, this.createShader(fragmentShaderLocation, fshStream, 35632));
      GL20.glLinkProgram(program);
      if (GL20.glGetProgrami(program, 35714) == 0) {
         System.err.println(GL20.glGetProgramInfoLog(program, 512));
         throw new IllegalStateException("Menu shader failed to link: " + fragmentShaderLocation);
      } else {
         GL20.glUseProgram(this.programId = program);
         this.timeUniform = GL20.glGetUniformLocation(program, "time");
         this.mouseUniform = GL20.glGetUniformLocation(program, "mouse");
         this.resolutionUniform = GL20.glGetUniformLocation(program, "resolution");
         GL20.glUseProgram(0);
      }
   }

   public void useShader(int width, int height, float mouseX, float mouseY, float time) {
      GL20.glUseProgram(this.programId);
      GL20.glUniform2f(this.resolutionUniform, (float)width, (float)height);
      GL20.glUniform2f(this.mouseUniform, mouseX / (float)width, 1.0F - mouseY / (float)height);
      GL20.glUniform1f(this.timeUniform, time);
   }

   private int createShader(String path, InputStream in, int type) throws IOException {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, this.readStream(in));
      GL20.glCompileShader(shader);
      if (GL20.glGetShaderi(shader, 35713) == 0) {
         System.err.println(GL20.glGetShaderInfoLog(shader, 512));
         throw new IllegalStateException("Failed to compile shader: " + path);
      } else {
         return shader;
      }
   }

   private String readStream(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[512];

      int read;
      while((read = in.read(buf, 0, buf.length)) != -1) {
         out.write(buf, 0, read);
      }

      return new String(out.toByteArray(), StandardCharsets.UTF_8);
   }
}
