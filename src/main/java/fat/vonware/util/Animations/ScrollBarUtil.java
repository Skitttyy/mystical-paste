package fat.vonware.util.Animations;

import fat.vonware.util.Animations.render.RenderUtil;

public class ScrollBarUtil {
   private boolean dragging = false;
   private float dragStartMouseY = 0.0F;
   private float dragStartOffset = 0.0F;
   private float scrollOffset = 0.0F;
   private float visualScrollOffset = 0.0F;
   private static final float SMOOTH_SPEED = 0.18F;
   public static final float BAR_WIDTH = 2.5F;
   private static final float MIN_THUMB_H = 16.0F;
   private final float[] thumbResult = new float[2];
   private static final float SCROLL_SPEED = 30.0F;

   public float getScrollOffset() {
      return this.scrollOffset;
   }

   public float getVisualScrollOffset() {
      return this.visualScrollOffset;
   }

   public void tickVisual() {
      this.visualScrollOffset += (this.scrollOffset - this.visualScrollOffset) * 0.18F;
      if (Math.abs(this.visualScrollOffset - this.scrollOffset) < 0.25F) {
         this.visualScrollOffset = this.scrollOffset;
      }

   }

   public void resetScroll() {
      this.scrollOffset = 0.0F;
      this.visualScrollOffset = 0.0F;
   }

   public void scroll(int delta, float contentH, float visibleH) {
      this.scroll(delta, contentH, visibleH, 30.0F);
   }

   public void scroll(int delta, float contentH, float visibleH, float sensitivity) {
      if (contentH <= visibleH) {
         this.scrollOffset = 0.0F;
      } else {
         this.scrollOffset -= (float)delta * sensitivity;
         this.scrollOffset = clamp(this.scrollOffset, contentH, visibleH);
      }
   }

   private static float clamp(float value, float contentH, float visibleH) {
      float max = Math.max(0.0F, contentH - visibleH);
      return Math.max(0.0F, Math.min(max, value));
   }

   public void mouseClicked(int mouseX, int mouseY, float trackX, float trackY, float trackH, float contentH, float visibleH) {
      if (!(contentH <= visibleH)) {
         float[] thumb = this.thumbBounds(trackY, trackH, contentH, visibleH);
         if ((float)mouseX >= trackX && (float)mouseX <= trackX + 2.5F && (float)mouseY >= thumb[0] && (float)mouseY <= thumb[0] + thumb[1]) {
            this.dragging = true;
            this.dragStartMouseY = (float)mouseY;
            this.dragStartOffset = this.scrollOffset;
         }

      }
   }

   public void mouseReleased() {
      this.dragging = false;
   }

   public void mouseDragged(int mouseY, float trackH, float contentH, float visibleH) {
      if (this.dragging && !(contentH <= visibleH)) {
         float thumbH = this.thumbH(trackH, contentH, visibleH);
         float trackRange = trackH - thumbH;
         float scrollRange = contentH - visibleH;
         if (!(trackRange <= 0.0F)) {
            float delta = ((float)mouseY - this.dragStartMouseY) / trackRange * scrollRange;
            this.scrollOffset = clamp(this.dragStartOffset + delta, contentH, visibleH);
         }
      }
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public void clampToContent(float contentH, float visibleH) {
      this.scrollOffset = clamp(this.scrollOffset, contentH, visibleH);
   }

   public void draw(float x, float y, float trackH, float contentH, float visibleH, int bR, int bG, int bB, int mouseX, int mouseY) {
      if (!(contentH <= visibleH)) {
         float[] thumb = this.thumbBoundsForOffset(this.visualScrollOffset, y, trackH, contentH, visibleH);
         float thumbY = thumb[0];
         float thumbH = thumb[1];
         boolean hov = (float)mouseX >= x && (float)mouseX <= x + 2.5F && (float)mouseY >= thumbY && (float)mouseY <= thumbY + thumbH || this.dragging;
         RenderUtil.drawRect(x, y, x + 2.5F, y + trackH, argb(12, bR, bG, bB));
         RenderUtil.drawRect(x, thumbY, x + 2.5F, thumbY + thumbH, argb(hov ? 90 : 30, bR, bG, bB));
      }
   }

   private float thumbH(float trackH, float contentH, float visibleH) {
      return Math.max(16.0F, visibleH / contentH * trackH);
   }

   private float[] thumbBounds(float trackY, float trackH, float contentH, float visibleH) {
      return this.thumbBoundsForOffset(this.scrollOffset, trackY, trackH, contentH, visibleH);
   }

   private float[] thumbBoundsForOffset(float offset, float trackY, float trackH, float contentH, float visibleH) {
      float th = this.thumbH(trackH, contentH, visibleH);
      float maxScroll = contentH - visibleH;
      this.thumbResult[0] = trackY + (maxScroll > 0.0F ? offset / maxScroll * (trackH - th) : 0.0F);
      this.thumbResult[1] = th;
      return this.thumbResult;
   }

   private static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }
}
