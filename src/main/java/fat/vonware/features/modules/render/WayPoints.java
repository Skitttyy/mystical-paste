package fat.vonware.features.modules.render;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.WayPointsManager;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.OtherRenderShit.WayPoint;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL11;

public class WayPoints extends Module {
   public static WayPoints INSTANCE;
   private static final WayPointsManager manager = new WayPointsManager();
   private static final float DIAMOND_HW = 0.8F;
   private static final float DIAMOND_HH = 1.0F;
   public final Setting<Boolean> throughWalls = new Setting<Boolean>("ThroughWalls", true);

   public WayPoints() {
      super("WayPoints", "use the waypoint command to show waypoints", Module.Category.RENDER, true, false, false);
      this.register(this.throughWalls);
      INSTANCE = this;
      manager.load();
   }

   public void onEnable() {
      manager.load();
   }

   public static WayPointsManager getManager() {
      return manager;
   }

   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         double rpX = Interpolation.getRenderPosX();
         double rpY = Interpolation.getRenderPosY();
         double rpZ = Interpolation.getRenderPosZ();
         List<WayPoint> waypoints = manager.getWaypoints();
         if (!waypoints.isEmpty()) {
            long time = System.currentTimeMillis();

            for(WayPoint wp : waypoints) {
               double dx = wp.getX() + (double)0.5F - rpX;
               double dy = wp.getY() - rpY;
               double dz = wp.getZ() + (double)0.5F - rpZ;
               double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
               double scale = Math.max((double)0.5F, Math.min(distance * 0.035, (double)3.5F));
               float bob = (float)Math.sin((double)(time % 2000L) / (double)2000.0F * Math.PI * (double)2.0F) * 0.15F;
               float rotation = (float)(time % 4000L) / 4000.0F * 360.0F;
               Color color = wp.getAwtColor();
               double ry = dy + (double)0.5F + (double)bob;
               this.renderDiamond(dx, ry, dz, scale, rotation, color);
               this.renderNametag(dx, ry, dz, scale, wp.getName(), distance, color);
            }

            GL11.glEnable(3553);
            GlStateManager.enableTexture2D();
         }
      }
   }

   private void renderDiamond(double x, double y, double z, double scale, float rotation, Color color) {
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      RenderMethods.startRender();
      GlStateManager.disableTexture2D();
      if ((Boolean)this.throughWalls.getValue()) {
         GlStateManager.disableDepth();
      }

      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GlStateManager.disableCull();
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
      GL11.glScaled(scale, scale, scale);
      float hw = 0.8F;
      float hh = 1.0F;
      GlStateManager.color(r, g, b, 0.3F);
      GL11.glBegin(4);
      tri(0.0F, 1.0F, 0.0F, 0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.8F);
      tri(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.8F, -0.8F, 0.0F, 0.0F);
      tri(0.0F, 1.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F);
      tri(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.8F, 0.8F, 0.0F, 0.0F);
      tri(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.8F, 0.8F, 0.0F, 0.0F);
      tri(0.0F, -1.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.8F);
      tri(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.8F, -0.8F, 0.0F, 0.0F);
      tri(0.0F, -1.0F, 0.0F, 0.8F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F);
      GL11.glEnd();
      GlStateManager.color(r, g, b, 0.9F);
      GL11.glLineWidth(1.5F);
      GL11.glBegin(1);
      edge(0.0F, 1.0F, 0.0F, 0.8F, 0.0F, 0.0F);
      edge(0.0F, 1.0F, 0.0F, -0.8F, 0.0F, 0.0F);
      edge(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.8F);
      edge(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.8F);
      edge(0.0F, -1.0F, 0.0F, 0.8F, 0.0F, 0.0F);
      edge(0.0F, -1.0F, 0.0F, -0.8F, 0.0F, 0.0F);
      edge(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.8F);
      edge(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.8F);
      edge(0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.8F);
      edge(0.0F, 0.0F, 0.8F, -0.8F, 0.0F, 0.0F);
      edge(-0.8F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F);
      edge(0.0F, 0.0F, -0.8F, 0.8F, 0.0F, 0.0F);
      GL11.glEnd();
      GL11.glLineWidth(1.0F);
      GL11.glPopMatrix();
      GlStateManager.enableCull();
      GL11.glDisable(2848);
      if ((Boolean)this.throughWalls.getValue()) {
         GlStateManager.enableDepth();
      }

      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      RenderMethods.endRender();
   }

   private void renderNametag(double x, double y, double z, double scale, String name, double distance, Color color) {
      String distText = String.format("\u00a77[%.0fm]", distance);
      String text = name + " " + distText;
      int textWidth = Vonware.textManager.getStringWidth(text);
      double tagY = y + (double)1.0F * scale + 0.9 * scale;
      float textScale = (float)(scale * 0.06);
      GL11.glPushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GL11.glEnable(3553);
      GlStateManager.enableTexture2D();
      GlStateManager.disableAlpha();
      if ((Boolean)this.throughWalls.getValue()) {
         GlStateManager.disableDepth();
      }

      GL11.glTranslated(x, tagY, z);
      GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScalef(-textScale, -textScale, textScale);
      Vonware.textManager.drawStringWithShadow(text, (float)(-textWidth) / 2.0F, 0.0F, -1);
      GL11.glEnable(3553);
      GlStateManager.enableTexture2D();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableBlend();
      if ((Boolean)this.throughWalls.getValue()) {
         GlStateManager.enableDepth();
      }

      GL11.glPopMatrix();
   }

   private static void tri(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x2, y2, z2);
      GL11.glVertex3f(x3, y3, z3);
   }

   private static void edge(float x1, float y1, float z1, float x2, float y2, float z2) {
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x2, y2, z2);
   }
}
