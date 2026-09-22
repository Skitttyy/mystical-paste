package fat.vonware.util.shader;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL20;

public final class BloomRenderUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();

   private BloomRenderUtil() {
   }

   public static void render(float partialTicks, float intensity, float bloomRadius, Color inside, Color outside, float outlineRadius, Runnable maskRenderer) {
      render(partialTicks, intensity, bloomRadius, inside, outside, outlineRadius, 1.0F, maskRenderer);
   }

   public static void render(float partialTicks, float intensity, float bloomRadius, Color inside, Color outside, float outlineRadius, float outputAlpha, Runnable maskRenderer) {
      if (maskRenderer != null && inside != null && outside != null) {
         Shader shader = Shader.INSTANCE;
         shader.setRainbowEnabled(false);
         shader.setGradientEnabled(false);
         shader.setBloomEnabled(true);
         shader.setBloomIntensity(intensity);
         shader.setBloomRadius(bloomRadius);
         shader.setBloomQuality(2);
         shader.setOutputAlpha(Math.max(0.0F, Math.min(1.0F, outputAlpha)));
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         boolean started = false;

         try {
            shader.startDraw(partialTicks);
            started = true;
            maskRenderer.run();
            shader.stopDraw(inside, outside, outlineRadius, 1.0F, 0, 0, false);
            started = false;
         } finally {
            if (started) {
               try {
                  shader.stopDraw(inside, outside, outlineRadius, 1.0F, 0, 0, false);
               } catch (Throwable var16) {
               }
            }

            shader.setOutputAlpha(1.0F);
            GL20.glUseProgram(0);
            mc.getFramebuffer().bindFramebuffer(true);
            resetTextureUnit(OpenGlHelper.defaultTexUnit + 2);
            resetTextureUnit(OpenGlHelper.lightmapTexUnit);
            resetTextureUnit(OpenGlHelper.defaultTexUnit);
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(515);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.resetColor();
         }

      }
   }

   private static void resetTextureUnit(int textureUnit) {
      GlStateManager.setActiveTexture(textureUnit);
      GlStateManager.bindTexture(0);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
   }
}
