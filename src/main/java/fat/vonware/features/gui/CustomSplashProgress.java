package fat.vonware.features.gui;

import fat.vonware.Vonware;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ICrashCallable;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.SharedDrawable;

public class CustomSplashProgress {
   private static Drawable drawable;
   private static volatile boolean pause = false;
   private static volatile boolean done = false;
   private static Thread thread;
   private static volatile Throwable threadError;
   private static final Lock lock = new ReentrantLock(true);
   static final Semaphore mutex = new Semaphore(1);
   private static final IResourcePack mcPack;
   private static Texture fontTexture;
   private static SplashFontRenderer fontRenderer;
   private static int shaderProgram;
   private static int uTime;
   private static int uResolution;
   private static int uProgress;
   static boolean isDisplayVSyncForced;
   private static int max_texture_size;
   private static final IntBuffer buf;
   private static final long startTime;
   private static SplashTextRenderer titleRenderer;
   private static SplashTextRenderer uiRenderer;

   public static void start() {
      Vonware.LOGGER.info("Starting Mystic Custom Splash Screen");
      FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
         public String call() {
            return "' Vendor: '" + GL11.glGetString(7936) + "' Version: '" + GL11.glGetString(7938) + "' Renderer: '" + GL11.glGetString(7937) + "'";
         }

         public String getLabel() {
            return "GL info";
         }
      });

      try {
         drawable = new SharedDrawable(Display.getDrawable());
         Display.getDrawable().releaseContext();
         drawable.makeCurrent();
      } catch (LWJGLException e) {
         FMLLog.log.error("Error starting SplashProgress:", e);
         throw new RuntimeException(e);
      }

      getMaxTextureSize();
      thread = new Thread(new Runnable() {
         private long updateTiming = 0L;
         private long framecount = 0L;

         public void run() {
            this.setGL();
            CustomSplashProgress.shaderProgram = CustomSplashProgress.compileShader();
            if (CustomSplashProgress.shaderProgram != -1) {
               CustomSplashProgress.uTime = GL20.glGetUniformLocation(CustomSplashProgress.shaderProgram, "time");
               CustomSplashProgress.uResolution = GL20.glGetUniformLocation(CustomSplashProgress.shaderProgram, "resolution");
               CustomSplashProgress.uProgress = GL20.glGetUniformLocation(CustomSplashProgress.shaderProgram, "progress");
            }

            CustomSplashProgress.titleRenderer = new SplashTextRenderer("/assets/vonware/fonts/FoughtKnight.ttf", 104f);
            CustomSplashProgress.uiRenderer = new SplashTextRenderer("/assets/vonware/fonts/Lora-Regular.ttf", 13.0F);

            while(!CustomSplashProgress.done) {
               ++this.framecount;
               ProgressManager.ProgressBar first = null;
               ProgressManager.ProgressBar last = null;
               Iterator<ProgressManager.ProgressBar> bars = ProgressManager.barIterator();

               while(bars.hasNext()) {
                  if (first == null) {
                     first = (ProgressManager.ProgressBar)bars.next();
                  } else {
                     last = (ProgressManager.ProgressBar)bars.next();
                  }
               }

               GL11.glClear(16384);
               int w = Display.getWidth();
               int h = Display.getHeight();
               GL11.glViewport(0, 0, w, h);
               GL11.glMatrixMode(5889);
               GL11.glLoadIdentity();
               GL11.glOrtho((double)0.0F, (double)w, (double)h, (double)0.0F, (double)-1.0F, (double)1.0F);
               GL11.glMatrixMode(5888);
               GL11.glLoadIdentity();
               float time = (float)(System.currentTimeMillis() - CustomSplashProgress.startTime) / 1000.0F;
               float progress = this.computeProgress(first);
               this.drawShaderBackground(w, h, time, progress);
               if (first != null) {
                  this.drawProgressBar(w, h, first, progress);
                  this.drawLoadingText(w, h, first);
               }

               this.drawTitle(w, h);
               this.drawVersionInfo(w, h);
               CustomSplashProgress.mutex.acquireUninterruptibly();
               long updateStart = System.nanoTime();
               Display.update();
               long dur = System.nanoTime() - updateStart;
               if (this.framecount < 200L) {
                  this.updateTiming += dur;
               }

               CustomSplashProgress.mutex.release();
               if (CustomSplashProgress.pause) {
                  this.clearGL();
                  this.setGL();
               }

               if (this.framecount >= 200L && this.updateTiming > 1000000000L) {
                  if (!CustomSplashProgress.isDisplayVSyncForced) {
                     CustomSplashProgress.isDisplayVSyncForced = true;
                     FMLLog.log.info("Using alternative sync timing");
                  }

                  try {
                     Thread.sleep(16L);
                  } catch (InterruptedException var13) {
                  }
               } else {
                  if (this.framecount == 200L) {
                     FMLLog.log.info("Using sync timing");
                  }

                  Display.sync(100);
               }
            }

            this.clearGL();
         }

         private void drawShaderBackground(int w, int h, float time, float progress) {
            GL11.glDisable(3553);
            GL11.glDisable(3042);
            if (CustomSplashProgress.shaderProgram != -1) {
               GL20.glUseProgram(CustomSplashProgress.shaderProgram);
               GL20.glUniform1f(CustomSplashProgress.uTime, time);
               GL20.glUniform2f(CustomSplashProgress.uResolution, (float)w, (float)h);
               GL20.glUniform1f(CustomSplashProgress.uProgress, progress);
            }

            GL11.glBegin(7);
            GL11.glVertex2f(0.0F, 0.0F);
            GL11.glVertex2f((float)w, 0.0F);
            GL11.glVertex2f((float)w, (float)h);
            GL11.glVertex2f(0.0F, (float)h);
            GL11.glEnd();
            if (CustomSplashProgress.shaderProgram != -1) {
               GL20.glUseProgram(0);
            }

            GL11.glEnable(3042);
            GL11.glEnable(3553);
         }

         private void drawTitle(int w, int h) {
            String title = "loading: Dogshit";
            int midX = w / 2;
            int titleY = h / 3;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            CustomSplashProgress.titleRenderer.drawString(title, (float)midX - (float)CustomSplashProgress.titleRenderer.getStringWidth(title) / 2.0F, (float)titleY, Color.RED.getRGB());
         }

         private void drawProgressBar(int w, int h, ProgressManager.ProgressBar bar, float progress) {
            int barW = 400;
            int barH = 8;
            int midX = w / 2;
            int barY = h - 100;
            int barX = midX - barW / 2;
            int pw = (int)((float)barW * progress);
            GL11.glDisable(3553);
            GL11.glColor4f(1.0F, 1.0F, 0.455F, 1.0F);
            this.drawRect(barX - 2, barY - 2, barW + 4, barH + 4);
            GL11.glColor4f(0.04F, 0.04F, 0.07F, 1.0F);
            this.drawRect(barX, barY, barW, barH);
            if (pw > 0) {
               GL11.glColor4f(1.0F, 0.863F, 0.451F, 1.0F);
               this.drawRect(barX, barY, pw, barH);
            }

            String pct = String.format("%.0f%%", progress * 100.0F);
            CustomSplashProgress.uiRenderer.drawString(pct, (float)midX - (float)CustomSplashProgress.uiRenderer.getStringWidth(pct) / 2.0F, (float)(barY + barH) + 8.0F, -9101);
         }

         private void drawLoadingText(int w, int h, ProgressManager.ProgressBar bar) {
            long elapsed = System.currentTimeMillis() - CustomSplashProgress.startTime;
            int dots = (int)(elapsed / 500L % 4L);
            StringBuilder ds = new StringBuilder();

            for(int i = 0; i < dots; ++i) {
               ds.append('.');
            }

            String text = bar.getTitle() + " - " + bar.getMessage() + ds;
            CustomSplashProgress.uiRenderer.drawString(text, (float)w / 2.0F - (float)CustomSplashProgress.uiRenderer.getStringWidth(text) / 2.0F, (float)h - 60.0F, -7829334);
         }

         private void drawVersionInfo(int w, int h) {
            CustomSplashProgress.uiRenderer.drawString("Minecraft 1.12.2", 10.0F, (float)h - 20.0F, -13421739);
         }

         private float computeProgress(ProgressManager.ProgressBar bar) {
            return bar == null ? 0.0F : (float)(bar.getStep() + 1) / (float)(bar.getSteps() + 1);
         }

         private void drawRect(int x, int y, int w, int h) {
            GL11.glBegin(7);
            GL11.glVertex2f((float)x, (float)y);
            GL11.glVertex2f((float)x, (float)(y + h));
            GL11.glVertex2f((float)(x + w), (float)(y + h));
            GL11.glVertex2f((float)(x + w), (float)y);
            GL11.glEnd();
         }

         private void setGL() {
            CustomSplashProgress.lock.lock();

            try {
               Display.getDrawable().makeCurrent();
            } catch (LWJGLException e) {
               throw new RuntimeException(e);
            }

            GL11.glClearColor(0.012F, 0.012F, 0.031F, 1.0F);
            GL11.glDisable(2896);
            GL11.glDisable(2929);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         private void clearGL() {
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayWidth = Display.getWidth();
            mc.displayHeight = Display.getHeight();
            mc.resize(mc.displayWidth, mc.displayHeight);
            GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(2929);
            GL11.glDepthFunc(515);
            GL11.glEnable(3008);
            GL11.glAlphaFunc(516, 0.1F);

            try {
               Display.getDrawable().releaseContext();
            } catch (LWJGLException e) {
               throw new RuntimeException(e);
            } finally {
               CustomSplashProgress.lock.unlock();
            }

         }
      });
      thread.setUncaughtExceptionHandler((t, ex) -> {
         FMLLog.log.error("Splash thread Exception", ex);
         threadError = ex;
      });
      thread.start();
      checkThreadState();
   }

   public static void finish() {
      try {
         checkThreadState();
         done = true;
         thread.join();
         GL11.glFlush();
         if (shaderProgram != -1) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
         }

         if (fontTexture != null) {
            fontTexture.delete();
         }

         drawable.releaseContext();
         Display.getDrawable().makeCurrent();
      } catch (Exception e) {
         FMLLog.log.error("Error finishing SplashProgress:", e);
      }

   }

   private static int compileShader() {
      try {
         InputStream vshStream = CustomSplashProgress.class.getResourceAsStream("/shaders/splash/splash.vsh");
         InputStream fshStream = CustomSplashProgress.class.getResourceAsStream("/shaders/splash/splash.fsh");
         if (vshStream == null) {
            FMLLog.log.error("[SplashShader] Vertex shader not found: /shaders/splash/splash.vsh");
            return -1;
         } else if (fshStream == null) {
            FMLLog.log.error("[SplashShader] Fragment shader not found: /shaders/splash/splash.fsh");
            return -1;
         } else {
            int vert = buildShader(35633, readStream(vshStream), "vertex");
            if (vert == -1) {
               return -1;
            } else {
               int frag = buildShader(35632, readStream(fshStream), "fragment");
               if (frag == -1) {
                  GL20.glDeleteShader(vert);
                  return -1;
               } else {
                  int prog = GL20.glCreateProgram();
                  GL20.glAttachShader(prog, vert);
                  GL20.glAttachShader(prog, frag);
                  GL20.glLinkProgram(prog);
                  GL20.glDeleteShader(vert);
                  GL20.glDeleteShader(frag);
                  if (GL20.glGetProgrami(prog, 35714) == 0) {
                     FMLLog.log.error("[SplashShader] Link failed: {}", GL20.glGetProgramInfoLog(prog, 512));
                     GL20.glDeleteProgram(prog);
                     return -1;
                  } else {
                     FMLLog.log.info("[SplashShader] Compiled and linked successfully (id={})", prog);
                     return prog;
                  }
               }
            }
         }
      } catch (IOException e) {
         FMLLog.log.error("[SplashShader] IO error during compile", e);
         return -1;
      }
   }

   private static int buildShader(int type, String src, String label) {
      int id = GL20.glCreateShader(type);
      GL20.glShaderSource(id, src);
      GL20.glCompileShader(id);
      if (GL20.glGetShaderi(id, 35713) == 0) {
         FMLLog.log.error("[SplashShader] {} shader compile error:\n{}", label, GL20.glGetShaderInfoLog(id, 512));
         GL20.glDeleteShader(id);
         return -1;
      } else {
         return id;
      }
   }

   private static String readStream(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[512];

      int read;
      while((read = in.read(buf)) != -1) {
         out.write(buf, 0, read);
      }

      IOUtils.closeQuietly(in);
      return out.toString(StandardCharsets.UTF_8.name());
   }

   private static void checkThreadState() {
      if (thread.getState() == State.TERMINATED || threadError != null) {
         throw new IllegalStateException("Splash thread", threadError);
      }
   }

   public static int getMaxTextureSize() {
      if (max_texture_size != -1) {
         return max_texture_size;
      } else {
         for(int i = 16384; i > 0; i >>= 1) {
            GlStateManager.glTexImage2D(32868, 0, 6408, i, i, 0, 6408, 5121, (IntBuffer)null);
            if (GlStateManager.glGetTexLevelParameteri(32868, 0, 4096) != 0) {
               max_texture_size = i;
               return i;
            }
         }

         return -1;
      }
   }

   static {
      mcPack = Minecraft.getMinecraft().defaultResourcePack;
      shaderProgram = -1;
      uTime = -1;
      uResolution = -1;
      uProgress = -1;
      isDisplayVSyncForced = false;
      max_texture_size = -1;
      buf = BufferUtils.createIntBuffer(4194304);
      startTime = System.currentTimeMillis();
   }

   private static class Texture {
      private final ResourceLocation location;
      private final int name;
      private final int width;
      private final int height;

      public Texture(ResourceLocation location, ResourceLocation fallback) {
         this.location = location;
         InputStream s = null;

         try {
            s = CustomSplashProgress.mcPack.getInputStream(location);
            ImageInputStream stream = ImageIO.createImageInputStream(s);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
               throw new IOException("No suitable reader found for image " + location);
            }

            ImageReader reader = (ImageReader)readers.next();
            reader.setInput(stream);
            BufferedImage image = reader.read(0);
            reader.dispose();
            this.width = image.getWidth();
            this.height = image.getHeight();
            GL11.glEnable(3553);
            synchronized(CustomSplashProgress.class) {
               this.name = GL11.glGenTextures();
               GL11.glBindTexture(3553, this.name);
            }

            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            CustomSplashProgress.buf.clear();

            for(int y = 0; y < this.height; ++y) {
               for(int x = 0; x < this.width; ++x) {
                  CustomSplashProgress.buf.put(image.getRGB(x, y));
               }
            }

            CustomSplashProgress.buf.position(0).limit(this.width * this.height);
            GL11.glTexImage2D(3553, 0, 6408, this.width, this.height, 0, 32993, 33639, CustomSplashProgress.buf);
            GL11.glBindTexture(3553, 0);
            GL11.glDisable(3553);
         } catch (IOException e) {
            FMLLog.log.error("Error reading texture: {}", location, e);
            throw new RuntimeException(e);
         } finally {
            IOUtils.closeQuietly(s);
         }

      }

      public void bind() {
         GL11.glBindTexture(3553, this.name);
      }

      public void delete() {
         GL11.glDeleteTextures(this.name);
      }

      public ResourceLocation getLocation() {
         return this.location;
      }
   }

   private static class SplashTextRenderer {
      private final Font font;
      private final FontMetrics metrics;

      public SplashTextRenderer(String resourcePath, float size) {
         Font loaded;
         try {
            InputStream is = CustomSplashProgress.class.getResourceAsStream(resourcePath);
            Throwable var5 = null;

            try {
               if (is == null) {
                  throw new IOException("Not found: " + resourcePath);
               }

               loaded = Font.createFont(0, is).deriveFont(size);
            } catch (Throwable var15) {
               var5 = var15;
               throw var15;
            } finally {
               if (is != null) {
                  if (var5 != null) {
                     try {
                        is.close();
                     } catch (Throwable var14) {
                        var5.addSuppressed(var14);
                     }
                  } else {
                     is.close();
                  }
               }

            }
         } catch (Exception e) {
            FMLLog.log.error("[SplashTextRenderer] Falling back to sans-serif: {}", e.getMessage());
            loaded = new Font("SansSerif", 0, (int)size);
         }

         this.font = loaded;
         BufferedImage dummy = new BufferedImage(1, 1, 2);
         Graphics2D g = dummy.createGraphics();
         g.setFont(this.font);
         this.metrics = g.getFontMetrics();
         g.dispose();
      }

      public int getStringWidth(String text) {
         return this.metrics.stringWidth(text);
      }

      public int getLineHeight() {
         return this.metrics.getHeight();
      }

      public void drawString(String text, float x, float y, int color) {
         if (text != null && !text.isEmpty()) {
            int a = color >> 24 & 255;
            int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            int imgW = this.metrics.stringWidth(text) + 4;
            int imgH = this.metrics.getHeight() + 4;
            if (imgW > 0 && imgH > 0) {
               BufferedImage img = new BufferedImage(imgW, imgH, 2);
               Graphics2D g2 = img.createGraphics();
               g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
               g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
               g2.setFont(this.font);
               int baseline = this.metrics.getAscent() + 2;
               g2.setColor(new Color(0, 0, 0, Math.min(255, a / 2)));
               g2.drawString(text, 2, baseline + 1);
               g2.setColor(new Color(r, g, b, a));
               g2.drawString(text, 1, baseline);
               g2.dispose();
               int[] pixels = new int[imgW * imgH];
               img.getRGB(0, 0, imgW, imgH, pixels, 0, imgW);
               IntBuffer pixBuf = BufferUtils.createIntBuffer(pixels.length);
               pixBuf.put(pixels).flip();
               GL11.glEnable(3553);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               int texId = GL11.glGenTextures();
               GL11.glBindTexture(3553, texId);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexImage2D(3553, 0, 6408, imgW, imgH, 0, 32993, 33639, pixBuf);
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               GL11.glBegin(7);
               GL11.glTexCoord2f(0.0F, 0.0F);
               GL11.glVertex2f(x, y);
               GL11.glTexCoord2f(1.0F, 0.0F);
               GL11.glVertex2f(x + (float)imgW, y);
               GL11.glTexCoord2f(1.0F, 1.0F);
               GL11.glVertex2f(x + (float)imgW, y + (float)imgH);
               GL11.glTexCoord2f(0.0F, 1.0F);
               GL11.glVertex2f(x, y + (float)imgH);
               GL11.glEnd();
               GL11.glDeleteTextures(texId);
               GL11.glDisable(3553);
            }
         }
      }
   }

   private static class SplashFontRenderer extends FontRenderer {
      public SplashFontRenderer() {
         super(Minecraft.getMinecraft().gameSettings, CustomSplashProgress.fontTexture.getLocation(), (TextureManager)null, false);
         super.onResourceManagerReload((IResourceManager)null);
      }

      protected void bindTexture(@Nonnull ResourceLocation location) {
         if (location != this.locationFontTexture) {
            throw new IllegalArgumentException();
         } else {
            CustomSplashProgress.fontTexture.bind();
         }
      }

      @Nonnull
      protected IResource getResource(@Nonnull ResourceLocation location) throws IOException {
         DefaultResourcePack pack = Minecraft.getMinecraft().defaultResourcePack;
         return new SimpleResource(pack.getPackName(), location, pack.getInputStream(location), (InputStream)null, (MetadataSerializer)null);
      }
   }
}
