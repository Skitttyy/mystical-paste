package fat.vonware.manager;

import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.gui.font.CFont;
import fat.vonware.features.gui.font.CustomFont;
import fat.vonware.features.modules.client.Fonts;
import java.awt.Font;
import net.minecraft.util.math.MathHelper;

public class TextManager extends Feature {
   public int scaledWidth;
   public int scaledHeight;
   public int scaleFactor;
   private CustomFont customFont;

   public TextManager() {
      this.customFont = new CustomFont(new Font("Verdana", 0, 18), true, true, CFont.FontQuality.HIGH);
      this.updateResolution();
   }

   public void init(boolean startup) {
      Fonts cFont = (Fonts)Vonware.moduleManager.getModuleByClass(Fonts.class);

      try {
         this.setFontRenderer(new Font(cFont.fontName.getValue(), ((Fonts.FontStyle)cFont.fontStyle.getValue()).getAwtValue(), (Integer)cFont.fontSize.getValue()), (Boolean)cFont.antiAlias.getValue(), (Boolean)cFont.fractionalMetrics.getValue(), cFont.fontQuality.getValue());
      } catch (Exception var4) {
      }

   }

   public void drawStringWithShadow(String text, float x, float y, int color) {
      this.drawString(text, x, y, color, true);
   }

   public int getFontHeight2() {
      return Fonts.getInstance().isOn() ? this.customFont.getStringHeight("A") + 3 : mc.fontRenderer.FONT_HEIGHT;
   }

   public String capitalSpace(String str) {
      str = str.replace("A", " A");
      str = str.replace("B", " B");
      str = str.replace("C", " C");
      str = str.replace("D", " D");
      str = str.replace("E", " E");
      str = str.replace("F", " F");
      str = str.replace("G", " G");
      str = str.replace("H", " H");
      str = str.replace("I", " I");
      str = str.replace("J", " J");
      str = str.replace("K", " K");
      str = str.replace("L", " L");
      str = str.replace("M", " M");
      str = str.replace("N", " N");
      str = str.replace("O", " O");
      str = str.replace("P", " P");
      str = str.replace("Q", " Q");
      str = str.replace("R", " R");
      str = str.replace("S", " S");
      str = str.replace("T", " T");
      str = str.replace("U", " U");
      str = str.replace("V", " V");
      str = str.replace("W", " W");
      str = str.replace("X", " X");
      str = str.replace("Y", " Y");
      str = str.replace("Z", " Z");
      str = str.replace("T P", "TP");
      str = str.replace("T N T", "TNT");
      str = str.replace("D M G", "DMG");
      str = str.replace("H U D", "HUD");
      str = str.replace("E S P", "ESP");
      str = str.replace("F P S", "FPS");
      str = str.replace("M C F", "MCF");
      if ((str = str.replace("2 D", "2D")).startsWith(" ")) {
         str = str.replaceFirst(" ", "");
      }

      return str;
   }

   public CustomFont getCustomFont() {
      return this.customFont;
   }

   public void drawString(String text, float x, float y, int color, boolean shadow) {
      if (Vonware.moduleManager.isModuleEnabled(Fonts.getInstance().getName())) {
         if ((Boolean)Fonts.getInstance().fontShadow.getValue()) {
            this.customFont.drawStringWithShadow(text, (double)x, (double)y, color);
         } else {
            this.customFont.drawString(text, x, y, color);
         }

      } else {
         mc.fontRenderer.drawString(text, x, y, color, shadow);
      }
   }

   public int getStringWidth(String text) {
      return Vonware.moduleManager.isModuleEnabled(Fonts.getInstance().getName()) ? this.customFont.getStringWidth(text) : mc.fontRenderer.getStringWidth(text);
   }

   public int getFontHeight() {
      if (Vonware.moduleManager.isModuleEnabled(Fonts.getInstance().getName())) {
         String text = "A";
         return this.customFont.getStringHeight(text);
      } else {
         return mc.fontRenderer.FONT_HEIGHT;
      }
   }

   public void setFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics, CFont.FontQuality quality) {
      this.customFont = new CustomFont(font, antiAlias, fractionalMetrics, quality);
   }

   public Font getCurrentFont() {
      return this.customFont.getFont();
   }

   public void updateResolution() {
      this.scaledWidth = mc.displayWidth;
      this.scaledHeight = mc.displayHeight;
      this.scaleFactor = 1;
      boolean flag = mc.isUnicode();
      int i = mc.gameSettings.guiScale;
      if (i == 0) {
         i = 1000;
      }

      while(this.scaleFactor < i && this.scaledWidth / (this.scaleFactor + 1) >= 320 && this.scaledHeight / (this.scaleFactor + 1) >= 240) {
         ++this.scaleFactor;
      }

      if (flag && this.scaleFactor % 2 != 0 && this.scaleFactor != 1) {
         --this.scaleFactor;
      }

      double scaledWidthD = (double)(this.scaledWidth / this.scaleFactor);
      double scaledHeightD = (double)(this.scaledHeight / this.scaleFactor);
      this.scaledWidth = MathHelper.ceil(scaledWidthD);
      this.scaledHeight = MathHelper.ceil(scaledHeightD);
   }
}
