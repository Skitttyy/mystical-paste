package fat.vonware.util.Animations.game;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Animations.render.RenderMethods;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class ReplyWaypointRenderer {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final float BEAM_HEIGHT = 256.0F;
   private static final int SEGMENTS = 18;
   private static final float RADIUS_OUTER = 0.55F;
   private static final float RADIUS_INNER = 0.28F;
   private static final float PROXIMITY_RADIUS = 10.0F;
   private static final long FADE_DURATION_MS = 1500L;
   private static final double FAR_PLANE_MARGIN = 0.8;
   private static final ConcurrentHashMap<String, ReplyWaypoint> waypoints = new ConcurrentHashMap();

   public static void add(ReplyWaypoint rw) {
      waypoints.put(rw.name, rw);
   }

   public static void remove(String name) {
      waypoints.remove(name);
   }

   public static void clear() {
      waypoints.clear();
   }

   private static double getMaxRenderDist() {
      double farPlane = (double)(mc.gameSettings.renderDistanceChunks * 16) * Math.sqrt((double)2.0F);
      return Math.max((double)64.0F, farPlane * 0.8);
   }

   private static void syncGlCache() {
      if (GL11.glIsEnabled(3042)) {
         GlStateManager.enableBlend();
      } else {
         GlStateManager.disableBlend();
      }

      if (GL11.glIsEnabled(2929)) {
         GlStateManager.enableDepth();
      } else {
         GlStateManager.disableDepth();
      }

      if (GL11.glIsEnabled(3008)) {
         GlStateManager.enableAlpha();
      } else {
         GlStateManager.disableAlpha();
      }

      if (GL11.glIsEnabled(3553)) {
         GlStateManager.enableTexture2D();
      } else {
         GlStateManager.disableTexture2D();
      }

      if (GL11.glIsEnabled(2884)) {
         GlStateManager.enableCull();
      } else {
         GlStateManager.disableCull();
      }

      if (GL11.glIsEnabled(2896)) {
         GlStateManager.enableLighting();
      } else {
         GlStateManager.disableLighting();
      }

      if (GL11.glIsEnabled(2912)) {
         GlStateManager.enableFog();
      } else {
         GlStateManager.disableFog();
      }

      if (GL11.glIsEnabled(32823)) {
         GlStateManager.enablePolygonOffset();
      } else {
         GlStateManager.disablePolygonOffset();
      }

      GlStateManager.depthMask(GL11.glGetInteger(2930) != 0);
      GlStateManager.blendFunc(GL11.glGetInteger(3041), GL11.glGetInteger(3040));
      GlStateManager.doPolygonOffset(GL11.glGetFloat(32824), GL11.glGetFloat(10752));
      GlStateManager.resetColor();
      GlStateManager.bindTexture(GL11.glGetInteger(32873));
   }

   public static void render(boolean throughWalls, WaypointMode mode) {
      if (!waypoints.isEmpty() && mc.player != null && mc.world != null) {
         GL11.glPushAttrib(1048575);
         GL11.glPushMatrix();
         syncGlCache();

         try {
            double rpX = Interpolation.getRenderPosX();
            double rpY = Interpolation.getRenderPosY();
            double rpZ = Interpolation.getRenderPosZ();
            long time = System.currentTimeMillis();
            double maxRDist = getMaxRenderDist();
            List<String> toRemove = null;

            for(ReplyWaypoint rw : waypoints.values()) {
               long elapsed = time - rw.addedTime;
               double distX = rw.x + (double)0.5F - mc.player.posX;
               double distY = rw.y - mc.player.posY;
               double distZ = rw.z + (double)0.5F - mc.player.posZ;
               double distance = Math.sqrt(distX * distX + distY * distY + distZ * distZ);
               if (rw.fadeStartTime == 0L && distance <= (double)10.0F) {
                  rw.fadeStartTime = time;
               }

               float fadeAlpha = 1.0F;
               if (rw.fadeStartTime != 0L) {
                  long fadeElapsed = time - rw.fadeStartTime;
                  if (fadeElapsed >= 1500L) {
                     if (toRemove == null) {
                        toRemove = new ArrayList();
                     }

                     toRemove.add(rw.name);
                     continue;
                  }

                  fadeAlpha = 1.0F - (float)fadeElapsed / 1500.0F;
               } else if (elapsed >= rw.lifetimeMs) {
                  if (toRemove == null) {
                     toRemove = new ArrayList();
                  }

                  toRemove.add(rw.name);
                  continue;
               }

               double dx = rw.x + (double)0.5F - rpX;
               double dy = rw.y - rpY;
               double dz = rw.z + (double)0.5F - rpZ;
               double renderScale = distance > maxRDist ? maxRDist / distance : (double)1.0F;
               double rdx = dx * renderScale;
               double rdy = dy * renderScale;
               double rdz = dz * renderScale;
               double tagScale = Math.max((double)0.5F, Math.min(distance * 0.055, (double)6.0F));
               long remaining = rw.lifetimeMs - elapsed;
               if (mode == ReplyWaypointRenderer.WaypointMode.Beam) {
                  renderBeam(rdx, rdy, rdz, time, throughWalls, fadeAlpha);
               } else {
                  renderBox(rw.x, rw.y, rw.z, rw.color, fadeAlpha);
               }

               renderNametag(rdx, rdy, rdz, tagScale, rw.name, distance, remaining, time, throughWalls, fadeAlpha, mode);
            }

            if (toRemove != null) {
               for(String key : toRemove) {
                  waypoints.remove(key);
               }
            }
         } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            syncGlCache();
         }

      }
   }

   private static void renderBox(double wx, double wy, double wz, Color color, float fadeAlpha) {
      double halfWidth = (double)0.75F;
      AxisAlignedBB bb = new AxisAlignedBB(wx - (double)0.75F, wy, wz - (double)0.75F, wx + (double)0.75F, wy + (double)4.5F, wz + (double)0.75F);
      AxisAlignedBB interpolated = Interpolation.interpolateAxis(bb);
      Color resolvedColor = Global.getGlobalColorRainbowS();
      RenderMethods.startRender();
      RenderMethods.drawBox(interpolated, new Color(resolvedColor.getRed(), resolvedColor.getGreen(), resolvedColor.getBlue(), Math.round(125.0F * fadeAlpha)));
      RenderMethods.drawOutline(interpolated, 1.0F, new Color(resolvedColor.getRed(), resolvedColor.getGreen(), resolvedColor.getBlue(), Math.round(255.0F * fadeAlpha)));
      RenderMethods.endRender();
   }

   private static void renderBeam(double x, double y, double z, long time, boolean throughWalls, float fadeAlpha) {
      Color gc = Global.getGlobalColor();
      float r = (float)gc.getRed() / 255.0F;
      float g = (float)gc.getGreen() / 255.0F;
      float b = (float)gc.getBlue() / 255.0F;
      float pulse = (float)((double)0.5F + (double)0.5F * Math.sin((double)time / (double)900.0F));
      float beamBottom = (float)(-y);
      float beamTop = (float)((double)256.0F - y);
      RenderMethods.startRender();
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2884);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glDisable(2912);
      GL11.glDisable(2896);
      GL11.glDepthMask(false);
      GL11.glDisable(2929);
      if (throughWalls) {
         drawBeamGeometry(x, y, z, r, g, b, pulse, beamBottom, beamTop, time, 0.3F * fadeAlpha);
      }

      drawBeamGeometry(x, y, z, r, g, b, pulse, beamBottom, beamTop, time, fadeAlpha);
      RenderMethods.endRender();
   }

   private static void drawBeamGeometry(double x, double y, double z, float r, float g, float b, float pulse, float beamBottom, float beamTop, long time, float alpha) {
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glColor4f(r, g, b, (0.08F + 0.06F * pulse) * alpha);
      GL11.glBegin(8);

      for(int i = 0; i <= 18; ++i) {
         double a = (Math.PI * 2D) * (double)i / (double)18.0F;
         float cx = (float)(Math.cos(a) * (double)0.55F);
         float cz = (float)(Math.sin(a) * (double)0.55F);
         GL11.glVertex3f(cx, beamTop, cz);
         GL11.glVertex3f(cx, beamBottom, cz);
      }

      GL11.glEnd();
      GL11.glColor4f(r, g, b, (0.28F + 0.18F * pulse) * alpha);
      GL11.glBegin(8);

      for(int i = 0; i <= 18; ++i) {
         double a = (Math.PI * 2D) * (double)i / (double)18.0F;
         float cx = (float)(Math.cos(a) * (double)0.28F);
         float cz = (float)(Math.sin(a) * (double)0.28F);
         GL11.glVertex3f(cx, beamTop, cz);
         GL11.glVertex3f(cx, beamBottom, cz);
      }

      GL11.glEnd();
      GL11.glPopMatrix();
   }

   private static void renderNametag(double x, double y, double z, double scale, String name, double distance, long remainingMs, long time, boolean throughWalls, float fadeAlpha, WaypointMode mode) {
      String timerText = String.format("%.1fs", (double)remainingMs / (double)1000.0F);
      String nameText = name + String.format(" \u00a77[\u00a7f%.0f\u00a77m]", distance);
      int timerWidth = Vonware.textManager.getStringWidth(timerText);
      int nameWidth = Vonware.textManager.getStringWidth(nameText);
      float bob = (float)Math.sin((double)(time % 2000L) / (double)2000.0F * Math.PI * (double)2.0F) * 0.15F;
      double baseOffset = mode == ReplyWaypointRenderer.WaypointMode.Box ? 5.2 : (double)3.5F;
      double tagY = y + baseOffset + (double)bob;
      float tScale = (float)(scale * 0.09);
      float lineH = 10.0F;
      int ghostAlpha = Math.round(85.0F * fadeAlpha);
      int solidAlpha = Math.round(255.0F * fadeAlpha);
      int solidColor = solidAlpha << 24 | 16777215;
      GL11.glEnable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDisable(2912);
      GL11.glDisable(2896);
      GL11.glDepthMask(false);
      syncGlCache();
      int alpha = throughWalls ? ghostAlpha : Math.round(40.0F * fadeAlpha);
      int color = alpha << 24 | 16777215;
      GL11.glPushMatrix();
      GL11.glTranslated(x, tagY, z);
      GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScalef(-tScale, -tScale, tScale);
      Vonware.textManager.drawStringWithShadow(timerText, (float)(-timerWidth) / 2.0F, -15.0F, color);
      Vonware.textManager.drawStringWithShadow(nameText, (float)(-nameWidth) / 2.0F, -5.0F, color);
      GL11.glPopMatrix();
      GL11.glEnable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDisable(2912);
      GL11.glDisable(2896);
      GL11.glDepthMask(false);
      syncGlCache();
      GL11.glPushMatrix();
      GL11.glTranslated(x, tagY, z);
      GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScalef(-tScale, -tScale, tScale);
      Vonware.textManager.drawStringWithShadow(timerText, (float)(-timerWidth) / 2.0F, -15.0F, solidColor);
      Vonware.textManager.drawStringWithShadow(nameText, (float)(-nameWidth) / 2.0F, -5.0F, solidColor);
      GL11.glPopMatrix();
   }

   public static enum WaypointMode {
      Beam,
      Box;
   }

   public static class ReplyWaypoint {
      public final String name;
      public final double x;
      public final double y;
      public final double z;
      public final long addedTime;
      public final long lifetimeMs;
      public final Color color;
      public volatile long fadeStartTime = 0L;

      public ReplyWaypoint(String name, double x, double y, double z, Color color, double distanceAtAdd) {
         this.name = name;
         this.x = x;
         this.y = y;
         this.z = z;
         this.color = color;
         this.addedTime = System.currentTimeMillis();
         this.lifetimeMs = Math.round(distanceAtAdd / (double)50.0F * (double)10000.0F);
      }
   }
}
