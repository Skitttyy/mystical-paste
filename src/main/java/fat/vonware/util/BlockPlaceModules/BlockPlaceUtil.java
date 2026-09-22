package fat.vonware.util.BlockPlaceModules;

import fat.vonware.features.modules.combat.CrystalBase;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;

public class BlockPlaceUtil implements Util {
   public static final List airBlocks;
   public static final List blackList;
   public static final List shulkerList;

   public static boolean canPlace(BlockPos pos) {
      return mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= (double)6.0F && canBlockFacing(pos) && BlockUtil.canReplace(pos) && strictPlaceCheck(pos) && !checkEntity(pos);
   }

   public static boolean checkEntity(BlockPos pos) {
      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
         if (!entity.isDead && !(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb) && !(entity instanceof EntityExpBottle) && !(entity instanceof EntityArrow) && !(entity instanceof EntityArmorStand)) {
            return true;
         }
      }

      return false;
   }

   public static boolean strictPlaceCheck(BlockPos pos) {
      if (!(Boolean)CrystalBase.getInstance().strictPlace.getValue()) {
         return true;
      } else {
         for(EnumFacing side : getPlacableFacings(pos, true, (Boolean)CrystalBase.getInstance().checkRaytrace.getValue())) {
            if (canClick(pos.offset(side))) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean canClick(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock().canCollideCheck(mc.world.getBlockState(pos), false);
   }

   public static List<EnumFacing> getPlacableFacings(BlockPos pos, boolean strictDirection, boolean rayTrace) {
      ArrayList<EnumFacing> validFacings = new ArrayList();

      for(EnumFacing side : EnumFacing.values()) {
         if (!getRaytrace(pos, side)) {
            getPlaceFacing(pos, strictDirection, validFacings, side);
         }
      }

      for(EnumFacing side : EnumFacing.values()) {
         if (!rayTrace || !getRaytrace(pos, side)) {
            getPlaceFacing(pos, strictDirection, validFacings, side);
         }
      }

      return validFacings;
   }

   private static boolean getRaytrace(BlockPos pos, EnumFacing side) {
      Vec3d testVec = (new Vec3d(pos)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(side.getDirectionVec())).scale((double)0.5F));
      RayTraceResult result = mc.world.rayTraceBlocks(mc.player.getPositionEyes(1.0F), testVec);
      return result != null && result.typeOfHit != Type.MISS;
   }

   public static ArrayList<EnumFacing> checkAxis(double diff, EnumFacing negativeSide, EnumFacing positiveSide, boolean bothIfInRange) {
      ArrayList<EnumFacing> valid = new ArrayList();
      if (diff < (double)-0.5F) {
         valid.add(negativeSide);
      }

      if (diff > (double)0.5F) {
         valid.add(positiveSide);
      }

      if (bothIfInRange) {
         if (!valid.contains(negativeSide)) {
            valid.add(negativeSide);
         }

         if (!valid.contains(positiveSide)) {
            valid.add(positiveSide);
         }
      }

      return valid;
   }

   private static void getPlaceFacing(BlockPos pos, boolean strictDirection, ArrayList<EnumFacing> validFacings, EnumFacing side) {
      BlockPos neighbour = pos.offset(side);
      if (strictDirection) {
         Vec3d eyePos = mc.player.getPositionEyes(1.0F);
         Vec3d blockCenter = new Vec3d((double)neighbour.getX() + (double)0.5F, (double)neighbour.getY() + (double)0.5F, (double)neighbour.getZ() + (double)0.5F);
         IBlockState blockState2 = mc.world.getBlockState(neighbour);
         boolean isFullBox = blockState2.getBlock() == Blocks.AIR || blockState2.isFullBlock();
         ArrayList<EnumFacing> validAxis = new ArrayList();
         validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, EnumFacing.WEST, EnumFacing.EAST, !isFullBox));
         validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, EnumFacing.DOWN, EnumFacing.UP, true));
         validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, EnumFacing.NORTH, EnumFacing.SOUTH, !isFullBox));
         if (!validAxis.contains(side.getOpposite())) {
            return;
         }
      }

      IBlockState blockState3;
      if ((blockState3 = mc.world.getBlockState(neighbour)).getBlock().canCollideCheck(blockState3, false) && !blockState3.getMaterial().isReplaceable()) {
         validFacings.add(side);
      }
   }

   public static boolean canBlockFacing(BlockPos pos) {
      boolean airCheck = false;

      for(EnumFacing side : EnumFacing.values()) {
         if (canClick(pos.offset(side))) {
            airCheck = true;
         }
      }

      return airCheck;
   }

   public static String getWelcomer() {
      return null;
   }

   static {
      shulkerList = Arrays.asList(Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.SILVER_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX);
      blackList = Arrays.asList(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.ANVIL, Blocks.WOODEN_BUTTON, Blocks.STONE_BUTTON, Blocks.UNPOWERED_COMPARATOR, Blocks.UNPOWERED_REPEATER, Blocks.POWERED_REPEATER, Blocks.POWERED_COMPARATOR, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.BREWING_STAND, Blocks.DISPENSER, Blocks.DROPPER, Blocks.LEVER, Blocks.NOTEBLOCK, Blocks.JUKEBOX, Blocks.BEACON, Blocks.BED, Blocks.FURNACE, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CAKE, Blocks.ENCHANTING_TABLE, Blocks.DRAGON_EGG, Blocks.HOPPER, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.CRAFTING_TABLE, Blocks.WALL_SIGN, Blocks.STANDING_SIGN, shulkerList);
      airBlocks = Arrays.asList(Blocks.AIR, Blocks.LAVA, Blocks.FLOWING_LAVA, Blocks.WATER, Blocks.FLOWING_WATER, Blocks.FIRE, Blocks.VINE, Blocks.SNOW_LAYER, Blocks.TALLGRASS);
   }
}
