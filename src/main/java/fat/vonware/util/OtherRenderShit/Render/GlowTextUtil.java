package fat.vonware.util.OtherRenderShit.Render;

import fat.vonware.Vonware;
import java.awt.Color;

public class GlowTextUtil {
   private static long glowStartTime = System.currentTimeMillis();
   private static final float GLOW_WIDTH = 2.5F;
   private static final float SWEEP_SPEED = 0.002F;

   public static void drawGlowString(String text, float x, float y, Color baseColor, int restColor, int alpha) {
      long currentTime = System.currentTimeMillis();
      float elapsed = (float)(currentTime - glowStartTime) * 0.002F;
      float glowPos = (float)Math.sin((double)elapsed) * 0.5F + 0.5F;
      float glowCenter = glowPos * (float)text.length();
      float currentX = x;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         float distanceFromCenter = Math.abs((float)i - glowCenter);
         int charColor;
         if (distanceFromCenter > 2.5F) {
            int r = restColor >> 16 & 255;
            int g = restColor >> 8 & 255;
            int b = restColor & 255;
            charColor = (new Color(r, g, b, alpha)).getRGB();
         } else {
            float normalizedDist = distanceFromCenter / 2.5F;
            float glowStrength = (float)Math.cos((double)normalizedDist * Math.PI / (double)2.0F);
            int rRest = restColor >> 16 & 255;
            int gRest = restColor >> 8 & 255;
            int bRest = restColor & 255;
            int rr = rRest + (int)(glowStrength * (float)(baseColor.getRed() - rRest));
            int gg = gRest + (int)(glowStrength * (float)(baseColor.getGreen() - gRest));
            int bb = bRest + (int)(glowStrength * (float)(baseColor.getBlue() - bRest));
            charColor = (new Color(Math.max(0, Math.min(255, rr)), Math.max(0, Math.min(255, gg)), Math.max(0, Math.min(255, bb)), alpha)).getRGB();
         }

         Vonware.textManager.drawStringWithShadow(String.valueOf(c), currentX, y, charColor);
         currentX += (float)Vonware.textManager.getStringWidth(String.valueOf(c));
      }

   }

   public static void drawGlowString(String text, float x, float y, Color baseColor) {
      drawGlowString(text, x, y, baseColor, 16777215, 255);
   }
}
