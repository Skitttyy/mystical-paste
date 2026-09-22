package fat.vonware.util.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class CustomShader extends FrameBufferShader {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static final CustomShader SMOOTH1 = tryLoad("smooth1");
   public static final CustomShader SMOOTH2 = tryLoad("smooth2");
   public static final CustomShader SMOOTH3 = tryLoad("smooth3");
   public static final CustomShader GALAXY = tryLoad("galaxy");
   public static final CustomShader WATER = tryLoad("water");
   public static final CustomShader LAVA = tryLoad("lava");
   public static final CustomShader OUTLINE = tryLoad("outline");
   public static final CustomShader PINK_GALACTIC = tryLoad("pink_galaxy");
   public static final CustomShader MATTER = tryLoad("matter");
   public static final CustomShader CATTYN = tryLoad("cattyn");
   public static final CustomShader HACKER = tryLoad("hacker");
   public static final CustomShader BARS = tryLoad("bars");
   public static final CustomShader STARS = tryLoad("cool_stars");

   public CustomShader(String name) {
      super(name);
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("texelSize");
      this.setupUniform("radius");
      this.setupUniform("resolution");
      this.setupUniform("time");
      this.setupUniform("color");
      this.setupUniform("alpha");
   }

   public void updateUniforms() {
      GL20.glUniform1i(this.getUniform("texture"), 0);
      GL20.glUniform2f(this.getUniform("texelSize"), 1.0F / (float)mc.displayWidth * this.radius * this.quality, 1.0F / (float)mc.displayHeight * this.radius * this.quality);
      GL20.glUniform1f(this.getUniform("radius"), this.radius);
      GL20.glUniform2f(this.getUniform("resolution"), (float)mc.displayWidth, (float)mc.displayHeight);
      GL20.glUniform1f(this.getUniform("time"), (float)(System.currentTimeMillis() - this.startTime) / 1000.0F);
      GL20.glUniform3f(this.getUniform("color"), (float)this.inside.getRed() / 255.0F, (float)this.inside.getGreen() / 255.0F, (float)this.inside.getBlue() / 255.0F);
      GL20.glUniform2f(this.getUniform("alpha"), (float)this.inside.getAlpha() / 255.0F, (float)this.outside.getAlpha() / 255.0F);
   }

   private static CustomShader tryLoad(String name) {
      try {
         return new CustomShader(name);
      } catch (Exception e) {
         System.err.println("[Vonware] Failed to load shader: " + name + " - " + e.getMessage());
         return null;
      }
   }
}
