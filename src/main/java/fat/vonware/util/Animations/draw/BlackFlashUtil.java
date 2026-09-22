package fat.vonware.util.Animations.draw;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class BlackFlashUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final List<BlackFlashEffect> EFFECTS = new ArrayList();
   private static final long DURATION = 650L;

   private BlackFlashUtil() {
   }

   public static void spawn(Entity entity) {
      if (entity != null) {
         spawn(entity.posX, entity.posY + (double)entity.height * 0.52, entity.posZ, Math.max(1.6F, entity.width * 2.8F));
      }
   }

   public static void spawn(double x, double y, double z) {
      spawn(x, y, z, 2.2F);
   }

   public static void spawn(double x, double y, double z, float radius) {
      EFFECTS.add(new BlackFlashEffect(x, y, z, radius));
   }

   public static void render(float partialTicks) {
      if (mc.world != null && mc.player != null) {
         RenderManager renderManager = mc.getRenderManager();
         double cameraX = renderManager.viewerPosX;
         double cameraY = renderManager.viewerPosY;
         double cameraZ = renderManager.viewerPosZ;
         int previousMatrixMode = GL11.glGetInteger(2976);
         RenderState previousState = BlackFlashUtil.RenderState.capture();

         try {
            setupRender();
            GL11.glMatrixMode(5888);
            Iterator<BlackFlashEffect> iterator = EFFECTS.iterator();

            while(iterator.hasNext()) {
               BlackFlashEffect effect = (BlackFlashEffect)iterator.next();
               float progress = effect.getProgress();
               if (progress >= 1.0F) {
                  iterator.remove();
               } else {
                  GL11.glPushMatrix();

                  try {
                     GL11.glTranslated(effect.x - cameraX, effect.y - cameraY, effect.z - cameraZ);
                     renderEffect(effect, progress);
                  } finally {
                     GL11.glPopMatrix();
                  }
               }
            }
         } finally {
            GL11.glMatrixMode(previousMatrixMode);
            previousState.restore();
         }

      } else {
         EFFECTS.clear();
      }
   }

   public static void clear() {
      EFFECTS.clear();
   }

   private static void renderEffect(BlackFlashEffect effect, float progress) {
      float impact = 1.0F - clamp(progress / 0.18F, 0.0F, 1.0F);
      float expansion = easeOutExpo(clamp(progress / 0.72F, 0.0F, 1.0F));
      float fade = 1.0F - smoothStep(0.56F, 1.0F, progress);
      float flashAlpha = Math.min(1.0F, impact * 1.7F) * fade;
      renderImpactCore(effect, progress, flashAlpha, expansion);
      renderShockwavePlane(effect, progress, expansion, fade, 0.0F, 0.0F, 0.0F, effect.radius * 1.25F, 0);
      renderShockwavePlane(effect, progress, expansion, fade * 0.86F, 58.0F, 18.0F, 27.0F, effect.radius, 1);
      renderShockwavePlane(effect, progress, expansion, fade * 0.7F, -36.0F, 62.0F, -14.0F, effect.radius * 0.82F, 2);
      float delayedProgress = clamp((progress - 0.12F) / 0.88F, 0.0F, 1.0F);
      if (delayedProgress > 0.0F) {
         float delayedExpansion = easeOutExpo(delayedProgress);
         float delayedFade = (1.0F - delayedProgress) * fade;
         renderShockwavePlane(effect, delayedProgress, delayedExpansion, delayedFade * 0.72F, 82.0F, -24.0F, 44.0F, effect.radius * 0.72F, 3);
      }

      renderEnergySpikes(effect, progress, expansion, fade);
      renderLightningCracks(effect, progress, expansion, fade);
   }

   private static void renderImpactCore(BlackFlashEffect effect, float progress, float impactAlpha, float expansion) {
      GL11.glPushMatrix();

      try {
         RenderManager renderManager = mc.getRenderManager();
         GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
         float pulse = 1.0F + (float)Math.sin((double)progress * Math.PI * (double)8.0F) * 0.09F;
         float size = effect.radius * (0.24F + expansion * 0.32F) * pulse;
         drawFilledDisc(size * 1.75F, 48, 200, 0, 15, (int)(120.0F * impactAlpha));
         drawFilledDisc(size * 1.35F, 48, 255, 8, 25, (int)(185.0F * impactAlpha));
         drawFilledDisc(size, 48, 0, 0, 0, (int)(245.0F * impactAlpha));
         drawFilledDisc(size * 0.24F, 32, 255, 35, 50, (int)(235.0F * impactAlpha));
      } finally {
         GL11.glPopMatrix();
      }

   }

   private static void renderShockwavePlane(BlackFlashEffect effect, float progress, float expansion, float alpha, float rotateX, float rotateY, float rotateZ, float baseRadius, int layer) {
      if (!(alpha <= 0.01F)) {
         GL11.glPushMatrix();

         try {
            GL11.glRotatef(rotateX, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(rotateY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rotateZ, 0.0F, 0.0F, 1.0F);
            float radius = baseRadius * (0.18F + expansion * 1.7F);
            drawDistortedRing(effect, radius, 0.055F, 96, layer, progress, 255, 5, 30, (int)(220.0F * alpha));
            drawDistortedRing(effect, radius * 0.89F, 0.026F, 80, layer + 11, progress + 0.13F, 80, 0, 5, (int)(180.0F * alpha));
            drawDistortedRing(effect, radius * 1.08F, 0.022F, 72, layer + 23, progress + 0.21F, 255, 30, 55, (int)(125.0F * alpha));
         } finally {
            GL11.glPopMatrix();
         }

      }
   }

   private static void drawDistortedRing(BlackFlashEffect effect, float radius, float width, int segments, int layer, float progress, int red, int green, int blue, int alpha) {
      GL11.glLineWidth(Math.max(1.0F, width * 35.0F));
      GL11.glBegin(3);

      for(int i = 0; i <= segments; ++i) {
         float angle = (float)((Math.PI * 2D) * (double)i / (double)segments);
         float distortion = getRingDistortion(effect.seed, angle, layer, progress);
         float currentRadius = radius * (1.0F + distortion);
         float x = (float)Math.cos((double)angle) * currentRadius;
         float y = (float)Math.sin((double)angle) * currentRadius;
         float z = (float)Math.sin((double)(angle * 5.0F + (float)layer + progress * 9.0F)) * radius * 0.035F;
         GlStateManager.color((float)red / 255.0F, (float)green / 255.0F, (float)blue / 255.0F, (float)alpha / 255.0F);
         GL11.glVertex3f(x, y, z);
      }

      GL11.glEnd();
   }

   private static float getRingDistortion(long seed, float angle, int layer, float progress) {
      float first = (float)Math.sin((double)(angle * 3.0F + (float)seed * 0.0017F + (float)layer)) * 0.07F;
      float second = (float)Math.sin((double)(angle * 8.0F - progress * 6.0F + (float)layer * 1.8F)) * 0.035F;
      float sharp = Math.abs((float)Math.sin((double)(angle * 5.0F + (float)seed * 0.0031F))) * 0.035F;
      return first + second + sharp;
   }

   private static void renderEnergySpikes(BlackFlashEffect effect, float progress, float expansion, float alpha) {
      float spikeFade = (1.0F - smoothStep(0.42F, 0.92F, progress)) * alpha;
      if (!(spikeFade <= 0.01F)) {
         Random random = new Random(effect.seed + 195948965L);
         int spikeCount = 18;
         GL11.glBegin(4);

         for(int i = 0; i < spikeCount; ++i) {
            float yaw = random.nextFloat() * 360.0F;
            float pitch = -65.0F + random.nextFloat() * 130.0F;
            float length = effect.radius * (0.55F + random.nextFloat() * 1.55F) * expansion;
            float width = effect.radius * (0.025F + random.nextFloat() * 0.045F) * (1.0F - progress);
            Vector direction = directionFromRotation(yaw, pitch);
            Vector side = perpendicular(direction);
            float baseDistance = effect.radius * 0.12F;
            float baseX = direction.x * baseDistance;
            float baseY = direction.y * baseDistance;
            float baseZ = direction.z * baseDistance;
            float tipX = direction.x * length;
            float tipY = direction.y * length;
            float tipZ = direction.z * length;
            int spikeAlpha = (int)((120.0F + random.nextFloat() * 100.0F) * spikeFade);
            if ((i & 1) == 0) {
               GlStateManager.color(1.0F, 0.019607844F, 0.13725491F, (float)spikeAlpha / 255.0F);
            } else {
               GlStateManager.color(0.03137255F, 0.0F, 0.007843138F, (float)spikeAlpha / 255.0F);
            }

            GL11.glVertex3f(baseX + side.x * width, baseY + side.y * width, baseZ + side.z * width);
            GL11.glVertex3f(baseX - side.x * width, baseY - side.y * width, baseZ - side.z * width);
            GL11.glVertex3f(tipX, tipY, tipZ);
         }

         GL11.glEnd();
      }
   }

   private static void renderLightningCracks(BlackFlashEffect effect, float progress, float expansion, float alpha) {
      float crackFade = (1.0F - smoothStep(0.5F, 1.0F, progress)) * alpha;
      if (!(crackFade <= 0.01F)) {
         Random random = new Random(effect.seed ^ 1906753263L);
         int arcCount = 11;

         for(int arc = 0; arc < arcCount; ++arc) {
            float yaw = random.nextFloat() * 360.0F;
            float pitch = -55.0F + random.nextFloat() * 110.0F;
            Vector direction = directionFromRotation(yaw, pitch);
            float length = effect.radius * (0.7F + random.nextFloat() * 1.4F) * expansion;
            int segments = 5 + random.nextInt(5);
            drawLightningArc(random, direction, length, segments, effect.radius, crackFade);
         }

      }
   }

   private static void drawLightningArc(Random random, Vector direction, float length, int segments, float baseRadius, float alpha) {
      float startDistance = baseRadius * 0.16F;
      float previousX = direction.x * startDistance;
      float previousY = direction.y * startDistance;
      float previousZ = direction.z * startDistance;
      GL11.glLineWidth(4.8F);
      GlStateManager.color(0.0F, 0.0F, 0.0F, 205.0F * alpha / 255.0F);
      GL11.glBegin(3);
      GL11.glVertex3f(previousX, previousY, previousZ);
      float[][] points = new float[segments][3];

      for(int segment = 0; segment < segments; ++segment) {
         float percentage = ((float)segment + 1.0F) / (float)segments;
         float jitter = baseRadius * 0.15F * (1.0F - percentage * 0.45F);
         float x = direction.x * length * percentage + randomSigned(random, jitter);
         float y = direction.y * length * percentage + randomSigned(random, jitter);
         float z = direction.z * length * percentage + randomSigned(random, jitter);
         points[segment][0] = x;
         points[segment][1] = y;
         points[segment][2] = z;
         GL11.glVertex3f(x, y, z);
      }

      GL11.glEnd();
      GL11.glLineWidth(1.7F);
      GlStateManager.color(1.0F, 0.019607844F, 0.13725491F, 245.0F * alpha / 255.0F);
      GL11.glBegin(3);
      GL11.glVertex3f(previousX, previousY, previousZ);

      for(float[] point : points) {
         GL11.glVertex3f(point[0], point[1], point[2]);
      }

      GL11.glEnd();
   }

   private static void drawFilledDisc(float radius, int segments, int red, int green, int blue, int alpha) {
      GL11.glBegin(6);
      GlStateManager.color((float)red / 255.0F, (float)green / 255.0F, (float)blue / 255.0F, (float)alpha / 255.0F);
      GL11.glVertex3f(0.0F, 0.0F, 0.0F);

      for(int i = 0; i <= segments; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)segments;
         GL11.glVertex3d(Math.cos(angle) * (double)radius, Math.sin(angle) * (double)radius, (double)0.0F);
      }

      GL11.glEnd();
   }

   private static Vector directionFromRotation(float yaw, float pitch) {
      double yawRadians = Math.toRadians((double)yaw);
      double pitchRadians = Math.toRadians((double)pitch);
      float horizontal = (float)Math.cos(pitchRadians);
      return new Vector((float)Math.cos(yawRadians) * horizontal, (float)Math.sin(pitchRadians), (float)Math.sin(yawRadians) * horizontal);
   }

   private static Vector perpendicular(Vector vector) {
      Vector side = new Vector(-vector.z, 0.0F, vector.x);
      float length = (float)Math.sqrt((double)(side.x * side.x + side.y * side.y + side.z * side.z));
      return length <= 1.0E-4F ? new Vector(1.0F, 0.0F, 0.0F) : new Vector(side.x / length, side.y / length, side.z / length);
   }

   private static float randomSigned(Random random, float amount) {
      return (random.nextFloat() * 2.0F - 1.0F) * amount;
   }

   private static float easeOutExpo(float value) {
      return value >= 1.0F ? 1.0F : 1.0F - (float)Math.pow((double)2.0F, (double)(-10.0F * value));
   }

   private static float smoothStep(float edge0, float edge1, float value) {
      float normalized = clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
      return normalized * normalized * (3.0F - 2.0F * normalized);
   }

   private static float clamp(float value, float minimum, float maximum) {
      return Math.max(minimum, Math.min(maximum, value));
   }

   private static void setupRender() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.depthMask(false);
      RenderHelper.disableStandardItemLighting();
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
   }

   private static final class RenderState {
      private final boolean texture2D;
      private final boolean lighting;
      private final boolean light0;
      private final boolean light1;
      private final boolean cull;
      private final boolean alpha;
      private final boolean depth;
      private final boolean blend;
      private final boolean depthMask;
      private final boolean lineSmooth;
      private final int blendSrcRgb;
      private final int blendDstRgb;
      private final int blendSrcAlpha;
      private final int blendDstAlpha;
      private final int lineSmoothHint;
      private final float lineWidth;
      private final float red;
      private final float green;
      private final float blue;
      private final float alphaValue;

      private RenderState(boolean texture2D, boolean lighting, boolean light0, boolean light1, boolean cull, boolean alpha, boolean depth, boolean blend, boolean depthMask, boolean lineSmooth, int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha, int lineSmoothHint, float lineWidth, float red, float green, float blue, float alphaValue) {
         this.texture2D = texture2D;
         this.lighting = lighting;
         this.light0 = light0;
         this.light1 = light1;
         this.cull = cull;
         this.alpha = alpha;
         this.depth = depth;
         this.blend = blend;
         this.depthMask = depthMask;
         this.lineSmooth = lineSmooth;
         this.blendSrcRgb = blendSrcRgb;
         this.blendDstRgb = blendDstRgb;
         this.blendSrcAlpha = blendSrcAlpha;
         this.blendDstAlpha = blendDstAlpha;
         this.lineSmoothHint = lineSmoothHint;
         this.lineWidth = lineWidth;
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alphaValue = alphaValue;
      }

      private static RenderState capture() {
         FloatBuffer color = BufferUtils.createFloatBuffer(16);
         GL11.glGetFloat(2816, color);
         return new RenderState(GL11.glIsEnabled(3553), GL11.glIsEnabled(2896), GL11.glIsEnabled(16384), GL11.glIsEnabled(16385), GL11.glIsEnabled(2884), GL11.glIsEnabled(3008), GL11.glIsEnabled(2929), GL11.glIsEnabled(3042), GL11.glGetBoolean(2930), GL11.glIsEnabled(2848), GL11.glGetInteger(32969), GL11.glGetInteger(32968), GL11.glGetInteger(32971), GL11.glGetInteger(32970), GL11.glGetInteger(3154), GL11.glGetFloat(2849), color.get(0), color.get(1), color.get(2), color.get(3));
      }

      private void restore() {
         setTexture2D(this.texture2D);
         setLighting(this.lighting);
         setLight(0, this.light0);
         setLight(1, this.light1);
         setCull(this.cull);
         setAlpha(this.alpha);
         setDepth(this.depth);
         setBlend(this.blend);
         GlStateManager.tryBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
         GlStateManager.depthMask(this.depthMask);
         GlStateManager.color(this.red, this.green, this.blue, this.alphaValue);
         if (this.lineSmooth) {
            GL11.glEnable(2848);
         } else {
            GL11.glDisable(2848);
         }

         GL11.glHint(3154, this.lineSmoothHint);
         GL11.glLineWidth(this.lineWidth);
      }

      private static void setTexture2D(boolean enabled) {
         if (enabled) {
            GlStateManager.enableTexture2D();
         } else {
            GlStateManager.disableTexture2D();
         }

      }

      private static void setLighting(boolean enabled) {
         if (enabled) {
            GlStateManager.enableLighting();
         } else {
            GlStateManager.disableLighting();
         }

      }

      private static void setLight(int light, boolean enabled) {
         if (enabled) {
            GlStateManager.enableLight(light);
         } else {
            GlStateManager.disableLight(light);
         }

      }

      private static void setCull(boolean enabled) {
         if (enabled) {
            GlStateManager.enableCull();
         } else {
            GlStateManager.disableCull();
         }

      }

      private static void setAlpha(boolean enabled) {
         if (enabled) {
            GlStateManager.enableAlpha();
         } else {
            GlStateManager.disableAlpha();
         }

      }

      private static void setDepth(boolean enabled) {
         if (enabled) {
            GlStateManager.enableDepth();
         } else {
            GlStateManager.disableDepth();
         }

      }

      private static void setBlend(boolean enabled) {
         if (enabled) {
            GlStateManager.enableBlend();
         } else {
            GlStateManager.disableBlend();
         }

      }
   }

   private static final class BlackFlashEffect {
      private final double x;
      private final double y;
      private final double z;
      private final float radius;
      private final long startTime;
      private final long seed;

      private BlackFlashEffect(double x, double y, double z, float radius) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.startTime = System.currentTimeMillis();
         this.seed = Double.doubleToLongBits(x) ^ Long.rotateLeft(Double.doubleToLongBits(y), 17) ^ Long.rotateLeft(Double.doubleToLongBits(z), 31) ^ this.startTime;
      }

      private float getProgress() {
         return BlackFlashUtil.clamp((float)(System.currentTimeMillis() - this.startTime) / 650.0F, 0.0F, 1.0F);
      }
   }

   private static final class Vector {
      private final float x;
      private final float y;
      private final float z;

      private Vector(float x, float y, float z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
