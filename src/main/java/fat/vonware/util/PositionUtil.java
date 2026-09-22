package fat.vonware.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PositionUtil implements Util {
   public static BlockPos getPosition() {
      return getPosition(mc.player);
   }

   public static BlockPos getPosition(Entity entity) {
      return getPosition(entity, (double)0.0F);
   }

   public static BlockPos getPosition(Entity entity, double yOffset) {
      double y = entity.posY + yOffset;
      if (entity.posY - Math.floor(entity.posY) > (double)0.5F) {
         y = Math.ceil(entity.posY);
      }

      return new BlockPos(entity.posX, y, entity.posZ);
   }

   public static Set<BlockPos> getBlockedPositions(Entity entity) {
      return getBlockedPositions(entity.getEntityBoundingBox());
   }

   public static Set<BlockPos> getBlockedPositions(AxisAlignedBB bb) {
      return getBlockedPositions(bb, (double)0.5F);
   }

   public static Set<BlockPos> getBlockedPositions(AxisAlignedBB bb, double offset) {
      Set<BlockPos> positions = new HashSet();
      double y = bb.minY;
      if (bb.minY - Math.floor(bb.minY) > offset) {
         y = Math.ceil(bb.minY);
      }

      positions.add(new BlockPos(bb.maxX, y, bb.maxZ));
      positions.add(new BlockPos(bb.minX, y, bb.minZ));
      positions.add(new BlockPos(bb.maxX, y, bb.minZ));
      positions.add(new BlockPos(bb.minX, y, bb.maxZ));
      return positions;
   }

   public static Vec3d getEyesPos() {
      return getEyesPos(mc.player);
   }

   public static boolean inLiquid() {
      return inLiquid(MathHelper.floor(requirePositionEntity().getEntityBoundingBox().minY + 0.01));
   }

   public static Entity requirePositionEntity() {
      return (Entity)Objects.requireNonNull(getPositionEntity());
   }

   public static boolean inLiquid(boolean feet) {
      return inLiquid(MathHelper.floor(requirePositionEntity().getEntityBoundingBox().minY - (feet ? 0.03 : 0.2)));
   }

   private static boolean inLiquid(int y) {
      return findState(BlockLiquid.class, y) != null;
   }

   private static IBlockState findState(Class<? extends Block> block, int y) {
      Entity entity = requirePositionEntity();
      int startX = MathHelper.floor(entity.getEntityBoundingBox().minX);
      int startZ = MathHelper.floor(entity.getEntityBoundingBox().minZ);
      int endX = MathHelper.ceil(entity.getEntityBoundingBox().maxX);
      int endZ = MathHelper.ceil(entity.getEntityBoundingBox().maxZ);

      for(int x = startX; x < endX; ++x) {
         for(int z = startZ; z < endZ; ++z) {
            IBlockState s = mc.world.getBlockState(new BlockPos(x, y, z));
            if (block.isInstance(s.getBlock())) {
               return s;
            }
         }
      }

      return null;
   }

   public static Entity getPositionEntity() {
      EntityPlayerSP player = mc.player;
      Entity ridingEntity;
      return (Entity)(player == null ? null : ((ridingEntity = player.getRidingEntity()) != null && !(ridingEntity instanceof EntityBoat) ? ridingEntity : player));
   }

   public static Vec3d getEyesPos(Entity entity) {
      return new Vec3d(entity.posX, getEyeHeight(entity), entity.posZ);
   }

   public static double getEyeHeight(Entity entity) {
      return entity.posY + (double)entity.getEyeHeight();
   }
}
