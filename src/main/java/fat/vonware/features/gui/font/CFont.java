package fat.vonware.features.gui.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class CFont {
   private final float imgSize = 1024.0F;
   private final int supersampleFactor = 2;
   protected CharData[] charData = new CharData[256];
   protected Font font;
   protected boolean antiAlias;
   protected boolean fractionalMetrics;
   protected int fontHeight = -1;
   protected int charOffset = 0;
   protected DynamicTexture tex;
   protected final FontQuality quality;

   public CFont(Font font, boolean antiAlias, boolean fractionalMetrics, FontQuality quality) {
      this.font = font;
      this.antiAlias = antiAlias;
      this.fractionalMetrics = fractionalMetrics;
      this.quality = quality;
      this.tex = this.setupTexture(font, antiAlias, fractionalMetrics, this.charData);
   }

   protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      return this.quality == CFont.FontQuality.LOW ? this.setupTextureLow(font, antiAlias, fractionalMetrics, chars) : this.setupTextureHigh(font, antiAlias, fractionalMetrics, chars);
   }

   protected DynamicTexture setupTextureLow(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      BufferedImage img = this.generateLowQualityFontImage(font, antiAlias, fractionalMetrics, chars);

      try {
         return new DynamicTexture(img);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   protected DynamicTexture setupTextureHigh(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      BufferedImage img = this.generateHighQualityFontImage(font, antiAlias, fractionalMetrics, chars);

      try {
         DynamicTexture texture = new DynamicTexture(img);
         GL11.glBindTexture(3553, texture.getGlTextureId());
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAniso = GL11.glGetFloat(34047);
            if (maxAniso > 1.0F) {
               GL11.glTexParameterf(3553, 34046, Math.min(4.0F, maxAniso));
            }
         }

         return texture;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      return this.quality == CFont.FontQuality.LOW ? this.generateLowQualityFontImage(font, antiAlias, fractionalMetrics, chars) : this.generateHighQualityFontImage(font, antiAlias, fractionalMetrics, chars);
   }

   private BufferedImage generateLowQualityFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      int imgSize = 512;
      BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, 2);
      Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
      g.setFont(font);
      g.setColor(new Color(255, 255, 255, 0));
      g.fillRect(0, 0, imgSize, imgSize);
      g.setColor(Color.WHITE);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
      FontMetrics fontMetrics = g.getFontMetrics();
      int charHeight = 0;
      int positionX = 0;
      int positionY = 1;

      for(int i = 0; i < chars.length; ++i) {
         char ch = (char)i;
         CharData charData = new CharData();
         Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
         charData.width = dimensions.getBounds().width + 8;
         charData.height = dimensions.getBounds().height;
         if (positionX + charData.width >= imgSize) {
            positionX = 0;
            positionY += charHeight;
            charHeight = 0;
         }

         if (charData.height > charHeight) {
            charHeight = charData.height;
         }

         charData.storedX = positionX;
         charData.storedY = positionY;
         charData.supersampledWidth = charData.width;
         charData.supersampledHeight = charData.height;
         if (charData.height > this.fontHeight) {
            this.fontHeight = charData.height;
         }

         chars[i] = charData;
         g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
         positionX += charData.width;
      }

      g.dispose();
      return bufferedImage;
   }

   private BufferedImage generateHighQualityFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
      this.getClass();
      int imgSize = 1024;
      BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, 2);
      Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
      Font supersampledFont = font.deriveFont(font.getSize2D() * 2.0F);
      g.setFont(supersampledFont);
      g.setColor(new Color(255, 255, 255, 0));
      g.fillRect(0, 0, imgSize, imgSize);
      g.setColor(Color.WHITE);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      if (antiAlias) {
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
      }

      FontMetrics fontMetrics = g.getFontMetrics();
      Font regularFont = font.deriveFont(font.getSize2D());
      Graphics2D tempG = (Graphics2D)(new BufferedImage(1, 1, 2)).getGraphics();
      tempG.setFont(regularFont);
      FontMetrics regularMetrics = tempG.getFontMetrics();
      int charHeight = 0;
      int positionX = 0;
      int positionY = 1;

      for(int i = 0; i < chars.length; ++i) {
         char ch = (char)i;
         CharData charData = new CharData();
         Rectangle2D regularDimensions = regularMetrics.getStringBounds(String.valueOf(ch), tempG);
         Rectangle2D supersampledDimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
         int supersampledWidth = supersampledDimensions.getBounds().width + 16;
         int supersampledHeight = supersampledDimensions.getBounds().height;
         if (positionX + supersampledWidth >= imgSize) {
            positionX = 0;
            positionY += charHeight;
            charHeight = 0;
         }

         if (supersampledHeight > charHeight) {
            charHeight = supersampledHeight;
         }

         charData.storedX = positionX;
         charData.storedY = positionY;
         charData.supersampledWidth = supersampledWidth;
         charData.supersampledHeight = supersampledHeight;
         charData.width = regularDimensions.getBounds().width + 8;
         charData.height = supersampledHeight;
         if (supersampledHeight > this.fontHeight) {
            this.fontHeight = supersampledHeight;
         }

         chars[i] = charData;
         g.drawString(String.valueOf(ch), positionX + 4, positionY + fontMetrics.getAscent());
         positionX += supersampledWidth;
      }

      tempG.dispose();
      g.dispose();
      return bufferedImage;
   }

   public void drawChar(CharData[] chars, char c, float x, float y) throws ArrayIndexOutOfBoundsException {
      try {
         this.drawQuad(x, y, (float)chars[c].supersampledWidth, (float)chars[c].supersampledHeight, (float)chars[c].storedX, (float)chars[c].storedY, (float)chars[c].supersampledWidth, (float)chars[c].supersampledHeight);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight) {
      float var10000;
      if (this.quality == CFont.FontQuality.LOW) {
         var10000 = 512.0F;
      } else {
         this.getClass();
         var10000 = 1024.0F;
      }

      float imgSize = var10000;
      float renderSRCX = srcX / imgSize;
      float renderSRCY = srcY / imgSize;
      float renderSRCWidth = srcWidth / imgSize;
      float renderSRCHeight = srcHeight / imgSize;
      if (this.quality == CFont.FontQuality.HIGH) {
         width /= 2.0F;
         height /= 2.0F;
      }

      GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
      GL11.glVertex2d((double)(x + width), (double)y);
      GL11.glTexCoord2f(renderSRCX, renderSRCY);
      GL11.glVertex2d((double)x, (double)y);
      GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
      GL11.glVertex2d((double)x, (double)(y + height));
      GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
      GL11.glVertex2d((double)x, (double)(y + height));
      GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
      GL11.glVertex2d((double)(x + width), (double)(y + height));
      GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
      GL11.glVertex2d((double)(x + width), (double)y);
   }

   public int getStringHeight(String text) {
      return this.getHeight();
   }

   public int getHeight() {
      return this.quality == CFont.FontQuality.LOW ? (this.fontHeight - 8) / 2 : (this.fontHeight - 16) / 4;
   }

   public int getStringWidth(String text) {
      int width = 0;

      for(char c : text.toCharArray()) {
         if (c < this.charData.length && c >= 0) {
            width += this.charData[c].width - 8 + this.charOffset;
         }
      }

      return width / 2;
   }

   public boolean isAntiAlias() {
      return this.antiAlias;
   }

   public void setAntiAlias(boolean antiAlias) {
      if (this.antiAlias != antiAlias) {
         this.antiAlias = antiAlias;
         this.tex = this.setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
      }

   }

   public boolean isFractionalMetrics() {
      return this.fractionalMetrics;
   }

   public void setFractionalMetrics(boolean fractionalMetrics) {
      if (this.fractionalMetrics != fractionalMetrics) {
         this.fractionalMetrics = fractionalMetrics;
         this.tex = this.setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
      }

   }

   public Font getFont() {
      return this.font;
   }

   public void setFont(Font font) {
      this.font = font;
      this.tex = this.setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
   }

   public static enum FontQuality {
      LOW,
      HIGH;
   }

   protected static class CharData {
      public int width;
      public int height;
      public int storedX;
      public int storedY;
      public int supersampledWidth;
      public int supersampledHeight;
   }
}
