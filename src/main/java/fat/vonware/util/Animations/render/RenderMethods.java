package fat.vonware.util.Animations.render;

import fat.vonware.util.Interpolation;
import fat.vonware.util.Util;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class RenderMethods implements Util {
   public static Entity getEntity() {
      return (Entity)(mc.getRenderViewEntity() == null ? mc.player : mc.getRenderViewEntity());
   }

   public static void glColor(Color color) {
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
   }

   public static void startRender() {
      GL11.glPushAttrib(1048575);
      GL11.glPushMatrix();
      GL11.glDisable(3008);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(2884);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4353);
      GL11.glDisable(2896);
   }

   public static void endRender() {
      GL11.glEnable(2896);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDisable(3042);
      GL11.glEnable(3008);
      GL11.glDepthMask(true);
      GL11.glCullFace(1029);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
   }

   public static void drawBlockHighlightAnimation(AxisAlignedBB bbPos, Color color, boolean secondC, Color secondColor, float lineWidth, boolean outline, boolean box, int boxAlpha, boolean depth) {
      AxisAlignedBB bb = Interpolation.interpolateAxis(bbPos);
      RenderUtil.camera.setPosition(((Entity)Objects.requireNonNull(RenderUtil.mc.getRenderViewEntity())).posX, RenderUtil.mc.getRenderViewEntity().posY, RenderUtil.mc.getRenderViewEntity().posZ);
      if (RenderUtil.camera.isBoundingBoxInFrustum(bbPos)) {
         GL11.glPushMatrix();
         GL11.glPushAttrib(1048575);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(3008);
         GL11.glDisable(3553);
         GL11.glDisable(2896);
         GL11.glDisable(2884);
         if (depth) {
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
         } else {
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
         }

         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glEnable(2881);
         GL11.glHint(3155, 4354);
         GL11.glLineWidth(lineWidth);
         if (box) {
            color(new Color(color.getRed(), color.getGreen(), color.getBlue(), boxAlpha));
            fillBox(bb);
         }

         if (outline) {
            drawBlockOutline(bb, secondC ? secondColor : color, lineWidth, depth);
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glPopAttrib();
         GL11.glPopMatrix();
      }

   }

   public static void drawBlockOutline(AxisAlignedBB bb, Color color, float lineWidth, boolean depth) {
      GL11.glPushMatrix();
      GL11.glPushAttrib(1048575);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2896);
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      if (depth) {
         GL11.glEnable(2929);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(true);
      } else {
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
      }

      GL11.glLineWidth(lineWidth);
      color(color);
      fillBlockOutlineSmooth(bb);
      GL11.glDepthFunc(513);
      GL11.glPopAttrib();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   public static void fillBlockOutlineSmooth(AxisAlignedBB bb) {
      if (bb != null) {
         GL11.glBegin(2);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glEnd();
         GL11.glBegin(2);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glEnd();
         GL11.glBegin(1);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glEnd();
      }
   }

   public static void drawBox(AxisAlignedBB bb, Color color) {
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2896);
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      color(color);
      fillBox(bb);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glEnable(2896);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   public static void drawCross(AxisAlignedBB bb, float lineWidth, Color color) {
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2896);
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(lineWidth);
      color(color);
      fillCross(bb);
      GL11.glLineWidth(1.0F);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glEnable(2896);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   public static void fillCross(AxisAlignedBB bb) {
      if (bb != null) {
         GL11.glBegin(1);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glEnd();
      }

   }

   public static void drawOutline(AxisAlignedBB bb, float lineWidth, Color color) {
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2896);
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(lineWidth);
      color(color);
      fillOutline(bb);
      GL11.glLineWidth(1.0F);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glEnable(2896);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   public static void fillOutline(AxisAlignedBB bb) {
      if (bb != null) {
         GL11.glBegin(1);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
         GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
         GL11.glEnd();
      }

   }

   public static void fillBox(AxisAlignedBB boundingBox) {
      if (boundingBox != null) {
         GL11.glBegin(7);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
         GL11.glVertex3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
         GL11.glVertex3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
         GL11.glEnd();
      }

   }

   public static void color(Color color) {
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
   }

   public static void color(int color) {
      float[] color4f = ColorUtil.toArray(color);
      GL11.glColor4f(color4f[0], color4f[1], color4f[2], color4f[3]);
   }

   public static void color(float r, float g, float b, float a) {
      GL11.glColor4f(r, g, b, a);
   }
}
