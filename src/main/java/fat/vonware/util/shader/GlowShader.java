package fat.vonware.util.shader;

import org.lwjgl.opengl.GL20;

public class GlowShader extends FrameBufferShader {
   public static final GlowShader GLOW_SHADER = new GlowShader();

   public GlowShader() {
      super("glow");
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("texelSize");
      this.setupUniform("color");
      this.setupUniform("divider");
      this.setupUniform("radius");
      this.setupUniform("maxSample");
   }

   public void updateUniforms() {
      GL20.glUniform1i(this.getUniform("texture"), 0);
      GL20.glUniform2f(this.getUniform("texelSize"), 1.0F / (float)this.mc.displayWidth * this.radius * this.quality, 1.0F / (float)this.mc.displayHeight * this.radius * this.quality);
      GL20.glUniform3f(this.getUniform("color"), 1.0F, 1.0F, 1.0F);
      GL20.glUniform1f(this.getUniform("divider"), 140.0F);
      GL20.glUniform1f(this.getUniform("radius"), this.radius);
      GL20.glUniform1f(this.getUniform("maxSample"), 10.0F);
   }
}
