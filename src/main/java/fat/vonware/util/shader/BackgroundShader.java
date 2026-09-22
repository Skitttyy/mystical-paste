package fat.vonware.util.shader;

import net.minecraft.client.Minecraft;

public class BackgroundShader extends FrameBufferShader {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static final BackgroundShader FRUITY = new BackgroundShader("fruity");
   public static final BackgroundShader GALAXY = new BackgroundShader("galaxy");
   public static final BackgroundShader WATER = new BackgroundShader("water");
   public static final BackgroundShader LAVA = new BackgroundShader("lava");
   public static final BackgroundShader OUTLINE = new BackgroundShader("outline");

   public BackgroundShader(String name) {
      super(name);
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("texelSize");
      this.setupUniform("color");
      this.setupUniform("radius");
      this.setupUniform("resolution");
      this.setupUniform("time");
      this.setupUniform("alpha");
   }

   public void updateUniforms() {
   }
}
