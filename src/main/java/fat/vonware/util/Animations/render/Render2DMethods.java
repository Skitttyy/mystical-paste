package fat.vonware.util.Animations.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.manager.BlurManager;
import fat.vonware.util.Util;
import java.awt.Color;
import java.awt.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class Render2DMethods implements Util {
   private static final Tessellator tessellator = Tessellator.getInstance();
   private static final int HUD_CORNER_SEGMENTS = 32;

   public static void drawNameTagRect(float left, float top, float right, float bottom, int color, int border, float width) {
      quickDrawRect(left, top, right, bottom, color, false);
      GL11.glLineWidth(width);
      quickDrawRect(left, top, right, bottom, border, true);
   }

   public static void drawOutline(float x, float y, float width, float height, float lineWidth, int color) {
      drawRect(x + lineWidth, y, x - lineWidth, y + lineWidth, color);
      drawRect(x + lineWidth, y, width - lineWidth, y + lineWidth, color);
      drawRect(x, y, x + lineWidth, height, color);
      drawRect(width - lineWidth, y, width, height, color);
      drawRect(x + lineWidth, height - lineWidth, width - lineWidth, height, color);
   }

   public static void quickDrawRect(float x, float y, float x2, float y2, int color, boolean line) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      bufferbuilder.begin(line ? 2 : 7, DefaultVertexFormats.POSITION_COLOR);
      bufferbuilder.pos((double)x, (double)y2, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x2, (double)y2, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x2, (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x, (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      tessellator.draw();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawGradientRect(float left, float top, float right, float bottom, boolean sideways, int topColor, int bottomColor) {
      float alpha = (float)(topColor >> 24 & 255) / 255.0F;
      float red = (float)(topColor >> 16 & 255) / 255.0F;
      float green = (float)(topColor >> 8 & 255) / 255.0F;
      float blue = (float)(topColor & 255) / 255.0F;
      float alpha2 = (float)(bottomColor >> 24 & 255) / 255.0F;
      float red2 = (float)(bottomColor >> 16 & 255) / 255.0F;
      float green2 = (float)(bottomColor >> 8 & 255) / 255.0F;
      float blue2 = (float)(bottomColor & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.shadeModel(7425);
      BufferBuilder builder = tessellator.getBuffer();
      builder.begin(7, DefaultVertexFormats.POSITION_COLOR);
      if (sideways) {
         builder.pos((double)left, (double)top, (double)0.0F).color(red, green, blue, alpha).endVertex();
         builder.pos((double)left, (double)bottom, (double)0.0F).color(red, green, blue, alpha).endVertex();
         builder.pos((double)right, (double)bottom, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         builder.pos((double)right, (double)top, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      } else {
         builder.pos((double)right, (double)top, (double)0.0F).color(red, green, blue, alpha).endVertex();
         builder.pos((double)left, (double)top, (double)0.0F).color(red, green, blue, alpha).endVertex();
         builder.pos((double)left, (double)bottom, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
         builder.pos((double)right, (double)bottom, (double)0.0F).color(red2, green2, blue2, alpha2).endVertex();
      }

      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawRect(Rectangle rectangle, int color) {
      drawRect((float)rectangle.x, (float)rectangle.y, (float)(rectangle.x + rectangle.width), (float)(rectangle.y + rectangle.height), color);
   }

   public static void drawRect(float x, float y, float x1, float y1, int color, float lineWidth) {
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GL11.glLineWidth(lineWidth);
      BufferBuilder builder = tessellator.getBuffer();
      builder.begin(7, DefaultVertexFormats.POSITION_COLOR);
      builder.pos((double)x, (double)y1, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x1, (double)y1, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x1, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
      tessellator.draw();
      GL11.glLineWidth(1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawRect(float x, float y, float x1, float y1, int color) {
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      BufferBuilder builder = tessellator.getBuffer();
      builder.begin(7, DefaultVertexFormats.POSITION_COLOR);
      builder.pos((double)x, (double)y1, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x1, (double)y1, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x1, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
      builder.pos((double)x, (double)y, (double)0.0F).color(red, green, blue, alpha).endVertex();
      tessellator.draw();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawRect(float x, float y, float x1, float y1) {
      BufferBuilder builder = tessellator.getBuffer();
      builder.begin(7, DefaultVertexFormats.POSITION);
      builder.pos((double)x, (double)y1, (double)0.0F).endVertex();
      builder.pos((double)x1, (double)y1, (double)0.0F).endVertex();
      builder.pos((double)x1, (double)y, (double)0.0F).endVertex();
      builder.pos((double)x, (double)y, (double)0.0F).endVertex();
      tessellator.draw();
   }

   public static void drawBorderedRect(float x, float y, float x1, float y1, int insideC, int borderC) {
      ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
      float line = 1.0F / (float)resolution.getScaleFactor();
      float left = Math.min(x, x1);
      float right = Math.max(x, x1);
      float top = Math.min(y, y1);
      float bottom = Math.max(y, y1);
      if (right - left > line * 2.0F && bottom - top > line * 2.0F) {
         drawRect(left + line, top + line, right - line, bottom - line, insideC);
      }

      drawRect(left, top, right, top + line, borderC);
      drawRect(left, bottom - line, right, bottom, borderC);
      drawRect(left, top + line, left + line, bottom - line, borderC);
      drawRect(right - line, top + line, right, bottom - line, borderC);
   }

   public static void disableGL2D(boolean ignored) {
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glDisable(2848);
      GL11.glHint(3154, 4352);
      GL11.glHint(3155, 4352);
   }

   public static void enableGL2D(boolean ignored) {
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(true);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glHint(3155, 4354);
   }

   public static void drawRect(float x, float y, float x1, float y1, int color, int ignored) {
      enableGL2D(false);
      glColor(color);
      GL11.glBegin(7);
      GL11.glVertex2f(x, y1);
      GL11.glVertex2f(x1, y1);
      GL11.glVertex2f(x1, y);
      GL11.glVertex2f(x, y);
      GL11.glEnd();
      disableGL2D(false);
   }

   public static void enableGL2D() {
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
   }

   public static void disableGL2D() {
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   public static void drawHLine(float x, float y, float x1, int y1) {
      if (y < x) {
         float var5 = x;
         x = y;
         y = var5;
      }

      drawRect(x, x1, y + 1.0F, x1 + 1.0F, y1);
   }

   public static void drawVLine(float x, float y, float x1, int y1) {
      if (x1 < y) {
         float var5 = y;
         y = x1;
         x1 = var5;
      }

      drawRect(x, y + 1.0F, x + 1.0F, x1, y1);
   }

   public static void glColor(int hex) {
      GL11.glColor4f((float)(hex >> 24 & 255) / 255.0F, (float)(hex >> 8 & 255) / 255.0F, (float)(hex & 255) / 255.0F, (float)(hex >> 24 & 255) / 255.0F);
   }

   public static void addCornerArc(float cx, float cy, float r, float startAngle, float endAngle, int segments) {
      float step = (endAngle - startAngle) / (float)segments;

      for(int i = 0; i <= segments; ++i) {
         double rad = Math.toRadians((double)(startAngle + step * (float)i));
         GL11.glVertex2f(cx + (float)Math.cos(rad) * r, cy + (float)Math.sin(rad) * r);
      }

   }

   public static void drawRoundedRectFill(float x, float y, float width, float height, float radius, int color) {
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float cx = x + width / 2.0F;
      float cy = y + height / 2.0F;
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.color(red, green, blue, alpha);
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);
      addCornerArc(x + r, y + r, r, 180.0F, 270.0F, 32);
      addCornerArc(x + width - r, y + r, r, 270.0F, 360.0F, 32);
      addCornerArc(x + width - r, y + height - r, r, 0.0F, 90.0F, 32);
      addCornerArc(x + r, y + height - r, r, 90.0F, 180.0F, 32);
      double closeRad = Math.toRadians((double)180.0F);
      GL11.glVertex2f(x + r + (float)Math.cos(closeRad) * r, y + r + (float)Math.sin(closeRad) * r);
      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawRoundedOutline(float x, float y, float width, float height, float radius, int color, float lineWidth) {
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float alpha = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.color(red, green, blue, alpha);
      GL11.glLineWidth(lineWidth);
      GL11.glBegin(2);
      addCornerArc(x + r, y + r, r, 180.0F, 270.0F, 32);
      addCornerArc(x + width - r, y + r, r, 270.0F, 360.0F, 32);
      addCornerArc(x + width - r, y + height - r, r, 0.0F, 90.0F, 32);
      addCornerArc(x + r, y + height - r, r, 90.0F, 180.0F, 32);
      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawItemIcon(ItemStack stack, int x, int y, int size) {
      Minecraft mc = Minecraft.getMinecraft();
      float scale = (float)size / 16.0F;
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)x, (float)y, 0.0F);
      GlStateManager.scale(scale, scale, 1.0F);
      RenderHelper.enableGUIStandardItemLighting();
      mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.popMatrix();
   }

   public static void drawDescriptionPillSurface(float x1, float y1, float x2, float y2, float radius, float alpha) {
      float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
      Color color = Global.getGlobalColorRainbowS();
      RenderUtil.drawBottomBarFill(x1, y1, x2, y2, radius, color.getRed(), color.getGreen(), color.getBlue(), Math.round(50.0F * clampedAlpha));
      RenderUtil.drawBottomBarOutline(x1, y1, x2, y2, radius, color.getRed(), color.getGreen(), color.getBlue(), Math.round(255.0F * clampedAlpha));
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawPillBlur(float x1, float y1, float x2, float y2, float radius, float blurStrength) {
      if (BlurManager.INSTANCE != null) {
         float width = x2 - x1;
         float height = y2 - y1;
         if (!(width <= 0.0F) && !(height <= 0.0F)) {
            float usedRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
            BlurManager.INSTANCE.drawRoundedBackground(x1, y1, width, height, usedRadius, Math.max(0.0F, blurStrength), -1);
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static void drawHudPillShadow(float x1, float y1, float x2, float y2, float radius) {
      RenderUtil.drawBottomBarFill(x1 - 1.0F, y1 - 1.0F, x2 + 1.0F, y2 + 1.0F, radius + 1.0F, 0, 0, 0, 58);
      RenderUtil.drawBottomBarFill(x1, y1, x2, y2, radius, 0, 0, 0, 105);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawHudPill(String text, ItemStack icon, int hotbarGap, float radius, float outlineThickness, int bgColor, int outlineColor, int textColor) {
      drawHudPill(text, icon, "", hotbarGap, radius, outlineThickness, bgColor, outlineColor, textColor);
   }

   public static void drawHudPill(String text, ItemStack icon, String suffix, int hotbarGap, float radius, float outlineThickness, int bgColor, int outlineColor, int textColor) {
      Minecraft mc = Minecraft.getMinecraft();
      ScaledResolution res = new ScaledResolution(mc);
      String safeText = text == null ? "" : text;
      String safeSuffix = suffix == null ? "" : suffix;
      int fontHeight = Vonware.textManager.getFontHeight();
      int textWidth = Vonware.textManager.getStringWidth(safeText);
      int suffixWidth = Vonware.textManager.getStringWidth(safeSuffix);
      int padding = 12;
      int iconGap = icon != null && !icon.isEmpty() ? 3 : 0;
      int suffixGap = safeSuffix.isEmpty() ? 0 : 2;
      int iconSize = icon != null && !icon.isEmpty() ? fontHeight + 4 : 0;
      float height = 20.0F;
      float width = (float)(textWidth + iconGap + iconSize + suffixGap + suffixWidth) + 24.0F;
      float x = ((float)res.getScaledWidth() - width) * 0.5F;
      float y = (float)(res.getScaledHeight() - hotbarGap) - 20.0F;
      float usedRadius = Math.min(radius, 10.0F);
      drawHudPillShadow(x, y, x + width, y + 20.0F, usedRadius);
      drawDescriptionPillSurface(x, y, x + width, y + 20.0F, usedRadius, 1.0F);
      float textY = y + (20.0F - (float)fontHeight) * 0.5F - 0.5F;
      float contentWidth = (float)(textWidth + iconGap + iconSize + suffixGap + suffixWidth);
      float cursorX = x + (width - contentWidth) * 0.5F;
      Vonware.textManager.drawStringWithShadow(safeText, cursorX, textY, -1);
      cursorX += (float)textWidth;
      if (iconSize > 0) {
         cursorX += (float)iconGap;
         drawItemIcon(icon, Math.round(cursorX), Math.round(y + (20.0F - (float)iconSize) * 0.5F), iconSize);
         cursorX += (float)iconSize;
      }

      if (!safeSuffix.isEmpty()) {
         cursorX += (float)suffixGap;
         Vonware.textManager.drawStringWithShadow(safeSuffix, cursorX, textY, -1);
      }

   }
}
