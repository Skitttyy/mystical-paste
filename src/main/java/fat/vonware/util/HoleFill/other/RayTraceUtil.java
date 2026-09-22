package fat.vonware.util.HoleFill.other;

import fat.vonware.Vonware;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Util;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.IBlockAccess;

public class RayTraceUtil implements Util {
   private static final EnumFacing[] T;
   private static final EnumFacing[] B;
   private static final EnumFacing[] S;

   private RayTraceUtil() {
      throw new AssertionError();
   }

   public static RayTraceResult rayTraceTo(BlockPos pos, IBlockAccess world) {
      return rayTraceTo(pos, world, (block, blockPos) -> blockPos.equals(pos));
   }

   public static RayTraceResult rayTraceTo(BlockPos pos, IBlockAccess world, BiPredicate<Block, BlockPos> check) {
      Entity from = mc.player;
      float yaw = Vonware.rotationManager.getServerYaw();
      float pitch = Vonware.rotationManager.getServerPitch();
      Vec3d start = Vonware.positionManager.getVec().add((double)0.0F, (double)from.getEyeHeight(), (double)0.0F);
      Vec3d look = RotationUtil.getVec3d(yaw, pitch);
      double distance = from.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) + (double)1.0F;
      Vec3d end = start.add(look.x * distance, look.y * distance, look.z * distance);
      return RayTracer.trace(mc.world, world, start, end, true, false, true, check);
   }

   public static RayTraceResult getRayTraceResult(float yaw, float pitch) {
      return getRayTraceResult(yaw, pitch, mc.playerController.getBlockReachDistance());
   }

   public static RayTraceResult getRayTraceResult(float yaw, float pitch, float distance) {
      Vec3d vec3d = PositionUtil.getEyesPos();
      Vec3d lookVec = getVec3d(yaw, pitch);
      Vec3d rotations = vec3d.add(lookVec.x * (double)distance, lookVec.y * (double)distance, lookVec.z * (double)distance);
      return (RayTraceResult)Optional.ofNullable(mc.world.rayTraceBlocks(vec3d, rotations, false, false, false)).orElseGet(() -> new RayTraceResult(Type.MISS, new Vec3d((double)0.5F, (double)1.0F, (double)0.5F), EnumFacing.UP, BlockPos.ORIGIN));
   }

   public static Vec3d getVec3d(float yaw, float pitch) {
      float vx = -MathHelper.sin(MathUtil.rad(yaw)) * MathHelper.cos(MathUtil.rad(pitch));
      float vz = MathHelper.cos(MathUtil.rad(yaw)) * MathHelper.cos(MathUtil.rad(pitch));
      float vy = -MathHelper.sin(MathUtil.rad(pitch));
      return new Vec3d((double)vx, (double)vy, (double)vz);
   }

   public static Ray rayTrace(Entity from, BlockPos on, EnumFacing facing, IBlockAccess access, IBlockState state, double res) {
      Vec3d start = PlayerUtil.getEyePos(from);
      AxisAlignedBB bb = state.getBoundingBox(access, on);
      if (res >= (double)1.0F) {
         float[] r = rots(on, facing, from, access, state);
         Vec3d look = RotationUtil.getVec3d(r[0], r[1]);
         double d = (double)mc.playerController.getBlockReachDistance();
         Vec3d rotations = start.add(look.x * d, look.y * d, look.z * d);
         RayTraceResult result = RayTracer.trace(mc.world, access, start, rotations, false, false, true);
         return result != null && result.sideHit == facing && on.equals(result.getBlockPos()) ? (new Ray(result, r, on, facing, (Vec3d)null)).setLegit(true) : dumbRay(on, facing, r);
      } else {
         Vec3i dirVec = facing.getDirectionVec();
         double dirX = dirVec.getX() < 0 ? bb.minX : (double)dirVec.getX() * bb.maxX;
         double dirY = dirVec.getY() < 0 ? bb.minY : (double)dirVec.getY() * bb.maxY;
         double dirZ = dirVec.getZ() < 0 ? bb.minZ : (double)dirVec.getZ() * bb.maxZ;
         double minX = (double)on.getX() + dirX + (dirVec.getX() == 0 ? bb.minX : (double)0.0F);
         double minY = (double)on.getY() + dirY + (dirVec.getY() == 0 ? bb.minY : (double)0.0F);
         double minZ = (double)on.getZ() + dirZ + (dirVec.getZ() == 0 ? bb.minZ : (double)0.0F);
         double maxX = (double)on.getX() + dirX + (dirVec.getX() == 0 ? bb.maxX : (double)0.0F);
         double maxY = (double)on.getY() + dirY + (dirVec.getY() == 0 ? bb.maxY : (double)0.0F);
         double maxZ = (double)on.getZ() + dirZ + (dirVec.getZ() == 0 ? bb.maxZ : (double)0.0F);
         boolean xEq = Double.compare(minX, maxX) == 0;
         boolean yEq = Double.compare(minY, maxY) == 0;
         boolean zEq = Double.compare(minZ, maxZ) == 0;
         if (xEq) {
            minX = maxX = minX - (double)dirVec.getX() * 5.0E-4;
         }

         if (yEq) {
            minY = maxY = minY - (double)dirVec.getY() * 5.0E-4;
         }

         if (zEq) {
            minZ = maxZ = minZ - (double)dirVec.getZ() * 5.0E-4;
         }

         double endX = Math.max(minX, maxX) - (xEq ? (double)0.0F : 5.0E-4);
         double endY = Math.max(minY, maxY) - (yEq ? (double)0.0F : 5.0E-4);
         double endZ = Math.max(minZ, maxZ) - (zEq ? (double)0.0F : 5.0E-4);
         if (res <= (double)0.0F) {
            double staX = Math.min(minX, maxX) + (xEq ? (double)0.0F : 5.0E-4);
            double staY = Math.min(minY, maxY) + (yEq ? (double)0.0F : 5.0E-4);
            double staZ = Math.min(minZ, maxZ) + (zEq ? (double)0.0F : 5.0E-4);
            Set<Vec3d> vectors = new HashSet();
            vectors.add(new Vec3d(staX, staY, staZ));
            vectors.add(new Vec3d(staX, staY, endZ));
            vectors.add(new Vec3d(staX, endY, staZ));
            vectors.add(new Vec3d(staX, endY, endZ));
            vectors.add(new Vec3d(endX, staY, staZ));
            vectors.add(new Vec3d(endX, staY, endZ));
            vectors.add(new Vec3d(endX, endY, staZ));
            vectors.add(new Vec3d(endX, endY, endZ));
            double x = (endX - staX) / (double)2.0F + staX;
            double y = (endY - staY) / (double)2.0F + staY;
            double z = (endZ - staZ) / (double)2.0F + staZ;
            vectors.add(new Vec3d(x, y, z));

            for(Vec3d vec : vectors) {
               RayTraceResult ray = RayTracer.trace(mc.world, access, start, vec, false, false, true);
               if (ray != null && on.equals(ray.getBlockPos()) && facing == ray.sideHit) {
                  return (new Ray(ray, rots(from, vec), on, facing, vec)).setLegit(true);
               }
            }

            return dumbRay(on, facing, rots(on, facing, from, access, state));
         } else {
            for(double x2 = Math.min(minX, maxX); x2 <= endX; x2 += res) {
               for(double y2 = Math.min(minY, maxY); y2 <= endY; y2 += res) {
                  for(double z2 = Math.min(minZ, maxZ); z2 <= endZ; z2 += res) {
                     Vec3d vector = new Vec3d(x2, y2, z2);
                     RayTraceResult ray2 = RayTracer.trace(mc.world, access, start, vector, false, false, true);
                     if (ray2 != null && facing == ray2.sideHit && on.equals(ray2.getBlockPos())) {
                        return (new Ray(ray2, rots(from, vector), on, facing, vector)).setLegit(true);
                     }
                  }
               }
            }

            return dumbRay(on, facing, rots(on, facing, from, access, state));
         }
      }
   }

   public static Ray dumbRay(BlockPos on, EnumFacing offset, float[] rotations) {
      return newRay(new RayTraceResult(Type.MISS, new Vec3d((double)0.5F, (double)1.0F, (double)0.5F), EnumFacing.UP, BlockPos.ORIGIN), on, offset, rotations);
   }

   public static Ray newRay(RayTraceResult result, BlockPos on, EnumFacing offset, float[] rotations) {
      return new Ray(result, rotations, on, offset, (Vec3d)null);
   }

   static float[] rots(Entity from, Vec3d vec3d) {
      return RotationUtil.getRotationsFrom(vec3d.x, vec3d.y, vec3d.z, from);
   }

   private static float[] rots(BlockPos pos, EnumFacing facing, Entity from, IBlockAccess world, IBlockState state) {
      return RotationUtil.getRotations(pos, facing, from, world, state);
   }

   private static EnumFacing[] getOptimalFacings(Entity player, BlockPos pos) {
      if ((double)pos.getY() > player.posY + (double)2.0F) {
         return T;
      } else {
         return (double)pos.getY() < player.posY ? B : S;
      }
   }

   public static boolean raytracePlaceCheck(Entity entity, BlockPos pos) {
      return getFacing(entity, pos, false) != null;
   }

   public static boolean canBeSeen(double x, double y, double z, Entity by) {
      return canBeSeen(new Vec3d(x, y, z), by.posX, by.posY, by.posZ, by.getEyeHeight());
   }

   public static boolean canBeSeen(Vec3d toSee, Entity by) {
      return canBeSeen(toSee, by.posX, by.posY, by.posZ, by.getEyeHeight());
   }

   public static boolean canBeSeen(Vec3d toSee, double x, double y, double z, float eyeHeight) {
      Vec3d start = new Vec3d(x, y + (double)eyeHeight, z);
      return mc.world.rayTraceBlocks(start, toSee, false, true, false) == null;
   }

   public static boolean canBeSeen(Entity toSee, EntityLivingBase by) {
      return by.canEntityBeSeen(toSee);
   }

   public static EnumFacing getFacing(Entity entity, BlockPos pos, boolean verticals) {
      for(EnumFacing facing : EnumFacing.values()) {
         RayTraceResult result = mc.world.rayTraceBlocks(PlayerUtil.getEyePos(entity), new Vec3d((double)pos.getX() + (double)0.5F + (double)facing.getDirectionVec().getX() * (double)1.0F / (double)2.0F, (double)pos.getY() + (double)0.5F + (double)facing.getDirectionVec().getY() * (double)1.0F / (double)2.0F, (double)pos.getZ() + (double)0.5F + (double)facing.getDirectionVec().getZ() * (double)1.0F / (double)2.0F), false, true, false);
         if (result != null && result.typeOfHit == Type.BLOCK && result.getBlockPos().equals(pos)) {
            return facing;
         }
      }

      if (!verticals) {
         return null;
      } else if ((double)pos.getY() > mc.player.posY + (double)mc.player.getEyeHeight()) {
         return EnumFacing.DOWN;
      } else {
         return EnumFacing.UP;
      }
   }

   public static float[] hitVecToPlaceVec(BlockPos pos, Vec3d hitVec) {
      float x = (float)(hitVec.x - (double)pos.getX());
      float y = (float)(hitVec.y - (double)pos.getY());
      float z = (float)(hitVec.z - (double)pos.getZ());
      return new float[]{x, y, z};
   }

   static {
      T = new EnumFacing[]{EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN};
      B = new EnumFacing[]{EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP};
      S = new EnumFacing[]{EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN};
   }
}
