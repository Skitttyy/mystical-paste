package fat.vonware.manager;

import fat.vonware.features.Feature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class BlurManager extends Feature {
   public static BlurManager INSTANCE;
   private Framebuffer blurFramebuffer;
   private Framebuffer swapFramebuffer;
   private int blurProgram = -1;
   private int blurDirectionUniform;
   private int blurRadiusUniform;
   private int blurTexelSizeUniform;
   private int blurTextureUniform;
   private int drawProgram = -1;
   private int drawTextureUniform;
   private int drawTintColorUniform;
   private int drawOpacityUniform;
   private boolean initialized = false;
   private boolean sceneCaptured = false;
   private int lastWidth;
   private int lastHeight;
   private static final String BLUR_VERTEX_PATH = "/shaders/blur/blur.vsh";
   private static final String BLUR_FRAGMENT_PATH = "/shaders/blur/gaussian.fsh";
   private static final String DRAW_VERTEX_PATH = "/shaders/blur/draw.vsh";
   private static final String DRAW_FRAGMENT_PATH = "/shaders/blur/draw.fsh";

   public BlurManager() {
      INSTANCE = this;
   }

   public void init() {
   }

   public void onUnload() {
      this.deleteResources();
   }

   private void initShaders() {
      if (!this.initialized) {
         try {
            this.blurProgram = this.createProgram("/shaders/blur/blur.vsh", "/shaders/blur/gaussian.fsh");
            if (this.blurProgram != -1) {
               this.blurDirectionUniform = GL20.glGetUniformLocation(this.blurProgram, "direction");
               this.blurRadiusUniform = GL20.glGetUniformLocation(this.blurProgram, "radius");
               this.blurTexelSizeUniform = GL20.glGetUniformLocation(this.blurProgram, "texelSize");
               this.blurTextureUniform = GL20.glGetUniformLocation(this.blurProgram, "texture");
            }

            this.drawProgram = this.createProgram("/shaders/blur/draw.vsh", "/shaders/blur/draw.fsh");
            if (this.drawProgram != -1) {
               this.drawTextureUniform = GL20.glGetUniformLocation(this.drawProgram, "blurTexture");
               this.drawTintColorUniform = GL20.glGetUniformLocation(this.drawProgram, "tintColor");
               this.drawOpacityUniform = GL20.glGetUniformLocation(this.drawProgram, "opacity");
            }

            this.initialized = this.blurProgram != -1 && this.drawProgram != -1;
         } catch (Exception var2) {
            this.initialized = false;
         }

      }
   }

   private void checkFramebuffers() {
      int fullW = mc.displayWidth;
      int fullH = mc.displayHeight;
      if (this.blurFramebuffer == null || this.swapFramebuffer == null || fullW != this.lastWidth || fullH != this.lastHeight) {
         if (this.blurFramebuffer != null) {
            this.blurFramebuffer.deleteFramebuffer();
         }

         if (this.swapFramebuffer != null) {
            this.swapFramebuffer.deleteFramebuffer();
         }

         int blurW = Math.max(1, fullW * 3 / 4);
         int blurH = Math.max(1, fullH * 3 / 4);
         this.blurFramebuffer = new Framebuffer(blurW, blurH, false);
         this.swapFramebuffer = new Framebuffer(blurW, blurH, false);
         int savedActiveUnit = GL11.glGetInteger(34016);
         GlStateManager.setActiveTexture(33984);
         this.blurFramebuffer.setFramebufferFilter(9729);
         GlStateManager.bindTexture(0);
         GlStateManager.setActiveTexture(33984);
         this.swapFramebuffer.setFramebufferFilter(9729);
         GlStateManager.bindTexture(0);
         GlStateManager.setActiveTexture(33985);
         GlStateManager.bindTexture(0);
         GL13.glActiveTexture(savedActiveUnit);
         this.lastWidth = fullW;
         this.lastHeight = fullH;
      }

   }

   public void captureAndBlur(float blurRadius) {
      if (OpenGlHelper.isFramebufferEnabled()) {
         if (!this.sceneCaptured) {
            this.initShaders();
            if (this.initialized) {
               this.checkFramebuffers();
               int fullW = mc.displayWidth;
               int fullH = mc.displayHeight;
               int blurW = Math.max(1, fullW * 3 / 4);
               int blurH = Math.max(1, fullH * 3 / 4);
               int savedActiveUnit = GL11.glGetInteger(34016);
               boolean scissorWasEnabled = GL11.glIsEnabled(3089);
               int[] scissorBox = null;
               if (scissorWasEnabled) {
                  IntBuffer scissorBuf = BufferUtils.createIntBuffer(4);
                  GL11.glGetInteger(3088, scissorBuf);
                  scissorBox = new int[]{scissorBuf.get(0), scissorBuf.get(1), scissorBuf.get(2), scissorBuf.get(3)};
                  GL11.glDisable(3089);
               }

               boolean stencilWasEnabled = GL11.glIsEnabled(2960);
               if (stencilWasEnabled) {
                  GL11.glDisable(2960);
               }

               boolean alphaWasEnabled = GL11.glIsEnabled(3008);
               int savedAlphaFunc = GL11.glGetInteger(3009);
               float savedAlphaRef = GL11.glGetFloat(3010);
               if (alphaWasEnabled) {
                  GlStateManager.disableAlpha();
               }

               GlStateManager.depthMask(false);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               this.blurFramebuffer.bindFramebuffer(true);
               GlStateManager.setActiveTexture(33984);
               mc.getFramebuffer().bindFramebufferTexture();
               GL20.glUseProgram(0);
               this.drawFullscreenQuad(blurW, blurH);
               this.swapFramebuffer.bindFramebuffer(true);
               GL20.glUseProgram(this.blurProgram);
               GL20.glUniform1i(this.blurTextureUniform, 0);
               GL20.glUniform2f(this.blurDirectionUniform, 1.0F, 0.0F);
               GL20.glUniform1f(this.blurRadiusUniform, blurRadius);
               GL20.glUniform2f(this.blurTexelSizeUniform, 1.0F / (float)blurW, 1.0F / (float)blurH);
               GlStateManager.setActiveTexture(33984);
               this.blurFramebuffer.bindFramebufferTexture();
               this.drawFullscreenQuad(blurW, blurH);
               this.blurFramebuffer.bindFramebuffer(true);
               GL20.glUniform2f(this.blurDirectionUniform, 0.0F, 1.0F);
               GlStateManager.setActiveTexture(33984);
               this.swapFramebuffer.bindFramebufferTexture();
               this.drawFullscreenQuad(blurW, blurH);
               GL20.glUseProgram(0);
               GlStateManager.setActiveTexture(33985);
               GlStateManager.bindTexture(0);
               GlStateManager.setActiveTexture(33984);
               GlStateManager.bindTexture(0);
               GL13.glActiveTexture(savedActiveUnit);
               GlStateManager.depthMask(true);
               if (alphaWasEnabled) {
                  GlStateManager.enableAlpha();
                  GlStateManager.alphaFunc(savedAlphaFunc, savedAlphaRef);
               }

               if (stencilWasEnabled) {
                  GL11.glEnable(2960);
               }

               if (scissorWasEnabled) {
                  GL11.glEnable(3089);
                  if (scissorBox != null) {
                     GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
                  }
               }

               mc.getFramebuffer().bindFramebuffer(true);
               this.sceneCaptured = true;
            }
         }
      }
   }

   public void drawBlurRect(float x1, float y1, float x2, float y2, int tintColor) {
      this.drawBlurRect(x1, y1, x2, y2, tintColor, 1.0F);
   }

   public void drawBlurRect(float x1, float y1, float x2, float y2, int tintColor, float opacity) {
      if (this.initialized && this.sceneCaptured) {
         float alpha = (float)(tintColor >> 24 & 255) / 255.0F;
         float red = (float)(tintColor >> 16 & 255) / 255.0F;
         float green = (float)(tintColor >> 8 & 255) / 255.0F;
         float blue = (float)(tintColor & 255) / 255.0F;
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.setActiveTexture(33984);
         this.blurFramebuffer.bindFramebufferTexture();
         GL20.glUseProgram(this.drawProgram);
         GL20.glUniform1i(this.drawTextureUniform, 0);
         GL20.glUniform4f(this.drawTintColorUniform, red, green, blue, alpha);
         GL20.glUniform1f(this.drawOpacityUniform, Math.max(0.0F, Math.min(1.0F, opacity)));
         GL11.glBegin(7);
         GL11.glVertex3f(x1, y2, 0.0F);
         GL11.glVertex3f(x2, y2, 0.0F);
         GL11.glVertex3f(x2, y1, 0.0F);
         GL11.glVertex3f(x1, y1, 0.0F);
         GL11.glEnd();
         GL20.glUseProgram(0);
         GlStateManager.setActiveTexture(33984);
         GlStateManager.bindTexture(0);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(true);
      }
   }

   public void drawBlurRoundedRect(float x, float y, float width, float height, float radius, int tintColor) {
      this.drawBlurRoundedRect(x, y, width, height, radius, tintColor, 1.0F);
   }

   public void drawBlurRoundedRect(float x, float y, float width, float height, float radius, int tintColor, float opacity) {
      if (this.initialized && this.sceneCaptured) {
         float alpha = (float)(tintColor >> 24 & 255) / 255.0F;
         float red = (float)(tintColor >> 16 & 255) / 255.0F;
         float green = (float)(tintColor >> 8 & 255) / 255.0F;
         float blue = (float)(tintColor & 255) / 255.0F;
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.setActiveTexture(33984);
         this.blurFramebuffer.bindFramebufferTexture();
         GL20.glUseProgram(this.drawProgram);
         GL20.glUniform1i(this.drawTextureUniform, 0);
         GL20.glUniform4f(this.drawTintColorUniform, red, green, blue, alpha);
         GL20.glUniform1f(this.drawOpacityUniform, Math.max(0.0F, Math.min(1.0F, opacity)));
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         int cornerSegments = 8;
         float cx = x + width / 2.0F;
         float cy = y + height / 2.0F;
         GL11.glBegin(6);
         GL11.glVertex2f(cx, cy);
         this.addCornerFan(x + r, y + r, r, 180.0F, 270.0F, cornerSegments);
         this.addCornerFan(x + width - r, y + r, r, 270.0F, 360.0F, cornerSegments);
         this.addCornerFan(x + width - r, y + height - r, r, 0.0F, 90.0F, cornerSegments);
         this.addCornerFan(x + r, y + height - r, r, 90.0F, 180.0F, cornerSegments);
         double closeRad = Math.toRadians((double)180.0F);
         GL11.glVertex2f(x + r + (float)Math.cos(closeRad) * r, y + r + (float)Math.sin(closeRad) * r);
         GL11.glEnd();
         GL20.glUseProgram(0);
         GlStateManager.setActiveTexture(33984);
         GlStateManager.bindTexture(0);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(true);
      }
   }

   private void addCornerFan(float cx, float cy, float r, float startAngle, float endAngle, int segments) {
      float step = (endAngle - startAngle) / (float)segments;

      for(int i = 0; i <= segments; ++i) {
         double rad = Math.toRadians((double)(startAngle + step * (float)i));
         GL11.glVertex2f(cx + (float)Math.cos(rad) * r, cy + (float)Math.sin(rad) * r);
      }

   }

   public void drawBackground(float x1, float y1, float x2, float y2, float strength, int tintColor) {
      this.captureAndBlur(strength);
      this.drawBlurRect(x1, y1, x2, y2, tintColor);
   }

   public void drawRoundedBackground(float x, float y, float width, float height, float cornerRadius, float strength, int tintColor) {
      this.captureAndBlur(strength);
      this.drawBlurRoundedRect(x, y, width, height, cornerRadius, tintColor);
   }

   public boolean hasCapturedScene() {
      return this.sceneCaptured;
   }

   public void endFrame() {
      this.sceneCaptured = false;
   }

   public boolean isAvailable() {
      return this.initialized && OpenGlHelper.isFramebufferEnabled();
   }

   private void drawFullscreenQuad(int width, int height) {
      boolean blendWasEnabled = GL11.glIsEnabled(3042);
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.ortho((double)0.0F, (double)width, (double)0.0F, (double)height, (double)-1.0F, (double)1.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.disableDepth();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex2f(0.0F, 0.0F);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex2f((float)width, 0.0F);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex2f((float)width, (float)height);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex2f(0.0F, (float)height);
      GL11.glEnd();
      GlStateManager.enableDepth();
      if (blendWasEnabled) {
         GlStateManager.enableBlend();
      }

      GlStateManager.matrixMode(5889);
      GlStateManager.popMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.popMatrix();
   }

   private int createProgram(String vertexPath, String fragmentPath) throws IOException {
      int vert = this.compileShader(35633, this.loadShaderSource(vertexPath));
      int frag = this.compileShader(35632, this.loadShaderSource(fragmentPath));
      if (vert != -1 && frag != -1) {
         int program = GL20.glCreateProgram();
         GL20.glAttachShader(program, vert);
         GL20.glAttachShader(program, frag);
         GL20.glLinkProgram(program);
         if (GL20.glGetProgrami(program, 35714) == 0) {
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            return -1;
         } else {
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            return program;
         }
      } else {
         return -1;
      }
   }

   private String loadShaderSource(String path) throws IOException {
      InputStream stream = BlurManager.class.getResourceAsStream(path);
      Throwable var3 = null;

      String var7;
      try {
         if (stream == null) {
            throw new IOException("Missing blur shader resource: " + path);
         }

         ByteArrayOutputStream output = new ByteArrayOutputStream();
         byte[] buffer = new byte[4096];

         int read;
         while((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
         }

         var7 = new String(output.toByteArray(), StandardCharsets.UTF_8);
      } catch (Throwable var16) {
         var3 = var16;
         throw var16;
      } finally {
         if (stream != null) {
            if (var3 != null) {
               try {
                  stream.close();
               } catch (Throwable var15) {
                  var3.addSuppressed(var15);
               }
            } else {
               stream.close();
            }
         }

      }

      return var7;
   }

   private int compileShader(int type, String source) {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      if (GL20.glGetShaderi(shader, 35713) == 0) {
         GL20.glDeleteShader(shader);
         return -1;
      } else {
         return shader;
      }
   }

   private void deleteResources() {
      if (this.blurProgram != -1) {
         GL20.glDeleteProgram(this.blurProgram);
         this.blurProgram = -1;
      }

      if (this.drawProgram != -1) {
         GL20.glDeleteProgram(this.drawProgram);
         this.drawProgram = -1;
      }

      if (this.blurFramebuffer != null) {
         this.blurFramebuffer.deleteFramebuffer();
         this.blurFramebuffer = null;
      }

      if (this.swapFramebuffer != null) {
         this.swapFramebuffer.deleteFramebuffer();
         this.swapFramebuffer = null;
      }

      this.initialized = false;
      this.sceneCaptured = false;
   }
}
