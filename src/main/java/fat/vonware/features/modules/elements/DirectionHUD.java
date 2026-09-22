package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class DirectionHUD extends HudModule {
   private final Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", true);
   private final Setting<Integer> compassSize = new Setting<Integer>("Size", 300, 100, 500);
   private static final String[] LABELS = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
   private static final int BAR_HEIGHT = 18;
   private static final int TICK_LARGE = 5;
   private static final int TICK_SMALL = 3;
   private static final float TICK_DOT_R = 0.65F;
   private static final int PAD_V = 3;
   private static final float DEGREES_RATIO = 0.6666667F;
   private static final float FADE_DEG = 22.0F;
   private static final float BRIGHT_DEG = 16.0F;
   private static final float MUTE_DEG = 48.0F;
   private static final Color C_N_BRIGHT = new Color(255, 72, 72);
   private static final Color C_N_MUTED = new Color(155, 38, 38);
   private static final Color C_CARD_BRIGHT = new Color(255, 255, 255);
   private static final Color C_CARD_MUTED = new Color(105, 105, 105);
   private static final Color C_INTER_BRIGHT = new Color(205, 205, 205);
   private static final Color C_INTER_MUTED = new Color(80, 80, 80);
   private static final Color C_TICK = new Color(200, 200, 200);
   private static final Color C_TICK_NUM = new Color(145, 145, 145);
   private static final Color C_POINTER = new Color(255, 255, 255, 200);
   private static final Color C_BACKGROUND = new Color(0, 0, 0, 90);
   private static final float TICK_LABEL_SCALE = 0.65F;
   private static final ResourceLocation ARROW_ICON = new ResourceLocation("vonware", "textures/gui/enumarrow.png");
   private static final ResourceLocation PEARL_ICON = new ResourceLocation("minecraft", "textures/items/ender_pearl.png");
   private static final int PEARL_ICON_SIZE = 14;
   private static final float GROW_SPEED = 4.5F;
   private static final float SHRINK_SPEED = 5.5F;
   private final Map<Integer, TrackedPearl> trackedPearls = new LinkedHashMap();
   private final AnimationUtil.LerpAnimation yawAnim = new AnimationUtil.LerpAnimation(0.0F, 0.18F, 0.05F);
   private float displayYaw = 0.0F;
   private long lastRenderTime = -1L;
   private float frameDt = 0.0F;
   private static final int CIRCLE_SEGMENTS = 28;

   public DirectionHUD() {
      super("DirectionHUD", "Compass bar showing your facing direction", Module.Category.Elements, true, false, false, 300.0F, 20.0F);
      this.register(this.autoPosition);
      this.register(this.compassSize);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   public void onRender2D(Render2DEvent event) {
      if (!Feature.fullNullCheck()) {
         this.syncPearls();
         int w = (Integer)this.compassSize.getValue();
         int totalH = 24;
         if ((Boolean)this.autoPosition.getValue()) {
            ScaledResolution sr = new ScaledResolution(mc);
            this.rect.setX((float)sr.getScaledWidth() / 2.0F - (float)w / 2.0F);
            this.rect.setY(4.0F);
         } else {
            this.rect.setX((Float)this.x.getValue());
            this.rect.setY((Float)this.y.getValue());
         }

         this.rect.setWidth((float)w);
         this.rect.setHeight((float)totalH);
         float yaw;
         if (mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor()) {
            yaw = 180.0F;
         } else {
            float rawYaw = mc.player.rotationYaw % 360.0F;
            if (rawYaw < 0.0F) {
               rawYaw += 360.0F;
            }

            long now = System.nanoTime();
            float dt = this.lastRenderTime < 0L ? 0.0F : (float)(now - this.lastRenderTime) / 1.0E9F;
            this.lastRenderTime = now;
            this.frameDt = dt;
            float factor = 1.0F - (float)Math.pow((double)0.82F, (double)(dt * 60.0F));
            float delta = rawYaw - this.displayYaw;
            if (delta > 180.0F) {
               delta -= 360.0F;
            }

            if (delta < -180.0F) {
               delta += 360.0F;
            }

            if (Math.abs(delta) < 0.05F) {
               this.displayYaw = rawYaw;
            } else {
               this.displayYaw += delta * factor;
               this.displayYaw = (this.displayYaw % 360.0F + 360.0F) % 360.0F;
            }

            yaw = this.displayYaw;
         }

         this.drawCompass(this.rect.getX(), this.rect.getY(), w, totalH, yaw);
      }
   }

   private void syncPearls() {
      if (mc.world != null) {
         for(Entity e : mc.world.loadedEntityList) {
            if (e instanceof EntityEnderPearl) {
               EntityEnderPearl pearl = (EntityEnderPearl)e;
               if (!this.trackedPearls.containsKey(pearl.getEntityId())) {
                  this.trackedPearls.put(pearl.getEntityId(), new TrackedPearl(pearl));
               }
            }
         }

         float dt = this.lastRenderTime < 0L ? 0.0F : (float)(System.nanoTime() - this.lastRenderTime) / 1.0E9F;
         Iterator<TrackedPearl> it = this.trackedPearls.values().iterator();

         while(it.hasNext()) {
            if (((TrackedPearl)it.next()).tick(dt)) {
               it.remove();
            }
         }

      }
   }

   private void drawCompass(float rx, float ry, int w, int totalH, float yaw) {
      float barTop = ry + 3.0F;
      float barBottom = barTop + 18.0F;
      float centerX = rx + (float)w / 2.0F;
      float degreesShown = Math.min((float)w * 0.6666667F, 340.0F);
      float halfRange = degreesShown / 2.0F;
      float pxPerDeg = (float)w / degreesShown;
      float fadePx = 22.0F * pxPerDeg;
      this.drawBackground(rx, ry, w, totalH, fadePx);
      this.drawPointer(centerX, barTop);
      this.drawTicks(rx, barBottom, centerX, w, yaw, halfRange, pxPerDeg, degreesShown);
      this.drawLabels(barTop, centerX, yaw, halfRange, pxPerDeg);
      this.drawTrackedPearls(rx, barTop, centerX, w, yaw, halfRange, pxPerDeg);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawBackground(float rx, float ry, int w, int totalH, float fadeW) {
      int solid = C_BACKGROUND.getRGB();
      int clear = solid & 16777215;
      float fade = Math.min(fadeW, (float)w / 2.0F);
      float x2 = rx + fade;
      float x3 = rx + (float)w - fade;
      float x4 = rx + (float)w;
      float y2 = ry + (float)totalH;
      RenderUtil.drawGradientSideways(rx, ry, x2, y2, clear, solid);
      RenderUtil.drawRect(x2, ry, x3, y2, solid);
      RenderUtil.drawGradientSideways(x3, ry, x4, y2, solid, clear);
   }

   private void drawPointer(float cx, float barTop) {
      int size = 8;
      float yOffset = 4.0F;
      float x1 = cx - (float)size / 2.0F;
      float y1 = barTop - yOffset;
      float x2 = cx + (float)size / 2.0F;
      float y2 = barTop + (float)size - yOffset;
      boolean wasBlend = GL11.glIsEnabled(3042);
      boolean wasTexture = GL11.glIsEnabled(3553);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.color(1.0F, 1.0F, 1.0F, (float)C_POINTER.getAlpha() / 255.0F);
      GlStateManager.enableTexture2D();
      mc.getTextureManager().bindTexture(ARROW_ICON);
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx, (y1 + y2) / 2.0F, 0.0F);
      GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.translate(-cx, -(y1 + y2) / 2.0F, 0.0F);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_TEX);
      buf.pos((double)x1, (double)y2, (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buf.pos((double)x2, (double)y2, (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buf.pos((double)x2, (double)y1, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buf.pos((double)x1, (double)y1, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.popMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      if (!wasTexture) {
         GlStateManager.disableTexture2D();
      }

      if (!wasBlend) {
         GlStateManager.disableBlend();
      }

   }

   private void drawTicks(float rx, float barBottom, float centerX, int w, float yaw, float halfRange, float pxPerDeg, float degreesShown) {
      int startDeg = (int)Math.floor((double)((yaw - halfRange) / 15.0F)) * 15;
      int endDeg = startDeg + (int)degreesShown + 15;
      float barTop = barBottom - 18.0F;

      for(int deg = startDeg; deg <= endDeg; deg += 15) {
         float offset = this.normaliseOffset((float)deg - yaw);
         float px = centerX + offset * pxPerDeg;
         if (!(px < rx) && !(px > rx + (float)w)) {
            float eA = this.angularEdgeAlpha(Math.abs(offset), halfRange);
            if (!(eA < 0.01F)) {
               int norm = (deg % 360 + 360) % 360;
               boolean is45 = norm % 45 == 0;
               if (is45) {
                  float tickBottomY = barTop + 5.0F;
                  RenderUtil.drawRect(px - 0.5F, barTop, px + 0.5F, tickBottomY, this.applyAlpha(C_TICK, 0.65F * eA));
               } else {
                  float cy = barTop + 2.7F;
                  String num = String.valueOf(norm);
                  float nw = (float)Vonware.textManager.getStringWidth(num) * 0.65F;
                  float ny = cy + 0.65F + 2.0F;
                  this.drawSmallText(num, px - nw / 2.0F, ny, 0.65F, this.applyAlpha(C_TICK_NUM, 0.9F * eA));
                  Color dotColor = new Color(C_TICK.getRed(), C_TICK.getGreen(), C_TICK.getBlue(), Math.round(0.75F * eA * 255.0F));
                  this.drawFilledCircle(px, cy, 0.65F, dotColor);
               }
            }
         }
      }

   }

   private void drawFilledCircle(float cx, float cy, float radius, Color color) {
      GL11.glPushAttrib(1048575);
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glDisable(2929);
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glEnable(2881);
      GL11.glHint(3155, 4354);
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);
      double step = 0.2243994752564138;

      for(int i = 0; i <= 28; ++i) {
         double angle = (double)i * step;
         GL11.glVertex2f((float)((double)cx + Math.cos(angle) * (double)radius), (float)((double)cy + Math.sin(angle) * (double)radius));
      }

      GL11.glEnd();
      GL11.glPopAttrib();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawLabels(float barTop, float centerX, float yaw, float halfRange, float pxPerDeg) {
      for(int i = 0; i < LABELS.length; ++i) {
         float labelYaw = (float)i * 45.0F;
         float offset = this.normaliseOffset(labelYaw - yaw);
         float eA = this.angularEdgeAlpha(Math.abs(offset), halfRange);
         if (!(eA < 0.01F)) {
            float px = centerX + offset * pxPerDeg;
            String lbl = LABELS[i];
            float t = this.clamp((Math.abs(offset) - 16.0F) / 32.0F);
            Color bright;
            Color muted;
            if (lbl.equals("N")) {
               bright = C_N_BRIGHT;
               muted = C_N_MUTED;
            } else if (i % 2 == 0) {
               bright = C_CARD_BRIGHT;
               muted = C_CARD_MUTED;
            } else {
               bright = C_INTER_BRIGHT;
               muted = C_INTER_MUTED;
            }

            int rgb = this.lerpRGB(bright, muted, t);
            boolean isCardinal = i % 2 == 0;
            float scale = isCardinal ? 1.1F : 1.0F;
            float lw = (float)Vonware.textManager.getStringWidth(lbl) * scale;
            float lh = (float)Vonware.textManager.getFontHeight2() * scale;
            float barBottom = barTop + 18.0F;
            float ly = barBottom - lh - 1.0F;
            this.drawSmallText(lbl, px - lw / 2.0F, ly, scale, this.applyAlpha(rgb, eA));
         }
      }

   }

   private void drawTrackedPearls(float rx, float barTop, float centerX, int w, float yaw, float halfRange, float pxPerDeg) {
      if (mc.player != null && !this.trackedPearls.isEmpty()) {
         for(TrackedPearl tp : this.trackedPearls.values()) {
            float scale = tp.displayScale();
            if (!(scale <= 0.01F)) {
               double dx = tp.entity.posX - mc.player.posX;
               double dz = tp.entity.posZ - mc.player.posZ;
               float pearlYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
               pearlYaw = (pearlYaw % 360.0F + 360.0F) % 360.0F;
               float offset = this.normaliseOffset(pearlYaw - yaw);
               float eA = this.angularEdgeAlpha(Math.abs(offset), halfRange);
               if (!(eA < 0.01F)) {
                  float px = centerX + offset * pxPerDeg;
                  if (!(px < rx) && !(px > rx + (float)w)) {
                     float size = 14.0F * scale;
                     float ix = px - size / 2.0F;
                     float iy = barTop + (18.0F - size) / 2.0F;
                     this.drawPearlIcon(ix, iy, size, eA);
                  }
               }
            }
         }

      }
   }

   private void drawPearlIcon(float x, float y, float size, float alpha) {
      boolean wasBlend = GL11.glIsEnabled(3042);
      boolean wasTexture = GL11.glIsEnabled(3553);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      mc.getTextureManager().bindTexture(PEARL_ICON);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_TEX);
      buf.pos((double)x, (double)(y + size), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + size), (double)(y + size), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + size), (double)y, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buf.pos((double)x, (double)y, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      if (!wasTexture) {
         GlStateManager.disableTexture2D();
      }

      if (!wasBlend) {
         GlStateManager.disableBlend();
      }

   }

   private float normaliseOffset(float d) {
      d %= 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private float angularEdgeAlpha(float angDist, float halfRange) {
      return this.clamp((halfRange - angDist) / 22.0F);
   }

   private float clamp(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private int lerp(int a, int b, float t) {
      return Math.round((float)a + (float)(b - a) * t);
   }

   private int lerpRGB(Color bright, Color muted, float t) {
      int r = this.lerp(bright.getRed(), muted.getRed(), t);
      int g = this.lerp(bright.getGreen(), muted.getGreen(), t);
      int b = this.lerp(bright.getBlue(), muted.getBlue(), t);
      return r << 16 | g << 8 | b;
   }

   private int applyAlpha(int rgb, float alpha) {
      int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      a = a >> 2 << 2;
      return a << 24 | rgb & 16777215;
   }

   private int applyAlpha(Color c, float alpha) {
      return this.applyAlpha(c.getRGB(), alpha);
   }

   private void drawSmallText(String text, float x, float y, float scale, int color) {
      if ((color >> 24 & 255) >= 10) {
         float rx = (float)Math.round(x * 2.0F) / 2.0F;
         float ry = (float)Math.round(y * 2.0F) / 2.0F;
         GlStateManager.pushMatrix();
         GlStateManager.translate(rx, ry, 0.0F);
         GlStateManager.scale(scale, scale, 1.0F);
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableAlpha();
         Vonware.textManager.drawStringWithShadow(text, 0.0F, 0.0F, color);
         GlStateManager.enableAlpha();
         GlStateManager.disableTexture2D();
         GlStateManager.popMatrix();
      }
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      this.rect.setWidth((float)(Integer)this.compassSize.getValue());
      this.rect.setHeight(24.0F);
   }

   private static final class TrackedPearl {
      final EntityEnderPearl entity;
      float progress = 0.0F;
      boolean shrinking = false;
      float smoothedYaw = Float.NaN;

      TrackedPearl(EntityEnderPearl entity) {
         this.entity = entity;
      }

      boolean tick(float dt) {
         if (!this.shrinking && this.entity.isDead) {
            this.shrinking = true;
         }

         if (this.shrinking) {
            this.progress -= dt * 5.5F;
            return this.progress <= 0.0F;
         } else {
            this.progress = Math.min(1.0F, this.progress + dt * 4.5F);
            return false;
         }
      }

      void lerpYaw(float targetYaw, float dt) {
         if (Float.isNaN(this.smoothedYaw)) {
            this.smoothedYaw = targetYaw;
         } else {
            float factor = 1.0F - (float)Math.pow((double)0.72F, (double)(dt * 60.0F));
            float delta = targetYaw - this.smoothedYaw;
            if (delta > 180.0F) {
               delta -= 360.0F;
            }

            if (delta < -180.0F) {
               delta += 360.0F;
            }

            if (Math.abs(delta) < 0.05F) {
               this.smoothedYaw = targetYaw;
            } else {
               this.smoothedYaw = ((this.smoothedYaw + delta * factor) % 360.0F + 360.0F) % 360.0F;
            }

         }
      }

      float displayScale() {
         float t = Math.max(0.0F, Math.min(1.0F, this.progress));
         if (this.shrinking) {
            return t * t * t;
         } else {
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), (double)3.0F) + c1 * (float)Math.pow((double)(t - 1.0F), (double)2.0F);
         }
      }
   }
}
