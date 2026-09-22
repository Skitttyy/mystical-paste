package fat.vonware.util.Animations.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.font.CFont;
import fat.vonware.features.gui.font.CustomFont;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.render.NameTags;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Util;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class RenderUtil implements Util {
   public static RenderItem itemRender;
   public static ICamera camera;
   private final CustomFont customFont;
   public static Tessellator tessellator;
   public static BufferBuilder builder;
   private static final Map<String, Float> animValues;
   private static final Map<String, Integer> animPrevTarget;
   private static final Map<String, Float> animVelocity;

   public RenderUtil() {
      this.customFont = new CustomFont(new Font("Arial", 3, 23), true, false, CFont.FontQuality.HIGH);
   }

   public static void drawAnimatedInt(String id, int target, float x, float y, String suffix, int color, boolean shadow, float externalScale) {
      int prevTarget = (Integer)animPrevTarget.getOrDefault(id, target);
      int rawDelta = Math.abs(target - prevTarget);
      animPrevTarget.put(id, target);
      float targetSpeed = Math.min((float)rawDelta / 10.0F, 1.0F);
      float velocity = (Float)animVelocity.getOrDefault(id, targetSpeed);
      velocity += (targetSpeed - velocity) * 0.25F;
      animVelocity.put(id, velocity);
      float lerpFactor = 0.06F + velocity * 0.39F;
      float cur = (Float)animValues.getOrDefault(id, (float)target);
      cur += ((float)target - cur) * lerpFactor;
      if (Math.abs(cur - (float)target) < 0.2F) {
         cur = (float)target;
      }

      animValues.put(id, cur);
      float fontH = (float)mc.fontRenderer.FONT_HEIGHT;
      int valA = (int)Math.floor((double)cur);
      int valB = valA + ((float)target >= cur ? 1 : -1);
      float t = cur - (float)Math.floor((double)cur);
      float tEased = t * t * (3.0F - 2.0F * t);
      float yA = y + tEased * fontH;
      float yB = yA - fontH;
      int baseAlpha = color >> 24 & 255;
      if (baseAlpha == 0) {
         baseAlpha = 255;
      }

      int alphaA = (int)((float)baseAlpha * (1.0F - tEased));
      int alphaB = (int)((float)baseAlpha * tEased);
      int colorA = color & 16777215 | alphaA << 24;
      int colorB = color & 16777215 | alphaB << 24;
      String strA = valA + suffix;
      String strB = valB + suffix;
      ScaledResolution sr = new ScaledResolution(mc);
      float tot = externalScale * (float)sr.getScaleFactor();
      int sx = (int)(x * tot);
      int sy = (int)((float)mc.displayHeight - (y + fontH) * tot);
      int sw = (int)((float)(Math.max(mc.fontRenderer.getStringWidth(strA), mc.fontRenderer.getStringWidth(strB)) + 2) * tot);
      int sh = (int)(fontH * tot);
      GL11.glEnable(3089);
      GL11.glScissor(sx, Math.max(0, sy), Math.max(1, sw), Math.max(1, sh));
      if (shadow) {
         Vonware.textManager.drawStringWithShadow(strA, x, yA, colorA);
         Vonware.textManager.drawStringWithShadow(strB, x, yB, colorB);
      } else {
         mc.fontRenderer.drawString(strA, (float)((int)x), (float)((int)yA), colorA, false);
         mc.fontRenderer.drawString(strB, (float)((int)x), (float)((int)yB), colorB, false);
      }

      GL11.glDisable(3089);
   }

   public static void clearAnimatedValues() {
      animValues.clear();
      animPrevTarget.clear();
      animVelocity.clear();
   }

   public static Vec3d updateToCamera(Vec3d vec) {
      return new Vec3d(vec.x - mc.getRenderManager().viewerPosX, vec.y - mc.getRenderManager().viewerPosY, vec.z - mc.getRenderManager().viewerPosZ);
   }

   public static void drawLine(Vec3d from, Vec3d to, Color color, float width) {
      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableDepth();
      GL11.glLineWidth(width);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      double renderPosX = mc.getRenderManager().viewerPosX;
      double renderPosY = mc.getRenderManager().viewerPosY;
      double renderPosZ = mc.getRenderManager().viewerPosZ;
      GL11.glBegin(1);
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      GL11.glVertex3d(from.x - renderPosX, from.y - renderPosY, from.z - renderPosZ);
      GL11.glVertex3d(to.x - renderPosX, to.y - renderPosY, to.z - renderPosZ);
      GL11.glEnd();
      GL11.glDisable(2848);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.enableTexture2D();
      GlStateManager.popMatrix();
   }

   public static void drawBlurredRect(int x1, int y1, int x2, int y2, int color, boolean useGlobalColor) {
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableTexture2D();
      GlStateManager.disableAlpha();
      GlStateManager.disableCull();
      GlStateManager.shadeModel(7425);
      float shadowRadius = 10.0F;
      float shadowAlpha = (Float)NameTags.INSTANCE.shadowOpacity.getValue();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      int centerAlpha = (int)(shadowAlpha * 255.0F);
      int outerAlpha = 0;
      int shadowRed;
      int shadowGreen;
      int shadowBlue;
      if (useGlobalColor) {
         Color globalCol = Global.getGlobalColor();
         shadowRed = globalCol.getRed();
         shadowGreen = globalCol.getGreen();
         shadowBlue = globalCol.getBlue();
      } else {
         shadowRed = 0;
         shadowGreen = 0;
         shadowBlue = 0;
      }

      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x1, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)x2, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)x2, (double)((float)y1 - 10.0F), (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)x1, (double)((float)y1 - 10.0F), (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      tessellator.draw();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x1, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)x1, (double)((float)y2 + 10.0F), (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)x2, (double)((float)y2 + 10.0F), (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)x2, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      tessellator.draw();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x1, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)((float)x1 - 10.0F), (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)((float)x1 - 10.0F), (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)x1, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      tessellator.draw();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x2, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)x2, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();
      buffer.pos((double)((float)x2 + 10.0F), (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      buffer.pos((double)((float)x2 + 10.0F), (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      tessellator.draw();
      int cornerSegments = 16;
      buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x1, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();

      for(int i = 0; i <= 16; ++i) {
         double angle = Math.PI + (Math.PI / 2D) * ((double)i / (double)16.0F);
         double xOffset = Math.cos(angle) * (double)10.0F;
         double yOffset = Math.sin(angle) * (double)10.0F;
         buffer.pos((double)x1 + xOffset, (double)y1 + yOffset, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      }

      tessellator.draw();
      buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x2, (double)y1, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();

      for(int i = 0; i <= 16; ++i) {
         double angle = (Math.PI * 1.5D) + (Math.PI / 2D) * ((double)i / (double)16.0F);
         double xOffset = Math.cos(angle) * (double)10.0F;
         double yOffset = Math.sin(angle) * (double)10.0F;
         buffer.pos((double)x2 + xOffset, (double)y1 + yOffset, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      }

      tessellator.draw();
      buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x1, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();

      for(int i = 0; i <= 16; ++i) {
         double angle = (Math.PI / 2D) + (Math.PI / 2D) * ((double)i / (double)16.0F);
         double xOffset = Math.cos(angle) * (double)10.0F;
         double yOffset = Math.sin(angle) * (double)10.0F;
         buffer.pos((double)x1 + xOffset, (double)y2 + yOffset, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      }

      tessellator.draw();
      buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x2, (double)y2, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, centerAlpha).endVertex();

      for(int i = 0; i <= 16; ++i) {
         double angle = (double)0.0F + (Math.PI / 2D) * ((double)i / (double)16.0F);
         double xOffset = Math.cos(angle) * (double)10.0F;
         double yOffset = Math.sin(angle) * (double)10.0F;
         buffer.pos((double)x2 + xOffset, (double)y2 + yOffset, (double)0.0F).color(shadowRed, shadowGreen, shadowBlue, 0).endVertex();
      }

      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.enableTexture2D();
      GlStateManager.enableAlpha();
      GlStateManager.enableCull();
      Render2DMethods.drawRect((float)x1, (float)y1, (float)x2, (float)y2, color);
      GlStateManager.popMatrix();
   }

   public static void drawCircleHead(float x, float y, float radius, int color) {
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.color(red, green, blue, alpha);
      GL11.glEnable(2848);
      GL11.glBegin(9);

      for(int i = 0; i <= 360; ++i) {
         GL11.glVertex2d((double)(x + MathHelper.sin((float)i * (float)Math.PI / 180.0F) * radius), (double)(y + MathHelper.cos((float)i * (float)Math.PI / 180.0F) * radius));
      }

      GL11.glEnd();
      GL11.glBegin(2);

      for(int i = 0; i <= 360; ++i) {
         GL11.glVertex2d((double)(x + MathHelper.sin((float)i * (float)Math.PI / 180.0F) * radius), (double)(y + MathHelper.cos((float)i * (float)Math.PI / 180.0F) * radius));
      }

      GL11.glColor4d((double)1.0F, (double)1.0F, (double)1.0F, (double)1.0F);
      GL11.glEnd();
      GL11.glDisable(2848);
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawHead(EntityLivingBase player, float x, float y, float width, float height) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(((AbstractClientPlayer)player).getLocationSkin());
      drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8.0F, 8.0F, width, height, 64.0F, 64.0F);
      drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8.0F, 8.0F, width, height, 64.0F, 64.0F);
   }

   public static void drawScaledCustomSizeModalRect(float x, float y, float u, float v, float uWidth, float vHeight, float width, float height, float tileWidth, float tileHeight) {
      float f = 1.0F / tileWidth;
      float f2 = 1.0F / tileHeight;
      GlStateManager.enableBlend();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      bufferBuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
      bufferBuilder.pos((double)x, (double)(y + height), (double)0.0F).tex((double)(u * f), (double)((v + vHeight) * f2)).endVertex();
      bufferBuilder.pos((double)(x + width), (double)(y + height), (double)0.0F).tex((double)((u + uWidth) * f), (double)((v + vHeight) * f2)).endVertex();
      bufferBuilder.pos((double)(x + width), (double)y, (double)0.0F).tex((double)((u + uWidth) * f), (double)(v * f2)).endVertex();
      bufferBuilder.pos((double)x, (double)y, (double)0.0F).tex((double)(u * f), (double)(v * f2)).endVertex();
      tessellator.draw();
      GlStateManager.disableBlend();
   }

   public static void drawGradientSideways(float left, float top, float right, float bottom, int leftColor, int rightColor) {
      float alpha1 = (float)(leftColor >> 24 & 255) / 255.0F;
      float red1 = (float)(leftColor >> 16 & 255) / 255.0F;
      float green1 = (float)(leftColor >> 8 & 255) / 255.0F;
      float blue1 = (float)(leftColor & 255) / 255.0F;
      float alpha2 = (float)(rightColor >> 24 & 255) / 255.0F;
      float red2 = (float)(rightColor >> 16 & 255) / 255.0F;
      float green2 = (float)(rightColor >> 8 & 255) / 255.0F;
      float blue2 = (float)(rightColor & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.shadeModel(7425);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
      bufferbuilder.pos((double)left, (double)top, (double)0.0F).color(red1, green1, blue1, alpha1).endVertex();
      bufferbuilder.pos((double)left, (double)bottom, (double)0.0F).color(red1, green1, blue1, alpha1).endVertex();
      bufferbuilder.pos((double)right, (double)bottom, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      bufferbuilder.pos((double)right, (double)top, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.enableTexture2D();
   }

   public void drawText(BlockPos pos, String text, Color color) {
      if (pos != null && text != null) {
         GlStateManager.pushMatrix();
         glBillboardDistanceScaled((float)pos.getX() + 0.5F, (float)pos.getY() + 0.5F, (float)pos.getZ() + 0.5F, mc.player, 1.0F);
         GlStateManager.disableDepth();
         GlStateManager.translate(-((double)Vonware.textManager.getStringWidth(text) / (double)2.0F), (double)0.0F, (double)0.0F);
         this.customFont.drawString(text, (double)0.0F, (double)0.0F, ColorUtil.toRGBA(color), true);
         GlStateManager.popMatrix();
      }
   }

   public static void drawRect(float x, float y, float x2, float y2, int color) {
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.color(red, green, blue, alpha);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION);
      buffer.pos((double)x, (double)y2, (double)0.0F).endVertex();
      buffer.pos((double)x2, (double)y2, (double)0.0F).endVertex();
      buffer.pos((double)x2, (double)y, (double)0.0F).endVertex();
      buffer.pos((double)x, (double)y, (double)0.0F).endVertex();
      tessellator.draw();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawGradientRect(float x, float y, float w, float h, long color, long color2, boolean vertical) {
      float alpha = (float)(color >> 24 & 255L) / 255.0F;
      float red = (float)(color >> 16 & 255L) / 255.0F;
      float green = (float)(color >> 8 & 255L) / 255.0F;
      float blue = (float)(color & 255L) / 255.0F;
      float alpha2 = (float)(color2 >> 24 & 255L) / 255.0F;
      float red2 = (float)(color2 >> 16 & 255L) / 255.0F;
      float green2 = (float)(color2 >> 8 & 255L) / 255.0F;
      float blue2 = (float)(color2 & 255L) / 255.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.disableAlpha();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.shadeModel(7425);
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      if (vertical) {
         buffer.pos((double)x, (double)h, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         buffer.pos((double)w, (double)h, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         buffer.pos((double)w, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
         buffer.pos((double)x, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
      } else {
         buffer.pos((double)w, (double)h, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         buffer.pos((double)w, (double)y, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         buffer.pos((double)x, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
         buffer.pos((double)x, (double)h, (double)0.0F).color(red, green, blue, alpha).endVertex();
      }

      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.enableAlpha();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawGradientRect(float x, float y, float x2, float y2, int color1, int color2) {
      float alpha1 = (float)(color1 >> 24 & 255) / 255.0F;
      float red1 = (float)(color1 >> 16 & 255) / 255.0F;
      float green1 = (float)(color1 >> 8 & 255) / 255.0F;
      float blue1 = (float)(color1 & 255) / 255.0F;
      float alpha2 = (float)(color2 >> 24 & 255) / 255.0F;
      float red2 = (float)(color2 >> 16 & 255) / 255.0F;
      float green2 = (float)(color2 >> 8 & 255) / 255.0F;
      float blue2 = (float)(color2 & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.shadeModel(7425);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)x2, (double)y, (double)0.0F).color(red1, green1, blue1, alpha1).endVertex();
      buffer.pos((double)x, (double)y, (double)0.0F).color(red1, green1, blue1, alpha1).endVertex();
      buffer.pos((double)x, (double)y2, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      buffer.pos((double)x2, (double)y2, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.enableTexture2D();
   }

   public static void drawBottomBarFill(float x1, float y1, float x2, float y2, float radius, int r, int g, int b, int alpha) {
      float maxH = (x2 - x1) * 0.5F;
      float maxV = (y2 - y1) * 0.5F;
      float rad = Math.max(0.0F, Math.min(radius, Math.min(maxH, maxV)));
      int fillColor = (alpha & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
      drawRect(x1, y1 + rad, x2, y2 - rad, fillColor);
      drawRect(x1 + rad, y1, x2 - rad, y1 + rad, fillColor);
      drawRect(x1 + rad, y2 - rad, x2 - rad, y2, fillColor);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)alpha / 255.0F);
      GlStateManager.glBegin(4);
      drawFilledArcVertices((double)(x1 + rad), (double)(y1 + rad), (double)rad, (double)180.0F, (double)270.0F);
      drawFilledArcVertices((double)(x2 - rad), (double)(y1 + rad), (double)rad, (double)270.0F, (double)360.0F);
      drawFilledArcVertices((double)(x2 - rad), (double)(y2 - rad), (double)rad, (double)0.0F, (double)90.0F);
      drawFilledArcVertices((double)(x1 + rad), (double)(y2 - rad), (double)rad, (double)90.0F, (double)180.0F);
      GlStateManager.glEnd();
   }

   public static void drawFilledArcVertices(double cx, double cy, double r, double startAngle, double endAngle) {
      double totalSteps = Math.abs(endAngle - startAngle);
      int steps = Math.max(1, (int)(totalSteps / (double)12.0F));
      double stepSize = (endAngle - startAngle) / (double)steps;

      for(int i = 0; i < steps; ++i) {
         double a1 = startAngle + (double)i * stepSize;
         double a2 = startAngle + (double)(i + 1) * stepSize;
         double rad1 = Math.toRadians(a1);
         double rad2 = Math.toRadians(a2);
         GL11.glVertex2d(cx, cy);
         GL11.glVertex2d(cx + Math.cos(rad2) * r, cy + Math.sin(rad2) * r);
         GL11.glVertex2d(cx + Math.cos(rad1) * r, cy + Math.sin(rad1) * r);
      }

   }

   public static void drawBottomBarOutline(float x1, float y1, float x2, float y2, float radius, int r, int g, int b, int alpha) {
      float maxH = (x2 - x1) * 0.5F;
      float maxV = (y2 - y1) * 0.5F;
      float rad = Math.max(0.0F, Math.min(radius, Math.min(maxH, maxV)));
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)alpha / 255.0F);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(1.5F);
      GL11.glBegin(2);
      barArc(x1 + rad, y1 + rad, rad, Math.PI, (Math.PI * 1.5D));
      barArc(x2 - rad, y1 + rad, rad, (Math.PI * 1.5D), (Math.PI * 2D));
      barArc(x2 - rad, y2 - rad, rad, (double)0.0F, (Math.PI / 2D));
      barArc(x1 + rad, y2 - rad, rad, (Math.PI / 2D), Math.PI);
      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(1.0F);
   }

   public static void barArc(float cx, float cy, float r, double startA, double endA) {
      for(int i = 0; i <= 14; ++i) {
         double a = startA + (endA - startA) * (double)i / (double)14.0F;
         GL11.glVertex2f(cx + r * (float)Math.cos(a), cy + r * (float)Math.sin(a));
      }

   }

   public static void drawBBFill(AxisAlignedBB BB, Color color, int alpha) {
      if (mc.getRenderViewEntity() != null) {
         AxisAlignedBB bb = new AxisAlignedBB(BB.minX - mc.getRenderManager().viewerPosX, BB.minY - mc.getRenderManager().viewerPosY, BB.minZ - mc.getRenderManager().viewerPosZ, BB.maxX - mc.getRenderManager().viewerPosX, BB.maxY - mc.getRenderManager().viewerPosY, BB.maxZ - mc.getRenderManager().viewerPosZ);
         camera.setPosition(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);
         if (camera.isBoundingBoxInFrustum(BB)) {
            GlStateManager.pushMatrix();

            try {
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.disableTexture2D();
               GlStateManager.disableDepth();
               GlStateManager.depthMask(false);
               GlStateManager.disableLighting();
               RenderGlobal.renderFilledBox(bb, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)alpha / 255.0F);
            } finally {
               GlStateManager.depthMask(true);
               GlStateManager.enableDepth();
               GlStateManager.enableTexture2D();
               GlStateManager.enableLighting();
               GlStateManager.disableBlend();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.popMatrix();
            }

         }
      }
   }

   public static void drawBBBox(AxisAlignedBB box, Color color, int alpha) {
      if (box != null) {
         AxisAlignedBB bb = Interpolation.interpolateAxis(box);
         float r = (float)color.getRed() / 255.0F;
         float g = (float)color.getGreen() / 255.0F;
         float b = (float)color.getBlue() / 255.0F;
         float a = (float)alpha / 255.0F;
         GlStateManager.pushMatrix();

         try {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GL11.glEnable(2848);
            GL11.glLineWidth((Float)BlockRender3D.getInstance().outlineWidth.getValue());
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
            buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
            Tessellator.getInstance().draw();
         } finally {
            GL11.glLineWidth(1.0F);
            GL11.glDisable(2848);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
         }

      }
   }

   public static void drawAirBlockBox(BlockPos pos, int box_color, int outline_color, boolean box, boolean outline, float outline_width) {
      float box_red = (float)(box_color >> 16 & 255) / 255.0F;
      float box_green = (float)(box_color >> 8 & 255) / 255.0F;
      float box_blue = (float)(box_color & 255) / 255.0F;
      float box_alpha = (float)(box_color >> 24 & 255) / 255.0F;
      float outline_red = (float)(outline_color >> 16 & 255) / 255.0F;
      float outline_green = (float)(outline_color >> 8 & 255) / 255.0F;
      float outline_blue = (float)(outline_color & 255) / 255.0F;
      float outline_alpha = (float)(outline_color >> 24 & 255) / 255.0F;
      double x = (double)pos.getX() - mc.getRenderManager().viewerPosX;
      double y = (double)pos.getY() - mc.getRenderManager().viewerPosY;
      double z = (double)pos.getZ() - mc.getRenderManager().viewerPosZ;
      AxisAlignedBB axisAlignedBB = new AxisAlignedBB((double)0.0F, (double)0.0F, (double)0.0F, (double)1.0F, (double)1.0F, (double)1.0F);
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.enableBlend();
      GlStateManager.disableDepth();
      GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
      GlStateManager.disableTexture2D();
      GlStateManager.depthMask(false);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(outline_width);
      if (outline) {
         RenderGlobal.drawSelectionBoundingBox(axisAlignedBB, outline_red, outline_green, outline_blue, outline_alpha);
      }

      if (box) {
         RenderGlobal.renderFilledBox(axisAlignedBB, box_red, box_green, box_blue, box_alpha);
      }

      GL11.glDisable(2848);
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.translate(-x, -y, -z);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }

   public static void drawBlockOutline(AxisAlignedBB bb, Color color) {
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      double x0 = bb.minX;
      double y0 = bb.minY;
      double z0 = bb.minZ;
      double x1 = bb.maxX;
      double y1 = bb.maxY;
      double z1 = bb.maxZ;
      GL11.glBegin(1);
      GL11.glColor4f(r, g, b, 1.0F);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glEnd();
   }

   public static void drawBoxESP(BlockPos pos, Color color, float lineWidth, boolean outline, boolean box, int boxAlpha) {
      AxisAlignedBB bb = new AxisAlignedBB((double)pos.getX() - mc.getRenderManager().viewerPosX, (double)pos.getY() - mc.getRenderManager().viewerPosY, (double)pos.getZ() - mc.getRenderManager().viewerPosZ, (double)(pos.getX() + 1) - mc.getRenderManager().viewerPosX, (double)(pos.getY() + 1) - mc.getRenderManager().viewerPosY, (double)(pos.getZ() + 1) - mc.getRenderManager().viewerPosZ);
      camera.setPosition(((Entity)Objects.requireNonNull(mc.getRenderViewEntity())).posX, mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);
      if (camera.isBoundingBoxInFrustum(new AxisAlignedBB(bb.minX + mc.getRenderManager().viewerPosX, bb.minY + mc.getRenderManager().viewerPosY, bb.minZ + mc.getRenderManager().viewerPosZ, bb.maxX + mc.getRenderManager().viewerPosX, bb.maxY + mc.getRenderManager().viewerPosY, bb.maxZ + mc.getRenderManager().viewerPosZ))) {
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.disableDepth();
         GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
         GlStateManager.disableTexture2D();
         GlStateManager.depthMask(false);
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth(lineWidth);
         double dist = mc.player.getDistance((double)((float)pos.getX() + 0.5F), (double)((float)pos.getY() + 0.5F), (double)((float)pos.getZ() + 0.5F)) * (double)0.75F;
         if (box) {
            RenderGlobal.renderFilledBox(bb, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)boxAlpha / 255.0F);
         }

         if (outline) {
            RenderGlobal.drawBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
         }

         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.popMatrix();
      }

   }

   public static void glBillboard(float x, float y, float z) {
      float scale = 0.02666667F;
      GlStateManager.translate((double)x - mc.getRenderManager().renderPosX, (double)y - mc.getRenderManager().renderPosY, (double)z - mc.getRenderManager().renderPosZ);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-mc.player.rotationYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(mc.player.rotationPitch, mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-scale, -scale, scale);
   }

   public static void glBillboardDistanceScaled(float x, float y, float z, EntityPlayer player, float scale) {
      glBillboard(x, y, z);
      int distance = (int)player.getDistance((double)x, (double)y, (double)z);
      float scaleDistance = (float)distance / 2.0F / (2.0F + (2.0F - scale));
      if (scaleDistance < 1.0F) {
         scaleDistance = 1.0F;
      }

      GlStateManager.scale(scaleDistance, scaleDistance, scaleDistance);
   }

   public static void setColor(Color color) {
      GL11.glColor4d((double)color.getRed() / (double)255.0F, (double)color.getGreen() / (double)255.0F, (double)color.getBlue() / (double)255.0F, (double)color.getAlpha() / (double)255.0F);
   }

   public static void prepare() {
      GlStateManager.pushMatrix();
      GlStateManager.disableDepth();
      GlStateManager.disableLighting();
      GlStateManager.depthMask(false);
      GlStateManager.disableAlpha();
      GlStateManager.disableCull();
      GlStateManager.enableBlend();
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glBlendFunc(770, 771);
   }

   public static void release() {
      GlStateManager.depthMask(true);
      GlStateManager.enableLighting();
      GlStateManager.enableDepth();
      GlStateManager.enableAlpha();
      GlStateManager.popMatrix();
      GL11.glEnable(3553);
      GL11.glPolygonMode(1032, 6914);
   }

   public static void renderBB(int glMode, AxisAlignedBB bb, Color bottom, Color top) {
      GL11.glShadeModel(7425);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      builder = tessellator.getBuffer();
      builder.begin(glMode, DefaultVertexFormats.POSITION_COLOR);
      buildBBBuffer(builder, bb, bottom, top);
      tessellator.draw();
      GL11.glShadeModel(7424);
   }

   public static void buildBBBuffer(BufferBuilder builder, AxisAlignedBB bb, Color bottom, Color top) {
      addBuilderVertex(builder, bb.minX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.minX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.maxZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.minX, bb.minY, bb.minZ, bottom);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.minZ, top);
      addBuilderVertex(builder, bb.maxX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.maxZ, top);
      addBuilderVertex(builder, bb.minX, bb.maxY, bb.minZ, top);
   }

   public static void addBuilderVertex(BufferBuilder bufferBuilder, double x, double y, double z, Color color) {
      bufferBuilder.pos(x, y, z).color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F).endVertex();
   }

   static {
      itemRender = mc.getItemRenderer().itemRenderer;
      camera = new Frustum();
      tessellator = Tessellator.getInstance();
      builder = tessellator.getBuffer();
      itemRender = mc.getRenderItem();
      camera = new Frustum();
      animValues = new ConcurrentHashMap();
      animPrevTarget = new ConcurrentHashMap();
      animVelocity = new ConcurrentHashMap();
   }
}
