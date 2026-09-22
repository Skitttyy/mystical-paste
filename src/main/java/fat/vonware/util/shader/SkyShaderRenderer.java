package fat.vonware.util.shader;

import fat.vonware.features.modules.render.Environment;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class SkyShaderRenderer {
   public static final SkyShaderRenderer INSTANCE = new SkyShaderRenderer();
   private int programId = -1;
   private int uboId = -1;
   private int quadVao = -1;
   private int quadVbo = -1;
   private boolean broken = false;
   private static final int UBO_SIZE = 64;
   private final ByteBuffer uboBuffer = BufferUtils.createByteBuffer(64);
   private static final float[] QUAD = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F};
   private static final String VERT_SRC = "#version 330\nin vec2 Position;\nout vec2 uv;\nvoid main() {\n    uv = Position * 0.5 + 0.5;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n";
   private static final String FRAG_SRC = "#version 330\nlayout(std140) uniform NoiseConfig {\n    vec4  Color1;\n    vec4  Color2;\n    float Time;\n    float Speed;\n    float Scale;\n    float Octaves;\n    float Yaw;\n    float Pitch;\n    int   CameraSync;\n    float Pad;\n};\nin vec2 uv;\nout vec4 fragColor;\nfloat hash(vec2 p) {\n    p = fract(p * vec2(127.1, 311.7));\n    p += dot(p, p + 19.19);\n    return fract(p.x * p.y);\n}\nfloat vnoise(vec2 p) {\n    vec2 i = floor(p), f = fract(p);\n    vec2 u = f * f * (3.0 - 2.0 * f);\n    return mix(\n        mix(hash(i), hash(i + vec2(1,0)), u.x),\n        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), u.x),\n        u.y);\n}\nfloat fbm(vec2 p) {\n    float v = 0.0, amp = 0.5, freq = 1.0;\n    int oct = clamp(int(Octaves), 1, 8);\n    for (int i = 0; i < oct; i++) {\n        v   += amp * vnoise(p * freq);\n        freq *= 2.0;\n        amp  *= 0.5;\n    }\n    return v;\n}\nvoid main() {\n    vec2 p = uv * Scale;\n    if (CameraSync == 0) {\n        p.x += Yaw   / 360.0;\n        p.y += Pitch / 180.0;\n    }\n    float t  = Time * Speed;\n    float n1 = fbm(p + vec2( t * 0.3,  t * 0.17));\n    float n2 = fbm(p + vec2(-t * 0.21, t * 0.27) + vec2(n1 * 1.5));\n    float blend = fbm(p + vec2(n1, n2) * 2.0);\n    blend = smoothstep(0.2, 0.8, blend);\n    vec3 col = mix(Color1.rgb, Color2.rgb, blend);\n    fragColor = vec4(col, 1.0);\n}\n";

   public void init() {
      if (!this.broken) {
         try {
            this.programId = this.buildProgram();
            this.uboId = GL15.glGenBuffers();
            GL15.glBindBuffer(35345, this.uboId);
            GL15.glBufferData(35345, BufferUtils.createByteBuffer(64), 35048);
            GL15.glBindBuffer(35345, 0);
            int blockIdx = GL31.glGetUniformBlockIndex(this.programId, "NoiseConfig");
            if (blockIdx != -1) {
               GL31.glUniformBlockBinding(this.programId, blockIdx, 0);
            }

            this.quadVao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(this.quadVao);
            this.quadVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(34962, this.quadVbo);
            FloatBuffer fb = BufferUtils.createFloatBuffer(QUAD.length);
            fb.put(QUAD);
            fb.flip();
            GL15.glBufferData(34962, fb, 35044);
            int posLoc = GL20.glGetAttribLocation(this.programId, "Position");
            GL20.glEnableVertexAttribArray(posLoc);
            GL20.glVertexAttribPointer(posLoc, 2, 5126, false, 0, 0L);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(34962, 0);
         } catch (Exception e) {
            this.broken = true;
            e.printStackTrace();
         }

      }
   }

   public void render(float partialTicks) {
      if (!this.broken) {
         if (this.programId == -1) {
            this.init();
         }

         if (!this.broken) {
            Environment env = Environment.INSTANCE;
            if (env != null) {
               GL11.glPushAttrib(1048575);
               GL11.glMatrixMode(5889);
               GL11.glPushMatrix();
               GL11.glLoadIdentity();
               GL11.glOrtho((double)-1.0F, (double)1.0F, (double)-1.0F, (double)1.0F, (double)-1.0F, (double)1.0F);
               GL11.glMatrixMode(5888);
               GL11.glPushMatrix();
               GL11.glLoadIdentity();
               GL11.glDepthMask(false);
               GL11.glDisable(2929);
               GL11.glDisable(2884);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               GL20.glUseProgram(this.programId);
               this.uploadUBO(env, partialTicks);
               GL30.glBindBufferBase(35345, 0, this.uboId);
               GL30.glBindVertexArray(this.quadVao);
               GL11.glDrawArrays(4, 0, 6);
               GL30.glBindVertexArray(0);
               GL30.glBindBufferBase(35345, 0, 0);
               GL20.glUseProgram(0);
               GL11.glMatrixMode(5889);
               GL11.glPopMatrix();
               GL11.glMatrixMode(5888);
               GL11.glPopMatrix();
               GL11.glPopAttrib();
            }
         }
      }
   }

   private void uploadUBO(Environment env, float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      float time = mc.world != null ? ((float)mc.world.getTotalWorldTime() + partialTicks) * 0.05F : 0.0F;
      float yaw = mc.player != null ? mc.player.rotationYaw : 0.0F;
      float pitch = mc.player != null ? mc.player.rotationPitch : 0.0F;
      float[] c1 = unpackRGB((Integer)env.skyShaderColor1.getValue());
      float[] c2 = unpackRGB((Integer)env.skyShaderColor2.getValue());
      this.uboBuffer.clear();
      this.uboBuffer.putFloat(c1[0]);
      this.uboBuffer.putFloat(c1[1]);
      this.uboBuffer.putFloat(c1[2]);
      this.uboBuffer.putFloat(1.0F);
      this.uboBuffer.putFloat(c2[0]);
      this.uboBuffer.putFloat(c2[1]);
      this.uboBuffer.putFloat(c2[2]);
      this.uboBuffer.putFloat(1.0F);
      this.uboBuffer.putFloat(time);
      this.uboBuffer.putFloat((Float)env.skyShaderSpeed.getValue());
      this.uboBuffer.putFloat((Float)env.skyShaderScale.getValue());
      this.uboBuffer.putFloat((float)(Integer)env.skyShaderOctaves.getValue());
      this.uboBuffer.putFloat(yaw);
      this.uboBuffer.putFloat(pitch);
      this.uboBuffer.putInt((Boolean)env.skyShaderCameraSync.getValue() ? 1 : 0);
      this.uboBuffer.putFloat(0.0F);
      this.uboBuffer.flip();
      GL15.glBindBuffer(35345, this.uboId);
      GL15.glBufferSubData(35345, 0L, this.uboBuffer);
      GL15.glBindBuffer(35345, 0);
   }

   private int buildProgram() throws Exception {
      int vert = this.compileShader(35633, "#version 330\nin vec2 Position;\nout vec2 uv;\nvoid main() {\n    uv = Position * 0.5 + 0.5;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n", "vert");
      int frag = this.compileShader(35632, "#version 330\nlayout(std140) uniform NoiseConfig {\n    vec4  Color1;\n    vec4  Color2;\n    float Time;\n    float Speed;\n    float Scale;\n    float Octaves;\n    float Yaw;\n    float Pitch;\n    int   CameraSync;\n    float Pad;\n};\nin vec2 uv;\nout vec4 fragColor;\nfloat hash(vec2 p) {\n    p = fract(p * vec2(127.1, 311.7));\n    p += dot(p, p + 19.19);\n    return fract(p.x * p.y);\n}\nfloat vnoise(vec2 p) {\n    vec2 i = floor(p), f = fract(p);\n    vec2 u = f * f * (3.0 - 2.0 * f);\n    return mix(\n        mix(hash(i), hash(i + vec2(1,0)), u.x),\n        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), u.x),\n        u.y);\n}\nfloat fbm(vec2 p) {\n    float v = 0.0, amp = 0.5, freq = 1.0;\n    int oct = clamp(int(Octaves), 1, 8);\n    for (int i = 0; i < oct; i++) {\n        v   += amp * vnoise(p * freq);\n        freq *= 2.0;\n        amp  *= 0.5;\n    }\n    return v;\n}\nvoid main() {\n    vec2 p = uv * Scale;\n    if (CameraSync == 0) {\n        p.x += Yaw   / 360.0;\n        p.y += Pitch / 180.0;\n    }\n    float t  = Time * Speed;\n    float n1 = fbm(p + vec2( t * 0.3,  t * 0.17));\n    float n2 = fbm(p + vec2(-t * 0.21, t * 0.27) + vec2(n1 * 1.5));\n    float blend = fbm(p + vec2(n1, n2) * 2.0);\n    blend = smoothstep(0.2, 0.8, blend);\n    vec3 col = mix(Color1.rgb, Color2.rgb, blend);\n    fragColor = vec4(col, 1.0);\n}\n", "frag");
      int prog = GL20.glCreateProgram();
      GL20.glAttachShader(prog, vert);
      GL20.glAttachShader(prog, frag);
      GL20.glLinkProgram(prog);
      if (GL20.glGetProgrami(prog, 35714) == 0) {
         String log = GL20.glGetProgramInfoLog(prog, 4096);
         GL20.glDeleteProgram(prog);
         throw new RuntimeException("Noise shader link failed:\n" + log);
      } else {
         GL20.glDeleteShader(vert);
         GL20.glDeleteShader(frag);
         return prog;
      }
   }

   private int compileShader(int type, String source, String name) throws Exception {
      int id = GL20.glCreateShader(type);
      GL20.glShaderSource(id, source);
      GL20.glCompileShader(id);
      if (GL20.glGetShaderi(id, 35713) == 0) {
         String log = GL20.glGetShaderInfoLog(id, 4096);
         GL20.glDeleteShader(id);
         throw new RuntimeException("Noise shader compile failed (" + name + "):\n" + log);
      } else {
         return id;
      }
   }

   private static float[] unpackRGB(int color) {
      return new float[]{(float)(color >> 16 & 255) / 255.0F, (float)(color >> 8 & 255) / 255.0F, (float)(color & 255) / 255.0F};
   }

   public void destroy() {
      if (this.programId != -1) {
         GL20.glDeleteProgram(this.programId);
         this.programId = -1;
      }

      if (this.uboId != -1) {
         GL15.glDeleteBuffers(this.uboId);
         this.uboId = -1;
      }

      if (this.quadVbo != -1) {
         GL15.glDeleteBuffers(this.quadVbo);
         this.quadVbo = -1;
      }

      if (this.quadVao != -1) {
         GL30.glDeleteVertexArrays(this.quadVao);
         this.quadVao = -1;
      }

      this.broken = false;
   }
}
