package fat.vonware.util.Animations.game;

import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.effect.EntityLightningBolt;
import org.lwjgl.opengl.GL20;

public class TintedLightningRenderer extends RenderLightningBolt {
   private static int program = -1;
   private static int tintLoc = -1;
   private final Supplier<float[]> colorSupplier;

   public TintedLightningRenderer(RenderManager renderManager, Supplier<float[]> colorSupplier) {
      super(renderManager);
      this.colorSupplier = colorSupplier;
   }

   private static void initShader() {
      if (program == -1) {
         int vert = GL20.glCreateShader(35633);
         GL20.glShaderSource(vert, "varying vec4 vColor;void main() {    gl_Position = ftransform();    vColor = gl_Color;}");
         GL20.glCompileShader(vert);
         int frag = GL20.glCreateShader(35632);
         GL20.glShaderSource(frag, "uniform vec3 tint;varying vec4 vColor;void main() {    gl_FragColor = vec4(vColor.rgb * tint, vColor.a);}");
         GL20.glCompileShader(frag);
         program = GL20.glCreateProgram();
         GL20.glAttachShader(program, vert);
         GL20.glAttachShader(program, frag);
         GL20.glLinkProgram(program);
         tintLoc = GL20.glGetUniformLocation(program, "tint");
      }
   }

   public void doRender(EntityLightningBolt entity, double x, double y, double z, float entityYaw, float partialTicks) {
      initShader();
      float[] color = (float[])this.colorSupplier.get();
      GL20.glUseProgram(program);
      GL20.glUniform3f(tintLoc, color[0], color[1], color[2]);
      super.doRender(entity, x, y, z, entityYaw, partialTicks);
      GL20.glUseProgram(0);
   }
}
