package fat.vonware.util.HoleFill.other;

import fat.vonware.util.Util;
import java.util.function.BiPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class RayTracer implements Util {
   public static RayTraceResult trace(World world, IBlockAccess access, Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
      return trace(world, access, start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, (BiPredicate)null);
   }

   public static RayTraceResult trace(World world, IBlockAccess access, Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, BiPredicate<Block, BlockPos> blockChecker) {
      TriPredicate<Block, BlockPos, EnumFacing> triPredicate = null;
      if (blockChecker != null) {
         triPredicate = (b, p, ef) -> blockChecker.test(b, p);
      }

      return traceTri(world, access, start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, triPredicate);
   }

   public static RayTraceResult traceTri(World world, IBlockAccess access, Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, TriPredicate<Block, BlockPos, EnumFacing> blockChecker) {
      return traceTri(world, access, start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, blockChecker, (TriPredicate)null);
   }

   public static RayTraceResult traceTri(World world, IBlockAccess access, Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, TriPredicate<Block, BlockPos, EnumFacing> blockChecker, TriPredicate<Block, BlockPos, EnumFacing> collideCheck) {
      return traceTri(world, access, start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, blockChecker, collideCheck, CollisionFunction.DEFAULT);
   }

   public static RayTraceResult traceTri(World world, IBlockAccess access, Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, TriPredicate<Block, BlockPos, EnumFacing> blockChecker, TriPredicate<Block, BlockPos, EnumFacing> collideCheck, CollisionFunction crt) {
      if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
         if (!Double.isNaN(end.x) && !Double.isNaN(end.y) && !Double.isNaN(end.z)) {
            int feX = MathHelper.floor(end.x);
            int feY = MathHelper.floor(end.y);
            int feZ = MathHelper.floor(end.z);
            int fsX = MathHelper.floor(start.x);
            int fsY = MathHelper.floor(start.y);
            int fsZ = MathHelper.floor(start.z);
            BlockPos pos = new BlockPos(fsX, fsY, fsZ);
            IBlockState state = access.getBlockState(pos);
            Block block = state.getBlock();
            if ((!ignoreBlockWithoutBoundingBox || state.getCollisionBoundingBox(access, pos) != Block.NULL_AABB) && (block.canCollideCheck(state, stopOnLiquid) || collideCheck != null && collideCheck.test(block, pos, null)) && (blockChecker == null || blockChecker.test(block, pos, null))) {
               RayTraceResult raytraceresult = crt.collisionRayTrace(state, world, pos, start, end);
               if (raytraceresult != null) {
                  return raytraceresult;
               }
            }

            RayTraceResult result = null;
            int steps = 200;

            while(steps-- >= 0) {
               if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)) {
                  return null;
               }

               if (fsX == feX && fsY == feY && fsZ == feZ) {
                  return returnLastUncollidableBlock ? result : null;
               }

               boolean xEq = true;
               boolean yEq = true;
               boolean zEq = true;
               double x = (double)999.0F;
               double y = (double)999.0F;
               double z = (double)999.0F;
               if (feX > fsX) {
                  x = (double)fsX + (double)1.0F;
               } else if (feX < fsX) {
                  x = (double)fsX + (double)0.0F;
               } else {
                  xEq = false;
               }

               if (feY > fsY) {
                  y = (double)fsY + (double)1.0F;
               } else if (feY < fsY) {
                  y = (double)fsY + (double)0.0F;
               } else {
                  yEq = false;
               }

               if (feZ > fsZ) {
                  z = (double)fsZ + (double)1.0F;
               } else if (feZ < fsZ) {
                  z = (double)fsZ + (double)0.0F;
               } else {
                  zEq = false;
               }

               double xOff = (double)999.0F;
               double yOff = (double)999.0F;
               double zOff = (double)999.0F;
               double diffX = end.x - start.x;
               double diffY = end.y - start.y;
               double diffZ = end.z - start.z;
               if (xEq) {
                  xOff = (x - start.x) / diffX;
               }

               if (yEq) {
                  yOff = (y - start.y) / diffY;
               }

               if (zEq) {
                  zOff = (z - start.z) / diffZ;
               }

               if (xOff == (double)-0.0F) {
                  xOff = -1.0E-4;
               }

               if (yOff == (double)-0.0F) {
                  yOff = -1.0E-4;
               }

               if (zOff == (double)-0.0F) {
                  zOff = -1.0E-4;
               }

               EnumFacing enumfacing;
               if (xOff < yOff && xOff < zOff) {
                  enumfacing = feX > fsX ? EnumFacing.WEST : EnumFacing.EAST;
                  start = new Vec3d(x, start.y + diffY * xOff, start.z + diffZ * xOff);
               } else if (yOff < zOff) {
                  enumfacing = feY > fsY ? EnumFacing.DOWN : EnumFacing.UP;
                  start = new Vec3d(start.x + diffX * yOff, y, start.z + diffZ * yOff);
               } else {
                  enumfacing = feZ > fsZ ? EnumFacing.NORTH : EnumFacing.SOUTH;
                  start = new Vec3d(start.x + diffX * zOff, start.y + diffY * zOff, z);
               }

               fsX = MathHelper.floor(start.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
               fsY = MathHelper.floor(start.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
               fsZ = MathHelper.floor(start.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
               pos = new BlockPos(fsX, fsY, fsZ);
               IBlockState state2 = access.getBlockState(pos);
               Block block2 = state2.getBlock();
               if (!ignoreBlockWithoutBoundingBox || state2.getMaterial() == Material.PORTAL || state2.getCollisionBoundingBox(access, pos) != Block.NULL_AABB) {
                  if ((block2.canCollideCheck(state2, stopOnLiquid) || collideCheck != null && collideCheck.test(block2, pos, enumfacing)) && (blockChecker == null || blockChecker.test(block2, pos, enumfacing))) {
                     RayTraceResult raytraceresult2 = crt.collisionRayTrace(state2, world, pos, start, end);
                     if (raytraceresult2 != null) {
                        return raytraceresult2;
                     }
                  } else {
                     result = new RayTraceResult(Type.MISS, start, enumfacing, pos);
                  }
               }
            }

            return returnLastUncollidableBlock ? result : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
