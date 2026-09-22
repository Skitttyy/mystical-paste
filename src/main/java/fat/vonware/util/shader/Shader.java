package fat.vonware.util.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class Shader extends FrameBufferShader {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static final Shader INSTANCE = new Shader("shader");
   protected int rainbowEnabled;
   protected int rainbowType;
   protected float rainbowSpeed;
   protected float rainbowSpread;
   protected float rainbowSaturation;
   protected float rainbowLightness;
   protected float rainbowHueOffset;
   protected int bloomEnabled;
   protected float bloomIntensity;
   protected float bloomRadius;
   protected int bloomQuality;
   protected float outputAlpha = 1.0F;
   protected int gradientEnabled;
   protected float gradientColor1R;
   protected float gradientColor1G;
   protected float gradientColor1B;
   protected float gradientColor1A;
   protected float gradientColor2R;
   protected float gradientColor2G;
   protected float gradientColor2B;
   protected float gradientColor2A;
   protected float gradientSpread;
   protected float gradientSpeed;

   public Shader(String name) {
      super(name);
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("texelSize");
      this.setupUniform("divider");
      this.setupUniform("radius");
      this.setupUniform("maxSample");
      this.setupUniform("resolution");
      this.setupUniform("time");
      this.setupUniform("insideColor");
      this.setupUniform("insideMode");
      this.setupUniform("outsideColor");
      this.setupUniform("outsideMode");
      this.setupUniform("glow");
      this.setupUniform("rainbowEnabled");
      this.setupUniform("rainbowType");
      this.setupUniform("rainbowSpeed");
      this.setupUniform("rainbowSpread");
      this.setupUniform("rainbowSaturation");
      this.setupUniform("rainbowLightness");
      this.setupUniform("rainbowHueOffset");
      this.setupUniform("bloomEnabled");
      this.setupUniform("bloomIntensity");
      this.setupUniform("bloomRadius");
      this.setupUniform("bloomQuality");
      this.setupUniform("outputAlpha");
      this.setupUniform("gradientEnabled");
      this.setupUniform("gradientColor1");
      this.setupUniform("gradientColor2");
      this.setupUniform("gradientSpread");
      this.setupUniform("gradientSpeed");
   }

   public void updateUniforms() {
      GL20.glUniform1i(this.getUniform("texture"), 0);
      GL20.glUniform2f(this.getUniform("texelSize"), 1.0F / (float)mc.displayWidth * this.radius * this.quality, 1.0F / (float)mc.displayHeight * this.radius * this.quality);
      GL20.glUniform1f(this.getUniform("divider"), 140.0F);
      GL20.glUniform1f(this.getUniform("radius"), this.radius);
      GL20.glUniform1f(this.getUniform("maxSample"), 10.0F);
      GL20.glUniform2f(this.getUniform("resolution"), (float)mc.displayWidth, (float)mc.displayHeight);
      GL20.glUniform1f(this.getUniform("time"), (float)(System.currentTimeMillis() - this.startTime) / 1000.0F);
      GL20.glUniform4f(this.getUniform("insideColor"), (float)this.inside.getRed() / 255.0F, (float)this.inside.getGreen() / 255.0F, (float)this.inside.getBlue() / 255.0F, (float)this.inside.getAlpha() / 255.0F);
      GL20.glUniform1i(this.getUniform("insideMode"), this.insideMode);
      GL20.glUniform4f(this.getUniform("outsideColor"), (float)this.outside.getRed() / 255.0F, (float)this.outside.getGreen() / 255.0F, (float)this.outside.getBlue() / 255.0F, (float)this.outside.getAlpha() / 255.0F);
      GL20.glUniform1i(this.getUniform("outsideMode"), this.outsideMode);
      GL20.glUniform1i(this.getUniform("glow"), this.glow);
      GL20.glUniform1i(this.getUniform("rainbowEnabled"), this.rainbowEnabled);
      GL20.glUniform1i(this.getUniform("rainbowType"), this.rainbowType);
      GL20.glUniform1f(this.getUniform("rainbowSpeed"), this.rainbowSpeed);
      GL20.glUniform1f(this.getUniform("rainbowSpread"), this.rainbowSpread);
      GL20.glUniform1f(this.getUniform("rainbowSaturation"), this.rainbowSaturation);
      GL20.glUniform1f(this.getUniform("rainbowLightness"), this.rainbowLightness);
      GL20.glUniform1f(this.getUniform("rainbowHueOffset"), this.rainbowHueOffset);
      GL20.glUniform1i(this.getUniform("bloomEnabled"), this.bloomEnabled);
      GL20.glUniform1f(this.getUniform("bloomIntensity"), this.bloomIntensity);
      GL20.glUniform1f(this.getUniform("bloomRadius"), this.bloomRadius);
      GL20.glUniform1i(this.getUniform("bloomQuality"), this.bloomQuality);
      GL20.glUniform1f(this.getUniform("outputAlpha"), Math.max(0.0F, Math.min(1.0F, this.outputAlpha)));
      GL20.glUniform1i(this.getUniform("gradientEnabled"), this.gradientEnabled);
      GL20.glUniform4f(this.getUniform("gradientColor1"), this.gradientColor1R, this.gradientColor1G, this.gradientColor1B, this.gradientColor1A);
      GL20.glUniform4f(this.getUniform("gradientColor2"), this.gradientColor2R, this.gradientColor2G, this.gradientColor2B, this.gradientColor2A);
      GL20.glUniform1f(this.getUniform("gradientSpread"), this.gradientSpread);
      GL20.glUniform1f(this.getUniform("gradientSpeed"), this.gradientSpeed);
   }

   public void setRainbowEnabled(boolean enabled) {
      this.rainbowEnabled = enabled ? 1 : 0;
   }

   public void setRainbowType(int type) {
      this.rainbowType = type;
   }

   public void setRainbowSpeed(float speed) {
      this.rainbowSpeed = speed;
   }

   public void setRainbowSpread(float spread) {
      this.rainbowSpread = spread;
   }

   public void setRainbowSaturation(float sat) {
      this.rainbowSaturation = sat;
   }

   public void setRainbowLightness(float light) {
      this.rainbowLightness = light;
   }

   public void setRainbowHueOffset(float offset) {
      this.rainbowHueOffset = offset;
   }

   public void setGradientEnabled(boolean enabled) {
      this.gradientEnabled = enabled ? 1 : 0;
   }

   public void setGradientColor1(float r, float g, float b, float a) {
      this.gradientColor1R = r;
      this.gradientColor1G = g;
      this.gradientColor1B = b;
      this.gradientColor1A = a;
   }

   public void setGradientColor2(float r, float g, float b, float a) {
      this.gradientColor2R = r;
      this.gradientColor2G = g;
      this.gradientColor2B = b;
      this.gradientColor2A = a;
   }

   public void setGradientSpread(float spread) {
      this.gradientSpread = spread;
   }

   public void setGradientSpeed(float speed) {
      this.gradientSpeed = speed;
   }

   public void setBloomEnabled(boolean enabled) {
      this.bloomEnabled = enabled ? 1 : 0;
   }

   public void setBloomIntensity(float intensity) {
      this.bloomIntensity = intensity;
   }

   public void setBloomRadius(float radius) {
      this.bloomRadius = radius;
   }

   public void setBloomQuality(int quality) {
      this.bloomQuality = quality;
   }

   public void setOutputAlpha(float alpha) {
      this.outputAlpha = alpha;
   }
}
