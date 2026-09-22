package fat.vonware.util.shape;

import org.lwjgl.opengl.GL11;

public final class RoundedStencilUtil {
   private static final int CORNER_SEGS = 14;

   private RoundedStencilUtil() {
   }

   public static void beginMask(float x1, float y1, float x2, float y2, float r) {
      beginMaskCorners(x1, y1, x2, y2, r, r, r, r);
   }

   public static void beginMaskCorners(float x1, float y1, float x2, float y2, float rTL, float rTR, float rBR, float rBL) {
      float maxH = (x2 - x1) * 0.5F;
      float maxV = (y2 - y1) * 0.5F;
      float cap = Math.min(maxH, maxV);
      rTL = Math.max(0.0F, Math.min(rTL, cap));
      rTR = Math.max(0.0F, Math.min(rTR, cap));
      rBR = Math.max(0.0F, Math.min(rBR, cap));
      rBL = Math.max(0.0F, Math.min(rBL, cap));
      GL11.glEnable(2960);
      GL11.glClearStencil(0);
      GL11.glClear(1024);
      GL11.glStencilFunc(519, 1, 255);
      GL11.glStencilOp(7680, 7680, 7681);
      GL11.glColorMask(false, false, false, false);
      GL11.glDepthMask(false);
      drawFilledShape(x1, y1, x2, y2, rTL, rTR, rBR, rBL);
      GL11.glColorMask(true, true, true, true);
      GL11.glDepthMask(true);
      GL11.glStencilFunc(514, 1, 255);
      GL11.glStencilOp(7680, 7680, 7680);
   }

   public static void endMask() {
      GL11.glDisable(2960);
   }

   private static void drawFilledShape(float x1, float y1, float x2, float y2, float rTL, float rTR, float rBR, float rBL) {
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBegin(9);
      arc(x1 + rTL, y1 + rTL, rTL, Math.PI, (Math.PI * 1.5D), 0);
      arc(x2 - rTR, y1 + rTR, rTR, (Math.PI * 1.5D), (Math.PI * 2D), 0);
      arc(x2 - rBR, y2 - rBR, rBR, (double)0.0F, (Math.PI / 2D), 0);
      arc(x1 + rBL, y2 - rBL, rBL, (Math.PI / 2D), Math.PI, 0);
      GL11.glEnd();
      GL11.glEnable(3553);
   }

   private static void arc(float cx, float cy, float r, double startA, double endA, int skipFirst) {
      for(int i = skipFirst; i <= 14; ++i) {
         double a = startA + (endA - startA) * (double)i / (double)14.0F;
         GL11.glVertex2f(cx + r * (float)Math.cos(a), cy + r * (float)Math.sin(a));
      }

   }
}
