package fat.vonware.util.AutoTrap;

import fat.vonware.util.PositionUtil;
import fat.vonware.util.Util;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

public class Raytracer implements Util {
   public static boolean strictDirectionCheck(BlockPos pos, EnumFacing direction, IBlockAccess provider) {
      BlockPos eyePos = containing(mc.player.posX, PositionUtil.getEyeHeight(mc.player), mc.player.posZ);
      if (checkInside(pos, eyePos)) {
         return true;
      } else {
         IBlockState state = provider.getBlockState(pos);
         boolean fullBounds = state.getCollisionBoundingBox(provider, pos) == Block.FULL_BLOCK_AABB;
         List<EnumFacing> interactableDirections = getInteractableDirections(pos, eyePos, fullBounds);
         return interactableDirections.contains(direction) && !isDirectionBlocked(pos, interactableDirections, direction, fullBounds);
      }
   }

   private static boolean checkInside(BlockPos pos, BlockPos eyePos) {
      return eyePos.getX() == pos.getX() && eyePos.getY() == pos.getY() && eyePos.getZ() == pos.getZ();
   }

   private static List<EnumFacing> getInteractableDirections(BlockPos pos, BlockPos eyePos, boolean fullBounds) {
      int locX = eyePos.getX();
      int locY = eyePos.getY();
      int locZ = eyePos.getZ();
      int blockX = pos.getX();
      int blockY = pos.getY();
      int blockZ = pos.getZ();
      return getDirections(locX - blockX, locY - blockY, locZ - blockZ, fullBounds);
   }

   private static List<EnumFacing> getDirections(int xDiff, int yDiff, int zDiff, boolean fullBounds) {
      List<EnumFacing> faces = new ArrayList(6);
      if (!fullBounds) {
         if (xDiff == 0) {
            faces.add(EnumFacing.EAST);
            faces.add(EnumFacing.WEST);
         }

         if (zDiff == 0) {
            faces.add(EnumFacing.SOUTH);
            faces.add(EnumFacing.NORTH);
         }
      }

      if (yDiff == 0) {
         faces.add(EnumFacing.UP);
         faces.add(EnumFacing.DOWN);
      } else {
         faces.add(yDiff > 0 ? EnumFacing.UP : EnumFacing.DOWN);
      }

      if (xDiff != 0) {
         faces.add(xDiff > 0 ? EnumFacing.EAST : EnumFacing.WEST);
      }

      if (zDiff != 0) {
         faces.add(zDiff > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH);
      }

      return faces;
   }

   private static boolean isDirectionBlocked(BlockPos pos, List<EnumFacing> interactable, EnumFacing direction, boolean hasFullBounds) {
      if (hasFullBounds) {
         BlockPos relative = pos.offset(direction);
         IBlockState state = mc.world.getBlockState(relative);
         return state.getCollisionBoundingBox(mc.world, pos) == Block.FULL_BLOCK_AABB && !state.getBlock().canCollideCheck(state, false);
      } else {
         for(EnumFacing dir : interactable) {
            BlockPos relative2 = pos.offset(dir);
            IBlockState state2 = mc.world.getBlockState(relative2);
            boolean fullBounds = state2.getCollisionBoundingBox(mc.world, pos) == Block.FULL_BLOCK_AABB;
            if (!fullBounds || !state2.getBlock().canCollideCheck(state2, false)) {
               return false;
            }
         }

         return true;
      }
   }

   public static BlockPos containing(double x, double y, double z) {
      return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
   }
}
