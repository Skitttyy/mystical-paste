package fat.vonware.util.Animations.draw;

import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class DismantleUtil {
   public static final float EXIT_SLASH_START = 0.55F;
   public static final float EXIT_SLASH_END = 1.1F;
   public static final float EXIT_HOLD_END = 1.24F;
   public static final float EXIT_FADE_START = 1.54F;
   public static final float EXIT_TOTAL = 2.2F;
   public boolean exitAnimating = false;
   public float exitProgress = 0.0F;
   public boolean exitInitialized = false;
   private boolean dismantleDestroySounded = false;
   private float exitPanelCX;
   private float exitPanelCY;
   private final List<ShardFragment> exitShards = new ArrayList();
   private final List<SlashLine> exitSlashes = new ArrayList();
   private final Random random = new Random();

   public void reset() {
      this.exitAnimating = false;
      this.exitProgress = 0.0F;
      this.exitInitialized = false;
      this.dismantleDestroySounded = false;
      this.exitShards.clear();
      this.exitSlashes.clear();
   }

   public void tickProgress(float delta) {
      if (this.exitAnimating) {
         this.exitProgress += delta;
      }

   }

   public void initIfNeeded(int sw, int sh, int panelX, int panelY, int panelW, int panelH, Color themeColor) {
      if (!this.exitInitialized) {
         this.init(sw, sh, panelX, panelY, panelW, panelH, themeColor);
      }

   }

   private void init(int sw, int sh, int panelX, int panelY, int panelW, int panelH, Color themeColor) {
      this.exitShards.clear();
      this.exitSlashes.clear();
      float pcx = (float)panelX + (float)panelW / 2.0F;
      float pcy = (float)panelY + (float)panelH / 2.0F;
      this.exitPanelCX = pcx;
      this.exitPanelCY = pcy;
      this.exitSlashes.add(new SlashLine(-80.0F, pcy - 55.0F, (float)(sw + 80), pcy + 72.0F, 0.55F));
      this.exitSlashes.add(new SlashLine(pcx - 78.0F, -60.0F, pcx + 92.0F, (float)(sh + 60), 0.61800003F));
      this.exitSlashes.add(new SlashLine(-80.0F, pcy + 62.0F, (float)(sw + 80), pcy - 20.0F, 0.68F));
      this.exitSlashes.add(new SlashLine(pcx + 58.0F, -60.0F, pcx - 92.0F, (float)(sh + 60), 0.77F));
      this.exitSlashes.add(new SlashLine(-80.0F, pcy - 8.0F, (float)(sw + 80), pcy + 30.0F, 0.88199997F));
      List<float[]> pieces = new ArrayList();
      pieces.add(new float[]{(float)panelX, (float)panelY, (float)(panelX + panelW), (float)panelY, (float)(panelX + panelW), (float)(panelY + panelH), (float)panelX, (float)(panelY + panelH)});

      for(SlashLine sl : this.exitSlashes) {
         List<float[]> next = new ArrayList();

         for(float[] poly : pieces) {
            float[][] halves = splitPolygonByLine(poly, sl.x1, sl.y1, sl.x2, sl.y2);
            if (halves[0] != null) {
               next.add(halves[0]);
            }

            if (halves[1] != null) {
               next.add(halves[1]);
            }
         }

         if (!next.isEmpty()) {
            pieces = next;
         }
      }

      int edgeColor = (new Color(Math.min(255, themeColor.getRed() + 70), Math.min(255, themeColor.getGreen() + 70), Math.min(255, themeColor.getBlue() + 90), 210)).getRGB();

      for(float[] poly : pieces) {
         int n = poly.length / 2;
         if (n >= 3) {
            float cx = 0.0F;
            float cy = 0.0F;

            for(int i = 0; i < n; ++i) {
               cx += poly[i * 2];
               cy += poly[i * 2 + 1];
            }

            cx /= (float)n;
            cy /= (float)n;
            float[] vx = new float[n];
            float[] vy = new float[n];

            for(int i = 0; i < n; ++i) {
               vx[i] = poly[i * 2];
               vy[i] = poly[i * 2 + 1];
            }

            float dx = cx - pcx;
            float dy = cy - pcy;
            float dist = (float)Math.sqrt((double)(dx * dx + dy * dy)) + 1.0F;
            float spd = 80.0F + this.random.nextFloat() * 200.0F;
            float vxD = dx / dist * spd + (this.random.nextFloat() - 0.5F) * 70.0F;
            float vyD = dy / dist * spd + (this.random.nextFloat() - 0.5F) * 70.0F;
            float av = (this.random.nextFloat() - 0.5F) * 300.0F;
            int dr = 15 + this.random.nextInt(14);
            int dg = 15 + this.random.nextInt(14);
            int db = 18 + this.random.nextInt(16);
            this.exitShards.add(new ShardFragment(vx, vy, n, cx, cy, vxD, vyD, av, (new Color(dr, dg, db, 235)).getRGB(), edgeColor));
         }
      }

      this.exitInitialized = true;
   }

   private static float[][] splitPolygonByLine(float[] poly, float lx1, float ly1, float lx2, float ly2) {
      int n = poly.length / 2;
      float ldx = lx2 - lx1;
      float ldy = ly2 - ly1;
      float[] sides = new float[n];

      for(int i = 0; i < n; ++i) {
         sides[i] = ldx * (poly[i * 2 + 1] - ly1) - ldy * (poly[i * 2] - lx1);
      }

      List<Float> pos = new ArrayList();
      List<Float> neg = new ArrayList();

      for(int i = 0; i < n; ++i) {
         int j = (i + 1) % n;
         float px = poly[i * 2];
         float py = poly[i * 2 + 1];
         float nx = poly[j * 2];
         float ny = poly[j * 2 + 1];
         float si = sides[i];
         float sj = sides[j];
         if (si >= 0.0F) {
            pos.add(px);
            pos.add(py);
         } else {
            neg.add(px);
            neg.add(py);
         }

         if (si > 0.0F && sj < 0.0F || si < 0.0F && sj > 0.0F) {
            float t = si / (si - sj);
            float ix = px + t * (nx - px);
            float iy = py + t * (ny - py);
            pos.add(ix);
            pos.add(iy);
            neg.add(ix);
            neg.add(iy);
         }
      }

      float[] posArr = pos.size() >= 6 ? listToArray(pos) : null;
      float[] negArr = neg.size() >= 6 ? listToArray(neg) : null;
      return new float[][]{posArr, negArr};
   }

   private static float[] listToArray(List<Float> list) {
      float[] arr = new float[list.size()];

      for(int i = 0; i < list.size(); ++i) {
         arr[i] = (Float)list.get(i);
      }

      return arr;
   }

   public void drawSlashCrackOverlay(int sw, int sh) {
      float p = this.exitProgress;
      long now = System.currentTimeMillis();
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();

      for(int si = 0; si < this.exitSlashes.size(); ++si) {
         SlashLine sl = (SlashLine)this.exitSlashes.get(si);
         if (!(p < sl.spawnAt)) {
            float age = p - sl.spawnAt;
            float revealT = Math.min(1.0F, age / 0.042F);
            float drawX2 = sl.x1 + (sl.x2 - sl.x1) * revealT;
            float drawY2 = sl.y1 + (sl.y2 - sl.y1) * revealT;
            boolean full = revealT >= 1.0F;
            float settleT = Math.min(1.0F, Math.max(0.0F, (age - 0.05F) / 0.22F));
            float cr = 1.0F;
            float cg = 1.0F - settleT * 0.88F;
            float cb = 1.0F - settleT * 0.92F;
            float lineA;
            if (age < 0.1F) {
               lineA = revealT;
            } else if (age < 0.28F) {
               lineA = 1.0F - (age - 0.1F) / 0.18F * 0.58F;
            } else {
               lineA = (float)(Math.sin((double)now * 0.0028 + (double)sl.spawnAt * (double)14.0F) * 0.09 + 0.26);
            }

            if (lineA > 0.005F) {
               GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 30.0F, 0.65F, 0.04F, 0.12F, lineA * 0.04F);
               drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 18.0F, 0.8F, 0.06F, 0.18F, lineA * 0.09F);
               drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 9.0F, cr * 0.95F, cg * 0.35F, cb * 0.45F, lineA * 0.22F);
               drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 4.0F, cr, cg * 0.65F, cb * 0.75F, lineA * 0.56F);
               drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 1.8F, cr, cg, cb, lineA * 0.88F);
               float hcA = Math.max(0.0F, 1.0F - age / 0.14F);
               if (hcA > 0.01F) {
                  drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 0.7F, 1.0F, 1.0F, 1.0F, hcA * 0.96F);
               }
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            drawUniformLine(buf, tess, sl.x1, sl.y1, drawX2, drawY2, 2.2F, 0.01F, 0.008F, 0.018F, Math.min(1.0F, age / 0.06F) * 0.98F);
            if (!full && revealT > 0.05F) {
               GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               drawFilledCircle(buf, tess, drawX2, drawY2, 9.0F, 1.0F, 0.2F, 0.3F, 0.28F);
               drawFilledCircle(buf, tess, drawX2, drawY2, 4.0F, 1.0F, 0.7F, 0.8F, 0.55F);
               drawFilledCircle(buf, tess, drawX2, drawY2, 1.5F, 1.0F, 1.0F, 1.0F, 0.9F);
            }

            float flashA = Math.max(0.0F, 1.0F - age / 0.1F);
            float flashScale = Math.max(0.35F, 1.0F - (float)si * 0.15F);
            if (flashA > 0.01F) {
               GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
               Gui.drawRect(0, 0, sw, sh, (new Color(255, 205, 210, (int)(flashA * flashScale * (float)(si == 0 ? 175 : 105)))).getRGB());
            }
         }
      }

      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
   }

   public void drawExitShatter(int sw, int sh) {
      float p = this.exitProgress;
      float shardT = p - 1.24F;
      float shardDur = 0.96000004F;
      if (!this.dismantleDestroySounded) {
         this.dismantleDestroySounded = true;
         ThreadUtil.startDaemon("Dismantle-Destroy", SoundUtil::playDismantleDestroy);
      }

      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      float shardFadeStart = 0.29999995F;
      float shardA = shardT <= shardFadeStart ? 1.0F : Math.max(0.0F, 1.0F - (shardT - shardFadeStart) / (shardDur - shardFadeStart));
      if (shardT > 0.0F && shardA > 0.01F) {
         GlStateManager.enableBlend();
         GlStateManager.disableTexture2D();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);

         for(ShardFragment s : this.exitShards) {
            float tx = s.velX * shardT;
            float ty = s.velY * shardT;
            float rad = (float)Math.toRadians((double)(s.angVel * shardT));
            float cos = (float)Math.cos((double)rad);
            float sin = (float)Math.sin((double)rad);
            float[] wx = new float[s.count];
            float[] wy = new float[s.count];

            for(int i = 0; i < s.count; ++i) {
               float lx = s.vx[i] - s.cx;
               float ly = s.vy[i] - s.cy;
               wx[i] = s.cx + tx + lx * cos - ly * sin;
               wy[i] = s.cy + ty + lx * sin + ly * cos;
            }

            float wcx = s.cx + tx;
            float wcy = s.cy + ty;
            int dc = s.darkColor;
            float dr = (float)(dc >> 16 & 255) / 255.0F;
            float dg = (float)(dc >> 8 & 255) / 255.0F;
            float db = (float)(dc & 255) / 255.0F;
            float da = (float)(dc >> 24 & 255) / 255.0F * shardA;
            buf.begin(6, DefaultVertexFormats.POSITION_COLOR);
            buf.pos((double)wcx, (double)wcy, (double)0.0F).color(dr, dg, db, da).endVertex();

            for(int i = 0; i < s.count; ++i) {
               buf.pos((double)wx[i], (double)wy[i], (double)0.0F).color(dr, dg, db, da * 0.82F).endVertex();
            }

            buf.pos((double)wx[0], (double)wy[0], (double)0.0F).color(dr, dg, db, da * 0.82F).endVertex();
            tess.draw();
            int ec = s.edgeColor;
            float er = (float)(ec >> 16 & 255) / 255.0F;
            float eg = (float)(ec >> 8 & 255) / 255.0F;
            float eb2 = (float)(ec & 255) / 255.0F;
            float ea = (float)(ec >> 24 & 255) / 255.0F * shardA * 0.65F;

            for(int i = 0; i < s.count; ++i) {
               int j = (i + 1) % s.count;
               drawUniformLine(buf, tess, wx[i], wy[i], wx[j], wy[j], 0.9F, er, eg, eb2, ea);
            }
         }

         GlStateManager.enableTexture2D();
      }

      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
      float slashFade = Math.max(0.0F, 1.0F - shardT / 0.22F);
      if (slashFade > 0.01F) {
         for(SlashLine sl : this.exitSlashes) {
            drawUniformLine(buf, tess, sl.x1, sl.y1, sl.x2, sl.y2, 30.0F, 0.65F, 0.04F, 0.12F, slashFade * 0.06F);
            drawUniformLine(buf, tess, sl.x1, sl.y1, sl.x2, sl.y2, 16.0F, 0.82F, 0.06F, 0.18F, slashFade * 0.17F);
            drawUniformLine(buf, tess, sl.x1, sl.y1, sl.x2, sl.y2, 6.0F, 1.0F, 0.12F, 0.22F, slashFade * 0.44F);
            drawUniformLine(buf, tess, sl.x1, sl.y1, sl.x2, sl.y2, 2.0F, 1.0F, 0.78F, 0.82F, slashFade * 0.88F);
         }
      }

      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      float overlayA = p < 1.54F ? 0.0F : Math.min(1.0F, (p - 1.54F) / 0.6600001F);
      if (overlayA > 0.01F) {
         Gui.drawRect(0, 0, sw, sh, (new Color(0, 0, 0, (int)(255.0F * overlayA))).getRGB());
      }

      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawFilledCircle(BufferBuilder buf, Tessellator tess, float cx, float cy, float radius, float r, float g, float b, float a) {
      if (!(a < 0.005F)) {
         a = Math.min(1.0F, a);
         int segs = Math.max(8, (int)(radius * 3.0F));
         buf.begin(6, DefaultVertexFormats.POSITION_COLOR);
         buf.pos((double)cx, (double)cy, (double)0.0F).color(r, g, b, a).endVertex();

         for(int i = 0; i <= segs; ++i) {
            double ang = (Math.PI * 2D) * (double)i / (double)segs;
            buf.pos((double)cx + Math.cos(ang) * (double)radius, (double)cy + Math.sin(ang) * (double)radius, (double)0.0F).color(r, g, b, a * 0.5F).endVertex();
         }

         tess.draw();
      }
   }

   public static void drawUniformLine(BufferBuilder buf, Tessellator tess, float x1, float y1, float x2, float y2, float width, float r, float g, float b, float a) {
      if (!(a < 0.005F)) {
         a = Math.min(1.0F, a);
         float dx = x2 - x1;
         float dy = y2 - y1;
         float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
         if (!(len < 0.01F)) {
            float nx = -dy / len * width * 0.5F;
            float ny = dx / len * width * 0.5F;
            buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
            buf.pos((double)(x1 + nx), (double)(y1 + ny), (double)0.0F).color(r, g, b, a).endVertex();
            buf.pos((double)(x1 - nx), (double)(y1 - ny), (double)0.0F).color(r, g, b, a).endVertex();
            buf.pos((double)(x2 - nx), (double)(y2 - ny), (double)0.0F).color(r, g, b, a).endVertex();
            buf.pos((double)(x2 + nx), (double)(y2 + ny), (double)0.0F).color(r, g, b, a).endVertex();
            tess.draw();
         }
      }
   }

   public static class ShardFragment {
      public float[] vx;
      public float[] vy;
      public int count;
      public float cx;
      public float cy;
      public float velX;
      public float velY;
      public float angVel;
      public int darkColor;
      public int edgeColor;

      public ShardFragment(float[] vx, float[] vy, int count, float cx, float cy, float velX, float velY, float angVel, int dark, int edge) {
         this.vx = vx;
         this.vy = vy;
         this.count = count;
         this.cx = cx;
         this.cy = cy;
         this.velX = velX;
         this.velY = velY;
         this.angVel = angVel;
         this.darkColor = dark;
         this.edgeColor = edge;
      }
   }

   public static class SlashLine {
      public float x1;
      public float y1;
      public float x2;
      public float y2;
      public float spawnAt;

      public SlashLine(float x1, float y1, float x2, float y2, float spawnAt) {
         this.x1 = x1;
         this.y1 = y1;
         this.x2 = x2;
         this.y2 = y2;
         this.spawnAt = spawnAt;
      }
   }
}
