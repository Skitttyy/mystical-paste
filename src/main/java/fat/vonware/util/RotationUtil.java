package fat.vonware.util;

import fat.vonware.Vonware;
import fat.vonware.features.modules.combat.Aura;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

public class RotationUtil implements Util {
   public static Vec3d getEyesPos() {
      return new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ);
   }

   public static float[] getBlockRotations(BlockPos pos, EnumFacing facing) {
      return getBlockRotations(pos, facing, mc.player);
   }

   public static float[] getBlockRotations(BlockPos pos, EnumFacing facing, Entity from) {
      return getBlockRotations(pos, facing, from, mc.world, mc.world.getBlockState(pos));
   }

   public static boolean hastRotatedYaw(float[] rot1, float[] rot2, float error) {
      float yawDifference = Math.abs(wrapAngleTo180_float(rot1[0] - rot2[0]));
      float pitchDifference = rot1[1] - rot2[1];
      return yawDifference <= error && pitchDifference <= 30.0F;
   }

   public static void packetRotate(float[] rots) {
      mc.getConnection().sendPacket(new CPacketPlayer.Rotation(rots[0], rots[1], mc.player.onGround));
   }

   public static float[] calculateLookAt(double px, double py, double pz) {
      double dirx = mc.player.posX - px;
      double diry = mc.player.posY - py;
      double dirz = mc.player.posZ - pz;
      double len = Math.sqrt(dirx * dirx + diry * diry + dirz * dirz);
      dirx /= len;
      diry /= len;
      dirz /= len;
      float pitch = (float)Math.asin(diry);
      float yaw = (float)Math.atan2(dirz, dirx);
      pitch = (float)((double)pitch * (double)180.0F / Math.PI);
      yaw = (float)((double)yaw * (double)180.0F / Math.PI);
      yaw += 90.0F;
      return new float[]{yaw, pitch};
   }

   public static float[] getRotationsFrom(double x, double y, double z, Entity from) {
      Vec3d position = from.getPositionEyes(mc.timer.renderPartialTicks);
      double deltaX = x - position.x;
      double deltaY = y - position.y;
      double deltaZ = z - position.z;
      double absDist = (double)MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - (double)90.0F);
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, absDist)));
      return new float[]{wrapAngleTo180_float(yaw), wrapAngleTo180_float(pitch)};
   }

   public static Vec3d getVec3d(float yaw, float pitch) {
      float vx = -MathHelper.sin(MathUtil.rad(yaw)) * MathHelper.cos(MathUtil.rad(pitch));
      float vz = MathHelper.cos(MathUtil.rad(yaw)) * MathHelper.cos(MathUtil.rad(pitch));
      float vy = -MathHelper.sin(MathUtil.rad(pitch));
      return new Vec3d((double)vx, (double)vy, (double)vz);
   }

   public static float wrapAngleTo180_float(float value) {
      value %= 360.0F;
      if (value >= 180.0F) {
         value -= 360.0F;
      }

      if (value < -180.0F) {
         value += 360.0F;
      }

      return value;
   }

   public static float[] getBlockRotations(BlockPos pos, EnumFacing facing, Entity from, IBlockAccess world, IBlockState state) {
      AxisAlignedBB bb = state.getBoundingBox(world, pos);
      double x = (double)pos.getX() + (bb.minX + bb.maxX) / (double)2.0F;
      double y = (double)pos.getY() + (bb.minY + bb.maxY) / (double)2.0F;
      double z = (double)pos.getZ() + (bb.minZ + bb.maxZ) / (double)2.0F;
      if (facing != null) {
         x += (double)facing.getDirectionVec().getX() * ((bb.minX + bb.maxX) / (double)2.0F);
         y += (double)facing.getDirectionVec().getY() * ((bb.minY + bb.maxY) / (double)2.0F);
         z += (double)facing.getDirectionVec().getZ() * ((bb.minZ + bb.maxZ) / (double)2.0F);
      }

      return getRotations(x, y, z, from);
   }

   public static float[] getRotations(BlockPos pos) {
      return getRotations((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
   }

   public static float[] getRotations(BlockPos pos, EnumFacing facing) {
      return getRotations(pos, facing, mc.player);
   }

   public static float[] getRotations(BlockPos pos, EnumFacing facing, Entity from) {
      return getRotations(pos, facing, from, mc.world, mc.world.getBlockState(pos));
   }

   public static float[] getLegitRotationsServer(Vec3d vec) {
      Vec3d eyesPos = getEyesPos();
      float serverYaw = Vonware.rotationManager.getServerYaw();
      float serverPitch = Vonware.rotationManager.getServerPitch();
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{serverYaw + MathHelper.wrapDegrees(yaw - serverYaw), serverPitch + MathHelper.wrapDegrees(pitch - serverPitch)};
   }

   public static float[] getLegitRotations(Vec3d vec) {
      Vec3d eyesPos = getEyesPos();
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{mc.player.rotationYaw + MathHelper.wrapDegrees(yaw - mc.player.rotationYaw), mc.player.rotationPitch + MathHelper.wrapDegrees(pitch - mc.player.rotationPitch)};
   }

   public static float[] getRotations(BlockPos pos, EnumFacing facing, Entity from, IBlockAccess world, IBlockState state) {
      AxisAlignedBB bb = state.getBoundingBox(world, pos);
      double x = (double)pos.getX() + (bb.minX + bb.maxX) / (double)2.0F;
      double y = (double)pos.getY() + (bb.minY + bb.maxY) / (double)2.0F;
      double z = (double)pos.getZ() + (bb.minZ + bb.maxZ) / (double)2.0F;
      if (facing != null) {
         x += (double)facing.getDirectionVec().getX() * ((bb.minX + bb.maxX) / (double)2.0F);
         y += (double)facing.getDirectionVec().getY() * ((bb.minY + bb.maxY) / (double)2.0F);
         z += (double)facing.getDirectionVec().getZ() * ((bb.minZ + bb.maxZ) / (double)2.0F);
      }

      return getRotations(x, y, z, from);
   }

   public static float[] getRotations(double x, double y, double z, Entity f) {
      return getRotations(x, y, z, f.posX, f.posY, f.posZ, f.getEyeHeight());
   }

   public static float[] getRotations(double x, double y, double z, double fromX, double fromY, double fromZ, float fromHeight) {
      double xDiff = x - fromX;
      double yDiff = y - (fromY + (double)fromHeight);
      double zDiff = z - fromZ;
      double dist = (double)MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);
      float yaw = (float)(Math.atan2(zDiff, xDiff) * (double)180.0F / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(yDiff, dist) * (double)180.0F / Math.PI));
      float prevYaw = mc.player.prevCameraYaw;
      float diff = yaw - prevYaw;
      if (diff < -180.0F || diff > 180.0F) {
         float round = (float)Math.round(Math.abs(diff / 360.0F));
         diff = diff < 0.0F ? diff + 360.0F * round : diff - 360.0F * round;
      }

      return new float[]{prevYaw + diff, pitch};
   }

   public static CPacketPlayer getRotationPacket(float[] rots) {
      boolean onGround = mc.player.onGround;
      return (CPacketPlayer)(!(Math.abs(mc.player.motionX) > (double)0.0F) && !(Math.abs(mc.player.motionY) > (double)0.0F) && !(Math.abs(mc.player.motionZ) > (double)0.0F) ? new CPacketPlayer.Rotation(rots[0], rots[1], onGround) : new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY, mc.player.posZ, rots[0], rots[1], onGround));
   }

   public static float[] getRotationsAtLocation(Aura.Location location, Entity entity) {
      double positionX = entity.posX - mc.player.posX;
      double positionZ = entity.posZ - mc.player.posZ;
      double locationMath = (double)0.0F;
      switch (location) {
         case Head:
            locationMath = (double)1.0F;
            break;
         case Chest:
            locationMath = 1.3;
            break;
         case Legs:
            locationMath = 2.9;
            break;
         case Feet:
            locationMath = (double)4.0F;
            break;
         default:
            locationMath = 1.3;
      }

      double positionY = entity.posY + (double)entity.getEyeHeight() / locationMath - (mc.player.posY + (double)mc.player.getEyeHeight());
      double positions = (double)MathHelper.sqrt(positionX * positionX + positionZ * positionZ);
      float yaw = (float)(Math.atan2(positionZ, positionX) * (double)180.0F / Math.PI - (double)90.0F);
      float pitch = (float)(-(Math.atan2(positionY, positions) * (double)180.0F / Math.PI));
      return new float[]{yaw, pitch};
   }

   public static float[] getRotations(double x, double y, double z) {
      double xDiff = x - mc.player.posX;
      double yDiff = y - EntityUtil.getEyeHeight(mc.player);
      double zDiff = z - mc.player.posZ;
      double dist = (double)MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);
      float yaw = (float)(Math.atan2(zDiff, xDiff) * (double)180.0F / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(yDiff, dist) * (double)180.0F / Math.PI));
      float diff = yaw - mc.player.rotationYaw;
      if (diff < -180.0F || diff > 180.0F) {
         float round = (float)Math.round(Math.abs(diff / 360.0F));
         diff = diff < 0.0F ? diff + 360.0F * round : diff - 360.0F * round;
      }

      return new float[]{mc.player.rotationYaw + diff, pitch};
   }

   public static void facePos(BlockPos blockPos) {
      EnumFacing enumFacing = BlockUtil.getFirstFacing(blockPos);
      if (enumFacing != null) {
         BlockPos blockPos2 = blockPos.offset(enumFacing);
         EnumFacing enumFacing2 = enumFacing.getOpposite();
         Vec3d vec3d = (new Vec3d(blockPos2)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(enumFacing2.getDirectionVec())).scale((double)0.5F));
         faceVector(vec3d, true);
      }
   }

   public static void faceYawAndPitch(float yaw, float pitch) {
      mc.player.connection.sendPacket(new CPacketPlayer.Rotation(yaw, pitch, mc.player.onGround));
   }

   public static void faceVector(Vec3d vec, boolean normalizeAngle) {
      float[] rotations = getLegitRotations(vec);
      mc.player.connection.sendPacket(new CPacketPlayer.Rotation(rotations[0], normalizeAngle ? (float)MathHelper.normalizeAngle((int)rotations[1], 360) : rotations[1], mc.player.onGround));
   }
}
