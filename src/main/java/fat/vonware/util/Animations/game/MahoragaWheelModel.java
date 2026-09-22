package fat.vonware.util.Animations.game;

import java.awt.Color;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public final class MahoragaWheelModel {
   private static final double TWO_PI = (Math.PI * 2D);
   private static final int RIM_SEGMENTS = 96;
   private static final int MEDALLION_SEGMENTS = 32;
   private static final int HUB_SPHERE_STACKS = 28;
   private static final int HUB_SPHERE_SLICES = 56;
   private static final int HANDLE_SPHERE_STACKS = 24;
   private static final int HANDLE_SPHERE_SLICES = 48;

   public void render(Color primaryColor, Color highlightColor, float pulse, boolean frontView) {
      int gold = toArgb(primaryColor);
      int highlight = toArgb(highlightColor);
      int warmGold = mix(gold, highlight, 0.34F);
      int bronze = shade(gold, 0.59F);
      int deepBronze = shade(gold, 0.31F);
      int recess = shade(gold, 0.18F);
      int edge = mix(bronze, highlight, 0.24F);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

      for(int i = 0; i < 8; ++i) {
         double angle = (double)i * Math.PI / (double)4.0F;
         addRadialPrism(buffer, angle, 0.17, 0.705, 0.045, 0.066, -0.065, 0.065, warmGold, bronze, deepBronze);
         addRadialPrism(buffer, angle, 0.755, 1.035, 0.052, 0.078, -0.075, 0.075, gold, bronze, deepBronze);
         addRadialPrism(buffer, angle, 0.885, 1.015, 0.084, 0.105, -0.088, 0.088, edge, bronze, deepBronze);
      }

      addAnnulusPrism(buffer, 0.665, 0.805, 0.105, 96, gold, shade(gold, 0.73F), bronze, deepBronze);
      if (frontView) {
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.108, 0.676, 0.699, 96, deepBronze);
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.11, 0.705, 0.76, 96, warmGold);
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.112, 0.766, 0.794, 96, highlight);
      }

      for(int i = 0; i < 8; ++i) {
         double angle = (double)i * Math.PI / (double)4.0F;
         if (frontView) {
            addRadialPrism(buffer, angle, 0.646, 0.824, 0.071, 0.071, 0.106, 0.13, edge, bronze, deepBronze);
            addSpokeEngraving(buffer, angle, recess, highlight);
         }

         addHandleMedallion(buffer, angle, gold, highlight, bronze, deepBronze, recess, frontView);
      }

      addHub(buffer, gold, highlight, bronze, deepBronze, recess, frontView);
      tessellator.draw();
      if (frontView) {
         drawEngravedOutlines(tessellator, buffer, highlight, edge, deepBronze);
         drawPulseGlow(tessellator, buffer, highlight, pulse);
      }

   }

   private static void addHub(BufferBuilder buffer, int gold, int highlight, int bronze, int deepBronze, int recess, boolean frontView) {
      addSphere(buffer, (double)0.0F, (double)0.0F, (double)0.0F, 0.245, 28, 56, gold, highlight, deepBronze);
      if (frontView) {
         GunMesh.cylinderZ(buffer, (double)0.0F, (double)0.0F, 0.208, 0.232, 0.17, 32, gold);
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.235, 0.124, 0.162, 32, highlight);
         GunMesh.cylinderZ(buffer, (double)0.0F, (double)0.0F, 0.232, 0.252, 0.112, 32, bronze);
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.255, 0.052, 0.101, 32, gold);
         GunMesh.cylinderZ(buffer, (double)0.0F, (double)0.0F, 0.252, 0.266, 0.047, 24, recess);

         for(int i = 0; i < 8; ++i) {
            double angle = (double)i * Math.PI / (double)4.0F;
            double x = Math.cos(angle) * 0.174;
            double y = Math.sin(angle) * 0.174;
            GunMesh.cylinderZ(buffer, x, y, 0.226, (double)0.25F, 0.018, 12, i % 2 == 0 ? highlight : gold);
         }

         for(int i = 0; i < 4; ++i) {
            addOrientedPrism(buffer, (double)i * Math.PI / (double)4.0F, (double)0.0F, (double)0.0F, 0.052, 0.01, 0.266, 0.275, deepBronze, recess, recess);
         }

      }
   }

   private static void addHandleMedallion(BufferBuilder buffer, double angle, int gold, int highlight, int bronze, int deepBronze, int recess, boolean frontView) {
      double centerRadius = 1.155;
      double x = Math.cos(angle) * centerRadius;
      double y = Math.sin(angle) * centerRadius;
      addSphere(buffer, x, y, (double)0.0F, 0.205, 24, 48, gold, highlight, deepBronze);
      if (frontView) {
         GunMesh.cylinderZ(buffer, x, y, 0.175, 0.199, 0.135, 32, gold);
         GunMesh.ringZ(buffer, x, y, 0.202, 0.096, 0.127, 32, highlight);
         GunMesh.cylinderZ(buffer, x, y, 0.199, 0.218, 0.086, 32, bronze);
         GunMesh.ringZ(buffer, x, y, 0.221, 0.042, 0.077, 32, gold);
         GunMesh.cylinderZ(buffer, x, y, 0.218, 0.231, 0.037, 20, recess);
         addOrientedPrism(buffer, angle + (Math.PI / 2D), x, y, 0.08, 0.017, 0.231, 0.242, highlight, bronze, deepBronze);
      }
   }

   private static void addSpokeEngraving(BufferBuilder buffer, double angle, int recess, int highlight) {
      addRadialPrism(buffer, angle, 0.292, 0.62, 0.01, 0.014, 0.066, 0.074, recess, recess, recess);
      double tangentX = -Math.sin(angle) * 0.03;
      double tangentY = Math.cos(angle) * 0.03;
      addRadialFace(buffer, angle, 0.305, (double)0.625F, 0.008, tangentX, tangentY, 0.076, highlight);
   }

   private static void drawEngravedOutlines(Tessellator tessellator, BufferBuilder buffer, int highlight, int edge, int deepBronze) {
      buffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
      addCircleOutline(buffer, 0.805, 0.132, 96, highlight);
      addCircleOutline(buffer, 0.665, 0.132, 96, deepBronze);
      addCircleOutline(buffer, 0.245, 0.277, 48, edge);
      addCircleOutline(buffer, 0.112, 0.278, 40, highlight);

      for(int i = 0; i < 8; ++i) {
         double angle = (double)i * Math.PI / (double)4.0F;
         double x = Math.cos(angle) * 1.155;
         double y = Math.sin(angle) * 1.155;
         addCircleOutline(buffer, x, y, 0.205, 0.244, 32, edge);
         addCircleOutline(buffer, x, y, 0.086, 0.245, 32, highlight);
         addLine(buffer, Math.cos(angle) * 0.245, Math.sin(angle) * 0.245, 0.215, Math.cos(angle) * 0.652, Math.sin(angle) * 0.652, 0.215, edge);
      }

      tessellator.draw();
   }

   private static void drawPulseGlow(Tessellator tessellator, BufferBuilder buffer, int highlight, float pulse) {
      if (!(pulse <= 0.01F)) {
         int alpha = Math.min(105, Math.max(0, Math.round(105.0F * pulse)));
         int glow = withAlpha(highlight, alpha);
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         GunMesh.ringZ(buffer, (double)0.0F, (double)0.0F, 0.15, 0.805, 0.835 + (double)pulse * 0.025, 96, glow);

         for(int i = 0; i < 8; ++i) {
            double angle = (double)i * Math.PI / (double)4.0F;
            double x = Math.cos(angle) * 1.155;
            double y = Math.sin(angle) * 1.155;
            GunMesh.ringZ(buffer, x, y, 0.252, 0.205, 0.222 + (double)pulse * 0.012, 32, glow);
         }

         tessellator.draw();
      }
   }

   private static void addAnnulusPrism(BufferBuilder buffer, double innerRadius, double outerRadius, double halfDepth, int segments, int frontColor, int backColor, int outerColor, int innerColor) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         double outerX1 = Math.cos(angle1) * outerRadius;
         double outerY1 = Math.sin(angle1) * outerRadius;
         double outerX2 = Math.cos(angle2) * outerRadius;
         double outerY2 = Math.sin(angle2) * outerRadius;
         double innerX1 = Math.cos(angle1) * innerRadius;
         double innerY1 = Math.sin(angle1) * innerRadius;
         double innerX2 = Math.cos(angle2) * innerRadius;
         double innerY2 = Math.sin(angle2) * innerRadius;
         quad(buffer, outerX1, outerY1, halfDepth, outerX2, outerY2, halfDepth, innerX2, innerY2, halfDepth, innerX1, innerY1, halfDepth, frontColor);
         quad(buffer, innerX1, innerY1, -halfDepth, innerX2, innerY2, -halfDepth, outerX2, outerY2, -halfDepth, outerX1, outerY1, -halfDepth, backColor);
         quad(buffer, outerX1, outerY1, -halfDepth, outerX2, outerY2, -halfDepth, outerX2, outerY2, halfDepth, outerX1, outerY1, halfDepth, outerColor);
         quad(buffer, innerX2, innerY2, -halfDepth, innerX1, innerY1, -halfDepth, innerX1, innerY1, halfDepth, innerX2, innerY2, halfDepth, innerColor);
      }

   }

   private static void addSphere(BufferBuilder buffer, double centerX, double centerY, double centerZ, double radius, int stacks, int slices, int baseColor, int highlightColor, int shadowColor) {
      for(int stack = 0; stack < stacks; ++stack) {
         double latitude1 = (-Math.PI / 2D) + Math.PI * (double)stack / (double)stacks;
         double latitude2 = (-Math.PI / 2D) + Math.PI * (double)(stack + 1) / (double)stacks;
         double cosLatitude1 = Math.cos(latitude1);
         double cosLatitude2 = Math.cos(latitude2);
         double sinLatitude1 = Math.sin(latitude1);
         double sinLatitude2 = Math.sin(latitude2);
         double ring1 = cosLatitude1 * radius;
         double ring2 = cosLatitude2 * radius;
         double z1 = centerZ + sinLatitude1 * radius;
         double z2 = centerZ + sinLatitude2 * radius;

         for(int slice = 0; slice < slices; ++slice) {
            double angle1 = (Math.PI * 2D) * (double)slice / (double)slices;
            double angle2 = (Math.PI * 2D) * (double)(slice + 1) / (double)slices;
            double cosAngle1 = Math.cos(angle1);
            double cosAngle2 = Math.cos(angle2);
            double sinAngle1 = Math.sin(angle1);
            double sinAngle2 = Math.sin(angle2);
            vertex(buffer, centerX + cosAngle1 * ring1, centerY + sinAngle1 * ring1, z1, sphereColor(cosLatitude1 * cosAngle1, cosLatitude1 * sinAngle1, sinLatitude1, baseColor, highlightColor, shadowColor));
            vertex(buffer, centerX + cosAngle2 * ring1, centerY + sinAngle2 * ring1, z1, sphereColor(cosLatitude1 * cosAngle2, cosLatitude1 * sinAngle2, sinLatitude1, baseColor, highlightColor, shadowColor));
            vertex(buffer, centerX + cosAngle2 * ring2, centerY + sinAngle2 * ring2, z2, sphereColor(cosLatitude2 * cosAngle2, cosLatitude2 * sinAngle2, sinLatitude2, baseColor, highlightColor, shadowColor));
            vertex(buffer, centerX + cosAngle1 * ring2, centerY + sinAngle1 * ring2, z2, sphereColor(cosLatitude2 * cosAngle1, cosLatitude2 * sinAngle1, sinLatitude2, baseColor, highlightColor, shadowColor));
         }
      }

   }

   private static int sphereColor(double normalX, double normalY, double normalZ, int baseColor, int highlightColor, int shadowColor) {
      double light = normalX * -0.3 + normalY * 0.42 + normalZ * 0.85;
      double diffuse = Math.max((double)0.0F, Math.min((double)1.0F, (light + (double)1.0F) * (double)0.5F));
      int litColor = mix(shadowColor, baseColor, (float)(0.18 + diffuse * 0.76));
      double positiveLight = Math.max((double)0.0F, light);
      double squaredLight = positiveLight * positiveLight;
      float specular = (float)(0.025 + squaredLight * squaredLight * positiveLight * 0.55);
      return mix(litColor, highlightColor, specular);
   }

   private static void addRadialPrism(BufferBuilder buffer, double angle, double innerRadius, double outerRadius, double innerHalfWidth, double outerHalfWidth, double minZ, double maxZ, int frontColor, int backColor, int sideColor) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double tangentX = -sin;
      double innerX = cos * innerRadius;
      double innerY = sin * innerRadius;
      double outerX = cos * outerRadius;
      double outerY = sin * outerRadius;
      addPolygonPrism(buffer, innerX + tangentX * innerHalfWidth, innerY + cos * innerHalfWidth, outerX + tangentX * outerHalfWidth, outerY + cos * outerHalfWidth, outerX - tangentX * outerHalfWidth, outerY - cos * outerHalfWidth, innerX - tangentX * innerHalfWidth, innerY - cos * innerHalfWidth, minZ, maxZ, frontColor, backColor, sideColor);
   }

   private static void addOrientedPrism(BufferBuilder buffer, double angle, double centerX, double centerY, double halfLength, double halfWidth, double minZ, double maxZ, int frontColor, int backColor, int sideColor) {
      double forwardX = Math.cos(angle) * halfLength;
      double forwardY = Math.sin(angle) * halfLength;
      double sideX = -Math.sin(angle) * halfWidth;
      double sideY = Math.cos(angle) * halfWidth;
      addPolygonPrism(buffer, centerX - forwardX + sideX, centerY - forwardY + sideY, centerX + forwardX + sideX, centerY + forwardY + sideY, centerX + forwardX - sideX, centerY + forwardY - sideY, centerX - forwardX - sideX, centerY - forwardY - sideY, minZ, maxZ, frontColor, backColor, sideColor);
   }

   private static void addPolygonPrism(BufferBuilder buffer, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double minZ, double maxZ, int frontColor, int backColor, int sideColor) {
      quad(buffer, x1, y1, maxZ, x2, y2, maxZ, x3, y3, maxZ, x4, y4, maxZ, frontColor);
      quad(buffer, x4, y4, minZ, x3, y3, minZ, x2, y2, minZ, x1, y1, minZ, backColor);
      quad(buffer, x1, y1, minZ, x2, y2, minZ, x2, y2, maxZ, x1, y1, maxZ, sideColor);
      quad(buffer, x2, y2, minZ, x3, y3, minZ, x3, y3, maxZ, x2, y2, maxZ, sideColor);
      quad(buffer, x3, y3, minZ, x4, y4, minZ, x4, y4, maxZ, x3, y3, maxZ, sideColor);
      quad(buffer, x4, y4, minZ, x1, y1, minZ, x1, y1, maxZ, x4, y4, maxZ, sideColor);
   }

   private static void addRadialFace(BufferBuilder buffer, double angle, double innerRadius, double outerRadius, double halfWidth, double offsetX, double offsetY, double z, int color) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double tx = -sin * halfWidth;
      double ty = cos * halfWidth;
      double innerX = cos * innerRadius + offsetX;
      double innerY = sin * innerRadius + offsetY;
      double outerX = cos * outerRadius + offsetX;
      double outerY = sin * outerRadius + offsetY;
      quad(buffer, innerX + tx, innerY + ty, z, outerX + tx, outerY + ty, z, outerX - tx, outerY - ty, z, innerX - tx, innerY - ty, z, color);
   }

   private static void addCircleOutline(BufferBuilder buffer, double radius, double z, int segments, int color) {
      addCircleOutline(buffer, (double)0.0F, (double)0.0F, radius, z, segments, color);
   }

   private static void addCircleOutline(BufferBuilder buffer, double centerX, double centerY, double radius, double z, int segments, int color) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         addLine(buffer, centerX + Math.cos(angle1) * radius, centerY + Math.sin(angle1) * radius, z, centerX + Math.cos(angle2) * radius, centerY + Math.sin(angle2) * radius, z, color);
      }

   }

   private static void addLine(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      vertex(buffer, x1, y1, z1, color);
      vertex(buffer, x2, y2, z2, color);
   }

   private static void quad(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
      vertex(buffer, x1, y1, z1, color);
      vertex(buffer, x2, y2, z2, color);
      vertex(buffer, x3, y3, z3, color);
      vertex(buffer, x4, y4, z4, color);
   }

   private static void vertex(BufferBuilder buffer, double x, double y, double z, int color) {
      buffer.pos(x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24 & 255).endVertex();
   }

   private static int toArgb(Color color) {
      return color.getAlpha() << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   private static int shade(int color, float factor) {
      int alpha = color >>> 24 & 255;
      int red = Math.min(255, Math.max(0, Math.round((float)(color >> 16 & 255) * factor)));
      int green = Math.min(255, Math.max(0, Math.round((float)(color >> 8 & 255) * factor)));
      int blue = Math.min(255, Math.max(0, Math.round((float)(color & 255) * factor)));
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private static int mix(int first, int second, float amount) {
      float clamped = Math.max(0.0F, Math.min(1.0F, amount));
      int alpha = Math.round((float)channel(first, 24) + (float)(channel(second, 24) - channel(first, 24)) * clamped);
      int red = Math.round((float)channel(first, 16) + (float)(channel(second, 16) - channel(first, 16)) * clamped);
      int green = Math.round((float)channel(first, 8) + (float)(channel(second, 8) - channel(first, 8)) * clamped);
      int blue = Math.round((float)channel(first, 0) + (float)(channel(second, 0) - channel(first, 0)) * clamped);
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private static int channel(int color, int shift) {
      return color >> shift & 255;
   }

   private static int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }
}
