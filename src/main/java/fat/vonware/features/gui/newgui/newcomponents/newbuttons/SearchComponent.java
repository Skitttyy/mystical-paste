package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class SearchComponent {
   public static String activeQuery = "";
   public static boolean searchActive = false;
   public static float highlightAlpha = 0.0F;
   private float slideProgress = 0.0F;
   private static final float SLIDE_OFFSET = 20.0F;
   private static final float SLIDE_SPEED = 0.2F;
   private static final float HIGHLIGHT_SPEED = 0.13F;
   private int heldKey = 0;
   private char heldChar = 0;
   private long keyHeldSince = 0L;
   private long lastRepeatTime = 0L;
   private static final long INITIAL_DELAY = 400L;
   private static final long REPEAT_INTERVAL = 45L;
   private final StringBuilder query = new StringBuilder();
   private long cursorBlinkStart = 0L;
   private static final long BLINK_MS = 530L;
   private static final float BAR_W = 230.0F;
   private static final float BAR_H = 22.0F;
   private static final float RADIUS = 11.0F;
   private static final float PAD_LEFT = 28.0F;
   private static final float PAD_RIGHT = 8.0F;
   private static final int ARC_SEGS = 16;
   private static final float MARGIN_ABOVE_CLIENT_BAR = 6.0F;
   private static final float MARGIN_NO_CLIENT_BAR = 10.0F;
   private static final float CLIENT_BAR_H = 40.0F;
   private static final Minecraft mc = Minecraft.getMinecraft();

   public void open() {
      searchActive = true;
      this.cursorBlinkStart = System.currentTimeMillis();
   }

   public void close() {
      searchActive = false;
      activeQuery = "";
      this.query.setLength(0);
      this.heldKey = 0;
      this.heldChar = 0;
   }

   public void toggle() {
      if (searchActive) {
         this.close();
      } else {
         this.open();
      }

   }

   public boolean isOpen() {
      return searchActive;
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (searchActive) {
         this.applyKey(typedChar, keyCode);
         if (keyCode != 1 && keyCode != 28) {
            this.heldKey = keyCode;
            this.heldChar = typedChar;
            this.keyHeldSince = System.currentTimeMillis();
            this.lastRepeatTime = this.keyHeldSince;
         } else {
            this.heldKey = 0;
         }

      }
   }

   private void applyKey(char typedChar, int keyCode) {
      if (keyCode != 1 && keyCode != 28) {
         if (keyCode == 14) {
            if (this.query.length() > 0) {
               this.query.deleteCharAt(this.query.length() - 1);
               activeQuery = this.query.toString().toLowerCase();
            }

         } else {
            if (typedChar >= ' ' && typedChar < 127 && this.query.length() < 40) {
               this.query.append(typedChar);
               activeQuery = this.query.toString().toLowerCase();
            }

         }
      } else {
         this.close();
      }
   }

   private void tick() {
      if (searchActive && this.heldKey != 0) {
         if (Keyboard.isKeyDown(this.heldKey)) {
            long now = System.currentTimeMillis();
            if (now - this.keyHeldSince > 400L && now - this.lastRepeatTime > 45L) {
               this.lastRepeatTime = now;
               this.applyKey(this.heldChar, this.heldKey);
            }
         } else {
            this.heldKey = 0;
            this.heldChar = 0;
         }

      }
   }

   public static boolean isMatch(String moduleName) {
      return searchActive && !activeQuery.isEmpty() ? moduleName.toLowerCase().contains(activeQuery) : true;
   }

   public void draw(float scaledW, float scaledH, int matchCount, long frameTimeMs) {
      this.tick();
      float slideTarget = searchActive ? 1.0F : 0.0F;
      this.slideProgress += (slideTarget - this.slideProgress) * 0.2F;
      if (this.slideProgress < 0.004F) {
         this.slideProgress = 0.0F;
      }

      float hlTarget = searchActive && !activeQuery.isEmpty() ? 1.0F : 0.0F;
      highlightAlpha += (hlTarget - highlightAlpha) * 0.13F;
      if (highlightAlpha < 0.004F) {
         highlightAlpha = 0.0F;
      }

      if (highlightAlpha > 0.996F) {
         highlightAlpha = 1.0F;
      }

      if (!(this.slideProgress <= 0.0F)) {
         boolean hasBar = (Boolean)ClickGui.getInstance().clientInfo.getValue();
         float restingY = hasBar ? scaledH - 40.0F - 6.0F - 22.0F : scaledH - 10.0F - 22.0F;
         float by1 = restingY + 20.0F * (1.0F - this.slideProgress);
         float by2 = by1 + 22.0F;
         float cx = scaledW / 2.0F;
         float bx1 = cx - 115.0F;
         float bx2 = cx + 115.0F;
         float alpha = this.slideProgress;
         Color theme = Global.getGlobalColorRainbowS();
         int tR = theme.getRed();
         int tG = theme.getGreen();
         int tB = theme.getBlue();
         drawPillFill(bx1, by1, bx2, by2, 11.0F, 10, 10, 10, (int)(210.0F * alpha));
         drawPillFill(bx1, by1, bx2, by2, 11.0F, tR, tG, tB, (int)(45.0F * alpha));
         drawPillOutline(bx1, by1, bx2, by2, 11.0F, tR, tG, tB, (int)(200.0F * alpha), 1.2F);
         drawSearchIcon(bx1 + 10.0F, by1 + 11.0F, 4.5F, tR, tG, tB, alpha);
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         float textY = by1 + (22.0F - (float)Vonware.textManager.getFontHeight()) / 2.0F + 0.5F;
         float textX = bx1 + 28.0F;
         float maxTextW = 194.0F - (this.query.length() > 0 ? 32.0F : 0.0F);
         if (this.query.length() == 0) {
            if (searchActive) {
               Vonware.textManager.getCustomFont().drawStringWithShadow("search modules...", (double)textX, (double)textY, argb((int)(65.0F * alpha), 255, 255, 255));
            }
         } else {
            Vonware.textManager.getCustomFont().drawStringWithShadow(clipToWidth(this.query.toString(), maxTextW), (double)textX, (double)textY, argb((int)(255.0F * alpha), 238, 238, 238));
         }

         if (searchActive && alpha > 0.25F) {
            long elapsed = (frameTimeMs - this.cursorBlinkStart) % 530L;
            boolean cursorOn = elapsed < 265L;
            if (cursorOn) {
               String vis = clipToWidth(this.query.toString(), maxTextW);
               float cursorX = textX + (float)Vonware.textManager.getCustomFont().getStringWidth(vis) + 1.0F;
               drawLine(cursorX, by1 + 4.0F, cursorX, by2 - 4.0F, tR, tG, tB, (int)(200.0F * alpha), 1.0F);
               GlStateManager.enableTexture2D();
               GlStateManager.disableBlend();
               GlStateManager.enableAlpha();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }

         if (this.query.length() > 0 && alpha > 0.15F) {
            String cs = String.valueOf(matchCount);
            float cW = (float)Vonware.textManager.getCustomFont().getStringWidth(cs) + 8.0F;
            float cH = 16.0F;
            float cbx1 = bx2 - 8.0F - cW;
            float cbx2 = cbx1 + cW;
            float cby1 = by1 + (22.0F - cH) / 2.0F;
            float cby2 = cby1 + cH;
            drawPillFill(cbx1, cby1, cbx2, cby2, cH / 2.0F, tR, tG, tB, (int)(55.0F * alpha));
            drawPillOutline(cbx1, cby1, cbx2, cby2, cH / 2.0F, tR, tG, tB, (int)(140.0F * alpha), 0.8F);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            float ctY = cby1 + (cH - (float)Vonware.textManager.getFontHeight()) / 2.0F + 0.5F;
            Vonware.textManager.getCustomFont().drawStringWithShadow(cs, (double)(cbx1 + 4.0F), (double)ctY, argb((int)(210.0F * alpha), tR, tG, tB));
         }

         if (searchActive && alpha > 0.75F) {
            float badgeOff = this.query.length() > 0 ? (float)Vonware.textManager.getCustomFont().getStringWidth(String.valueOf(matchCount)) + 14.0F : 0.0F;
            String hint = "esc";
            float hx = bx2 - (float)Vonware.textManager.getCustomFont().getStringWidth(hint) - 8.0F - badgeOff - 2.0F;
            Vonware.textManager.getCustomFont().drawStringWithShadow(hint, (double)hx, (double)textY, argb((int)(38.0F * alpha), 255, 255, 255));
         }

      }
   }

   private static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   private static String clipToWidth(String text, float maxW) {
      if ((float)Vonware.textManager.getCustomFont().getStringWidth(text) <= maxW) {
         return text;
      } else {
         for(int i = 1; i < text.length(); ++i) {
            String sub = text.substring(i);
            if ((float)Vonware.textManager.getCustomFont().getStringWidth(sub) <= maxW) {
               return sub;
            }
         }

         return "";
      }
   }

   private static void drawPillFill(float x1, float y1, float x2, float y2, float r, int cr, int cg, int cb, int alpha) {
      r = Math.min(r, Math.min((x2 - x1) / 2.0F, (y2 - y1) / 2.0F));
      int col = argb(alpha, cr, cg, cb);
      RenderUtil.drawRect(x1, y1 + r, x2, y2 - r, col);
      RenderUtil.drawRect(x1 + r, y1, x2 - r, y1 + r, col);
      RenderUtil.drawRect(x1 + r, y2 - r, x2 - r, y2, col);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)cr / 255.0F, (float)cg / 255.0F, (float)cb / 255.0F, (float)alpha / 255.0F);
      GL11.glBegin(4);
      filledArc((double)(x1 + r), (double)(y1 + r), (double)r, (double)180.0F, (double)270.0F);
      filledArc((double)(x2 - r), (double)(y1 + r), (double)r, (double)270.0F, (double)360.0F);
      filledArc((double)(x2 - r), (double)(y2 - r), (double)r, (double)0.0F, (double)90.0F);
      filledArc((double)(x1 + r), (double)(y2 - r), (double)r, (double)90.0F, (double)180.0F);
      GL11.glEnd();
   }

   private static void filledArc(double cx, double cy, double r, double a1, double a2) {
      int steps = Math.max(1, (int)(Math.abs(a2 - a1) / (double)22.5F));
      double step = (a2 - a1) / (double)steps;

      for(int i = 0; i < steps; ++i) {
         double ra1 = Math.toRadians(a1 + (double)i * step);
         double ra2 = Math.toRadians(a1 + (double)(i + 1) * step);
         GL11.glVertex2d(cx, cy);
         GL11.glVertex2d(cx + Math.cos(ra2) * r, cy + Math.sin(ra2) * r);
         GL11.glVertex2d(cx + Math.cos(ra1) * r, cy + Math.sin(ra1) * r);
      }

   }

   private static void drawPillOutline(float x1, float y1, float x2, float y2, float r, int cr, int cg, int cb, int alpha, float lw) {
      r = Math.min(r, Math.min((x2 - x1) / 2.0F, (y2 - y1) / 2.0F));
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)cr / 255.0F, (float)cg / 255.0F, (float)cb / 255.0F, (float)alpha / 255.0F);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(lw);
      GL11.glBegin(2);
      arcLoop(x1 + r, y1 + r, r, Math.PI, (Math.PI * 1.5D));
      arcLoop(x2 - r, y1 + r, r, (Math.PI * 1.5D), (Math.PI * 2D));
      arcLoop(x2 - r, y2 - r, r, (double)0.0F, (Math.PI / 2D));
      arcLoop(x1 + r, y2 - r, r, (Math.PI / 2D), Math.PI);
      GL11.glEnd();
      GL11.glDisable(2848);
   }

   private static void arcLoop(float cx, float cy, float r, double a1, double a2) {
      for(int i = 0; i <= 16; ++i) {
         double a = a1 + (a2 - a1) * (double)i / (double)16.0F;
         GL11.glVertex2f(cx + r * (float)Math.cos(a), cy + r * (float)Math.sin(a));
      }

   }

   private static void drawLine(float x1, float y1, float x2, float y2, int r, int g, int b, int alpha, float lw) {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)alpha / 255.0F);
      GL11.glLineWidth(lw);
      GL11.glBegin(1);
      GL11.glVertex2f(x1, y1);
      GL11.glVertex2f(x2, y2);
      GL11.glEnd();
   }

   private static void drawSearchIcon(float cx, float cy, float radius, int r, int g, int b, float alpha) {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)((int)(180.0F * alpha)) / 255.0F);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(1.2F);
      GL11.glBegin(2);

      for(int i = 0; i < 12; ++i) {
         double a = (Math.PI * 2D) * (double)i / (double)12.0F;
         GL11.glVertex2f(cx + radius * (float)Math.cos(a), cy + radius * (float)Math.sin(a));
      }

      GL11.glEnd();
      float ha = (float)Math.toRadians((double)135.0F);
      GL11.glBegin(1);
      GL11.glVertex2f(cx + (radius - 0.5F) * (float)Math.cos((double)ha), cy + (radius - 0.5F) * (float)Math.sin((double)ha));
      GL11.glVertex2f(cx + (radius + 3.2F) * (float)Math.cos((double)ha), cy + (radius + 3.2F) * (float)Math.sin((double)ha));
      GL11.glEnd();
      GL11.glDisable(2848);
   }
}
