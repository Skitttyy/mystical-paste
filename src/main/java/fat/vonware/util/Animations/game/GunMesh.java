package fat.vonware.util.Animations.game;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class GunMesh {
   private GunMesh() {
   }

   public static BufferBuilder begin() {
      return begin(true);
   }

   public static BufferBuilder beginTransparent() {
      return begin(false);
   }

   private static BufferBuilder begin(boolean depthWrite) {
      GL11.glPushAttrib(287049);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      GL11.glDisable(2896);
      GL11.glDisable(2884);
      GL11.glEnable(2929);
      GL11.glDepthMask(depthWrite);
      BufferBuilder buffer = Tessellator.getInstance().getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      return buffer;
   }

   public static void finish() {
      Tessellator.getInstance().draw();
      GL11.glPopAttrib();
   }

   public static void box(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
      int front = shade(color, 0.9F);
      int back = shade(color, 0.72F);
      int top = shade(color, 1.12F);
      int bottom = shade(color, 0.54F);
      int left = shade(color, 0.72F);
      int right = shade(color, 0.98F);
      quad(buffer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, front);
      quad(buffer, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, back);
      quad(buffer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, top);
      quad(buffer, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, bottom);
      quad(buffer, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, left);
      quad(buffer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, right);
   }

   public static void taperedBoxZ(BufferBuilder buffer, double minZMinX, double minZMinY, double minZMaxX, double minZMaxY, double maxZMinX, double maxZMinY, double maxZMaxX, double maxZMaxY, double minZ, double maxZ, int color) {
      double[][] points = new double[][]{{minZMinX, minZMinY, minZ}, {minZMaxX, minZMinY, minZ}, {minZMaxX, minZMaxY, minZ}, {minZMinX, minZMaxY, minZ}, {maxZMinX, maxZMinY, maxZ}, {maxZMaxX, maxZMinY, maxZ}, {maxZMaxX, maxZMaxY, maxZ}, {maxZMinX, maxZMaxY, maxZ}};
      face(buffer, points, 0, 1, 2, 3, shade(color, 0.74F));
      face(buffer, points, 5, 4, 7, 6, shade(color, 0.88F));
      face(buffer, points, 3, 2, 6, 7, shade(color, 1.12F));
      face(buffer, points, 4, 5, 1, 0, shade(color, 0.54F));
      face(buffer, points, 4, 0, 3, 7, shade(color, 0.72F));
      face(buffer, points, 1, 5, 6, 2, shade(color, 0.98F));
   }

   public static void roundedBoxZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double radius, int cornerPoints, int color) {
      emitRoundedBoxZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, radius, cornerPoints, (double)0.0F, (double)0.0F, (double)0.0F, color);
   }

   public static void rotatedRoundedBoxZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double pivotY, double pivotZ, double angleDegrees, double radius, int cornerPoints, int color) {
      emitRoundedBoxZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, radius, cornerPoints, pivotY, pivotZ, angleDegrees, color);
   }

   private static void emitRoundedBoxZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double radius, int cornerPoints, double pivotY, double pivotZ, double angleDegrees, int color) {
      int pointsPerCorner = Math.max(2, cornerPoints);
      double maxRadius = Math.min((maxX - minX) * (double)0.5F, (maxY - minY) * (double)0.5F);
      double curveRadius = Math.max(0.001, Math.min(radius, maxRadius));
      int pointCount = pointsPerCorner * 4;
      double[][] front = new double[pointCount][3];
      double[][] back = new double[pointCount][3];
      double[] centerX = new double[]{maxX - curveRadius, maxX - curveRadius, minX + curveRadius, minX + curveRadius};
      double[] centerY = new double[]{minY + curveRadius, maxY - curveRadius, maxY - curveRadius, minY + curveRadius};
      double[] startAngle = new double[]{(double)-90.0F, (double)0.0F, (double)90.0F, (double)180.0F};
      int index = 0;

      for(int corner = 0; corner < 4; ++corner) {
         for(int point = 0; point < pointsPerCorner; ++point) {
            double fraction = (double)point / (double)(pointsPerCorner - 1);
            double radians = Math.toRadians(startAngle[corner] + fraction * (double)90.0F);
            double x = centerX[corner] + Math.cos(radians) * curveRadius;
            double y = centerY[corner] + Math.sin(radians) * curveRadius;
            front[index] = transformRoundedPoint(x, y, minZ, pivotY, pivotZ, angleDegrees);
            back[index] = transformRoundedPoint(x, y, maxZ, pivotY, pivotZ, angleDegrees);
            ++index;
         }
      }

      double centerProfileX = (minX + maxX) * (double)0.5F;
      double centerProfileY = (minY + maxY) * (double)0.5F;
      double[] frontCenter = transformRoundedPoint(centerProfileX, centerProfileY, minZ, pivotY, pivotZ, angleDegrees);
      double[] backCenter = transformRoundedPoint(centerProfileX, centerProfileY, maxZ, pivotY, pivotZ, angleDegrees);
      double width = Math.max(0.001, maxX - minX);
      double height = Math.max(0.001, maxY - minY);

      for(int i = 0; i < pointCount; ++i) {
         int next = (i + 1) % pointCount;
         double averageX = ((front[i][0] + front[next][0]) * (double)0.5F - minX) / width;
         double averageY = ((front[i][1] + front[next][1]) * (double)0.5F - minY) / height;
         float sideLight = (float)Math.max(0.48, Math.min(1.15, 0.62 + averageY * 0.28 + averageX * 0.1));
         quad(buffer, front[i][0], front[i][1], front[i][2], front[next][0], front[next][1], front[next][2], back[next][0], back[next][1], back[next][2], back[i][0], back[i][1], back[i][2], shade(color, sideLight));
         quad(buffer, frontCenter[0], frontCenter[1], frontCenter[2], front[next][0], front[next][1], front[next][2], front[i][0], front[i][1], front[i][2], frontCenter[0], frontCenter[1], frontCenter[2], shade(color, 0.88F));
         quad(buffer, backCenter[0], backCenter[1], backCenter[2], back[i][0], back[i][1], back[i][2], back[next][0], back[next][1], back[next][2], backCenter[0], backCenter[1], backCenter[2], shade(color, 0.7F));
      }

   }

   private static double[] transformRoundedPoint(double x, double y, double z, double pivotY, double pivotZ, double angleDegrees) {
      return angleDegrees == (double)0.0F ? new double[]{x, y, z} : rotateX(x, y, z, pivotY, pivotZ, angleDegrees);
   }

   public static void rotatedBoxX(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double pivotY, double pivotZ, double angleDegrees, int color) {
      double[][] points = new double[][]{rotateX(minX, minY, minZ, pivotY, pivotZ, angleDegrees), rotateX(maxX, minY, minZ, pivotY, pivotZ, angleDegrees), rotateX(maxX, maxY, minZ, pivotY, pivotZ, angleDegrees), rotateX(minX, maxY, minZ, pivotY, pivotZ, angleDegrees), rotateX(minX, minY, maxZ, pivotY, pivotZ, angleDegrees), rotateX(maxX, minY, maxZ, pivotY, pivotZ, angleDegrees), rotateX(maxX, maxY, maxZ, pivotY, pivotZ, angleDegrees), rotateX(minX, maxY, maxZ, pivotY, pivotZ, angleDegrees)};
      face(buffer, points, 0, 1, 2, 3, shade(color, 0.9F));
      face(buffer, points, 5, 4, 7, 6, shade(color, 0.72F));
      face(buffer, points, 3, 2, 6, 7, shade(color, 1.12F));
      face(buffer, points, 4, 5, 1, 0, shade(color, 0.54F));
      face(buffer, points, 4, 0, 3, 7, shade(color, 0.72F));
      face(buffer, points, 1, 5, 6, 2, shade(color, 0.98F));
   }

   public static void rotatedBoxZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double pivotX, double pivotY, double angleDegrees, int color) {
      double[][] points = new double[][]{rotateZ(minX, minY, minZ, pivotX, pivotY, angleDegrees), rotateZ(maxX, minY, minZ, pivotX, pivotY, angleDegrees), rotateZ(maxX, maxY, minZ, pivotX, pivotY, angleDegrees), rotateZ(minX, maxY, minZ, pivotX, pivotY, angleDegrees), rotateZ(minX, minY, maxZ, pivotX, pivotY, angleDegrees), rotateZ(maxX, minY, maxZ, pivotX, pivotY, angleDegrees), rotateZ(maxX, maxY, maxZ, pivotX, pivotY, angleDegrees), rotateZ(minX, maxY, maxZ, pivotX, pivotY, angleDegrees)};
      face(buffer, points, 0, 1, 2, 3, shade(color, 0.9F));
      face(buffer, points, 5, 4, 7, 6, shade(color, 0.72F));
      face(buffer, points, 3, 2, 6, 7, shade(color, 1.12F));
      face(buffer, points, 4, 5, 1, 0, shade(color, 0.54F));
      face(buffer, points, 4, 0, 3, 7, shade(color, 0.72F));
      face(buffer, points, 1, 5, 6, 2, shade(color, 0.98F));
   }

   public static void rotatedBoxY(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double pivotX, double pivotZ, double angleDegrees, int color) {
      double[][] points = new double[][]{rotateY(minX, minY, minZ, pivotX, pivotZ, angleDegrees), rotateY(maxX, minY, minZ, pivotX, pivotZ, angleDegrees), rotateY(maxX, maxY, minZ, pivotX, pivotZ, angleDegrees), rotateY(minX, maxY, minZ, pivotX, pivotZ, angleDegrees), rotateY(minX, minY, maxZ, pivotX, pivotZ, angleDegrees), rotateY(maxX, minY, maxZ, pivotX, pivotZ, angleDegrees), rotateY(maxX, maxY, maxZ, pivotX, pivotZ, angleDegrees), rotateY(minX, maxY, maxZ, pivotX, pivotZ, angleDegrees)};
      face(buffer, points, 0, 1, 2, 3, shade(color, 0.9F));
      face(buffer, points, 5, 4, 7, 6, shade(color, 0.72F));
      face(buffer, points, 3, 2, 6, 7, shade(color, 1.12F));
      face(buffer, points, 4, 5, 1, 0, shade(color, 0.54F));
      face(buffer, points, 4, 0, 3, 7, shade(color, 0.72F));
      face(buffer, points, 1, 5, 6, 2, shade(color, 0.98F));
   }

   public static void cylinderZ(BufferBuilder buffer, double centerX, double centerY, double minZ, double maxZ, double radius, int segments, int color) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         double x1 = centerX + Math.cos(angle1) * radius;
         double y1 = centerY + Math.sin(angle1) * radius;
         double x2 = centerX + Math.cos(angle2) * radius;
         double y2 = centerY + Math.sin(angle2) * radius;
         int side = shade(color, 0.72F + 0.3F * (float)Math.max((double)0.0F, Math.sin((angle1 + angle2) * (double)0.5F)));
         quad(buffer, x1, y1, minZ, x2, y2, minZ, x2, y2, maxZ, x1, y1, maxZ, side);
         quad(buffer, centerX, centerY, minZ, x2, y2, minZ, x1, y1, minZ, centerX, centerY, minZ, shade(color, 0.82F));
         quad(buffer, centerX, centerY, maxZ, x1, y1, maxZ, x2, y2, maxZ, centerX, centerY, maxZ, shade(color, 0.66F));
      }

   }

   public static void cylinderX(BufferBuilder buffer, double minX, double maxX, double centerY, double centerZ, double radius, int segments, int color) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         double y1 = centerY + Math.cos(angle1) * radius;
         double z1 = centerZ + Math.sin(angle1) * radius;
         double y2 = centerY + Math.cos(angle2) * radius;
         double z2 = centerZ + Math.sin(angle2) * radius;
         int side = shade(color, 0.7F + 0.34F * (float)Math.max((double)0.0F, Math.cos((angle1 + angle2) * (double)0.5F)));
         quad(buffer, minX, y1, z1, maxX, y1, z1, maxX, y2, z2, minX, y2, z2, side);
         quad(buffer, minX, centerY, centerZ, minX, y2, z2, minX, y1, z1, minX, centerY, centerZ, shade(color, 0.72F));
         quad(buffer, maxX, centerY, centerZ, maxX, y1, z1, maxX, y2, z2, maxX, centerY, centerZ, shade(color, 1.02F));
      }

   }

   public static void cylinderY(BufferBuilder buffer, double centerX, double minY, double maxY, double centerZ, double radius, int segments, int color) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         double x1 = centerX + Math.cos(angle1) * radius;
         double z1 = centerZ + Math.sin(angle1) * radius;
         double x2 = centerX + Math.cos(angle2) * radius;
         double z2 = centerZ + Math.sin(angle2) * radius;
         int side = shade(color, 0.7F + 0.34F * (float)Math.max((double)0.0F, Math.cos((angle1 + angle2) * (double)0.5F)));
         quad(buffer, x1, minY, z1, x2, minY, z2, x2, maxY, z2, x1, maxY, z1, side);
         quad(buffer, centerX, minY, centerZ, x2, minY, z2, x1, minY, z1, centerX, minY, centerZ, shade(color, 0.58F));
         quad(buffer, centerX, maxY, centerZ, x1, maxY, z1, x2, maxY, z2, centerX, maxY, centerZ, shade(color, 1.02F));
      }

   }

   public static void planeZ(BufferBuilder buffer, double minX, double minY, double z, double maxX, double maxY, int color) {
      quad(buffer, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z, color);
   }

   public static void ringZ(BufferBuilder buffer, double centerX, double centerY, double z, double innerRadius, double outerRadius, int segments, int color) {
      for(int i = 0; i < segments; ++i) {
         double angle1 = (Math.PI * 2D) * (double)i / (double)segments;
         double angle2 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         double outerX1 = centerX + Math.cos(angle1) * outerRadius;
         double outerY1 = centerY + Math.sin(angle1) * outerRadius;
         double outerX2 = centerX + Math.cos(angle2) * outerRadius;
         double outerY2 = centerY + Math.sin(angle2) * outerRadius;
         double innerX1 = centerX + Math.cos(angle1) * innerRadius;
         double innerY1 = centerY + Math.sin(angle1) * innerRadius;
         double innerX2 = centerX + Math.cos(angle2) * innerRadius;
         double innerY2 = centerY + Math.sin(angle2) * innerRadius;
         quad(buffer, outerX1, outerY1, z, outerX2, outerY2, z, innerX2, innerY2, z, innerX1, innerY1, z, color);
      }

   }

   public static int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }

   private static double[] rotateX(double x, double y, double z, double pivotY, double pivotZ, double degrees) {
      double radians = Math.toRadians(degrees);
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      double localY = y - pivotY;
      double localZ = z - pivotZ;
      return new double[]{x, pivotY + localY * cos - localZ * sin, pivotZ + localY * sin + localZ * cos};
   }

   private static double[] rotateY(double x, double y, double z, double pivotX, double pivotZ, double degrees) {
      double radians = Math.toRadians(degrees);
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      double localX = x - pivotX;
      double localZ = z - pivotZ;
      return new double[]{pivotX + localX * cos + localZ * sin, y, pivotZ - localX * sin + localZ * cos};
   }

   private static double[] rotateZ(double x, double y, double z, double pivotX, double pivotY, double degrees) {
      double radians = Math.toRadians(degrees);
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      double localX = x - pivotX;
      double localY = y - pivotY;
      return new double[]{pivotX + localX * cos - localY * sin, pivotY + localX * sin + localY * cos, z};
   }

   private static void face(BufferBuilder buffer, double[][] points, int first, int second, int third, int fourth, int color) {
      quad(buffer, points[first][0], points[first][1], points[first][2], points[second][0], points[second][1], points[second][2], points[third][0], points[third][1], points[third][2], points[fourth][0], points[fourth][1], points[fourth][2], color);
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

   private static int shade(int color, float factor) {
      int alpha = color >>> 24 & 255;
      int red = Math.min(255, Math.round((float)(color >> 16 & 255) * factor));
      int green = Math.min(255, Math.round((float)(color >> 8 & 255) * factor));
      int blue = Math.min(255, Math.round((float)(color & 255) * factor));
      return alpha << 24 | red << 16 | green << 8 | blue;
   }
}
