package fat.vonware.util;

import java.nio.FloatBuffer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Interpolation implements Util {
   public static void applyPerspective(float fovy, float aspect, float zNear, float zFar) {
      float ymax = (float)((double)zNear * Math.tan((double)fovy * Math.PI / (double)360.0F));
      float ymin = -ymax;
      float xmin = ymin * aspect;
      float xmax = ymax * aspect;
      applyFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
   }

   public static void applyFrustum(float left, float right, float bottom, float top, float zNear, float zFar) {
      FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
      float a = (right + left) / (right - left);
      float b = (top + bottom) / (top - bottom);
      float c = -(zFar + zNear) / (zFar - zNear);
      float d = -(2.0F * zFar * zNear) / (zFar - zNear);
      float[] m = new float[]{2.0F * zNear / (right - left), 0.0F, 0.0F, 0.0F, 0.0F, 2.0F * zNear / (top - bottom), 0.0F, 0.0F, a, b, c, -1.0F, 0.0F, 0.0F, d, 0.0F};
      matrix.put(m).flip();
      GL11.glMultMatrix(matrix);
   }

   public static Vec3d interpolateEntity(Entity entity) {
      double x = interpolateLastTickPos(entity.posX, entity.lastTickPosX) - getRenderPosX();
      double y = interpolateLastTickPos(entity.posY, entity.lastTickPosY) - getRenderPosY();
      double z = interpolateLastTickPos(entity.posZ, entity.lastTickPosZ) - getRenderPosZ();
      return new Vec3d(x, y, z);
   }

   public static boolean isInCamera(Frustum frustum, AxisAlignedBB bb) {
      return frustum.isBoundingBoxInFrustum(new AxisAlignedBB(bb.minX + mc.getRenderManager().viewerPosX, bb.minY + mc.getRenderManager().viewerPosY, bb.minZ + mc.getRenderManager().viewerPosZ, bb.maxX + mc.getRenderManager().viewerPosX, bb.maxY + mc.getRenderManager().viewerPosY, bb.maxZ + mc.getRenderManager().viewerPosZ));
   }

   public static double interpolateLastTickPos(double pos, double lastTickPos) {
      return lastTickPos + (pos - lastTickPos) * (double)mc.getRenderPartialTicks();
   }

   public static AxisAlignedBB interpolatePos(BlockPos pos) {
      return interpolatePos(pos, 1.0F);
   }

   public static AxisAlignedBB interpolatePos(BlockPos pos, float height) {
      return new AxisAlignedBB((double)pos.getX() - mc.getRenderManager().viewerPosX, (double)pos.getY() - mc.getRenderManager().viewerPosY, (double)pos.getZ() - mc.getRenderManager().viewerPosZ, (double)pos.getX() - mc.getRenderManager().viewerPosX + (double)1.0F, (double)pos.getY() - mc.getRenderManager().viewerPosY + (double)height, (double)pos.getZ() - mc.getRenderManager().viewerPosZ + (double)1.0F);
   }

   public static AxisAlignedBB interpolateAxis(AxisAlignedBB bb) {
      return new AxisAlignedBB(bb.minX - mc.getRenderManager().viewerPosX, bb.minY - mc.getRenderManager().viewerPosY, bb.minZ - mc.getRenderManager().viewerPosZ, bb.maxX - mc.getRenderManager().viewerPosX, bb.maxY - mc.getRenderManager().viewerPosY, bb.maxZ - mc.getRenderManager().viewerPosZ);
   }

   public static double getRenderPosX() {
      return mc.getRenderManager().renderPosX;
   }

   public static double getRenderPosY() {
      return mc.getRenderManager().renderPosY;
   }

   public static double getRenderPosZ() {
      return mc.getRenderManager().renderPosZ;
   }

   public static Frustum createFrustum(Entity entity) {
      Frustum frustum = new Frustum();
      setFrustum(frustum, entity);
      return frustum;
   }

   public static void setFrustum(Frustum frustum, Entity entity) {
      double x = interpolateLastTickPos(entity.posX, entity.lastTickPosX);
      double y = interpolateLastTickPos(entity.posY, entity.lastTickPosY);
      double z = interpolateLastTickPos(entity.posZ, entity.lastTickPosZ);
      frustum.setPosition(x, y, z);
   }
}
